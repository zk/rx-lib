(ns rx.node
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def process (nodejs/require "process"))

(defn handle-uncaught-exception [e]
  (ks/pp (anom/from-err e)))

(defonce !last-handler (atom nil))

(defn set-catch-global-exceptions! []
  (when @!last-handler
    (.off process "uncaughtException" @!last-handler))
  (reset! !last-handler handle-uncaught-exception)
  (.on process "uncaughtException" handle-uncaught-exception))

(def fs (js/require "fs"))

(defn read-file-sync [path & [opts]]
  (.readFileSync fs path opts))

(comment
  
  (set-catch-global-exceptions!)

  (clear-catch-global-exceptions!)

  
  )

