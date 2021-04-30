(ns rx.box.common
  (:require [rx.kitchen-sink :as ks]
            [cljs.spec.alpha :as s]
            [cljs.spec.gen.alpha :as gen]
            [rx.specs :as specs]
            [clojure.string :as str]))

(def nes :rx.specs/non-empty-string)
(def ti :rx.specs/time-index)

(s/def :box/sync-ddb-uri nes)

(s/def :box/ddb-data-table-name nes)
(s/def :box/ddb-creds map?)

(s/def :box/attr-map
  (s/keys
    :opt [:box.attr/value-type]))

(s/def :box/attrs
  (s/map-of keyword? :box/attr-map))

;; Ops

;; box.op/update

(s/def :box/local-db-name string?)
(s/def :box/local-data-table-name nes)
(s/def :box/local-refs-table-name nes)

;; Attributes

(s/def :box.attr/index? boolean?)
(s/def :box.attr/ddb-owner-key keyword?)
(s/def :box.attr/ddb-sort-numeric-key keyword?)

;; Connection

(s/def :box.pl/sqlite-db #(not (nil? %)))
(s/def :box.pl/SQL #(not (nil? %)))

(s/def :box.pers/sort-val nat-int?)
(s/def :box.pers/created-ts ti)
(s/def :box.pers/updated-ts ti)
(s/def :box.pers/archived-ts ti)
(s/def :box.pers/archive-marked-ts ti)

(s/def :box.sync/delete-marked-ts ti)
(s/def :box.sync/delete-success-ts ti)
(s/def :box.sync/dirty-ts ti)
(s/def :box.sync/in-progress-ts ti)
(s/def :box.sync/success-ts ti)
(s/def :box.sync/failure-ts ti)
(s/def :box.sync/failure-code keyword?)
(s/def :box.sync/inflight-ts ti)
(s/def :box.sync/dirty-ts ti)
(s/def :box.sync/version nat-int?)
(s/def :box.sync/owner-id :rx.specs/id)
(s/def :box.sync/ignore-match-version? boolean?)
(s/def :box.sync/op-key keyword?)
(s/def :box.sync/anom-code keyword?)
(s/def :box.sync/results seq?)
(s/def :box.sync/has-failures? boolean?)
(s/def :box.sync/ops seq?)
(s/def :box.sync/auth map?)
(s/def :box.sync/entities seq?)
(s/def :box.sync/entity map?)

(s/def :box.sync/op
  (s/keys
    :req [:box.sync/op-key
          :box.sync/entity]
    :opt [:box.sync/ignore-match-version?]))

;; box.sync/version-mismatch


(s/def :box/ident
  (s/tuple keyword? string?))

(s/def :box/value-type
  #(get
     #{:box.type/long
       :box.type/string
       :box.type/boolean}
     %))

(s/def :box/schema
  (s/keys
    :opt [:box/sync-ddb-uri
          :box/attrs]))

(s/def :box/persist-local-schema
  (s/keys
    :req [:box/local-db-name]))

(s/def :box/conn
  (s/keys
    :opt [:box/schema
          :box.pl/db-adapter
          :box.pl/db]))


;; Access

(defn local-db-name [schema]
  (:box/local-db-name schema))

(defn conn->schema [conn]
  (let [schema (:box/schema conn)]
    (ks/spec-check-throw :box/schema schema)
    schema))

;; Resolve

(defn resolve-ident-key [schema entity]
  (->> entity
       keys
       (some
         (fn [k]
           (let [attr-spec (get (-> schema :box/attrs) k)]
             (when (and attr-spec (:box.attr/ident? attr-spec))
               k))))))

(defn resolve-ident-val [schema m]
  (let [k (resolve-ident-key schema m)]
    (get m k)))

(defn resolve-ident [schema m]
  (let [ident-key (resolve-ident-key schema m)
        ident-val (get m ident-key)]
    (when (and ident-key ident-val)
      [ident-key ident-val])))

(defn schema->sync-ddb-uri [schema]
  (-> schema
      :box/sync-ddb-uri))

(defn schema->data-table-name [schema]
  (-> schema
      :box/local-data-table-name))

(defn schema->refs-table-name [schema]
  (-> schema
      :box/local-refs-table-name))

(defn schema->ref-keys-set [schema]
  (->> schema
       :box/attrs
       (filter (fn [[attr-key attr-spec]]
                 (:box.attr/ref? attr-spec)))
       (map first)
       set))

(defn schema->index-keys-set [schema]
  (->> schema
       :box/attrs
       (filter #(-> % second :box.attr/index?))
       (map first)
       set))

(defn indexed-key->val [schema entity]
  (let [indexed-keys-set (schema->index-keys-set schema)]
    (->> entity
         (filter #(get indexed-keys-set (first %)))
         (into {}))))

(defn schema->sort-keys [schema]
  (->> schema
       :box/attrs
       (filter #(-> % second :box.attr/sort-key))
       (map first)
       set))

(defn attr-sort-key [schema attr-key]
  (-> schema
      :box/attrs
      (get attr-key)
      :box.attr/sort-key))

(defn schema->cog-config [schema]
  (->> schema :box/cog-config))

(defn schema->cog-client-creds [schema]
  (->> schema :box/cog-client-creds))

(defn schema->attr-spec [schema k]
  (get-in schema [:box/attrs k]))

(defn schema->attr-cardinality [schema k]
  (-> schema
      :box/attrs
      k
      :box.attr/cardinality))

(def default-attrs
  {:box.sync/in-progress-ts {:box.attr/index? true
                             :box.attr/value-type :box.type/long}
   :box.sync/delete-marked-ts {:box.attr/index? true
                               :box.attr/value-type :box.type/long}
   :box.sync/dirty-ts {:box.attr/index? true
                       :box.attr/value-type :box.type/long}
   :box.sync/failure-ts {:box.attr/index? true
                         :box.attr/value-type :box.type/long}
   :box.pers/created-ts {:box.attr/index? true
                         :box.attr/value-type :box.type/long}
   :box.pers/updated-ts {:box.attr/index? true
                         :box.attr/value-type :box.type/long}
   :box.pers/archived-ts {:box.attr/index? true
                          :box.attr/value-type :box.type/long}
   :box/id {:box.attr/index? true
            :box.attr/value-type :box.type/string}
   :box.pers/sort-val {:box.attr/index? true
                       :box.attr/value-type :box.type/long}
   :box.canary/id {:box.attr/ident? true
                   :box.attr/value-type :box.type/string}})

(defn default-schema []
  {:box/local-data-table-name "data_table"
   :box/local-refs-table-name "refs_table"
   :box/local-db-name
   ":memory:"
   #_(str "file:" (ks/uuid) "?mode=memory")})

(defn apply-default-attrs [schema]
  (when schema
    (update-in
      schema
      [:box/attrs]
      merge
      default-attrs)))

;; Anomaly categories

:box.anom/unavailable
:box.anom/interrupted
:box.anom/incorrect
:box.anom/forbidden
:box.anom/unsupported
:box.anom/not-found
:box.anom/conflict
:box.anom/fault
:box.anom/busy
:box.anom/unknown

(defn anom? [m]
  (and (map? m)
       (or (:box.anom/category m)
           (:box.anom/desc m))))

(defn stack-vec [e]
  (->> (-> (.-stack e)
           (str/split #"\n+"))
       (remove empty?)
       vec))

(defn error->anom [e & [category]]
  {:box.anom/desc (.-message e)
   :box.anom/js-stack (stack-vec e)
   :box.anom/category (or category :box.anom/undetermined)})
