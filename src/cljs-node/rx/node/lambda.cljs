(ns rx.node.lambda
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer-macros [<?]]
            [clojure.string :as str]
            [goog.object :as gobj]
            [rx.node.test-data :as td]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go go-loop]]))

(defn lambda-println [& xs]
  (.write
    (.-stdout js/process)
    (apply str (concat xs ["\n"]))))

(defn format-headers [pm]
  (->> (get pm "headers")
       (map (fn [[k v]]
              [(str/lower-case k)
               v]))
       (into {})))

(defn parse-query-string [proxy-map]
  (let [qsp (get proxy-map "queryStringParameters")]
    (when (and qsp (not (empty? qsp)))
      (str
        "?"
        (->> qsp
             (map (fn [[k v]]
                    (str (ks/url-encode k)
                         "="
                         (ks/url-encode v))))
             (interpose "&")
             (apply str))))))

(defn parse-request-method [proxy-map]
  (-> proxy-map
      (get-in ["requestContext" "httpMethod"])
      str/lower-case
      keyword))

(defn proxy-map->ring-map [pm]
  (when (get pm "path")
    {:server-port 443
     :server-name (get-in pm ["headers" "Host"])
     :remote-addr (get-in pm ["requestContext"
                              "identity"
                              "sourceIp"])
     :uri (get pm "path")
     :query-string (parse-query-string pm)
     :scheme :https
     :request-method (parse-request-method pm)
     :protocol (get-in pm ["requestContext" "protocol"])
     :headers (format-headers pm)
     :body (get pm "body")}))

(defn lambda-request-obj->ring-request [request-js-obj]
  (-> request-js-obj
      js->clj
      proxy-map->ring-map))

(defn <readable->string [readable]
  (let [ch (chan)]
    (let [!out (atom [])]
      (.on readable
           "data"
           (fn [s]
             (swap! !out conj s)))
      (.on readable
           "end"
           (fn []
             (put! ch (apply str @!out))
             (close! ch))))
    ch))

(defn readable-stream? [o]
  (exists? (.-isPaused o)))

(defn wrap-realize-body [handler]
  (fn [req respond _]
    (handler
      req
      (fn [{:keys [body] :as resp}]
        (go
          (let [body (if (readable-stream? body)
                       (<! (<readable->string body))
                       body)]

            (respond (assoc resp :body body))))))))

(defn ring-response->lambda-response
  [{:keys [status
           headers
           body]}]
  (clj->js
    {:statusCode (or status 200)
     :headers headers
     :body body
     :isBase64Encoded false}))

(defn valid-ring-request? [{:keys [uri request-method]}]
  (and uri
       (string? uri)
       request-method))

(defn env-map []
  (let [o js/process.env
        ks (gobj/getKeys o)]
    (->> ks
         (map (fn [k]
                [k (gobj/get o k)]))
         (into {}))))

(defn async-adapter
  [<handler]
  (when-not <handler (ks/throw-str "<handler cannot be nil"))
  (fn [request-js-obj context-js-obj callback]
    (go
      (try
        (let [ring-request (lambda-request-obj->ring-request request-js-obj)
              ring-request (merge ring-request
                             {:env (env-map)}
                             {:awslambda-context context-js-obj})

              _ (when-not (valid-ring-request? ring-request)
                  (ks/throw-str
                    "Invalid ring request, lambda request object: "
                    (js->clj request-js-obj)))
              
              ring-resp (<? (<handler ring-request))
              lambda-resp (ring-response->lambda-response ring-resp)]

          (callback nil lambda-resp))

        (catch js/Error e
          (println
            "Exception"
            {:stacktrace (.-stack e)
             :request-edn (pr-str request-js-obj)
             :context-edn (pr-str context-js-obj)})

          (lambda-println
            (ks/to-json
              {:request request-js-obj
               :error-message (str e)
               :error-stacktrace (.-stack e)}))
          
          (callback e))))))

(defn chan? [x]
  (instance? cljs.core.async.impl.channels.ManyToManyChannel x))

(defn exports [exps]
  (gobj/set js/module "exports" (clj->js exps)))

(defn export-handler [f]
  (exports {:handler f}))

(defn export-ring-handler [<handler]
  (export-handler
    (async-adapter <handler)))

(defn test-exports [handler-key & [req ctx env]]
  (let [req (or req td/blank-request)
        ctx (or ctx {:context "dummy-context"})
        exports (gobj/get js/module "exports")]
    (println "Exports:" exports)
    (if-let [handler (aget exports (name handler-key))]
      (do
        (println "Handler:" handler)
        (prn "ENV" env)
        (when env
          (doseq [[k v] env]
            (prn "setting" k v)
            (gobj/set js/process.env k v)))
        (handler
          (clj->js req)
          (clj->js ctx)
          (fn [err resp]
            (println "Err/Resp:")
            (println "vvvvvvv ERR")
            (ks/pp err)
            (println "vvvvvvv RESP")
            (ks/pp resp))))
      (println "Handler not found"))))

#_(comment
    (require 'cljs.repl.node)
    (cider.piggieback/cljs-repl (cljs.repl.node/repl-env)))






