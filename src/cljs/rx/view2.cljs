(ns rx.view2
  (:require [rx.kitchen-sink :as ks :refer-macros [<&]]
            [reagent.core :as r]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [cljs.spec.alpha :as s]
            [clojure.core.async
             :as async
             :refer [go <!]]))
;; view-fn (fn [])
;; route [view-fn opts]
;; view

(<defn <realize-route [route & [adtl-opts]]
  (let [[route-fn route-opts] route

        _ (when-not (fn? route-fn)
            (anom/throw-anom
              {:desc "Route fn not a function"
               ::route route}))

        _ (when (and route-opts (not (map? route-opts)))
            (anom/throw-anom
              {:desc "Route opts is not a map"
               ::route route}))

        opts (merge
               route-opts
               adtl-opts)

        spec (<& (route-fn opts))

        {:keys [::spec ::opts]}
        (if (vector? spec)
          (<! (<realize-route spec opts))
          {::spec spec ::opts opts})]

    (merge
      spec
      {::opts opts
       ::id (ks/uuid)})))

(defn render-view [view render-key & [adtl-opts]]
  (when view
    (when (anom/? view)
      (anom/throw-anom view))
    (if-let [render (get view render-key)]
      [render (merge
                (->> view ::opts)
                adtl-opts)]
      (anom/throw-anom {:desc "Render fn not found in view"
                        ::view-keys (keys view)
                        ::render-key render-key}))))

(defn render-route [route render-key & [adtl-opts]]
  (let [!comp (r/atom nil)]
    (go
      (reset! !comp
        (render-view
          (<! (<realize-route route adtl-opts))
          render-key)))
    [(r/create-class
       {:reagent-render
        (fn [] @!comp)})]))

(defn id [view]
  (::id view))

(defn wrap [render]
  (fn [_]
    {:render render}))

(defn spec [rr]
  (::spec rr))

(def dummy-route [(fn [] {:render (fn [])})])

(defn opts [rr]
  (::opts rr))
