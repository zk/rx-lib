(ns rx.node.box.conn
  (:require [rx.kitchen-sink :as ks]
            [rx.box.db-adapter-sqlite :as dba]
            [rx.node.sqlite :as sql]
            [rx.box :as box]))

(defn <conn [schema]
  (box/<conn
    {:box/db-adapter
     (dba/create-db-adapter sql/SQL)}
    schema))

(comment

  (ks/<pp (<conn {}))


  )
