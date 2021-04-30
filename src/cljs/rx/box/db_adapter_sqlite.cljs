(ns rx.box.db-adapter-sqlite
  (:require [rx.kitchen-sink :as ks
             :refer-macros [go-try <?]]
            [rx.anom :as anom]
            [rx.box.db-adapter :as da]
            [rx.box.common :as com]
            [honeysql.core :as hc]
            [honeysql.types :as ht]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go go-loop]]))

(defn dbg-prn-obj [message o]
  (ks/spy message o))

(defn honey-col [k]
  (ht/raw
    (str "\""
         (str (namespace k) "/" (name k))
         "\"")))

(defn ensure-sql-data-table-name [schema]
  (ks/spec-check-throw
    :box/local-data-table-name
    (com/schema->data-table-name schema)
    "Invalid sql data table name"))

(defn ensure-sql-refs-table-name [schema]
  (ks/spec-check-throw
    :box/local-refs-table-name
    (com/schema->refs-table-name schema)
    "Invalid sql refs table name"))


;; SQL gen

(defn create-data-table-stmt [schema]
  (ensure-sql-data-table-name schema)
  [(str "create table if not exists "
        (com/schema->data-table-name schema)
        "(ident_val TEXT NOT NULL PRIMARY KEY, ident_key TEXT, transit TEXT)")])

(defn create-refs-table-stmt [schema]
  (ensure-sql-refs-table-name schema)
  [(str "create table if not exists "
        (com/schema->refs-table-name schema)
        "(from_ident_key TEXT NOT NULL, from_ident_val TEXT NOT NULL, from_ref_key TEXT NOT NULL, to_ident_key TEXT NOT NULL, to_ident_val TEXT NOT NULL, sort_val TEXT, UNIQUE(from_ident_key, from_ident_val, from_ref_key, to_ident_key, to_ident_val))")])

(defn create-indexes-stmts [schema]
  (ensure-sql-data-table-name schema)
  (ensure-sql-refs-table-name schema)
  [[(str "create index idx_ident_key ON "
         (com/schema->data-table-name schema)
         " (\"ident_key\")")]
   [(str "create index idx_appt_start_ts ON "
         (com/schema->data-table-name schema)
         " (\"appt/start-ts\")")]])

(defn destroy-tables-stmts [schema]
  (ensure-sql-data-table-name schema)
  (ensure-sql-refs-table-name schema)
  [[(str "drop table " (com/schema->data-table-name schema))]
   [(str "drop table " (com/schema->refs-table-name schema))]])

(defn key->column-name [k]
  (when k
    (let [ns (namespace k)
          nm (name k)]
      (str ns "/" nm))))

(defn column-name->key [s]
  (when s
    (let [[ns-str name-str] (str/split s "/")]
      (keyword ns-str name-str))))

(defn find-missing-index-keys [index-keys sql]
  (if sql
    (->> index-keys
         (remove (fn [k]
                   (let [cn (key->column-name k)]
                     (str/includes? sql (str "\"" cn "\"")))))
         vec)
    index-keys))

(defn value-type->sqlite-data-type [vt]
  (try
    (condp = vt
      :box.type/long "INTEGER"
      :box.type/string "TEXT"
      :box.type/boolean "INTEGER"
      :box.type/double "REAL")
    (catch js/Error e
      (throw (ex-info
               (str "No matching box type for value: " vt)
               {:var ::value-type->sqlite-data-type
                :vt vt})))))

(defn attr->stmt [schema
                  [key {:keys [:box.attr/value-type]}]]
  (when-not value-type
    (ks/throw-str "Invalid schema, indexed attr " key " missing :box/value-type"))
  [(str "alter table " (com/schema->data-table-name schema)
        " add column "
        "\"" (key->column-name key) "\""
        " "
        (value-type->sqlite-data-type value-type))])

