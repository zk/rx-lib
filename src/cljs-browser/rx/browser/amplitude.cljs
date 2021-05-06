(ns rx.browser.amplitude
  (:require [rx.kitchen-sink :as ks]
            [amplitude]))

(defn init [api-key]
  (.init (.getInstance js/amplitude) api-key))

(defn log-event [event-name & [event-payload]]
  (.logEvent (.getInstance js/amplitude)
    event-name
    (clj->js event-payload)))

(comment

  

  

  )
