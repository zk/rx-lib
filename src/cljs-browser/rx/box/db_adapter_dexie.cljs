(ns rx.box.db-adapter-dexie
  (:require [rx.kitchen-sink :as ks]
            [rx.box.db-adapter :as da]
            [rx.box.common :as com]
            [rx.anom :as anom]
            [goog.object :as gobj]
            #_[dexie]
            [clojure.string :as str]
            [cljs.core.async :as async
             :refer [<! put! close! chan]
             :refer-macros [go go-loop]]))


(def Dexie
  nil
  #_(.-default dexie))

(defn dbg-prn-obj [message o]
  (ks/spy message o))

(defn key->keypath [k]
  (-> (pr-str k)
      (str/replace ":" "_COLON_")
      (str/replace "." "_DOT_")
      (str/replace "/" "_FSLASH_")
      (str/replace "-" "_DASH_")))

(defn <ensure-db-structure [_ db schema opts]
  (go
    (.stores (.version db (ks/now))
      (clj->js
        {(com/schema->data-table-name schema)
         (->> schema
              com/schema->index-keys-set
              (map key->keypath)
              sort
              (concat
                ["data_table_key"
                 "ident_key"
                 "ident_val"])
              (interpose ",")
              (apply str))
         (com/schema->refs-table-name schema) "ref_table_key,from_index_key,[from_index_key+sort_val]"}))

    "ok"))

(defn <open-db [_ schema opts]
  (go
    (Dexie. (com/local-db-name schema))))

(defn <destroy-db [_ schema opts]
  (go
    (let [{:keys [result error]}
          (<! (ks/promise->ch2
                (.delete Dexie (com/local-db-name schema))))]
      (if error
        (anom/from-err error)
        result))))

(defn ident->db-str [ident]
  (pr-str ident))

(defn db-str->ident [db-str]
  (ks/edn-read-string db-str))

(defn <delete-idents [_ db schema idents {:keys [debug-log?]}]
  (when debug-log?
    (dbg-prn-obj
      "dexie <delete-idents" idents))
  (go
    (if (empty? idents)
      "empty idents"
      (let [{:keys [result error]}
            (<! (ks/promise->ch2
                  (.bulkDelete
                    (gobj/get db (com/schema->data-table-name schema))
                    (clj->js
                      (->> idents
                           (map ident->db-str))))))]
        (if error
          (anom/from-err error)
          idents)))))

(defn <fetch-entities [_ db schema idents {:keys [debug-log?]}]
  (when debug-log?
    (dbg-prn-obj
      "dexie <fetch-entities"
      [db
       (com/local-db-name schema)
       (com/schema->data-table-name schema)
       idents]))
  (go
    (try
      (loop [idents idents
             ident->entity {}]
        (if (empty? idents)
          (do
            (when debug-log?
              (dbg-prn-obj
                "dexie <fetch-entities ident->entity"
                ident->entity))
            ident->entity)
          (let [ident (first idents)

                {:keys [result error]}
                (<! (ks/promise->ch2
                      (.get
                        (gobj/get db (com/schema->data-table-name schema))
                        (ident->db-str ident))))
                entity (when result
                         (when-let [transit-str (.-transit result)]
                           (ks/from-transit (.-transit result))))]
            
            (if error
              (throw error)
              (recur (rest idents)
                (merge
                  ident->entity
                  (when entity
                    {ident entity})))))))
      (catch js/Error e
        (anom/from-err e)))))

(defn entity->record [schema entity]
  (let [ident (com/resolve-ident schema entity)]
    (merge
      {"data_table_key" (ident->db-str ident)
       "ident_key" (pr-str (first ident))
       "ident_val" (second ident)
       "transit" (ks/to-transit entity)}
      (->> (com/indexed-key->val schema entity)
           (map (fn [[k v]]
                  [(key->keypath k) v]))
           (into {})))))

(defn <store-entities [_ db schema entities {:keys [debug-log?]}]
  (when debug-log?
    (dbg-prn-obj
      "dexie <store-entities"
      entities))
  (go
    (let [{:keys [result error]}
          (<! (ks/promise->ch2
                (.bulkPut
                  (gobj/get db (com/schema->data-table-name schema))
                  (clj->js
                    (->> entities
                         (map
                           #(entity->record schema %)))))))]
      (if error
        (anom/from-err error)
        entities))))

(defn ident+ref-key->idb-key [ident ref-key]
  (str
    (ident->db-str ident)
    "__"
    (pr-str ref-key)))

(defn ref-txf->record [schema txf & [sort-val]]
  (let [table-name (com/schema->refs-table-name schema)]
    (when table-name
      (let [[_ ;; :db/conj
             from-ident
             from-ref-key
             to-ident] txf]
        {:ref_table_key (str
                          (ident->db-str from-ident)
                          "__"
                          (pr-str from-ref-key)
                          "__"
                          (ident->db-str to-ident))
         :from_index_key (ident+ref-key->idb-key from-ident from-ref-key)
         :from_ref_key (pr-str from-ref-key)
         :from_ident (ident->db-str from-ident)
         :to_ident (ident->db-str to-ident)
         :sort_val (or sort-val 0)}))))

(defn <update-refs [_ db schema ident->ref-txfs opts]
  (go
    (try
      (let [delete-ident-keys (->> ident->ref-txfs
                                   (mapcat second)
                                   (filter #(= :box/disj (first %)))
                                   (map #(ref-txf->record schema %))
                                   (map :ref_table_key))

            {:keys [del-result error]}
            (<! (ks/promise->ch2
                  (.bulkDelete
                    (gobj/get db (com/schema->refs-table-name schema))
                    (clj->js
                      delete-ident-keys))))

            _ (when error
                (throw error))

            ident->entity (<! (<fetch-entities
                                nil
                                db
                                schema
                                (->> ident->ref-txfs
                                     (mapcat second)
                                     (filter #(= :box/conj (first %)))
                                     (map #(nth % 3)))
                                opts))
            
            

            update-records (->> ident->ref-txfs
                                (mapcat second)
                                (filter #(= :box/conj (first %)))
                                (map (fn [[_ from-ident ref-key to-ident
                                           :as txf]]
                                       (ref-txf->record
                                         schema
                                         txf
                                         (get-in
                                           ident->entity
                                           [to-ident
                                            (com/attr-sort-key
                                              schema
                                              ref-key)])))))

            {:keys [upd-result error]}
            (<! (ks/promise->ch2
                  (.bulkPut
                    (gobj/get db (com/schema->refs-table-name schema))
                    (clj->js
                      update-records))))

            _ (when error
                (throw error))]
        (if error
          (anom/from-err error)
          "ok"))
      (catch js/Error e
        (anom/from-err e)))))

(defn <resolve-ref-idents
  [_ db schema ident+ref-key+opts-tuples
   & [{:keys [debug-log?] :as opts}]]
  (go
    (try
      (loop [ident+ref-key+opts-tuples
             ident+ref-key+opts-tuples
             ident->prop->child-idents {}
             errs []]
        (cond
          (not (empty? errs))
          (anom/from-err (first errs))

          (empty? ident+ref-key+opts-tuples)
          ident->prop->child-idents

          :else (let [[ident ref-key {:keys [:box.attr/limit
                                             :box.attr/sort-order]
                                      :or {limit 1000}}]
                      (first ident+ref-key+opts-tuples)

                      {:keys [result error]}
                      (<! (ks/promise->ch2
                            (let [o (gobj/get db (com/schema->refs-table-name schema))
                            
                                  o (.where o "[from_index_key+sort_val]")
                            
                                  o (.between o
                                      (clj->js
                                        [(ident+ref-key->idb-key ident ref-key)
                                         (.-minKey Dexie)])
                                      (clj->js
                                        [(ident+ref-key->idb-key ident ref-key)
                                         (.-maxKey Dexie)]))

                                  o (.limit o limit)

                                  o (if (= sort-order :box.sort/descending)
                                      (.reverse o)
                                      o)]
                              (.toArray o))))
                
                      to-idents
                      (when result
                        (->> result
                             (mapv (fn [rec]
                                     (let [from_ident (gobj/get rec "from_ident")
                                           to_ident (gobj/get rec "to_ident")
                                           from_ref_key (gobj/get rec "from_ref_key")

                                           from-ident (db-str->ident from_ident)
                                           to-ident (db-str->ident to_ident)
                                           from-ref-key (ks/edn-read-string from_ref_key)]
                                       to-ident)))))]
            
                  (if error
                    (throw error)
                    (recur
                      (rest ident+ref-key+opts-tuples)
                      (merge-with ks/deep-merge
                        ident->prop->child-idents
                        {ident {ref-key to-idents}})
                      (if error
                        (conj errs error)
                        errs))))))
      (catch js/Error e
        (anom/from-err e)))))

(defn ident-key-clause [query]
  (->> query
       :where
       (filter sequential?)
       (filter #(= :ident-key (first %)))
       first))

(defn where-ident-array [query]
  (when (ident-key-clause query)
    ["ident_key"]))

(defn where-range-array [query]
  (->> query
       :where
       (filter sequential?)
       (filter #(get #{:> :>= :< :<=} (first %)))
       (map second)
       (map key->keypath)
       (take 1)))

(defn where-array [query]
  (concat
    (where-ident-array query)
    (where-range-array query)))

(defn where-arg [query]
  (when-let [first-clause (->> query
                               :where
                               (filter sequential?)
                               first)]
    (let [[op k v :as comparator-clause]
          (->> query
               :where
               (filter sequential?)
               (filter #(get #{:> :>= :< :<= :=} (first %)))
               first)
          
          same-key-clauses (->> query
                                :where
                                (filter sequential?)
                                (filter #(= k (second %))))

          lower-clause (->> same-key-clauses
                            (filter #(get #{:> :>=} (first %)))
                            first)

          upper-clause (->> same-key-clauses
                            (filter #(get #{:< :<=} (first %)))
                            first)

          eq-clause (->> same-key-clauses
                         (filter #(get #{:=} (first %)))
                         first)

          ident-clause (when-not comparator-clause
                         (->> query
                              :where
                              (filter sequential?)
                              (filter #(get #{:ident-key} (first %)))
                              first))]
      (if (= :ident-key (first first-clause))
        {:ident-key (second first-clause)
         :where "ident_key"
         :eq-value (pr-str (second first-clause))}
        (do
          (when (> (count same-key-clauses) 2)
            (ks/throw-str "Invalid query, can't have more than two comparator clauses with the same prop key: " same-key-clauses))
          (merge
            (when (or lower-clause
                      upper-clause
                      eq-clause)
              {:where (key->keypath k)})
            (when lower-clause
              {:lower-value (nth lower-clause 2)
               :lower-inclusive? (= :>= (first lower-clause))})
            (when upper-clause
              {:upper-value (nth upper-clause 2)
               :upper-inclusive? (= :<= (first upper-clause))})
            (when eq-clause
              {:eq-value (nth eq-clause 2)})))))))

(defn remove-clauses-used-in-where [clauses]
  (when-let [clause (first clauses)]
    (cond
      (= :ident-key (first clause))
      (->> clauses
           (remove #(= :ident-key (first %))))

      (get #{:= :> :>= :< :<=} (first clause))
      (->> clauses
           (remove #(= (second clause)
                       (second %))))

      :else (ks/throw-str "Unknown clause op: " clause))))

(defn apply-and-clause [clause js-obj]
  (cond
    (= :ident-key (first clause))
    (= (pr-str (second clause))
       (gobj/get js-obj "ident_key"))

    (get #{:= :> :>= :< :<=} (first clause))
    (let [f (get #{:= = :> > :>= >= :< < :<= <=} (first clause))
          js-obj-value (gobj/get js-obj (key->keypath (second clause)))
          clause-value (nth clause 2)]
      (when-not f
        (ks/throw-str "Can't find comparator functionf for " (first clause)))
      (f js-obj-value clause-value))))

(defn and-filter [query]
  (let [clauses (->> query
                     :where
                     (filter sequential?))

        and-clauses (remove-clauses-used-in-where clauses)]
    (when-not (empty? and-clauses)
      (fn [v]
        (every?
          identity
          (->> and-clauses
               (map #(apply-and-clause % v))))))))

(comment

  (where-arg
    {:where
     [:and [:> :bar/order 1] [:< :bar/order 10] [:ident-key :foo/id]]})

  )

(defn between [query lower?]
  (->> query
       :where
       (filter sequential?)
       (filter #(get (if lower?
                       #{:> :>= :=}
                       #{:< :<= :=})
                  (first %)))
       (group-by second)
       (map (fn [[k clauses]]
              (when (> (count clauses) 1)
                (ks/throw-str "Multiple lower bound comparators found, only provide one of :<, :<=, or := per query: " query))
              (let [[op prop-key value] (first clauses)]
                {:array (concat
                          (where-ident-array query)
                          [value])
                 :value value
                 :inclusive? (not
                               (nil?
                                 (get (if lower?
                                        #{:<= :=}
                                        #{:>= :=})
                                   op)))})))
       first))

(defn between-lower [query]
  (between query true))

(defn between-upper [query]
  (between query false))


(comment
  
  (ks/spy
    (where-array {:where [:and
                          [:ident-key :t0/id]
                          [:>= :foo/order 10]
                          [:< :foo/order 100]]}))
  
  (ks/spy
    (between-upper {:where [:and
                            [:ident-key :t0/id]
                            [:>= :foo/order 10]
                            [:< :foo/order 100]]}))

  )

(defn <fetch-by-query [_ db schema query opts]
  (go
    (let [{:keys [result error]}
          (<! (ks/promise->ch2
                (let [o (gobj/get db (com/schema->data-table-name schema))
                      {:keys [where
                              eq-value
                              lower-value
                              lower-inclusive?
                              upper-value
                              upper-inclusive?
                              ]}
                      (where-arg query)

                      o (.where o where)

                      o (if (and lower-value upper-value)
                          (.between o
                            lower-value
                            upper-value
                            lower-inclusive?
                            upper-inclusive?)
                          o)

                      o (if eq-value
                          (.equals o (clj->js eq-value))
                          o)

                      filter-fn (and-filter query)

                      o (if filter-fn
                          (.and o filter-fn)
                          o)]
                  (.toArray o))))]
      (if error
        (anom/from-err error)
        (->> result
             (map #(.-transit %))
             (remove nil?)
             (mapv ks/from-transit))))))

(deftype DBAdapter []
  da/IDBAdapter
  (-<open-db [_ schema opts]
    (<open-db nil schema opts))
  (-<destroy-db [_ schema opts]
    (<destroy-db nil schema opts))
  (-<ensure-db-structure [_ db schema opts]
    (<ensure-db-structure nil db schema opts))
  (-<delete-idents [_ db schema idents opts]
    (<delete-idents nil db schema idents opts))
  (-<fetch-entities [_ db schema idents opts]
    (<fetch-entities nil db schema idents opts))
  (-<store-entities [_ db schema entities opts]
    (<store-entities nil db schema entities opts))
  (-<update-refs [_ db schema ident->ref-txfs opts]
    (<update-refs nil db schema ident->ref-txfs opts))
  (-<resolve-ref-idents [_ db schema ident+ref-key+opts-tuples opts]
    (<resolve-ref-idents nil db schema ident+ref-key+opts-tuples opts))
  (-<fetch-by-query [_ db schema query opts]
    (<fetch-by-query nil db schema query opts))
  (-<count-by-query [_ db schema query opts]))

(defn create-db-adapter []
  (DBAdapter.))

(comment

  (.open (.. js/window -indexedDB) "foo")

  (let [req (.open (.. js/window -indexedDB) "test-database2" 1)
        _ (set! (.-onerror req)
            (fn [event]
              (ks/prn "ERROR" event)))
        
        _ (set! (.-onsuccess req)
            (fn [event]
              (ks/prn "SUCCESS" event
                (.. event -target -result))))

        _ (set! (.-onupgradeneeded req)
            (fn [event]
              (ks/prn "Upgrade Needed" event)))]
    req)

  )
