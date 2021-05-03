(ns rx.node.sqlite
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :as async
             :refer [chan <! close! put! timeout]
             :refer-macros [go]]))

(def SQLite (.verbose (js/require "sqlite3")))

(defn decode-rows [rows]
  (js->clj rows))

(defn <raw-exec [db sql-str]
  (let [ch (chan)]
    (.exec db sql-str
      (fn [err]
        (when err
          (put! ch (anom/from-err err)))
        (close! ch)))
    ch))

(defn <njs-exec [db sql-vec]
  (let [ch (chan)]
    (try
      (.all db
        (first sql-vec)
        (clj->js (rest sql-vec))
        (fn [err rows]
          (put! ch
            (cond
              err (anom/from-err err)
              (anom/anom? rows) rows
              :else (decode-rows rows)))
          (close! ch)))
      (catch js/Error e
        (put! ch (anom/from-err e))
        (close! ch)))
    ch))

(defn <njs-batch-exec [db sql-vecs]
  (let [ch (chan)]
    (.serialize db
      (fn []
        (go
          (<! (<raw-exec db "begin transaction"))
          (try
            (let [{:keys [outs errs]}
                  (loop [vs sql-vecs
                         outs []
                         errs []]
                    (if (empty? vs)
                      {:outs outs
                       :errs errs}
                      (let [v (first vs)
                            res (<! (<njs-exec db v))

                            new-outs (if (anom/anom? res)
                                       outs
                                       (conj outs res))
                            new-errs (if (anom/anom? res)
                                       (conj errs res)
                                       errs)]
                        (recur
                          (rest vs)
                          new-outs
                          new-errs))))]
              
              (if (empty? errs)
                (do
                  (<! (<raw-exec db "commit"))
                  (put! ch (doall (reduce into outs))))
                (do
                  (ks/pn "ROLLBACK")
                  (<! (<raw-exec db "rollback"))
                  (put! ch (first errs))))
              (close! ch))
            (catch js/Error err
              (put! ch (anom/from-err err))
              (close! ch))))))
    ch))

(defn -<open-db [{:keys [name version display-name size]
                 :or {version "0.0.1"
                      display-name name
                      size (* 1024 1024)}
                 :as db-spec}]
  (let [ch (chan)]
    (let [!db (atom nil)]
      (reset! !db
        (SQLite.Database.
          name
          (fn [err]
            (when err
              (prn "ERR" err))
            (when-not err
              (put! ch @!db))
            (close! ch)))))
    ch))

(defn <run-exec-payload [[type db sql-vec out-ch :as pl]]
  (go
    (<! (timeout 1))
    (let [out-ch (or out-ch (last pl))]
      (try
        (let [res (if (keyword? (first pl))
                    (condp = type
                      :exec (<! (<njs-exec db sql-vec))
                      :batch-exec (<! (<njs-batch-exec db sql-vec)))
                    (<! (apply
                          (first pl)
                          (butlast (rest pl)))))]
          (put! out-ch res)
          (close! out-ch))
        (catch js/Error e
          (put! out-ch (anom/from-err e))
          (close! out-ch))))))

(defonce exec-ch (chan 1024))

(defonce exec-loop
  (go
    (loop [pl (<! exec-ch)]
      (<! (<run-exec-payload pl))
      (recur (<! exec-ch)))))

#_(defn <exec [db sql-vec]
  (<njs-exec db sql-vec))

(defn queue-exec [db sql-vec ch]
  (put! exec-ch [:exec db sql-vec ch]))

(defn queue-batch-exec [db sql-vec ch]
  (put! exec-ch [:batch-exec db sql-vec ch]))

(defn <exec [db sql-vec]
  (let [ch (chan)]
    (queue-exec
      db
      sql-vec
      ch)
    ch))

(defn <batch-exec [db sql-vec]
  (let [ch (chan)]
    (queue-batch-exec
      db
      sql-vec
      ch)
    ch))

(defn <open-db [{:keys [name version display-name size]
                 :or {version "0.0.1"
                      display-name name
                      size (* 1024 1024)}
                 :as db-spec}]
  (let [ch (chan)]
    (put! exec-ch [-<open-db db-spec ch])
    ch))

(defn with-test-db [f opts]
  (go
    (try
      (let [db (<! (<open-db (merge {:name "testdb"} opts)))]
        (println)
        (ks/spy
          "RES---"
          (<! (f db))))
      (catch js/Error e
        (.error js/console e)))))

(comment

  

  (with-test-db
    (fn [db]
      (<exec db ["select * from datoms"]))
    {:name "/Users/zk/Desktop/testsqlite3db.db"})

  (with-test-db
    (fn [db]
      (<exec db ["select * from foo"])))

  (with-test-db
    (fn [db]
      (<exec
        db
        ["insert into foo(bar) values(?)" "val1"])))

  (with-test-db
    (fn [db]
      (<exec db ["create table foo (bar TEXT UNIQUE)"])))

  (with-test-db
    (fn [db]
      (<exec
        db
        ["drop table foo"])))

  (go
    (let [db (<! (<open-db {:name "testdb"}))]
      (.all db
        "SELECT * FROM foo"
        nil
        (fn [err res]
          (prn err res)))))



  (go
    (let [db (<! (<open-db {:name "testdb"}))]
      (.all db
        "create table foo (bar TEXT)"
        (fn [err res]
          (prn err res))))))


(def SQL {:<exec <exec
          :<batch-exec <batch-exec
          :<open-db <open-db})

