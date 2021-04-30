(ns rx.box.persist-local
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [rx.box.common :as com]
            [rx.box.db-adapter :as da]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.set :as set]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(defn dbg-prn-obj [message o]
  (ks/spy message o))

(defn <destroy-db [db-adapter schema]
  (da/<destroy-db
    db-adapter
    schema))

(<defn <setup-db [SQL-or-modules schema]
  (let [{:keys [SQL]
         :as opts}
        (if (:<exec SQL-or-modules)
          {:SQL SQL-or-modules}
          SQL-or-modules)

        SQL (or SQL
                (:rx.sqlite/SQL opts))

        db-adapter (or (:box/db-adapter opts)
                       (:rx.box/db-adapter opts))]

    #_(when-not db-adapter
        (when-not SQL
          (ks/throw-anom
            {:desc "SQL call container is nil"
             :code :invalid})))

    #_(ks/spec-check-throw
        :box/schema
        schema
        "Invalid schema")

    (when db-adapter
      (let [schema (merge
                     (com/default-schema)
                     (update-in
                       schema
                       [:box/attrs]
                       merge
                       com/default-attrs
                       (select-keys
                         opts
                         [:box/cog-config
                          :box/cog-client-creds])))]

        (let [db (<? (da/<open-db db-adapter schema))

              ensure-structure-res
              (<! (da/<ensure-db-structure db-adapter db schema))]

          (when (anom/anom? ensure-structure-res)
            (throw (ex-info
                     "Error creating structure"
                     ensure-structure-res)))

          {:box/schema schema
           :box.pl/db db
           :box.pl/db-adapter db-adapter})))))

(defn setup-aws [SQL-or-modules schema]
  (let [{:keys [AWS]
         :as opts}
        (if (:<exec SQL-or-modules)
          {:SQL SQL-or-modules}
          SQL-or-modules)

        AWS (or AWS
                (:box.pl/AWS opts))]
    (when AWS
      {:box.pl/AWS AWS
       :box/schema (select-keys schema
                     [:box/cog-config
                      :box/cog-client-creds])})))

(<defn <conn
  [SQL-or-modules schema]
  (merge-with ks/deep-merge
    (<? (<setup-db SQL-or-modules schema))
    (setup-aws SQL-or-modules schema)))

;; Exec

(defn ensure-conn [conn]
  (ks/spec-check-throw
    :box/conn
    conn
    "Conn invalid"))

(defn valid-ident-map? [schema m]
  (and (map? m)
       (com/resolve-ident-key schema m)))

(defn valid-ident? [o]
  (and
    (vector? o)
    (first o)
    (second o)))

(defn valid-txf? [txf]
  (and (vector? txf)
       (vector? (second txf))
       (-> txf second second string?)
       (get
         #{:box/assoc :box/dissoc :box/conj :box/disj :box/delete}
         (first txf))))

(defn delete-txf? [txf]
  (and (vector? txf)
       (= :box/delete (first txf))))

(defn txf->ident [schema txf]
  (cond
    (map? txf)
    (com/resolve-ident schema txf)
    (and (vector? txf)
         (vector? (second txf)))
    (second txf)
    :else nil))

(defn validate-txf [schema txf]
  (cond
    (and (not (vector? txf))
         (not (map? txf)))
    {:rx.anom/desc "TXF is not a map or vector"
     :box/invalid-txf txf}

    (and (map? txf)
         (not (com/resolve-ident schema txf)))
    {:rx.anom/desc "Couldn't find valid ident in map TXF"
     :box/invalid-txf txf}

    (and (vector? txf)
         (not (valid-ident? (second txf))))
    {:rx.anom/desc "Invalid ident"
     :box/invalid-txf txf}

    ;; conj
    

    (and (vector? txf)
         (= :box/conj (first txf))
         (not (get-in
                schema
                [:box/attrs
                 (-> txf last first)
                 :box.attr/ident?])))
    {:rx.anom/desc "To ident key not found in schema attrs"
     :to-ident (-> txf last first)}

    :else nil))

