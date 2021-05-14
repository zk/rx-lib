(ns rx.browser.amplitude
  (:require [rx.kitchen-sink :as ks]
            [amplitude]))

(defn init [api-key]
  (.init (.getInstance js/amplitude) api-key))

(defn log-event [event-name & [event-payload]]
  (println "[AMP]" event-name (pr-str event-payload))
  (.logEvent (.getInstance js/amplitude)
    event-name
    (clj->js event-payload)))

(defn identify [m]
  (println "[AMP] identify" (pr-str m))
  (let [o (js/amplitude.Identify.)]
    (doseq [[k v] m]
      (.set o (clj->js k) (clj->js v)))
    (.identify (.getInstance js/amplitude) o)))


(comment)
