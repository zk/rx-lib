(ns rx.browser.box
  (:require [rx.kitchen-sink :as ks]
            [rx.box.persist-local :as pl]
            [rx.box.query-local :as ql]
            [rx.box.sync-client :as sync-client]
            [rx.box.db-adapter-dexie :as db-adapter-dexie]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(when-not (exists? js/window)
  (set! js/XMLHttpRequest (js/require "xhr2")))

(defn <conn [schema]
  (pl/<conn {:box/db-adapter
             (db-adapter-dexie/create-db-adapter)}
    schema))

(defn <transact
  [conn txfs
   & [{:keys [debug-log?
              skip-entity-specs?]
       :as opts}]]
  (pl/<transact conn txfs opts))

(defn <pull [conn ident-or-ident-val & [deref-spec opts]]
  (pl/<pull conn ident-or-ident-val deref-spec opts))

(defn <pull-multi [conn ident-or-ident-val & [deref-spec opts]]
  (pl/<pull-multi conn ident-or-ident-val deref-spec opts))

(defn has-failures? [res] (sync-client/has-failures? res))

(defn <sync-entities [conn ents auth]
  [conn ents auth]
  (sync-client/<sync-entities conn ents auth))

(defn <query [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<query conn query opts))

#_(defn <count [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<count conn query opts))

(defn <recreate-refs-table
  [{:keys [:box.pl/sqlite-db
           :box.pl/SQL
           :box/schema]
    :as conn}]
  #_(pl/<recreate-refs-table conn))

#_(defn <referenced-by [conn
                      ident
                      from-ref-key
                      & [{:keys [pull-keys
                                 honeysql]
                          :as opts}]]
  (ql/<referenced-by conn ident from-ref-key opts))

(defn col [k]
  (ql/col k))

(comment

  (ks/<pp
    (pl/<conn
      (sql/gen-sql)
      {:box/local-db-name "test_db_name"
       :box/local-data-table-name "box_data"
       :box/local-refs-table-name "box_refs"}))

  (go
    (pl/<show-local-tables
      (<! (pl/<conn
            (sql/gen-sql)
            {:box/local-db-name "test_db_name"
             :box/local-data-table-name "box_data"
             :box/local-refs-table-name "box_refs"}))))

  (ks/pn "hi")

  
  )

