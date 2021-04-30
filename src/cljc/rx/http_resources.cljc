(ns rx.http-resources
  (:require [rx.kitchen-sink :as ks]))

(defn url [store k]
  (get store k))

(defonce !default-lookup (atom {}))

(defn url! [k]
  (url @!default-lookup k))

(defn set-lookup! [m]
  (reset! !default-lookup m))

(defn img [opts]
  [:img (ks/deep-merge
          {:src (url! (:key opts))
           :style {:width "100%"
                   :display 'block}}
          opts)])

;; reqs
;; 1. Serve different urls based on environment (dev, prod)
;; 2. Checkable for missing resources during build
;; 3. Usable to deploy