(defn schema->alter-stmts-for-keys [schema ks]
  (let [ks-set (set ks)]
    (->> schema
         :box/attrs
         (filter #(get ks-set (first %)))
         (mapv #(attr->stmt schema %)))))


(defn schema->index-stmts-for-keys [schema ks]
  (->> ks
       set
       (mapcat (fn [k]
                 [[(str "DROP INDEX IF EXISTS"
                         "\"" (key->column-name k) "_index" "\"")]
                  [(str
                     "CREATE INDEX "
                     "\"" (key->column-name k) "_index" "\" "
                     "ON "
                     (com/schema->data-table-name schema) " "
                     "("
                     "\"" (key->column-name k) "\""
                     " DESC) "
                     ;;"WHERE "
                     ;;"\"" (key->column-name k) "\""
                     ;;" NOT NULL"

                     )]]))))

;; SQL Calls

(defn <ensure-tables [{:keys [<exec] :as SQL} db schema]
  (go
    (let [ress (->> [(create-data-table-stmt schema)
                     (create-refs-table-stmt schema)]
                    (map #(<exec db %))
                    async/merge
                    (async/reduce conj [])
                    <!)

          anoms (filter anom/anom? ress)]

      (if (empty? anoms)
        {:create-success? true}
        (first anoms)))))

(defn <ensure-index-columns [{:keys [<exec]} db schema]
  (go
    (try
      (let [table-name (com/schema->data-table-name schema)
            index-keys (com/schema->index-keys-set schema)

            rows (<! (<exec
                       db
                       ["SELECT sql FROM sqlite_master WHERE tbl_name = :0 AND type = 'table'"
                        table-name]))

            _ (anom/throw-if-anom rows)

            create-table-sql (-> rows
                                 first
                                 (get "sql"))

            missing-index-keys (find-missing-index-keys
                                 index-keys
                                 create-table-sql)

            alter-stmts (schema->alter-stmts-for-keys
                          schema
                          missing-index-keys)

            alter-rowss (->> alter-stmts
                             (map #(<exec db %))
                             async/merge
                             (async/reduce conj [])
                             <!)

            alter-rows (first alter-rowss)]

        (anom/throw-if-anom alter-rows)

        alter-rows)
      (catch js/Error e (anom/from-err e)))))

(defn <ensure-sqlite-indexes [{:keys [<exec]} db schema]
  (go
    (let [table-name (com/schema->data-table-name schema)
          index-keys (com/schema->index-keys-set schema)

          rows (<! (<exec
                     db
                     ["SELECT sql FROM sqlite_master WHERE tbl_name = :0 AND type = 'index'"
                      table-name]))

          indexes-sql rows

          missing-index-keys
          index-keys
          #_(find-missing-index-keys
              index-keys
              create-table-sql)

          index-stmts (schema->index-stmts-for-keys
                        schema
                        missing-index-keys)

          index-res (->> index-stmts
                         (map #(<exec db %))
                         async/merge
                         (async/reduce conj [])
                         <!)]

      #_(ks/spy
          "here"
          (<? (<exec
                db
                ["SELECT sql FROM sqlite_master WHERE tbl_name = :0 AND type = 'index'"
                 table-name])))

      index-res)))

(defn <destroy-refs-table [{:keys [<exec]} db schema]
  (->> [[(str "drop table " (com/schema->refs-table-name schema))]]
       (map #(<exec db %))
       async/merge
       (async/reduce conj [])))

(defn <destroy-tables [{:keys [<exec]} db schema]
  (->> [[(str "drop table " (com/schema->data-table-name schema))]
        [(str "drop table " (com/schema->refs-table-name schema))]]
       (map #(<exec db %))
       async/merge
       (async/reduce conj [])))

(defn <recreate-sql-tables [{:keys [:box.pl/sqlite-db
                                    :box.pl/SQL
                                    :box/schema]}]
  (go
    (<! (<destroy-tables SQL sqlite-db schema))
    (<! (<ensure-tables SQL sqlite-db schema))
    (<! (<ensure-index-columns SQL sqlite-db schema))
    #_(<! (<ensure-sqlite-indexes SQL sqlite-db schema))))

(defn <recreate-refs-table [{:keys [:box.pl/sqlite-db
                                    :box.pl/SQL
                                    :box/schema]}]
  (go
    (<! (<destroy-refs-table SQL sqlite-db schema))
    (<! (<ensure-tables SQL sqlite-db schema))
    (<! (<ensure-index-columns SQL sqlite-db schema))
    #_(<! (<ensure-sqlite-indexes SQL sqlite-db schema))))

(defn <open-sql-db [{:keys [<open-db]} schema opts]
  (let [db-name (com/local-db-name schema)]
    (go-try
      (ks/spec-check-throw
        :box/persist-local-schema
        schema
        "Invalid persist local schema")
      (<! (<open-db
            {:name db-name
             :size (* 1024 1024 200)})))))

(defn <ensure-db-structure [SQL db schema opts]
  (go
    (try
      (let [ensure-tables-res
            (<! (<ensure-tables SQL db schema))

            _ (anom/throw-if-anom ensure-tables-res)

            ensure-index-columns-res
            (<! (<ensure-index-columns SQL db schema))

            _ (anom/throw-if-anom ensure-index-columns-res)]
        [ensure-tables-res
         ensure-index-columns-res])
      (catch js/Error e
        (anom/from-err e)))))

(defn delete-stmts [schema idents]
  (->> idents
       (map (fn [[ident-key ident-val]]
              [(str "DELETE FROM "
                    (com/schema->data-table-name schema)
                    " WHERE ident_key=:ident_key AND ident_val=:ident_val")
               (key->column-name ident-key)
               ident-val]))))

(defn <delete-idents [SQL db schema idents & [{:keys [debug-log?]}]]
  (go
    (if (and idents (not (empty? idents)))
      (let [<batch-exec (-> SQL :<batch-exec)
            stmts (delete-stmts schema idents)
            
            _ (when debug-log?
                (dbg-prn-obj "<delete-idents stmts" stmts))
            
            rows (<! (<batch-exec db stmts))]

        rows)
      [])))

(defn update-if-exists [m k f & args]
  (if (contains? m k)
    (assoc
      m
      k
      (apply f (get m k) args))
    m))

(defn fetch-by-ident-stmt [schema
                           idents]

  (when (empty? idents)
    (throw (ex-info
             "List of idents to fetch cannot be empty"
             {:rx.res/var ::fetch-by-ident-stmt})))
  
  (into
    [(str "select * from "
          (com/schema->data-table-name schema)
          " where "
          (->> idents
               (map-indexed
                 (fn [i _]
                   (str
                     "("
                     "ident_key=:ik" i
                     " AND "
                     "ident_val=:iv" i
                     ")")))
               (interpose " OR ")
               (apply str)))]
    (->> idents
         (map (fn [[ident-key ident-val]]
                [(key->column-name ident-key)
                 ident-val]))
         (reduce concat))))

(defn <fetch-ident->entity-step [SQL db schema idents
                                 & [{:keys [debug-log?] :as opts}]]
  (go
    (try
      (let [<exec (-> SQL :<exec)
            idents (distinct idents)
            sql-vec (fetch-by-ident-stmt schema idents)

            _ (when debug-log?
                (dbg-prn-obj
                  "<fetch-ident->entity-step sql-vec"
                  sql-vec))
            
            rows (<! (<exec
                       db
                       sql-vec))

            _ (when debug-log?
                (dbg-prn-obj
                  "<fetch-ident->entity-step rows" rows))]

        (->> rows
             (map (fn [{:strs [ident_key ident_val transit]}]
                    [[(column-name->key ident_key)
                      ident_val]
                     (ks/from-transit transit)]))
             (into {})))
      (catch js/Error e
        (anom/from-err e)))))

(defn <fetch-ident->entity [SQL db schema idents & [opts]]
  (go-loop [batches (partition-all 400 idents)
            out {}]
    (if (empty? batches)
      out
      (let [ident->entity (<! (<fetch-ident->entity-step
                                SQL db schema idents opts))]
        (if (anom/anom? ident->entity)
          ident->entity
          (recur
            (rest batches)
            (merge out ident->entity)))))))

(s/def ::conj-txf
  (s/cat
    :op #{:box/conj}
    :ident :box/ident
    :prop-key keyword?
    :prop-val (s/or :map map? :ident :box/ident)))

(defn conj-ref-txf->stmt [schema txf]
  (ks/spec-check-throw ::conj-txf txf)
  (let [table-name (com/schema->refs-table-name schema)]
    (when table-name
      (let [[_ ; :db/conj
             [from-ident-key
              from-ident-val]
             from-ref-key
             [to-ident-key
              to-ident-val]] txf]
        [(str "insert or replace into "
              table-name
              "("
              (->> (concat
                     ["from_ident_key"
                      "from_ident_val"
                      "from_ref_key"
                      "to_ident_key"
                      "to_ident_val"])
                   (map #(str "\"" % "\""))
                   (interpose ",")
                   (apply str))
              ") VALUES("
              (->> (concat
                     ["from_ident_key"
                      "from_ident_val"
                      "from_ref_key"
                      "to_ident_key"
                      "to_ident_val"])
                   (map-indexed (fn [i _]
                                  (str ":" i)))
                   (interpose ",")
                   (apply str))
              ")")
         (key->column-name from-ident-key)
         from-ident-val
         (key->column-name from-ref-key)
         (key->column-name to-ident-key)
         to-ident-val]))))

(s/def ::disj-txf
  (s/cat
    :op #{:box/disj}
    :ident :box/ident
    :prop-key keyword?
    :prop-val (s/or :map map? :ident :box/ident)))

(defn disj-ref-txf->stmt [schema txf]
  (let [table-name (com/schema->refs-table-name schema)
        [_
         [from-ident-key
          from-ident-val]
         from-ref-key
         [to-ident-key
          to-ident-val]] txf]
    [(str "DELETE FROM "
          table-name
          " WHERE from_ident_key = :0 AND from_ident_val = :1 AND from_ref_key = :2 AND to_ident_key = :3 AND to_ident_val = :4")
     (key->column-name from-ident-key)
     from-ident-val
     (key->column-name from-ref-key)
     (key->column-name to-ident-key)
     to-ident-val]))

(defn ref-txf->stmt [schema txf]
  (condp = (first txf)
    :box/conj (conj-ref-txf->stmt schema txf)
    :box/disj (disj-ref-txf->stmt schema txf)
    (ks/throw-str "Invalid tx op: " txf)))

(defn <update-refs [SQL db schema
                    ident->ref-txfs
                    & [{:keys [debug-log?]
                        :as opts}]]
  (go
    (try
      (when debug-log?
        (dbg-prn-obj
          "sqlite <update-refs"
          ident->ref-txfs))
      (let [<batch-exec (:<batch-exec SQL)
            stmts (map
                    #(ref-txf->stmt schema %)
                    (->> ident->ref-txfs
                         (map second)
                         (reduce concat)))
            _ (when debug-log?
                (ks/spy "<exec-ref-txfs stmts" stmts))

            rows (<! (<batch-exec db stmts))]
        rows)
      (catch js/Error e
        (anom/from-err e)))))

(defn schema+key->attr-spec [schema ident-key]
  (get-in schema [:box/attrs ident-key]))

(defn val->sqlite-val [schema [k v]]
  (let [{:keys [:box.attr/value-type]}
        (schema+key->attr-spec schema k)]
    (condp = value-type
      :box.type/boolean (if v 1 0)
      (identity v))))

(defn obj->put-stmt [schema o]
  (let [ident-key (com/resolve-ident-key schema o)

        spec (schema+key->attr-spec schema ident-key)

        index-entries (->> o
                           (filter
                             #(get
                                (com/schema->index-keys-set schema)
                                (first %))))

        table-name (com/schema->data-table-name schema)

        qs (->> (repeat "?")
                (take (+ 2 (count index-entries)))
                (interpose ",")
                (apply str))]

    (vec
      (concat
        [(str "insert or replace into "
              table-name
              "("
              (->> (concat
                     ["ident_val" "ident_key" "transit"]
                     (->> index-entries
                          (map first)
                          (map key->column-name)))
                   (map #(str "\"" % "\""))
                   (interpose ",")
                   (apply str))
              ") VALUES("
              (->> (concat
                     ["ident_val" "ident_key" "transit"]
                     (->> index-entries
                          (map first)
                          (map key->column-name)))
                   (map-indexed (fn [i _]
                                  (str ":" i)))
                   (interpose ",")
                   (apply str))
              ")")]

        [(get o ident-key)
         (key->column-name ident-key)
         (ks/to-transit o)]
        (->> index-entries
             (map #(val->sqlite-val schema %)))))))

(defn <store-entities [SQL db schema os & [{:keys [debug-log?]}]]
  (when debug-log?
    (dbg-prn-obj "<put os" os))
  (go-try
    (if (and os (not (empty? os)))
      (let [<batch-exec (-> SQL :<batch-exec)
            stmts (->> os
                       (map #(obj->put-stmt schema %))
                       doall)
            res (<? (<batch-exec db stmts))]

        (when debug-log?
          (dbg-prn-obj "<put stmts" stmts)
          (dbg-prn-obj "<put batch-exec res" res))

        os)
      [])))

(defn fetch-ref-rows-stmt
  [schema [ident ref-key {:keys [:box.attr/limit
                                 :box.attr/sort-key
                                 :box.attr/sort-order]}]]
  (let [data-table-name (com/schema->data-table-name schema)
        refs-table-name (com/schema->refs-table-name schema)
        sort-key-column-name (key->column-name sort-key)]
    [(str "SELECT " "from_ident_key, from_ident_val, from_ref_key, to_ident_key, to_ident_val "
          "FROM "
          refs-table-name " "

          "LEFT JOIN " data-table-name " ON "
          "" data-table-name ".ident_key = " refs-table-name ".to_ident_key AND "
          "" data-table-name ".ident_val = " refs-table-name ".to_ident_val "

          
          "WHERE "
          "" refs-table-name ".from_ident_key=:from_ident_key AND "
          "" refs-table-name ".from_ident_val=:from_ident_val AND "
          "" refs-table-name ".from_ref_key=:from_ref_key "

          #_(when sort-key
              "ORDER BY ROWID ")
          
          (when sort-key
            (str
              "ORDER BY "
              data-table-name
              ".\""
              sort-key-column-name
              "\" "))
          
          (when sort-order
            (str
              (if (= :box.sort/descending
                     sort-order)
                "DESC"
                "ASC")
              " "))

          "LIMIT "
          (or limit 1000))
     (key->column-name (first ident))
     (second ident)
     (key->column-name ref-key)]))

(defn <resolve-ref-idents [SQL db schema ident+ref-key+opts-tuples
                           & [{:keys [debug-log?]
                               :as opts}]]
  (go
    (try
      (if (empty? ident+ref-key+opts-tuples)
        []
        (let [stmts (->> ident+ref-key+opts-tuples
                         (map #(fetch-ref-rows-stmt schema %)))

              _ (when debug-log?
                  (ks/spy "sqlite/<resolve-ref-idents stmts"
                    stmts))
              
              rows (<! ((:<batch-exec SQL) db stmts))

              _ (when debug-log?
                  (ks/spy "sqlite/<resolve-ref-idents batch exec rows"
                    rows))

              _ (anom/throw-if-anom rows)

              ident->prop->child-idents
              (->> rows
                   (map (fn [{:strs [from_ident_key
                                     from_ident_val
                                     from_ref_key
                                     to_ident_key
                                     to_ident_val]}]
                          (let [from-ident
                                [(keyword from_ident_key)
                                 from_ident_val]
                                from-ref-key
                                (keyword from_ref_key)
                                to-ident
                                [(keyword to_ident_key)
                                 to_ident_val]]
                            {:from-ident from-ident
                             :from-ref-key from-ref-key
                             :to-ident to-ident})))
                   (group-by :from-ident)
                   (map (fn [[ident ms]]
                          [ident
                           (->> ms
                                (group-by :from-ref-key)
                                (map (fn [[k ms]]
                                       [k (mapv :to-ident ms)]))
                                (into {}))]))
                   (into {}))]
          ident->prop->child-idents))
      (catch js/Error e
        (anom/from-err e)))))

(defn replace-qs [s]
  (let [!n (atom 0)]
    (str/replace
      s
      #"(\?)([^\"])"
      (fn [[_ m1 m2]]
        (str ":" (swap! !n inc) m2)))))

(defn query-stmt [schema
                  query
                  & [{:keys [debug-log?
                             limit
                             explain?]
                      :or {limit 1000}}]]
  (when debug-log?
    (ks/spy "SCHEMA" schema)
    (ks/spy "QUERY" query))
  (let [stmt
        (hc/format
          {:select [:*]
           :from [(keyword (com/schema->data-table-name schema))]
           :where query
           :limit limit}
          :quoting :ansi
          :allow-dashed-names? true)

        _ (when debug-log?
            (ks/spy "STMT" stmt))

        [query-string & rest] stmt]
    (into
      [(replace-qs query-string)]
      rest)))

(defn honey-query-map->sql-stmt [schema
                                 {:keys [honeysql]}
                                 {:keys [debug-log?]}]
  (let [stmt (hc/format
               (merge
                 {:select [:*]
                  :from [(keyword (com/schema->data-table-name schema))]}
                 honeysql)
               :quoting :ansi
               :allow-dashed-names? true)
        [query-string & rest] stmt]
    (into
      [(replace-qs query-string)]
      rest)))

(defn box-query-map->sql-stmt
  [schema
   {:keys [where sort limit]}
   opts]
  (honey-query-map->sql-stmt
    schema
    {:honeysql
     (merge
       {:where
        (into
          [:and]
          (->> where
               (filter sequential?)
               (map (fn [[op & _ :as clause]]
                      (condp = op
                        :ident-key [:= :ident_key (honey-col (second clause))]
                        :> [:> (honey-col (second clause)) (nth clause 2)]
                        :< [:< (honey-col (second clause)) (nth clause 2)]
                        :>= [:>= (honey-col (second clause)) (nth clause 2)]
                        :<= [:<= (honey-col (second clause)) (nth clause 2)]
                        := [:= (honey-col (second clause)) (nth clause 2)]
                        :like [:like (honey-col (second clause)) (nth clause 2)])))))}
       (when sort
         {:order-by (->> sort
                         (mapv (fn [[k dir]]
                                 [(honey-col k) dir])))})
       (when limit
         {:limit limit}))}
    opts))

(defn query-map->sql-stmt [schema query-map opts]
  (cond
    (:honeysql query-map) (honey-query-map->sql-stmt schema query-map opts)
    :else (box-query-map->sql-stmt schema query-map opts)))

(defn query->sql-stmt [schema query opts]
  (if (map? query)
    (query-map->sql-stmt schema query opts)
    (query-stmt schema query opts)))

(defn sql-query-result->result [res & [query opts]]
  (let [data (->> res
                  (map #(get % "transit"))
                  (mapv ks/from-transit))

        debug-log? (:debug-log? opts)]
    (when debug-log?
      (ks/spy "DATA COUNT" (count data)))
    (if (anom/anom? res)
      res
      data)))

(defn <fetch-by-query [SQL db schema query
                       & [{:keys [debug-log?]
                           :as opts}]]
  (go
    (try
      (let [<exec (-> SQL :<exec)
            stmt (query->sql-stmt schema query opts)

            _ (when debug-log?
                (ks/spy "SQL STMT" stmt))

            sql-result (<! (<exec
                             db
                             stmt))

            _ (when debug-log?
                (ks/spy "SQL RES" sql-result))

            _ (anom/throw-if-anom sql-result)
            
            ds (sql-query-result->result
                 sql-result
                 query
                 opts)]
        (when (:explain? opts)
          (println "--- EXPLAIN: " stmt)
          (ks/pp
            (<! (<exec
                  db
                  (into
                    [(str "EXPLAIN QUERY PLAN " (first stmt))]
                    (rest stmt))))))
        ds)
      (catch js/Error e
        (anom/from-err e)))))

(defn convert-stmt-to-count [stmt]
  (when (and stmt (string? (first stmt)))
    (vec
      (concat
        [(str/replace
           (first stmt)
           "SELECT * FROM"
           "SELECT COUNT(ident_key) FROM")]
        (rest stmt)))))

(defn <count-by-query [SQL db schema query
                       & [{:keys [debug-log?]
                           :as opts}]]
  (go
    (try
      (let [<exec (-> SQL :<exec)
            stmt (query->sql-stmt schema query opts)

            stmt (convert-stmt-to-count stmt)

            _ (when debug-log?
                (ks/spy "SQL STMT" stmt))

            sql-result (<! (<exec db stmt))

            _ (when debug-log?
                (ks/spy "SQL RES" sql-result))

            _ (anom/throw-if-anom sql-result)]
        
        (when (:explain? opts)
          (println "--- EXPLAIN: " stmt)
          (ks/pp
            (<! (<exec
                  db
                  (into
                    [(str "EXPLAIN QUERY PLAN " (first stmt))]
                    (rest stmt))))))

        (-> sql-result
            first
            first
            second))
      
      (catch js/Error e
        (anom/from-err e)))))

(deftype DBAdapter [SQL]
  da/IDBAdapter
  (-<open-db [DS schema opts]
    (<open-sql-db SQL schema opts))
  (-<destroy-db [DS schema opts]
    (go {:rx.res/data "ok"}))
  (-<ensure-db-structure [_ db schema opts]
    (<ensure-db-structure SQL db schema opts))
  (-<delete-idents [_ db schema idents opts]
    (<delete-idents SQL db schema idents opts))
  (-<fetch-entities [_ db schema idents opts]
    (<fetch-ident->entity SQL db schema idents opts))
  (-<store-entities [_ db schema entities opts]
    (<store-entities SQL db schema entities opts))
  (-<update-refs [_ db schema ident->ref-txfs opts]
    (<update-refs SQL db schema ident->ref-txfs opts))
  (-<resolve-ref-idents [_ db schema ident+ref-key+opts-tuples opts]
    (<resolve-ref-idents SQL db schema ident+ref-key+opts-tuples opts))
  (-<fetch-by-query [_ db schema query opts]
    (<fetch-by-query SQL db schema query opts))
  (-<count-by-query [_ db schema query opts]
    (<count-by-query SQL db schema query opts)))

(defn create-db-adapter [SQL]
  (DBAdapter. SQL))

(comment

  (ds/<open-db (DataStore. nil) nil)
  )