(defn validate-txfs [schema txfs]
  (let [invalid-txfs
        (->> txfs
             (map
               (fn [txf]
                 [(validate-txf schema txf) txf]))
             (remove #(nil? (first %)))
             vec)]
    (when-not (empty? invalid-txfs)
      (throw (ex-info "Invalid txfs"
               (anom/anom
                 {:code ::invalid-txfs}
                 {::validation-res invalid-txfs}))))))

(defn schema+key->attr-spec [schema ident-key]
  (get-in schema [:box/attrs ident-key]))

(defn assert-entity-spec [schema m]
  (let [[ident-key ident-val] (com/resolve-ident schema m)
        {:keys [:box.attr/entity-spec-key]}
        (schema+key->attr-spec schema ident-key)]
    (when entity-spec-key
      (when-not (s/valid? entity-spec-key m)
        (throw
          (ex-info
            "Assert entity spec failed"
            (s/explain-data entity-spec-key m)))))))

(defn check-entity-specs [schema ident->entity]
  (doseq [[ident entity] ident->entity]
    (assert-entity-spec schema entity)))

(declare expand-map)
(declare expand-txf)
(declare expand-txfs)

(defn map->ident [schema m]
  (let [ident-key (com/resolve-ident-key schema m)
        ident-val (get m ident-key)]
    (when (and ident-key ident-val)
      {ident-key ident-val})))

(defn vec->idents [schema k vs]
  (->> vs
       (mapv
         (fn [m]
           (let [ident-key (com/resolve-ident-key schema m)]
             (when ident-key
               {ident-key (get m ident-key)}))))
       (remove nil?)))

(defn expand-map [schema m]
  (let [ident-key (com/resolve-ident-key schema m)
        ident-value (get m ident-key)
        wo-ident (dissoc m ident-key)]
    (->> m
         (map (fn [[k v]]
                [:box/assoc
                 [ident-key ident-value]
                 k
                 v]))
         (expand-txfs schema))))

(defn fetch-stmt [schema
                  ident-vals]
  (when (empty? ident-vals)
    (throw (ex-info "ident-vals cannot be empty"
             {:rx.anom/var ::fetch-stmt})))
  (into
    [(str "select * from "
          (com/schema->data-table-name schema)
          " where "
          (->> ident-vals
               (map-indexed
                 (fn [i _]
                   (str "ident_val=:" i)))
               (interpose " OR ")
               (apply str)))]
    ident-vals))

(defn update-if-exists [m k f & args]
  (if (contains? m k)
    (assoc
      m
      k
      (apply f (get m k) args))
    m))

(defn <fetch-ident-val->m [conn ident-vals
                           & [{:keys [debug-log?]}]]
  (go
    (try
      (let [<exec (-> conn :box.pl/SQL :<exec)
            schema (:box/schema conn)
            ident-vals (distinct ident-vals)
            sql-vec (fetch-stmt schema ident-vals)

            _ (when debug-log?
                (ks/spy
                  "<fetch-ident-val->m sql-vec"
                  sql-vec))
            
            exec-res (<! (<exec
                           (:box.pl/sqlite-db conn)
                           sql-vec))

            _ (when debug-log?
                (dbg-prn-obj "<fetch-ident-val->m exec-res" exec-res))]

        (if (anom/anom? exec-res)
          exec-res
          (->> exec-res
               (map (fn [{:strs [ident_val transit]}]
                      [ident_val (ks/from-transit transit)]))
               (into {}))))
      (catch js/Error e
        (anom/from-err e)))))

(defn ref-key-txfs [schema txf]
  (let [[op ident k v-or-vs] txf
        {:keys [:box.attr/cardinality]
         :as attr-spec}
        (schema+key->attr-spec schema k)

        many? (= cardinality :box.cardinality/many)

        _ (when (and many?
                     (map? v-or-vs))
            (throw
              (ex-info
                "Map passed to :box/assoc when cardinality is many"
                {:attr-spec attr-spec
                 :txf txf})))

        vs (if many?
             v-or-vs
             [v-or-vs])

        child-maps (when (get #{:box/assoc :box/dissoc} op)
                     (->> vs
                          (filter map?)))

        child-idents (->> child-maps
                          (map #(com/resolve-ident schema %)))

        invalid-child-idents (remove valid-ident? child-idents)

        valid-child-idents (filter valid-ident? child-idents)]

    (when (not (empty? invalid-child-idents))
      (ks/throw-str "Found invalid child idents when generating ref key txfs: " invalid-child-idents " " txf ", perhaps id key is not in schema"))

    (condp = op
      :box/assoc
      (vec
        (concat
          (mapv
            (fn [child-ident]
              (if many?
                [:box/conj ident k child-ident]
                [:box/assoc ident k {(first child-ident)
                                     (second child-ident)}]))
            valid-child-idents)
          (mapcat #(expand-map schema %) child-maps)))

      :box/dissoc
      (map (fn [child-ident]
             (if many?
               [:box/disj ident k child-ident]
               [:box/dissoc ident k]))
        valid-child-idents)

      :box/disj [txf]
      :box/conj [txf]
      (ks/throw-str "Op " op " not supported for ref keys: " txf))))

(defn expand-txf [schema txf]
  (let [[op ident k v] txf
        ref-key? (get (-> schema com/schema->ref-keys-set) k)]
    (vec
      (concat
        (if ref-key?
          (ref-key-txfs schema txf)
          [txf])))))

(defn expand-txfs [schema txfs]
  (->> txfs
       (mapcat
         (fn [map-or-vec]
           (if (map? map-or-vec)
             (expand-map schema map-or-vec)
             (expand-txf schema map-or-vec))))
       distinct
       vec))

(defn apply-conj [m [op ident k child-ident]]
  (assoc m k [])
  #_(dissoc m k))

(defn apply-disj [m [op ident k child-ident]]
  (assoc m k [])
  #_(dissoc m k))

(defn apply-txf [m txf]
  (let [[op ident & args] txf]
    (condp = op
      :box/assoc (apply assoc m (first ident) (second ident) args)
      :box/dissoc (apply dissoc m args)
      :box/conj (apply-conj m txf)
      :box/disj (apply-disj m txf))))

(defn apply-txfs [m txfs]
  (->> txfs
       (reduce
         (fn [m txf]
           (apply-txf m txf))
         m)))

(defn <put [{:keys [:box.pl/db-adapter
                    :box.pl/db
                    :box/schema]}
            entities & [opts]]
  (da/<store-entities db-adapter db schema entities opts))

(defn <update-by-ident-val [conn ident-vals f
                            & [{:keys [debug-log?]
                                :as opts}]]
  (ensure-conn conn)
  (go
    (try
      (if (and ident-vals (not (empty? ident-vals)))
        (do
          (let [ident-val->m
                (<! (<fetch-ident-val->m conn ident-vals))

                _ (when debug-log?
                    (ks/spy "<update-by-ident-val ident-val->m"
                      ident-val->m))

                _ (anom/throw-if-anom ident-val->m)

                us (->> ident-vals
                        (map (fn [ident-val]
                               (f (get ident-val->m ident-val) ident-val)))
                        (remove nil?)
                        doall)

                res (<! (<put conn us opts))]

            (when debug-log?
              (ks/spy "<update-by-ident-val res" res))
            res))
        [])
      (catch js/Error e
        (anom/from-err e)))))


(defn apply-update-txfs [ident->entity
                         ident->update-txfs
                         ident->ref-txfs]
  (let [all-idents (distinct
                     (concat
                       (keys ident->update-txfs)
                       (keys ident->ref-txfs)))]
    (->> all-idents
         (map (fn [ident]
                (let [entity (get ident->entity ident)
                      update-txfs (get ident->update-txfs ident)
                      ref-txfs (get ident->ref-txfs ident)

                      updated-entity (apply-txfs entity
                                       (concat update-txfs ref-txfs))
                      ensure-ident-on-entity (merge
                                               {(first ident) (second ident)}
                                               updated-entity)]
                  [ident ensure-ident-on-entity])))
         (into {}))))

(defn <fetch-ident->entity [{:keys [:box.pl/db-adapter
                                    :box.pl/db
                                    :box/schema]}
                            idents & [{:keys [debug-log?] :as opts}]]
  (da/<fetch-entities
    db-adapter
    db
    schema
    idents
    opts))

(defn <update-entities [conn
                        ident->update-txfs
                        ident->ref-txfs
                        & [{:keys [debug-log?] :as opts}]]
  (ensure-conn conn)

  (go
    (if (empty? ident->update-txfs)
      []
      (try
        (let [schema (:box/schema conn)
              ident->existing-entity (<! (<fetch-ident->entity
                                           conn
                                           (keys ident->update-txfs)
                                           opts))

              _ (anom/throw-if-anom ident->existing-entity)

              _ (when debug-log?
                  (ks/spy "<update-entities ident->existing-entity"
                    ident->existing-entity))

              ident->updated-entity
              (apply-update-txfs
                ident->existing-entity
                ident->update-txfs
                ident->ref-txfs)

              _ (when debug-log?
                  (dbg-prn-obj "<update-entities ident->updated-entity"
                    ident->updated-entity))

              _ (check-entity-specs schema ident->updated-entity)

              update-ents-res
              (<! (<put conn (vals ident->updated-entity) opts))]

          (when debug-log?
            (dbg-prn-obj "<update-entities <put res"
              update-ents-res))

          (if (anom/? update-ents-res)
            update-ents-res
            (vec update-ents-res)))
        (catch js/Error e
          (anom/from-err e))))))

(defn replace-with-ident [conn ident-keys-set txf]
  (let [schema (com/conn->schema conn)
        [op eid k m] txf]
    (if (or (list? m) (vector? m))
      (into
        [[op eid k
          (->> m
               (map (fn [m]
                      (let [ident-k (->> m
                                         (some #(get
                                                  ident-keys-set
                                                  (first %))))
                            ident-v (when ident-k (get m ident-k))]
                        (if ident-k
                          {ident-k ident-v}
                          m)))))]]
        (expand-txfs schema m))
      (let [ident-k (->> m
                         (some #(get
                                  ident-keys-set
                                  (first %))))
            ident-v (when ident-k (get m ident-k))]
        (if ident-k
          (into
            [[op eid k {ident-k ident-v}]]
            (->> m
                 (map (fn [[mk mv]]
                        [:box/assoc
                         [ident-k ident-v]
                         mk
                         mv]))))
          [[op eid k m]])))))

(defn replace-refs [{:keys [ref-keys-set
                            ident-keys-set
                            schema]
                     :as conn}
                    txfs]
  (->> txfs
       (mapcat
         (fn [[op eid k m :as txf]]
           (cond
             (get ref-keys-set k)
             (replace-with-ident
               conn
               ident-keys-set
               txf)
             :else [txf])))))

(defn ref-txf? [schema txf]
  (get
    #{:box/conj
      :box/disj}
    (first txf)))

(defn process-transact-data [schema txfs & [{:keys [debug-log?]}]]

  (when debug-log?
    (ks/spy "process-transact-data txfs" txfs))

  (let [expanded (expand-txfs schema txfs)

        _ (when debug-log?
            (ks/spy "process-transact-data expanded" expanded))

        delete-txfs (->> expanded
                         (filter #(= :box/delete (first %))))

        delete-ident-vals
        (->> delete-txfs
             (map second)
             (map second))

        _ (when debug-log?
            (ks/spy "delete-ident-vals" delete-ident-vals))

        update-txfs (->> expanded
                         (filter #(get
                                    #{:box/assoc
                                      :box/dissoc
                                      :box/disj}
                                    (first %))))

        ref-txfs (->> expanded
                      (filter #(ref-txf? schema %)))

        ident-val->update-txfs
        (group-by
          #(-> % second second)
          update-txfs)

        update-ident-vals
        (->> ident-val->update-txfs
             keys
             (remove nil?))

        ident-val->ref-txfs
        (group-by
          #(-> % second second)
          ref-txfs)]

    {:delete-ident-vals delete-ident-vals
     :update-ident-vals update-ident-vals
     :ident-val->update-txfs ident-val->update-txfs
     :ident-val->ref-txfs ident-val->ref-txfs}))

(defn valid-cardinality-txf? [schema txf]
  (let [prop-key (nth txf 2)]
    (= :box.cardinality/many
       (get-in
         schema
         [:box/attrs prop-key :box.attr/cardinality]))))

(defn calculate-changes [schema txfs & [{:keys [debug-log?]}]]

  (when debug-log?
    (ks/spy "calculate-changes txfs" txfs))

  (let [expanded (expand-txfs schema txfs)

        _ (when debug-log?
            (ks/spy "calculate-changes expanded" expanded))

        delete-idents (->> expanded
                           (filter #(= :box/delete (first %)))
                           (map second)
                           doall)

        ident->update-txfs (->> expanded
                                (filter #(get
                                           #{:box/assoc
                                             :box/dissoc
                                             :box/disj}
                                           (first %)))
                                (group-by second))

        ref-txfs (->> expanded
                      (filter #(ref-txf? schema %)))

        ident->ref-txfs (->> ref-txfs
                             (group-by second))

        ref-to-idents (->> ref-txfs
                           (map #(nth % 3)))]

    (doseq [[ident ref-txfs] ident->ref-txfs]
      (doseq [ref-txf ref-txfs]
        (when-not (valid-cardinality-txf? schema ref-txf)
          (throw
            (ex-info (str "Property "
                          (nth ref-txf 2)
                          " not marked :box.cardinality/many")
              {:ref-txf ref-txf})))))

    {:delete-idents delete-idents
     :ident->update-txfs ident->update-txfs
     :ident->ref-txfs ident->ref-txfs
     :ref-to-idents ref-to-idents}))

(defn <update-refs
  [{:keys [:box.pl/db-adapter
           :box.pl/db
           :box/schema]}
   ident->ref-txfs
   & [opts]]
  (da/<update-refs
    db-adapter
    db
    schema
    ident->ref-txfs
    opts))

(defn <delete-idents [{:keys [:box.pl/db-adapter
                              :box.pl/db
                              :box/schema]}
                      delete-idents opts]
  (da/<delete-idents
    db-adapter
    db
    schema
    delete-idents
    opts))

(defn <transact [conn txfs
                 & [{:keys [debug-log?
                            skip-entity-specs?]
                     :as opts}]]

  (ensure-conn conn)

  (when debug-log?
    (ks/pn "operating on"
      (com/schema->data-table-name (:box/schema conn))))

  (when (empty? txfs)
    (throw (ex-info "Txfs can't be empty" {:txfs txfs})))
  
  (go
    (try
      (when-not (sequential? txfs)
        (ks/throw-str "Invalid txfs, not sequential"))
      
      (let [schema (:box/schema conn)]

        (validate-txfs schema txfs)
        
        (let [{:keys [delete-idents
                      ident->update-txfs
                      ident->ref-txfs]
               :as calculated-changes}
              (calculate-changes schema txfs opts)

              _ (when debug-log?
                  (dbg-prn-obj "<transact calculated-changes"
                    calculated-changes))

              delete-ret (<! (<delete-idents conn delete-idents opts))

              _ (when debug-log?
                  (dbg-prn-obj "<transact delete-ret" delete-ret))

              _ (anom/throw-if-anom delete-ret)

              update-ents-ret (<! (<update-entities
                                    conn
                                    ident->update-txfs
                                    ident->ref-txfs
                                    opts))

              _ (when debug-log?
                  (dbg-prn-obj "<transact update-ents-ret" update-ents-ret))

              _ (anom/throw-if-anom update-ents-ret)


              update-refs-ret (when-not (empty? ident->ref-txfs)
                                (<! (<update-refs
                                      conn
                                      ident->ref-txfs
                                      opts)))

              _ (when debug-log?
                  (dbg-prn-obj "<transact update-refs-ret" update-refs-ret))

              _ (anom/throw-if-anom update-refs-ret)]

          update-ents-ret))
      (catch js/Error e
        (anom/from-err e)))))


;; Pull

(defn calc-ident->resolve-refs-data
  [schema
   ident->entity
   ident->deref-spec
   {:keys [debug-log?] :as opts}]
  (->> ident->entity
       (map
         (fn [[ident entity]]
           (let [deref-spec
                 (get ident->deref-spec ident)

                 deref-key->child-spec
                 (->> deref-spec
                      (filter map?)
                      (reduce merge))

                 deref-keys-set
                 (->> deref-spec
                      (map (fn [o]
                             (if (map? o) (ffirst o) o)))
                      set)

                 _ (when debug-log?
                     (dbg-prn-obj
                       "calc-ident->resolve-refs-data deref-keys-set"
                       deref-keys-set))

                 found-deref-keys
                       
                 (set/intersection
                   deref-keys-set
                   (set (keys entity)))

                 _ (when debug-log?
                     (dbg-prn-obj
                       "calc-ident->resolve-refs-data found-deref-keys"
                       found-deref-keys))

                 found-deref-key->attr-schema
                 (->> found-deref-keys
                      (map (fn [k]
                             [k (merge
                                  (com/schema->attr-spec schema k)
                                  {:child-deref-spec
                                   (get deref-key->child-spec k)
                                   :entity entity})]))
                      (into {}))]

             (when debug-log? 
               (dbg-prn-obj
                 "gen-ident-val->unrealized-refs ident-val->deref-spec"
                 ident->deref-spec))
                   
             [ident found-deref-key->attr-schema])))
       (into {})))

(defn calc-ident+ref-key+opts
  [schema
   ident->entity
   ident->deref-spec
   {:keys [debug-log?] :as opts}]
  (->> ident->entity
       (mapcat
         (fn [[ident entity]]
           (let [deref-spec
                 (get ident->deref-spec ident)

                 deref-key->child-spec
                 (->> deref-spec
                      (filter map?)
                      (reduce merge))

                 deref-keys-set
                 (->> deref-spec
                      (map (fn [o]
                             (if (map? o) (ffirst o) o)))
                      set)

                 _ (when debug-log?
                     (dbg-prn-obj
                       "calc-ident->resolve-refs-data deref-keys-set"
                       deref-keys-set))

                 found-deref-keys
                       
                 (set/intersection
                   deref-keys-set
                   (set (keys entity)))

                 _ (when debug-log?
                     (dbg-prn-obj
                       "calc-ident->resolve-refs-data found-deref-keys"
                       found-deref-keys))]
             (->> found-deref-keys
                  (map (fn [k]
                         [ident k (merge
                                    (com/schema->attr-spec schema k)
                                    {:child-deref-spec
                                     (get deref-key->child-spec k)
                                     :entity entity})]))))))))

;; ident -> entity, deref-spec
;; structure, data calls

(defn <resolve-ref-idents
  [{:keys [:box.pl/db-adapter
           :box.pl/db
           :box/schema]}
   ident+ref-key+opts-tuples
   opts]
  (da/<resolve-ref-idents
    db-adapter
    db
    schema
    ident+ref-key+opts-tuples
    opts))

(<defn <expand-level [conn ident->entity ident->deref-spec
                      & [{:keys [debug-log?
                                 call-level]
                          :as opts}]]

  (when debug-log?
    (ks/prn "<expand-level call-level " (or call-level 0))
    (dbg-prn-obj "<expand-level call ident->entity"
      ident->entity)
    (dbg-prn-obj "<expand-level call ident->deref-spec"
      ident->deref-spec))
  
  (let [ ;; Deref keys are keys in maps we want to expand,
        ;; so we need to know which input maps have these keys

        ;; It helps to know which maps have which deref keys, their
        ;; corresponding values, and anything else

        call-level (or call-level 0)

        schema (:box/schema conn)

        ident->resolve-refs-data

        (calc-ident->resolve-refs-data
          schema
          ident->entity
          ident->deref-spec
          opts)

        _ (when debug-log?
            (ks/spy (str "<expand-level ident->resolve-refs-data")
              ident->resolve-refs-data))

        ident->prop->child-idents
        (<? (<resolve-ref-idents
              conn
              (->> ident->resolve-refs-data
                   (mapcat
                     (fn [[ident resolve-refs-map]]
                       (->> resolve-refs-map
                            (map (fn [[k refs-data]]
                                   [ident k refs-data]))))))
              opts))

        _ (when debug-log?
            (dbg-prn-obj "<expand-level ident->prop->child-idents"
              ident->prop->child-idents))
        
        ;; Add cardinality one idents
        card-one-ident->prop->child-idents

        (->> ident->resolve-refs-data
             (map
               (fn [[ident k->refs-data]]
                 [ident
                  (->> k->refs-data
                       (remove #(= :box.cardinality/many
                                   (-> %
                                       second
                                       :box.attr/cardinality)))
                       (map (fn [[k refs-data]]
                              (let [ident (first (get-in refs-data [:entity k]))]
                                [k [ident]])))
                       (into {}))]))
             (into {}))

        ident->prop->child-idents
        (->> (concat
               (keys ident->resolve-refs-data)
               (keys card-one-ident->prop->child-idents))
             distinct
             (map (fn [ident]
                    [ident (merge
                             (get ident->prop->child-idents ident)
                             (get card-one-ident->prop->child-idents ident))]))
             (into {}))
        

        _ (when debug-log?
            (dbg-prn-obj "<expand-level ident->prop->child-idents with cardinality one"
              ident->prop->child-idents))
        
        idents-to-fetch
        (->> ident->prop->child-idents
             (mapcat
               (fn [[ident prop->child-idents]]
                 (->> prop->child-idents
                      vals
                      (reduce concat)))))

        _ (when debug-log?
            (ks/spy "<expand-level idents-to-fetch"
              idents-to-fetch))

        next-ident->deref-spec
        (->> ident->prop->child-idents
             (mapcat
               (fn [[parent-ident prop->child-idents]]
                 (let [prop->attr-refs-data (get ident->resolve-refs-data parent-ident)]
                   (->> prop->child-idents
                        (mapcat
                          (fn [[prop child-idents]]
                            (let [{:keys [child-deref-spec]}
                                  (get prop->attr-refs-data prop)]
                              (->> child-idents
                                   (map (fn [child-ident]
                                          [child-ident
                                           child-deref-spec]))))))))))
             (into {}))

        _ (when debug-log?
            (ks/spy "<expand-level next-ident->deref-spec"
              next-ident->deref-spec))

        fetched-ident->m
        (when (> (count idents-to-fetch) 0)
          (<?
            (<fetch-ident->entity
              conn
              idents-to-fetch
              opts)))

        _ (when debug-log?
            (ks/spy "<expand-level fetch-ident->m"
              fetched-ident->m))

        expanded-fetched-ident->m
        (when-not (empty? idents-to-fetch)
          (<? (<expand-level
                conn
                fetched-ident->m
                next-ident->deref-spec
                (merge
                  opts
                  {:call-level (inc call-level)}))))

        _ (when debug-log?
            (ks/spy "<expand-level expanded-fetch-ident->m"
              expanded-fetched-ident->m))

        expanded-level
        (->> ident->prop->child-idents
             (map
               (fn [[ident prop->child-idents]]
                 (let [entity (get ident->entity ident)]
                   [ident
                    (merge
                      entity
                      (->> prop->child-idents
                           (map (fn [[child-prop idents]]
                                  (let [child-values (->> idents
                                                          (mapv #(get expanded-fetched-ident->m %)))]
                                    [child-prop (if (= :box.cardinality/many
                                                       
                                                       (com/schema->attr-cardinality
                                                         schema
                                                         child-prop))
                                                  child-values
                                                  (first child-values))])))
                           (into {})))])))
             (into {})
             (merge-with ks/deep-merge
               ident->entity))]
    
    (when debug-log?
      (dbg-prn-obj
        "<expand-level expanded-level return"
        expanded-level))
    expanded-level))

(<defn <expand [conn ms deref-spec & [{:keys [debug-log?] :as opts}]]
  (if (empty? ms)
    ms
    (let [schema (:box/schema conn)

          idents-in-order (->> ms
                               (map #(com/resolve-ident schema %))
                               (remove nil?))

          _ (when (empty? idents-in-order)
              (ks/throw-anom
                {:code :incorrect
                 :desc "Couldn't resolve some idents"
                 ::entities ms
                 :box/schema schema}))

          _ (when debug-log?
              (dbg-prn-obj "<expand idents-in-order"
                idents-in-order))

          ident->deref-spec
          (->> ms
               (map
                 (fn [m]
                   (let [ident-key (com/resolve-ident-key schema m)
                         ident-val (get m ident-key)]
                     [[ident-key ident-val] deref-spec])))
               (into {}))
          ident->expanded-entity
          (->> (<? (<expand-level
                     conn
                     (->> ms
                          (map
                            (fn [m]
                              (let [ident-key (com/resolve-ident-key schema m)
                                    ident-val (get m ident-key)]
                                [[ident-key ident-val] m])))
                          (into {}))
                     ident->deref-spec
                     opts)))]
      (->> idents-in-order
           (mapv
             (fn [ident]
               (get ident->expanded-entity ident)))))))

(defn <expand-map-entry [conn deref-spec [k v]]
  (go
    [k v]))

(<defn <maybe-expand-map-entry [conn deref-spec [k v]]
  (let [deref-keys (->> deref-spec
                        (map (fn [o]
                               (if (map? o)
                                 (ffirst o)
                                 o)))
                        set)]
    (if (get deref-keys k)
      (<? (<expand-map-entry conn deref-spec [k v]))
      [k v])))

(defn <pull [conn
             ident
             & [deref-spec {:keys [debug-log?] :as opts}]]
  (ensure-conn conn)
  (go
    (try
      (let [m (-> (<? (<fetch-ident->entity conn [ident] opts))
                  first
                  second)]

        (when debug-log?
          (dbg-prn-obj
            "<pull fetch-ident->entity result" m))

        (if m
          (first
            (<? (<expand
                  conn
                  [m]
                  deref-spec
                  opts)))
          nil))
      (catch js/Error e e))))

(<defn <pull-multi [conn
                    idents
                    & [deref-spec opts]]
  (when (not (empty? idents))
    (let [ms (-> (<? (<fetch-ident->entity
                       conn
                       idents
                       opts))
                 vals)]
      (<? (<expand
            conn
            ms
            deref-spec
            opts)))))

;; Debug Helpers

(<defn debug-conn [SQL
                   {:keys [schema
                           destroy-db?]}
                   <f]
  (let [<open-db (:<open-db SQL)
        #__
        #_(when destroy-db?
            (<! (<destroy-tables
                  SQL
                  (<? (<open-db {:name (com/local-db-name schema)}))
                  schema)))
        conn (<? (<conn SQL schema))
        start-time (system-time)
        res (<? (<f conn))
        end-time (system-time)]
    (ks/spy
      (str "RESULT ----- "
           (/
             (ks/round (* (- end-time start-time) 100))
             100)
           " ms")
      res)))

