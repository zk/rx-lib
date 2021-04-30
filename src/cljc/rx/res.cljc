(ns rx.res
  #?(:clj (:require [rx.kitchen-sink :as ks]
                    [clojure.spec.alpha :as s]
                    [clojure.string :as str]
                    [clojure.core.async
                     :as async
                     :refer [go chan <! close! put!]])
     :cljs (:require [rx.kitchen-sink :as ks]
                     [cljs.spec.alpha :as s]
                     [clojure.string :as str]
                     [cljs.core.async
                      :as async
                      :refer [chan <! close! put!]
                      :refer-macros [go]])))

(s/def ::res
  (s/keys
    :opt [:rx.res/data
          :rx.res/anom]))

(s/def ::non-empty-string
  (s/and string? #(not (empty? %))))

(s/def :rx.anom/desc ::non-empty-string)

(s/def :rx.anom/anom
  (s/keys
    :opt [:rx.anom/desc
          :rx.anom/js-stack]))

(s/def ::category #{::unavailable
                    ::interrupted
                    ::incorrect
                    ::forbidden
                    ::unsupported
                    ::not-found
                    ::conflict
                    ::fault
                    ::busy
                    ::has-data})

(defn unavailable? [m]
  (= ::unavailable (get m ::category)))

(defn interrupted? [m]
  (= ::interrupted (get m ::category)))

(defn incorrect? [m]
  (= ::incorrect (get m ::category)))

(defn forbidden? [m]
  (= ::forbidden (get m ::category)))

(defn unsupported? [m]
  (= ::unsupported (get m ::category)))

(defn not-found? [m]
  (= ::not-found (get m ::category)))

(defn conflict? [m]
  (= ::conflict (get m ::category)))

(defn fault? [m]
  (= ::fault (get m ::category)))

(defn busy? [m]
  (= ::busy (get m ::category)))

(defn has-data? [m]
  (or
    (= ::has-data (get m ::category))
    (::data m)))

(def data :rx.res/data)

(defn <data [ch]
  (go
    (data (<! ch))))

(def anom :rx.res/anom)

(defn <anom [ch]
  (go
    (anom (<! ch))))

(defn anom-desc [res]
  (-> res :rx.res/anom :rx.anom/desc))

(defn describe [res]
  (println ">> -- Data")
  (ks/pp (data res))
  (println "!! -- Anom")
  (ks/pp (anom res))
  (println "%% -- Other")
  (ks/pp (dissoc res :rx.res/anom :rx.res/data))
  (println "###########")
  (println " "))

(defn error-code [res]
  (-> res
      anom
      :rx.anom/error-code))

(defn err->anom [err]
  (let [d (or (ex-data err)
              (and (map? err) err))]
    (merge
      (if (and d (anom d))
        {:rx.anom/cause (anom d)}
        (when-let [d (ex-data err)] d))
      {:rx.anom/desc (.-message err)
       :rx.anom/stack
       #?(:clj (->> (.getStackTrace err)
                    (mapv str))
          :cljs (->> (str/split
                       (.-stack err)
                       #"\n")
                     (map str/trim)
                     (remove empty?)
                     vec))})))

(defn err->res [err]
  (merge
    {:rx.res/anom (err->anom err)}
    (when-let [d (ex-data err)]
      (select-keys
        d
        [:rx.res/category]))))

(defn throw-anom [res & [message]]
  (if (anom res)
    (throw (ex-info (or message "Anomaly found") res))
    res))


