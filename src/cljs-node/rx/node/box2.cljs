(ns rx.node.box2
  (:require [rx.box2 :as box]
            [rx.box2.sql :as bsql]
            [rx.node.sqlite2 :as sql]))

(defn <conn [schema]
  (box/<conn
    (merge
      {:box/schema schema}
      {::bsql/<exec sql/<exec
       ::bsql/<batch-exec sql/<batch-exec
       ::bsql/<open-db sql/<open-db})))

(defn <transact [conn txfs-or-ents]
  (box/<transact conn txfs-or-ents))

(defn <pull [conn pattern ident-or-ent & [not-found]]
  (box/<pull conn pattern ident-or-ent not-found))

(defn <query [query conn & inputs]
  (apply box/<query query conn inputs))
