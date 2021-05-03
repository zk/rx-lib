(ns rx.node.entry
  (:require [rx.kitchen-sink :as ks]
            [rx.http :as http]
            [rx.css :as rcss]
            #_[macchiato.middleware.resource :refer [wrap-resource]]
            #_[macchiato.middleware.content-type :refer [wrap-content-type]]
            #_[hiccups.runtime :as rt]
            #_[macchiato.fs.path :as path]
            #_[macchiato.fs :as fs]
            [clojure.string :as str]
            #_[nsfw.gi :as gi]
            [httpurr.client :as hc]
            [httpurr.client.node :refer [client]]
            [rx.node.awsclient :as ac]
            [rx.node.mongodb :as mdb]
            [rx.node.devbus :as devbus]
            [cljs-node-io.core :as io]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go go-loop]]))

(def FS (js/require "fs"))

(defn read-file-sync [path & [opts]]
  (.readFileSync
    FS
    path
    opts))

(defn <http-get [url & [opts]]
  (let [ch (chan)]
    (.catch
      (.then
        (hc/send!
          client
          (merge
            {:method :get
             :url url}
            opts))
        (fn [resp]
          (put! ch [resp])))
      (fn [err]
        (put! ch [nil err])))
    ch))

(def ga-tracking-id "foo")

