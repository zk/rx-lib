(ns rx.sqlite
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.rn-sqlite :as rnsql]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :as async
             :refer [chan <! close! put!]
             :refer-macros [go]]))

(def RN? nil)
(def SQR nil)
(def SQN nil)

(defn <open-db [{:keys [name version display-name size]
                 :or {version "0.0.1"
                      display-name name
                      size (* 1024 1024)}
                 :as db-spec}]
  (let [ch (chan)]
    (if SQR
      (do
        (put! ch
          (.openDatabase
            SQR
            name
            version
            display-name
            size))
        (close! ch))
      (let [!db (atom nil)]
        (reset! !db
          (SQN.Database.
            name
            (fn [err]
              (when-not err
                (put! ch @!db))
              (close! ch))))))
    ch))

(defn decode-rows [rows]
  (js->clj rows))

(defn <njs-exec [db sql-vec]
  (let [ch (chan)]
    (.all db
      (first sql-vec)
      (clj->js (rest sql-vec))
      (fn [err rows]
        (put! ch
          (merge
            (when rows
              {:rx.res/data (decode-rows rows)})
            (when err
              {:rx.res/anom (res/err->res err)})))
        (close! ch)))
    ch))

(defn <exec [db sql-vec]
  (if RN?
    (rnsql/<exec db sql-vec)
    (<njs-exec db sql-vec)))

(defn <njs-batch-exec [db sql-vecs]
  (let [ch (chan)]
    (.serialize db
      (fn []
        (go
          (.run db "begin transaction")
          (try
            (let [{:keys [outs errs]}
                  (loop [vs sql-vecs
                         outs []
                         errs []]
                    (if (empty? vs)
                      {:outs outs
                       :errs errs}
                      (let [v (first vs)
                            {:keys [:rx.res/data
                                    :rx.res/anom]}
                            (<! (<exec db v))

                            new-outs (if data
                                       (conj outs data)
                                       data)
                            new-errs (if anom
                                       (conj errs anom)
                                       errs)]
                        (recur
                          (rest vs)
                          new-outs
                          new-errs))))]
              (if (empty? errs)
                (do
                  (.run db "commit")
                  (put! ch {:rx.res/data (reduce into outs)}))
                (do
                  (.run db "rollback")
                  (put! ch {:rx.res/anom (first errs)})))
              (close! ch))
            (catch js/Error err
              (put! ch {:rx.res/anom (res/err->res err)})
              (close! ch))))))
    ch))

(defn <batch-exec [db sql-vec]
  (if RN?
    (rnsql/<batch-exec db sql-vec)
    (<njs-batch-exec db sql-vec)))

(defn with-test-db [f]
  (go
    (let [db (<! (<open-db {:name "testdb"}))]
      (ks/spy
        "RES---"
        (<! (f db))))))

(defn data [m] (:rx.res/data m))
(defn anom [m] (:rx.res/anom m))


(comment

  (with-test-db
    (fn [db]
      (<batch-exec db [["insert into foo(bar) values(?)" "val3"]
                       ["select * from foo"]
                       ["select * from foo"]])))

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
      (prn (<! ))))


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
          (prn err res)))))

  )
