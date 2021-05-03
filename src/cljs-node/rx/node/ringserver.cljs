(ns rx.node.ringserver
  (:require [rx.kitchen-sink :as ks
             :refer-macros [<&]]
            [cljs.nodejs :as nodejs]
            [rx.node :as node]
            [rx.anom :as anom
             :refer-macros [gol <? <defn]]
            [clojure.string :as str]
            [goog.object :as gobj]

            [httpurr.client :as hc]
            [httpurr.client.node :as hcn]
            [lambdaisland.uri :as luri]
            [clojure.core.async
             :as async
             :refer [chan put! close!]]))

(def HTTP (nodejs/require "http"))
(def URL (nodejs/require "url"))

(defn header-key->name [k]
  (cond
    (keyword? k) (name k)
    (string? k) k
    :else (str k)))

(defn http-respond [r ring]
  (let [{:keys [status
                body
                headers]
         :or {status 200}}
        ring]
    (when headers
      (doseq [[k v] headers]
        (.setHeader r (header-key->name k) (clj->js v))))
    (.writeHead r status)
    (.end r body)))

(defn error->resp [e]
  {:status 500
   :body
   #_(->> [(.-name e)
           (.-message e)
           (.-fileName e)
           (.-lineNumber e)
           (.-columnNumber e)
           (.-stack e)]
          (map #(str "[" % "]"))
          (interpose "\n\n")
          (apply str))
   (.-stack e)})

(defn anom->resp [a]
  {:status 500
   :body (ks/pp a)})

(defn req-obj->scheme [ro]
  (cond
    (.. ro -connection -encrypted)
    :https
    
    (= (gobj/get (.-headers ro) "x-forwarded-proto") "https")
    :https
    
    :else :http))

(defn socket->ring [s]
  (let [addy (.address s)]
    {:server-port (.-port addy)
     :remote-addr (.-address addy)
     :server-name (.-address addy)
     :addy addy}))

(defn req-obj->ring [ro]
  (let [{:keys [server-port] :as socket-parts}
        (socket->ring (.. ro -connection))
        uri (luri/uri (.-url ro))]
    (ks/spy uri)
    (merge
      {:host (.. ro -headers -host)
       :request-method (keyword (str/lower-case (.-method ro)))
       :uri (:path uri)
       :query-string (:query uri)
       :scheme (req-obj->scheme ro)
       :headers (js->clj (.-headers ro) :keywordize-keys false)
       :protocol (str "HTTP/" (.-httpVersion ro))}
      socket-parts)))

(defn <read-body [req]
  (let [ch (chan)
        !data (atom nil)]
    (.on req "data"
      (fn [chunk]
        (swap! !data str chunk)))
    (.on req "end"
      (fn []
        (when @!data
          (put! ch @!data))
        (close! ch)))
    (.on req "error"
      (fn [err]
        (put! ch (anom/from-err err))
        (close! ch)))
    ch))

(defn wrap-request [f]
  (fn [req res]
    (gol
      (try
        (let [body (<? (<read-body req))
              req-clj (merge
                        (req-obj->ring req)
                        {:body body})
              ring-resp (<& (f req-clj))]
          (cond
            (ks/error? ring-resp)
            (http-respond res (error->resp ring-resp))
            (anom/? ring-resp)
            (http-respond res (anom->resp ring-resp))
            :else
            (http-respond res ring-resp)))
        (catch js/Error e
          (http-respond res (error->resp e)))))))

(defn start [{:keys [port handler]
              :or {port 5000}
              :as spec}]
  (try
    (println "[ringserver] Starting server" (pr-str spec))
    (let [server (.createServer HTTP (wrap-request handler))]
      (.listen server port)
      server)
    (catch js/Error e
      (prn "ERRR")
      (anom/from-err e))))

(defn stop [server]
  (println "[ringserver] Stopping server" server)
  (when server
    (.close server)))

(defn listener [req res]
  (.writeHead res 200)
  (.end res "hello world"))

(defonce !test-server (atom nil))

(defn test-server []
  (when @!test-server
    (stop @!test-server))
  (reset! !test-server
    (start {:port 5000
            :handler
            (fn [req]
              {:status 200
               :headers {:content-type "application/json"}
               :body (ks/pp-str req)})})))

(comment

  (test-server)

  (set! *print-namespace-maps* false)

  (gol
    (test-server)
    (let [res (<? (ks/<promise
                    (hc/post
                      hcn/client
                      "http://localhost:5000/foo?bar"
                      {:body "hello world"
                       :headers {"foo" ["bar" "baz"]}})))]
      (if (:body res)
        (println (str (:body res)))
        (ks/pp res))))

  )

