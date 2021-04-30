(ns rx.box.db-adapter
  (:require [rx.box.common :as com]
            [rx.kitchen-sink :as ks]
            [cljs.core.async
             :refer-macros [go]]))

(defprotocol IDBAdapter
  (-<open-db [_ schema opts])
  (-<destroy-db [_ schema opts])
  (-<ensure-db-structure [_ db schema opts])
  (-<delete-idents [_ db schema idents opts])
  (-<fetch-entities [_ db schema idents opts])
  (-<store-entities [_ db schema entities opts])
  (-<update-refs [_ db schema ident->ref-txfs opts])
  (-<resolve-ref-idents [_ db schema ident->ref-txfs opts])
  (-<fetch-by-query [_ db schema query opts])
  (-<count-by-query [_ db schema query opts]))

(defn <open-db [DS schema & [opts]]
  (try
    (when-not DS
      (ks/throw-str "DataStore is nil"))

    (ks/spec-check-throw
      :box/persist-local-schema
      schema
      "Invalid persist local schema")

    (-<open-db DS schema opts)
    (catch js/Error e
      (go e))))

#_(defn <open-db [DS schema & [opts]]
  (go
    (try
      (when-not DS
        (ks/throw-str "DataStore is nil"))

      (ks/spec-check-throw
        :box/persist-local-schema
        schema
        "Invalid persist local schema")

      (<! (-<open-db DS schema opts))
      (catch js/Error e e))))

(defn <destroy-db [DS schema & [opts]]
  (-<destroy-db DS schema opts))

(defn <ensure-db-structure [DS db schema & [opts]]
  (-<ensure-db-structure DS db schema opts))

(defn <delete-idents [DS db schema idents & [opts]]
  (-<delete-idents DS db schema idents opts))

(defn <fetch-entities [DS db schema idents & [opts]]
  (-<fetch-entities DS db schema idents opts))

(defn <store-entities [DS db schema entities & [opts]]
  (-<store-entities DS db schema entities opts))

(defn <update-refs [DS db schema ident->ref-txfs & [opts]]
  (-<update-refs DS db schema ident->ref-txfs opts))

(defn <resolve-ref-idents [DS db schema ident+ref-key+opts-tuples & [opts]]
  (-<resolve-ref-idents DS db schema ident+ref-key+opts-tuples opts))

(defn <fetch-by-query [DS db schema query & [opts]]
  (-<fetch-by-query DS db schema query opts))

(defn <count-by-query [DS db schema query & [opts]]
  (-<count-by-query DS db schema query opts))


