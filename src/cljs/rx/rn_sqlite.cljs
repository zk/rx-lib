(ns rx.rn-sqlite
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into columns values]]
            [goog.object :as gobj]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(goog-define ^boolean REACT_NATIVE false)

(def SQLite
  (when REACT_NATIVE
    #_(js/require "react-native-sqlite-2")))

(defn open-db [{:keys [name version display-name size]
                :or {version "0.0.1"
                     display-name name
                     size (* 1024 1024)}
                :as db-spec}]
  (.openDatabase
    SQLite
    name
    version
    display-name
    size))

(defn rs->clj [o]
  (when o
    {:insert-id (.-insertId o)
     :rows-affected (.-rowsAffected o)
     :rows (when (and o (> (.. o -rows -length) 0))
             (for [i (range (.. o -rows -length))]
               (js->clj (.item (.-rows o) i) :keywordize-keys false)))}))

(defn rs->rows [o]
  (when o
    (when (and o (> (.. o -rows -length) 0))
      (for [i (range (.. o -rows -length))]
        (js->clj (.item (.-rows o) i) :keywordize-keys false)))))

(defn err->clj [err]
  (when err
    {:message (.-message err)}))

(defonce !exec-queue-running? (atom nil))

(defonce !exec-queue
  (atom #queue []))

(defn -<exec [db sql-vec]
  (let [id nil]
    #_(prn "<exec" id)
    (when-not db
      (ks/throw-str "vee.sqlite/<exec db can't be nil"))
    (let [ch (chan)]
      (.transaction
        db
        (fn [tx]
          (.executeSql
            tx
            (clj->js (first sql-vec))
            (clj->js (rest sql-vec))
            (fn [tx results]
              #_(prn "RES" sql-vec)
              #_(.log js/console results)

              (put! ch {:rx.res/data (rs->rows results)})
              (close! ch)
              #_(prn "<exec ok" id))
            (fn [tx err]
              (prn ">sqlite err" err)
              (put! ch (res/err->res err))
              (close! ch)
              #_(prn "<exec failed" id)))))
      ch)))

(defn <exec [db sql-vec]
  (when-not db
    (ks/throw-str "vee.sqlite/<exec db can't be nil"))
  (let [ch (chan)]
    (go
      (swap! !exec-queue
        conj
        [db sql-vec ch])
      (when-not @!exec-queue-running?
        (reset! !exec-queue-running? true)
        (loop [tuple (first @!exec-queue)
               i 0]
          (let [[db sql-vec ch] tuple]
            (when tuple
              (swap! !exec-queue pop)
              (put! ch (<! (-<exec db sql-vec)))
              (close! ch)
              (recur
                (first @!exec-queue)
                (inc i)))
            #_(prn "> <exec ran" i)))
        (reset! !exec-queue-running? false)
        #_(prn "done exec queue")))
    ch))

(defonce !batch-exec-queue-running? (atom nil))
(defonce !batch-exec-queue (atom #queue []))

(defn -<batch-exec [db sql-vecs]
  (let [id (gensym)]
    #_(prn "> -<batch-exec" id)
    (when-not db
      (ks/throw-str "vee.sqlite/<batch-exec db can't be nil"))
    (when-not (empty? (filter nil? sql-vecs))
      (ks/throw-str "vee.sqlite/<batch-exec: statements input contained null statement"))
    (when (empty? sql-vecs)
      (ks/throw-str "vee.sqlite/<batch-exec: statements empty"))
    (let [ch (chan)]
      (.transaction
        db
        (fn [tx]
          (go
            (let [first-sql-vecs (butlast sql-vecs)
                  last-sql-vec (last sql-vecs)]
              (doseq [sql-vec first-sql-vecs]
                (.executeSql
                  tx
                  (clj->js (first sql-vec))
                  (clj->js (rest sql-vec))
                  (fn [tx results]
                    #_(prn "SUB SUCCESS"))
                  (fn [tx err]
                    (prn "SQL ERR" err)
                    (put! ch (res/err->res err))
                    (close! ch)
                    (prn "> <batch-exec failed early" id))))
              (.executeSql
                tx
                (clj->js (first last-sql-vec))
                (clj->js (rest last-sql-vec))
                (fn [tx results]
                  (put! ch {:rx.res/data (rs->rows results)})
                  (close! ch)
                  #_(prn "> <batch-exec ok" id))
                (fn [tx err]
                  (prn ">sqlite err" err)
                  (put! ch (res/err->res err))
                  (close! ch)
                  (prn "> <batch-exec failed last" id)))))))
      ch)))

#_(reset! !batch-exec-queue-running? false)

(defn <batch-exec [db sql-vecs]
  #_(when-not db
      (ks/throw-str "vee.sqlite/<exec db can't be nil"))
  (let [ch (chan)
        start-ts (system-time)]
    (go
      (swap! !batch-exec-queue
        conj
        [db sql-vecs ch start-ts])
      (when-not @!batch-exec-queue-running?
        (reset! !batch-exec-queue-running? true)
        (loop [tuple (first @!batch-exec-queue)
               i 0]
          (let [[db sql-vecs ch start-ts] tuple]
            (when tuple
              (swap! !batch-exec-queue pop)
              (try
                (put! ch (<! (-<batch-exec db sql-vecs)))
                (catch js/Error e
                  (put! ch [nil e])))
              (close! ch)
              #_(prn ">" (- (system-time) start-ts) "ms")
              (recur
                (first @!batch-exec-queue)
                (inc i)))
            #_(prn "> <exec-batch ran" i)))
        (reset! !batch-exec-queue-running? false)))
    ch))

(defn <select [db query-map]
  (<exec db (sql/format
              (merge query-map))))

#_(defn <close [db]
    (.log js/console db)
    (let [ch (chan)]
      (.close db
        (fn [& args]
          (prn "close" args)
          (put! ch ["ok"]))
        (fn [err]
          (put! ch [nil err])))
      ch))

(defn <drop-table [db name]
  (<exec
    db
    [(str "drop table " name)]))

(comment

  (go
    (prn (<! (<drop-table
               (open-db {:name "test"})
               "foo"))))

  (go


    #_(.log js/console
        (first (<! (<exec
                     ["create table if not exists foo(text TEXT, ts INT)"]
                     (<! (<open-db "test" "1.0" "test-db" (* 1024 1024)))))))


    #_(.log js/console
        (first (<! (<exec
                     ["select * from foo"]
                     (<! (<open-db "test" "1.0" "test-db" (* 1024 1024))))))
        0))


  (go
    (let [db (open-db {:name "test"
                       :version "1.0"
                       :display-name "test-db"
                       :size (* 1024 1024)})]
      (ks/pp
        (time
          (<! (<batch-exec
                db
                [["drop table foo"]
                 ["create table if not exists foo(text TEXT, userid TEXT, ts INT)"]
                 #_["create index if not exists foo_idx on foo (ts DESC,userid)"]
                 ["select * from foo order by ts desc limit 10 offset 0 "]]))))))



  (go
    (let [db (open-db {:name "test"
                       :version "1.0"
                       :display-name "test-db"
                       :size (* 1024 1024)})]
      (ks/pp
        (time
          (<! (<batch-exec
                db
                [["select * from foo where userid='user1' order by ts desc limit 10 offset 200"]]))))))


  (go

    (let [db (open-db {:name "test"
                       :version "1.0"
                       :display-name "test-db"
                       :size (* 1024 1024)})]


      (prn
        (<! (<exec
              db
              (sql/format
                (-> (insert-into :foo)
                    (columns :text :userid :ts)
                    (values
                      (concat
                        (->> (range 100)
                             (mapcat
                               (fn [i]
                                 (->> (range 1000)
                                      (map (fn [j]
                                             [(str "foo" i "-" j)
                                              (str "user" i)
                                              (+ (ks/now)
                                                 (+ (* i 1000) j))])))))))))))))))


  (ks/pp
    (-> (insert-into :foo)
        (columns :text :userid :ts)
        (values
          (concat
            (->> (range 2)
                 (mapcat
                   (fn [i]
                     (->> (range 2)
                          (map (fn [j]
                                 [(str "foo" i "-" j)
                                  (str "user" i)
                                  (+ (ks/now)
                                     (+ (* i 1000) j))]))))))))))



  (sql/format
    (-> (honeysql.helpers/from :foo)
        (honeysql.helpers/where
          [:and
           [:> "appt/start-ts" "bar"]]))
    :quoting :ansi)

  (-> (honeysql.helpers/from :foo)
      (honeysql.helpers/where
        [:and
         [:> "appt/start-ts" "bar"]])))
