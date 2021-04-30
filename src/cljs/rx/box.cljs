(ns rx.box
  (:require [rx.kitchen-sink :as ks]
            [rx.box.persist-local :as pl]
            [rx.box.query-local :as ql]
            [rx.box.auth :as auth]
            [rx.box.sync-client :as sync-client]
            [rx.anom :as anom]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

#_(when (exists? js/window)
  (set! js/XMLHttpRequest (js/require "xhr2")))

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

(defn <sync [conn txfs auth & [opts]]
  (sync-client/<sync conn txfs auth opts))

(defn <sync-mark [conn txfs auth & [opts]]
  (sync-client/<sync-mark conn txfs auth opts))

(defn <sync-imd [conn txfs auth & [opts]]
  (sync-client/<sync-imd conn txfs auth opts))

(defn <query [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<query conn query opts))

(defn <count [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<count conn query opts))

(defn <recreate-refs-table
  [{:keys [:box.pl/sqlite-db
           :box.pl/SQL
           :box/schema]
    :as conn}]
  #_(pl/<recreate-refs-table conn))

(defn <referenced-by [conn
                      ident
                      from-ref-key
                      & [{:keys [pull-keys
                                 honeysql]
                          :as opts}]]
  (ql/<referenced-by conn ident from-ref-key opts))

(defn col [k]
  (ql/col k))

(defn <conn [& args]
  (apply pl/<conn args))

(defn <refresh-auth [conn auth]
  (auth/<refresh-auth conn auth))

(defn <reindex-ident-key [conn ident-key]
  (go
    (let [entities (<! (<query conn
                         {:where [:and
                                  [:ident-key ident-key]]}))]
      (if (anom/? entities)
        entities
        (do
          (ks/pn "Reindexing" (count entities) ident-key "entities")
          (<! (<transact conn entities)))))))

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

  (ks/pn "hi"))


;; TODO -- bug where providing pull keys that don't exists throws exeptio

#_ (<! (box/<query
              conn
              {:where
               [:and
                [:ident-key ::list-tpl-id]]
               :sort [[::title :asc]]}
              {:pull-keys
                 [{::list-items [{::list-items [::list-items]}]}]}))


(comment

  


  )