(def google-analytics
  (let [src (str
              "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', '" ga-tracking-id "', 'auto');
  ga('send', 'pageview');")]
    {:head-js [[:script {:type "text/javascript"} src]]
     :csp [{:script-src ["https://www.google-analytics.com"]}]}))

#_["var CLOSURE_UNCOMPILED_DEFINES = {};"
   "var CLOSURE_NO_DEPS = true;"
   "if(typeof goog == \"undefined\") document.write('<script src=\"cljs/goog/base.js\"></script>');"
   "document.write('<script src=\"cljs/goog/deps.js\"></script>');"
   "document.write('<script src=\"cljs/cljs_deps.js\"></script>');"
   "document.write('<script>if (typeof goog == \"undefined\") console.warn(\"ClojureScript could not load :main, did you forget to specify :asset-path?\");</script>');"
   "document.write('<script>goog.require(\"figwheel.connect\");</script>');"
   "document.write('<script>goog.require(\"process.env\");</script>');"
   "document.write('<script>goog.require(\"main.browser\");</script>');"
   "document.write(\"<script>figwheel.connect.start();</script>\");"]

(def figwheel
  {:csp
   [{:script-src
     (vec
       (concat
         ["'unsafe-eval'"]
         (->> ["if (typeof goog == \"undefined\") console.warn(\"ClojureScript could not load :main, did you forget to specify :asset-path?\");"
               "goog.require(\"figwheel.connect\");"
               "goog.require(\"process.env\");"
               "figwheel.connect.start();"
               "goog.require(\"main.browserfigwheel\");"]
              (map (fn [s]
                     (str "'sha512-"
                          (ks/to-base64-str (ks/sha512-bytes s))
                          "'"))))
         ["ws://localhost:3449"
          "'sha256-RnIrup63MTO631n1+K9Ycir54hhC/4FwOnKAV2wNu0M='"]))
     :frame-ancestors ["'none'"]}]})

(def mapbox
  {:csp
   [{:child-src ["blob: " "data: "]
     :img-src ["'self'" "blob: " "data: "]
     :worker-src ["'self'" "blob:"]
     :connect-src ["https://*.tiles.mapbox.com" "https://api.mapbox.com"]
     :script-src ["'unsafe-eval'"]}]
   :css ["https://api.tiles.mapbox.com/mapbox-gl-js/v0.44.2/mapbox-gl.css"]})

(def google-maps
  {:csp [{:script-src ["https://maps.googleapis.com"]}]
   :head-js [(str "https://maps.googleapis.com/maps/api/js?key=AIzaSyD4UK2xtTJozwVOZ7GyZrDjaQlCRTa3VpI")]})

(def youtube
  {:csp [{:script-src ["https://www.youtube.com" "https://s.ytimg.com"]
          :frame-src ["https://www.youtube.com"]}]})

(def SIGS {})

(def app-assets
  {:body-js [(str "/cljs/app.js?" (SIGS :appjs))]
   :head-css ["https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css"
              (str "/css/app.css?" (SIGS :appcss))]
   :meta-names [{:viewport "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"}]
   :head (->> rcss/font-hrefs
              (map
                (fn [href]
                  [:link {:href href
                          :rel :stylesheet}])))})

(def favicons
  ;; https://www.favicon-generator.org/
  {:head
   [[:link {:rel "apple-touch-icon" :sizes "57x57" :href "/apple-icon-57x57.png"}]
    [:link {:rel "apple-touch-icon" :sizes "60x60" :href "/apple-icon-60x60.png"}]
    [:link {:rel "apple-touch-icon" :sizes "72x72" :href "/apple-icon-72x72.png"}]
    [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/apple-icon-76x76.png"}]
    [:link {:rel "apple-touch-icon" :sizes "114x114" :href "/apple-icon-114x114.png"}]
    [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/apple-icon-120x120.png"}]
    [:link {:rel "apple-touch-icon" :sizes "144x144" :href "/apple-icon-144x144.png"}]
    [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/apple-icon-152x152.png"}]
    [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/apple-icon-180x180.png"}]

    [:link {:rel "icon" :type "image/png" :sizes "192x192" :href "/android-icon-192x192.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "32x32" :href "/android-icon-32x32.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "96x96" :href "/android-icon-96x96.png"}]
    [:link {:rel "icon" :type "image/png" :sizes "16x16" :href "/android-icon-16x16.png"}]

    [:meta {:name "msapplication-TileColor" :content "#ffffff"}]
    [:meta {:name "msapplication-TileImage" :content "/ms-icon-144x144.png"}]
    [:meta {:name "theme-color" :content "#ffffff"}]]})

(defn root-handler [req respond raise]
  ((-> (fn [req respond raise]
         (respond
           (http/render-specs
             [figwheel
              google-analytics
              google-maps
              app-assets
              favicons
              {:body [[:div#cljs-entry]]}])))
       http/wrap-html-response)
   req
   respond
   raise))

(def cache-mime-types
  {"text/javascript" "public, max-age=31536000"
   "text/css" "public, max-age=31536000"})

(defn handle-dynamodb [fn-name payload]
  (go
    (let [[res err](<! (ac/<dyndoc fn-name payload))]
      (if err
        [nil (pr-str err)]
        [res]))))

(defn handle-mongodb-coll-op [& args]
  (go
    (let [[res err] (<! (mdb/<coll-op
                          {:url (aget js/process.env "MONGO_URL")
                           :db (aget js/process.env "MONGO_DATABASE")}
                          args))]
      (if err
        [nil (pr-str err)]
        [res]))))

(defn handle-mongodb-find [coll-name query & [opts]]
  (go
    (let [[res err] (<! (mdb/<find
                          {:url (aget js/process.env "MONGO_URL")
                           :db (aget js/process.env "MONGO_DATABASE")}
                          coll-name
                          query
                          opts))]
      (when err
        (println err))
      (if err
        [nil (pr-str err)]
        [res]))))

(def gi-handlers (merge
                   {:dynamodb handle-dynamodb
                    :mongodb-coll-op handle-mongodb-coll-op
                    :mongodb-find handle-mongodb-find}
                   (devbus/gi-handlers
                     {:s3-bucket "nalopastures"
                      :s3-prefix "debug-app-state"})))

(defn map-async [<f coll]
  (go-loop [coll coll
            out []]
    (if (empty? coll)
      out
      (let [res (<! (<f (first coll)))]
        (recur (rest coll) (conj out res))))))

(defn chan? [x]
  (instance? cljs.core.async.impl.channels.ManyToManyChannel x))

(defn gi-adapter [handlers]
  (fn [req respond raise]
    (go
      (let [command-ress nil #_ (gi/handle-commands gi-handlers req)
            realized (loop [crs command-ress
                            out []]
                       (if (empty? crs)
                         out
                         (let [next-res (first crs)
                               next-res (if (chan? next-res)
                                          (<! next-res)
                                          next-res)]
                           (recur
                             (rest crs)
                             (concat out [next-res])))))]
        (respond
          (merge
            #_(gi/format-response
                req
                realized)
            {:status 200
             :headers {"Access-Control-Allow-Origin" "*"}}))))))

#_((gi-adapter gi-handlers)
   {:ssl-client-cert #js {},
    :protocol "HTTPS/1.1",
    :subdomains nil,
    :cookies
    {"_ga" {:value nil},
     "amplitude_id_43316fd1a98a364a996cba88fcd94d7c" {:value nil},
     "amplitude_id_9cfb537503cbe43a8fdf5453853dc174" {:value nil}},
    :remote-addr "::1",
    :secure? nil,
    :params nil,
    :stale? nil,
    :hostname "localhost",
    :xhr? nil,
    :route-params nil,
    :headers
    {"origin" "https://localhost:5000",
     "host" "localhost:5000",
     "user-agent"
     "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36",
     "content-type" "application/transit+json",
     "cookie"
     "_ga=GA1.1.633402034.1457074349; amplitude_id_43316fd1a98a364a996cba88fcd94d7c=eyJkZXZpY2VJZCI6IjMyY2JkMzYyLTg5NGYtNDhjMy1iNmYyLTJhMGE5MmU4NTVlOFIiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTUyMjc0NTA5ODc4OSwibGFzdEV2ZW50VGltZSI6MTUyMjc0NTUwNjA5MywiZXZlbnRJZCI6NSwiaWRlbnRpZnlJZCI6MCwic2VxdWVuY2VOdW1iZXIiOjV9; amplitude_id_9cfb537503cbe43a8fdf5453853dc174=eyJkZXZpY2VJZCI6IjJjMjQzMDljLTMyOTktNGM3Ni1hMGIxLTU3NTFmZjQzNjkzM1IiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTUyNzMyNzQwMzIyNCwibGFzdEV2ZW50VGltZSI6MTUyNzMyOTI5NTYzNiwiZXZlbnRJZCI6MTcyMSwiaWRlbnRpZnlJZCI6MCwic2VxdWVuY2VOdW1iZXIiOjE3MjF9",
     "content-length" "48",
     "referer" "https://localhost:5000/",
     "connection" "keep-alive",
     "accept" "*/*",
     "accept-language" "en-US,en;q=0.9",
     "accept-encoding" "gzip, deflate, br"},
    :server-port 5000,
    :content-length "48",
    :signed-cookies nil,
    :url "/api/gi",
    :content-type "application/transit+json",
    :uri "/api/gi",
    :fresh? nil,
    :server-name "::1",
    :query-string nil,
    :body [[:dynamodb "get" {:Table "foobar"}]],
    :scheme :https,
    :request-method :post,}
   (fn [resp]
     (prn "RESP vvvvvvvvvvv")
     (ks/pp resp)))

(defn wrap-log-request [h]
  (fn [req respond raise]
    (println "Request" (:request-method req) (:uri req))
    (h req respond raise)))

(defn wrap-access-control [h]
  (fn [req respond raise]
    (h
      req
      (fn [resp]
        (respond (assoc-in resp [:headers "Access-Control-Allow-Origin"] "*")))
      raise)))

(def handler
  (-> (http/gen-handler
        ["" [["/" :root]
             ["/api/gi" :gi]]]
        {:root root-handler
         :gi (-> (gi-adapter gi-handlers)
                 http/wrap-transit-request
                 (http/wrap-transit-response
                   {:handlers (merge
                                mdb/to-transit-handlers)}))})
      http/wrap-html-response
      #_(wrap-resource "resources/public")
      #_wrap-content-type
      (http/wrap-cache-control cache-mime-types)
      wrap-access-control))
