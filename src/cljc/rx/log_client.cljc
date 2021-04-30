(ns rx.log-client
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            #?(:clj [clj-http.client :as hc]
               :cljs [cljs-http.client :as hc])
            #?(:cljs [cljs.tagged-literals])))

(defn fn->map [v]
  (let [xs (clojure.string/split (.-name v) #"\$")
        name (last xs)
        ns (clojure.string/join "." (butlast xs))]
    {:name (str ns "/" name)
     :ns ns
     :display-name name
     :type "js/Function"
     :fn? true
     :string (.toString v)}))

#?(:cljs (deftype FunctionHandler []
           Object
           (tag [this v] "map")
           (rep [this v] (fn->map v))
           (stringRep [this v])))

#_(defn log [{:keys [event-name
                   body-str
                   body-edn
                   group-id]}]
  (hc/post
    "http://localhost:7330"
    {:body (ks/to-transit
             (merge
               (when event-name
                 {:rx.log/event-name event-name})
               (when body-str
                 {:rx.log/body-str body-str})
               (when body-edn
                 {:rx.log/body-edn body-edn})
               (when group-id
                 {:rx.log/group-id group-id}))
             {:handlers (merge
                          #?(:cljs {js/Function (FunctionHandler.)}))})
     :headers {"Content-Type"
               "application/json+transit"}}))

#_(defn edn [edn-payload]
  (log {:body-edn (pr-str edn-payload)})
  edn-payload)

(defn state [{:keys [uri]}]
  {::uri uri})

(defn js-value? [v]
  #?(:cljs (instance? cljs.tagged-literals/JSValue v)
     :clj false))

(defn serialize-step [v]
  (cond
    (map? v) (->> v
                  (map (fn [[k v]]
                         [k (serialize-step v)]))
                  (into {}))

    (vector? v) (->> v
                     (mapv serialize-step))

    (set? v) (->> v
                  (map serialize-step)
                  set)

    (list? v) (->> v
                   (map serialize-step))

    (seq? v) (->> v
                  (map serialize-step))

    (nil? v) nil

    (or (number? v)
        (string? v)
        (keyword? v)
        (symbol? v)
        (js-value? v)) v

    :else (pr-str v)))

(defn serialize [v]
  (pr-str (serialize-step v)))

(def ^:dynamic *logger-state*
  {::uri "http://localhost:7330"
   ::app-name "Rx Test App"})

(defn on! [& [logger-state]]
  (let [logger-state (or logger-state
                         {::uri "http://localhost:7330"
                          ::app-name "Rx Test App"})]
    #?(:clj (alter-var-root #'*logger-state*
              (fn [_] logger-state))
       :cljs (set! *logger-state* logger-state))))

(defn off! []
  #?(:clj (alter-var-root #'*logger-state*
              (fn [_] nil))
     :cljs (set! *logger-state* nil)))

(defn log [map-or-string & [pl override]]
  (when *logger-state*
    (let [{:keys [::uri ::app-name]} *logger-state*
          {:keys [group event]} (if (string? map-or-string)
                                  {:event map-or-string}
                                  map-or-string)]
      (hc/post
        uri
        {:body (ks/to-transit
                 (merge
                   {:rx.log/event-id (ks/uuid)
                    :rx.log/app-name app-name
                    :rx.log/event-name event
                    :rx.log/body-edn (serialize pl)
                    :rx.log/created-ts (ks/now)}
                   (when group
                     {:rx.log/group-name group})
                   override)
                 {:handlers (merge
                              #?(:cljs {js/Function (FunctionHandler.)}))})
         :headers {"Content-Type"
                   "application/json+transit"}}))))

