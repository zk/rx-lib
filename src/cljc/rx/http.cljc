(ns rx.http
  #?(:clj
     (:require [rx.kitchen-sink :as ks]
               [clojure.string :as str]
               [reitit.core :as reitit]
               [bidi.bidi :as bidi]
               [garden.core :as garden]
               [rum.core :as rc])

     :cljs
     (:require [rx.kitchen-sink :as ks]
               [clojure.string :as str]
               #_[hiccups.runtime :as rt]
               [bidi.bidi :as bidi]
               [garden.core :as garden]
               [rum.core :as rc])))


(defn compile-specs [specs]
  (apply
    merge-with
    (fn [v1 v2]
      (if (and (map? v1) (map? v2))
        (compile-specs [v1 v2])
        (vec (concat v1 v2))))
    specs))

(defn css-html [csss]
  (->> csss
       (remove nil?)
       (map (fn [css]
              [:link
               (merge
                 {:rel "stylesheet"}
                 (if (string? css)
                   {:href css}
                   css))]))))

(defn js-html [jss]
  (->> jss
       (remove nil?)
       (map (fn [js]
              (cond
                (string? js) [:script {:type "text/javascript" :src js}]
                (map? js) [:script js]
                :else js)))))


;; Content Security Policy

(defn csp-str->map [s]
  (when s
    (->> (str/split (str/trim s) #";")
         (map (fn [csp-entry]
                (let [parts (str/split (str/trim csp-entry) #"\s+")
                      directive (first parts)
                      args (rest parts)]
                  [directive args])))
         (into {}))))

(defn script-block->csp-frag [[_ & args]]
  (let [srcs (if (map? (first args))
               (rest args)
               args)
        src (apply str srcs)
        base64-sha512 (ks/to-base64-str (ks/sha512-bytes src))
        source (str "'sha512-" base64-sha512 "'")]
    {:script-src [source]}))

(defn remove-path [url]
  (when url
    (first (re-find #"(https?|ws)://[^/]*" url))))

(defn script-source->csp-frag [s]
  (when (str/starts-with? s "http")
    {:script-src [(remove-path s)]}))

(defn js-entry->csp-frag [o]
  (cond
    (vector? o) (script-block->csp-frag o)
    (string? o) (script-source->csp-frag o)))

(defn merge-csp [base frag]
  (->> frag
       (reduce (fn [base [k v]]
                 (update
                   base
                   (keyword k)
                   (fn [xs]
                     (vec (concat xs v)))))
               base)))

(defn js-entries->csp-frag [os]
  (->> os
       (map js-entry->csp-frag)
       (reduce merge-csp)))

(defn gen-response-body [spec]
  (let [title (-> spec :title last)
        head-js (-> spec :head-js js-html)
        head-css (css-html (:head-css spec))
        head (:head spec)


        meta-names (->> spec
                        :meta-names
                        (reduce merge)
                        (map (fn [[k v]]
                               [:meta {:name (name k)
                                       :content v}])))

        meta-properties (->> spec
                             :meta-properties
                             (reduce merge)
                             (map (fn [[k v]]
                                    [:meta {:property (name k)
                                            :content v}])))

        metas (concat meta-names meta-properties)

        body-attrs (->> spec
                        :body-attrs
                        (reduce
                          merge
                          {}))
        body-attrs (update
                     body-attrs
                     :style
                     (fn [style]
                       (when style
                         (garden/style style))))
        body-js (-> spec :body-js js-html)
        body-css (-> spec :body-css css-html)
        body (:body spec)]

    [:html
     (vec
       (concat
         [:head]
         (when title
           [[:title title]])
         metas
         head-css
         head-js
         head))

     (vec
       (concat
         [:body]
         (when body-attrs
           [body-attrs])
         body
         (when body-js
           body-js)))]))

(defn csp-map->str [m]
  (->> m
       (map (fn [[k vs]]
              (str
                (name k)
                " "
                (->> vs
                     (interpose " ")
                     (apply str)))))
       (interpose "; ")
       (apply str)))

(defn gen-response-headers [spec]
  (let [all-js (concat
                 (:head-js spec)
                 (:body-js spec))

        csp-js (js-entries->csp-frag all-js)
        provided-csps (->> spec :csp)

        default-csp {:script-src ["'self'"]}

        csp (reduce
              merge-csp
              {}
              (concat
                [default-csp]
                [csp-js]
                provided-csps))]
    {"content-type" "text/html;charset=utf-8"
     "content-security-policy" (csp-map->str csp)}))


(defn google-analytics-frag [ga-tracking-id]
  (let [src (str
              "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', '" ga-tracking-id "', 'auto');
  ga('send', 'pageview');")]
    {:head-js [[:script {:type "text/javascript"} src]]
     :csp [{:script-src ["https://www.google-analytics.com"]}]}))

(defn figwheel-frag []
  {:csp
   [{:script-src
     (vec
       (concat
         (->> ["if (typeof goog == \"undefined\") console.warn(\"ClojureScript could not load :main, did you forget to specify :asset-path?\");"
               "goog.require(\"figwheel.connect\");"
               "goog.require(\"process.env\");"
               "figwheel.connect.start();"
               "goog.require(\"main.browser\");"]
              (map (fn [s]
                     (str "'sha512-"
                          (ks/to-base64-str (ks/sha512-bytes s))
                          "'"))))
         ["ws://localhost:3449"]))

     :frame-ancestors ["'none'"]}]})

(defn google-maps-frag []
  {:csp [{:script-src ["https://maps.googleapis.com"]}]
   :head-js [(str "https://maps.googleapis.com/maps/api/js?key=")]})

(def default-cache-mime-types
  {"text/javascript" "public, max-age=31536000"
   "text/css" "public, max-age=31536000"})

(defn render-specs [specs]
  (let [compiled-spec (compile-specs specs)
        resp-body (gen-response-body compiled-spec)
        resp-headers (gen-response-headers compiled-spec)]
    {:status 200
     :headers resp-headers
     :body resp-body}))

#?(:cljs
   (defn wrap-cache-control [handler opts]
     (fn [req respond raise]
       (handler
         req
         (fn [res]
           (let [content-type (or (get-in res [:headers "Content-Type"])
                                  (get-in res [:headers "content-type"]))
                 cache-control-value (get opts content-type)
                 res (if cache-control-value
                       (update-in
                         res
                         [:headers]
                         merge
                         {"Cache-Control" cache-control-value})
                       res)]
             (respond res)))
         raise))))


(defn html-response? [r]
  (some-> r
          :headers
          (#(or (get % "Content-Type")
                (get % "content-type")))
          (str/includes? "text/html")))

#?
(:cljs
 (do
   (defn wrap-html-response
     "Render hiccup vector into an HTML string when the content type is text/html."
     [handler]
     (fn [req respond raise]
       (handler
         req
         (fn [resp]
           (respond
             (if (html-response? resp)
               (update
                 resp
                 :body
                 #(str
                    "<!DOCTYPE html>\n"
                    "HICCUPS HERE"
                    #_(rt/render-html %)))
               resp)))
         raise)))))

#?(:cljs
   (defn wrap-transit-response [h & [handlers]]
     (fn [req respond raise]
       (h
         req
         (fn [res]
           (if (or (get-in res [:headers "Content-Type"])
                   (get-in res [:headers "content-type"]))
             (respond res)
             (let [encoded (try
                             (-> res
                                 (update-in [:body] #(ks/to-transit % handlers))
                                 (assoc-in
                                   [:headers "Content-Type"]
                                   "application/transit+json;charset=utf-8"))
                             (catch js/Error e
                               (ks/prn "Couldn't encode response"
                                 res
                                 respond
                                 e)
                               (raise e)))]
               (respond (or encoded res)))))
         raise))))

(defn decode-content-type [cts]
  (let [parts (str/split cts #";")
        media-type (some->
                     (first parts)
                     str/trim)]
    {:media-type media-type
     :params (->> parts
                  rest
                  (map str/trim)
                  (remove empty?)
                  (map #(str/split % #"="))
                  (map (fn [[k v]]
                         [(keyword k) v]))
                  (into {}))}))

(defn content-type [request]
  (when-let [cts (or (get-in request [:headers "Content-Type"])
                     (get-in request [:headers "content-type"]))]
    (decode-content-type cts)))

(defn wrap-transit-request [h & [handlers]]
  (fn [req respond raise]
    (if (= "application/transit+json"
           (:media-type (content-type req)))
      (let [body (:body req)]
        (if (string? body)
          (h (assoc-in req
                       [:transit-body]
                       (try
                         (ks/from-transit body handlers)
                         (catch #?(:clj Exception :cljs js/Error) e
                           (ks/throw-str "Couldn't parse body as transit: " body))))
             respond
             raise)
          (let [stream (:body req)
                !body (atom nil)]

            (.on stream "readable" (fn []
                                     (swap! !body str (.read stream))))

            (.on stream "end" (fn []
                                (let [body @!body]
                                  (h (assoc-in req
                                               [:transit-body]
                                               (ks/from-transit body handlers))
                                     respond
                                     raise)))))))

      (h req respond raise))))

(defn wrap-decode-body [h]
  (fn [req respond raise]
    (let [stream (:body req)
          !body (atom nil)]

      (.on stream "readable" (fn []
                               (swap! !body str (.read stream))))

      (.on stream "end" (fn []
                          (let [body @!body]
                            (h (assoc-in req
                                 [:body-str]
                                 body)
                              respond
                              raise)))))))

(defn compile-handlers [handlers]
  (->> handlers
       (map (fn [[k fn-or-map]]
              [k (if (map? fn-or-map)
                   (let [{:keys [middleware data render]} fn-or-map
                         middleware (or middleware identity)
                         data (or data identity)]
                     (middleware
                       (fn [req]
                         (render (data req)))))
                   fn-or-map)]))
       (into {})))

(defn gen-handler [routes handlers]
  (let [compiled-handlers (compile-handlers handlers)]
    (fn #?(:clj [{:keys [uri path-info] :as req}]
           :cljs [{:keys [uri path-info] :as req} respond raise])
      (let [path (or path-info uri)
            {:keys [handler route-params] :as match-context}
            (bidi/match-route* routes path req)
            handler-fn (get compiled-handlers handler)]
        (if handler-fn
          #?(:clj
             (handler-fn
               (-> req
                   (update-in [:route-params] merge route-params)))

             :cljs (handler-fn
                     (-> req
                         (update-in [:route-params] merge route-params))
                     respond
                     raise))
          #?(:cljs
             (respond {:status 404 :body "not found"})))))))


(comment

  (rc/render-html
    [:div "hi"])


  )
