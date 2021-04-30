(ns rx.box.test-helpers
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [rx.box.persist-local :as p]
            [cljs.test :as test
             :refer [deftest is]]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go]]
            [rx.anom :as anom]))

(defonce !SQL (atom nil))

(defn set-sql-obj [so]
  (reset! !SQL so))

(defn sql-obj [so]
  @!SQL)

(defn test-schema-db-props []
  {:box/local-db-name (str "file:"
                           (ks/uuid)
                           "?mode=memory")
   :box/local-data-table-name "test_data_table_6"
   :box/local-refs-table-name "test_refs_table_6"})

(defn gen-schema [overrides]
  (merge
    (test-schema-db-props)
    overrides))

(defn with-test-conn [schema <f]
  (let [schema (gen-schema schema)]
    (test/async done
      (go
        (try
          (let [conn (<? (p/<conn @!SQL schema))
                #__ #_(<? (p/<recreate-sql-tables conn))
                res (<? (<f conn))]
            (done))
          (catch js/Error e
            (is false (str "Error running test: " e))
            (done)))))))

#_(defn with-conn [SQL schema-override <f]
  (fn []
    (go-try
      (let [conn (<? (p/<conn SQL
                       (gen-schema schema-override)))]
        (<? (<f conn))))))

(defn with-conn [conn-opts schema-override <f]
  (go
    (try
      (let [schema (gen-schema schema-override)

            conn (<! (p/<conn
                       conn-opts
                       schema))

            _ (<? (p/<destroy-db
                    (:box/db-adapter conn-opts)
                    schema))
            res (<? (<f conn))]
        res)
      (catch js/Error e e))))

(<defn <test-conn [conn-opts & [schema-override initial-data]]
  (let [schema (gen-schema (merge
                             (:box/schema conn-opts)
                             schema-override))

        conn (<? (p/<conn
                   conn-opts
                   schema))]
    
    (when initial-data
      (<? (p/<transact
            conn
            initial-data)))
    
    conn))


