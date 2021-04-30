(ns rx.browser.webgl
  (:require [rx.kitchen-sink :as ks]))

(defn get-context [canvas]
  (when-not canvas
    (ks/throw-str "Canvas invalid"))
  
  (.getContext canvas "webgl"))
