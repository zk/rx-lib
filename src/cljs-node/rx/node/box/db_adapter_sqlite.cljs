(ns rx.node.box.db-adapter-sqlite
  (:require [rx.kitchen-sink :as ks]
            [rx.box.db-adapter-sqlite :as das]
            [rx.node.sqlite :as sql]
            [rx.box :as box]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(defn create-db-adapter []
  (das/create-db-adapter sql/SQL))

(comment

  (go
    (ks/pp
      (box/anom?
        (<! (box/<conn
              {:box/db-adapter (create-db-adapter)}
              {})))))

  )



