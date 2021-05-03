(ns rx.node.otto
  (:require [rx.kitchen-sink :as ks]
            [cljs.core.async
             :as async
             :refer-macros [go]]))

(def http (js/require "http"))

(defn <log-map [m]
  (go
    (let [req (.request
                http
                (clj->js
                  {:hostname "localhost"
                   :port 6886
                   :method "POST"
                   :headers {"Content-Type" "application/transit+json"}})
                (fn [resp]
                  (let [!data (atom nil)]
                    (.on resp
                      "data"
                      (fn [chunk]
                        (swap! !data str chunk)))
                    (.on resp
                      "end"
                      (fn []
                        (prn "GOT" @!data))))))]
      (.on
        req
        "error"
        (fn [err]
          (prn "ERR" err)))
      (.write req (ks/to-transit m))
      (.end req))))
