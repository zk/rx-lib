(ns rx.box.query-local
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [rx.box.common :as com]
            [rx.box.persist-local :as pl]
            [rx.box.db-adapter :as da]
            [honeysql.core :as hc]
            [honeysql.helpers :as hh]
            [honeysql.types :as ht]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [datascript.parser :as dp]
            [datascript.query :as dq]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(defn <query [{:keys [:box.pl/db-adapter
                      :box.pl/db
                      :box/schema]
               :as conn}
              query & [{:keys [debug-log?] :as opts}]]
  (go
    (try
      (pl/ensure-conn conn)
      (let [entities (<! (da/<fetch-by-query
                           db-adapter
                           db
                           schema
                           query
                           opts))

            _ (anom/throw-if-anom entities)

            _ (when debug-log?
                (ks/spy "<query entities" entities))

            expanded (<! (pl/<expand
                           conn
                           entities
                           (:pull-keys opts)
                           opts))]
        (when debug-log?
          (ks/spy "<query expanded" expanded))
        expanded)
      (catch js/Error e
        (anom/from-err e)))))

(defn <count [{:keys [:box.pl/db-adapter
                      :box.pl/db
                      :box/schema]
               :as conn}
              query & [opts]]
  (da/<count-by-query
    db-adapter
    db
    schema
    query
    opts))

(defn <referenced-by [conn
                      ident
                      from-ref-key
                      & [{:keys [pull-keys
                                 honeysql
                                 debug-log?]
                          :as opts}]]
  (go
    #_(let [<exec (-> conn :box.pl/SQL :<exec)
            sql (let [stmt (hc/format
                             (merge
                               {:select [:*]
                                :from [(keyword (com/schema->refs-table-name (:box/schema conn)))]
                                :where
                                [:and
                                 [:= :from_ref_key from-ref-key]
                                 [:= :to_ident_key (first ident)]
                                 [:= :to_ident_val (second ident)]]}
                               honeysql)
                             :quoting :ansi
                             :allow-dashed-names? true)
                      [query-string & rest] stmt]
                  (into
                    [(replace-qs query-string)]
                    rest))

            _ (when debug-log?
                (ks/spy "<referenced-by sql" sql))
          
            res (<! (<exec
                      (:box.pl/sqlite-db conn)
                      sql))]
        (let [from-idents
              (->> res
                   res/data
                   (map (fn [{:strs [from_ident_key
                                     from_ident_val]}]
                          [(keyword from_ident_key)
                           from_ident_val]))
                   distinct)]
          (when (not (empty? from-idents))
            (<! (pl/<pull-multi
                  conn
                  from-idents
                  pull-keys
                  opts)))))))

(defn decode-into-tuple [[sql-res err]]
  (if sql-res
    [(->> sql-res
          :rows
          (map #(get % "transit"))
          (mapv ks/from-transit))]
    [nil err]))

#_(defn <query-ids [conn query & [opts]]
  (go
    (decode-into-tuple
      (<! ((-> conn :box.pl/SQL :<exec)
           (:box.pl/sqlite-db conn)
           (query-stmt conn query opts))))))

(defn col [k]
  (ht/raw
    (str "\""
         (str (namespace k) "/" (name k))
         "\"")))


(comment

  (ks/pp
    (dp/parse-query
      '[:find ?e
        :where [?e :foo/bar 42]]))

  )
