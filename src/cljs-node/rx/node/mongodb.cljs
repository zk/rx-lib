(ns rx.node.mongodb
  (:require [rx.kitchen-sink :as ks]
            [goog.object :as gobj]
            [cljs.core.async
             :refer [<! chan put!]
             :refer-macros [go go-loop]]))

(def MDB (js/require "mongodb"))

(def MC (.-MongoClient MDB))

(def Cursor (.-Cursor MDB))

(def ObjectID (.-ObjectID MDB))

(defn <client [{:keys [url]}]
  (let [ch (chan)]
    (.connect MC
      url
      #js {:useNewUrlParser true}
      (fn [err client]
        (put! ch [client err])))
    ch))

(defn get-db [client db-name]
  (when-not client
    (ks/throw-str "get-db client nil"))
  (when-not db-name
    (ks/throw-str "get-db db-name nil"))
  (.db client db-name))

(defn cursor-result? [o]
  (instance? Cursor o))

(defn command-result? [o]
  (and
    (instance? js/Object o)
    (gobj/containsKey o "result")))

(defn collection-op [db op-str-or-kw coll-name-str-or-kw args]
  (let [ch (chan)
        coll-name (name coll-name-str-or-kw)
        coll (.collection db coll-name)
        op-str (name op-str-or-kw)
        js-args (map clj->js args)

        invoke-result (apply js-invoke coll op-str js-args)]

    (if (cursor-result? invoke-result)
      (.catch
        (.then
          (.toArray invoke-result)
          (fn [docs]
            (put! ch [(-> docs
                          (js->clj :keywordize-keys true))])))
        (fn [err]
          (put! ch [nil err])))
      (.catch
        (.then
          invoke-result
          (fn [res]
            (let [res
                  (cond
                    (command-result? res) (.-result res)
                    :else res)]
              (put! ch [(js->clj res :keywordize-keys true)]))))
        (fn [err]
          (put! ch [nil err]))))
    ch))

(defn <coll-op [db-config [op-str coll-name & args]]
  (when-not (and (:url db-config) (:db db-config))
    (ks/throw-str "DB config missing :url or :db"))
  (go
    (let [[client err] (<! (<client db-config))]
      (if err
        [nil err]
        (let [db-name (name (:db db-config))
              db (get-db client db-name)
              res (<! (collection-op
                        db
                        op-str
                        coll-name
                        args))]
          (.close client)
          res)))))

(defn coll-handler [op-str]
  (fn [db coll-name & args]
    (collection-op db
      op-str
      coll-name
      args)))

(def <insert-one (coll-handler "insertOne"))

(def <insert-many (coll-handler "insertMany"))

(def <update-one (coll-handler "updateOne"))

(def <update-many (coll-handler "updateMany"))

(def <count-documents (coll-handler "countDocuments"))

(def <find-one (coll-handler "findOne"))

(def <indexes (coll-handler "indexes"))

(defn <find [db-or-config coll-name query & [opts]]
  (let [ch (chan)]
    (go
      (try
        (let [client (when (map? db-or-config)
                       (let [[client err] (<! (<client db-or-config))]
                         (if err
                           (throw err)
                           client)))
              db (if (map? db-or-config)
                   (get-db client (:db db-or-config))
                   db-or-config)
              coll (.collection db coll-name)
              {:keys [projection]} opts
              cursor (.find coll
                       (clj->js query)
                       (clj->js projection))
              cursor-opts (dissoc opts :projection)]
          (doseq [[k v] cursor-opts]
            (let [fn-name (name k)
                  arg (clj->js v)]
              (js-invoke cursor fn-name arg)))
          (.catch
            (.then
              (.toArray cursor)
              (fn [docs]
                (put! ch [(-> docs
                              (js->clj :keywordize-keys true))])
                (when client
                  (.close client))))
            (fn [err]
              (put! ch [nil err])
              (when client
                (.close client)))))
        (catch js/Error e
          (put! ch [nil e]))))
    ch))


#_(go
  (prn (<! (<find
             #_{:url "mongodb://localhost:27017"
                :db "nalopastures"}
             {:url "mongodb://naloroot:KV9MFzPcuwcmLxvBhxezp93N2FNmr@35.160.38.45:44301/admin"
              :db "nalopastures"}
             "testcoll"

             {}))))

#_(defn <insert-one [db coll-name obj]
  (when-not obj
    (ks/throw-str "Insert object cannot be nil"))
  (let [ch (chan)
        coll (.collection db coll-name)]
    (.catch
      (.then
        (.insertOne coll (clj->js obj))
        (fn [res]
          (put! ch [(js->clj (.toJSON res)
                      :keywordize-keys true)])))
      (fn [err]
        (put! ch [nil err])))
    ch))

(defn <test-command [desc f]
  (go
    (let [[client] (<! (<client
                         {:url "mongodb://localhost:27017"
                          :db "rxtest"}))
          db (get-db client "rxtest")]
      (println "vvvv" desc)
      (try
        (ks/pp (<! (f db)))
        (catch js/Error e
          (println " !!!!! Caught exception")
          (throw e))))))

#_(<test-command
  "Insert One"
  #(<insert-one
     %
     "testcoll"
     {:foo "bar"}))

#_(go
  (prn (<! (<coll-op
             {:url "mongodb://localhost:27017"
              :db "rxtest"}
             ["insertOne"
              "testcoll"
              {:foo "bar"
               :ts (ks/now)}]))))

#_(go
    (ks/pp (<! (<coll-op
                 {:url "mongodb://localhost:27017"
                  :name "rxtest"}
                 ["find" "testcoll" {:foo "bar"}]))))

#_(go
  (ks/pp
    (<!
      (<coll-op
        {:url "mongodb://localhost:27017"
         :db "rxtest"}
        ["update"
         "testcoll"
         {:id "foo"}
         {:id "foo" :foo "bar"}
         {:upsert true}]))))

#_(<test-command
    "Insert One Nil"
    #(<insert-one
       %
       "testcoll"
       nil))

#_(<test-command
  "Insert Many"
  #(<insert-many
     %
     "testcoll"
     [{:foo "bar"} {:baz "bap"}]))

#_(<test-command
  "Update One"
  #(<update-one
     %
     "testcoll"
     {:foo "bar"}
     {:$set {:baz "bap"}}))

#_(<test-command
    "Find"
    #(<find-one
       %
       "testcoll"
       {:foo {:$eq "bar"}}))

#_(<test-command
    "Count Documents"
    #(<count-documents
       %
       "testcoll"
       {:foo {:$eq "bar"}}))

#_(<test-command
    "Indexes"
    #(<count-documents
       %
       "indexes"))

#_(<test-command
  "Find"
  #(<find
     %
     "testcoll"
     {}
     {:limit 3
      :skip 1}))

(deftype ObjectIDHandler []
  Object
  (tag [this v] "mongodb.objectid")
  (rep [this v] (str v))
  (stringRep [this v] (str v)))


(def to-transit-handlers {ObjectID (ObjectIDHandler.)})

#_(ks/to-transit {:foo (ObjectID.)}
  {:handlers to-transit-handlers})
