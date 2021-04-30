(ns rx.browser.local-storage
  (:require [rx.kitchen-sink :as ks]
            [clojure.core.async
             :as async
             :refer [go <!]]))

(def LS (.-localStorage js/window))

(defn set-item [k v]
  (.setItem LS k v)
  v)

(defn get-item [k]
  (.getItem LS k))

(defn set-transit [k v & [opts]]
  (set-item k (ks/to-transit v opts))
  v)

(defn get-transit [k & [opts]]
  (ks/from-transit (get-item k) opts))

(comment

  (set-item "foo" "bar")

  (get-item "foo")

  
  (set-transit "foo" {:bar "baz"})

  (get-transit "foo")

  )
