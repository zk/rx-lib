(ns rx.view
  (:require [rx.kitchen-sink :as ks]
            [reagent.core :as r]
            [cljs.spec.alpha :as s]))

(defonce !registry (atom nil))
(defonce !reg-opts (atom nil))
(defonce !callbacks (atom {}))

(defn listen! [evt-key callback]
  (swap! !callbacks update
    evt-key
    (fn [cb-set]
      (conj
        (set cb-set)
        callback)))
  callback)

(defn unlisten! [evt-key callback]
  (swap! !callbacks update
    evt-key
    (fn [cb-set]
      (disj
        (set cb-set)
        callback)))
  callback)

(defn fire! [evt-key args]
  (doseq [cb (get @!callbacks evt-key)]
    (apply cb args)))

(defn set-opts! [opts]
  (reset! !reg-opts opts))

(defn register-views [m & [reg-opts]]
  (swap! !reg-opts merge reg-opts)
  (swap! !registry merge m)
  (fire! :register-view [m @!reg-opts]))

#_(defn realize-route [route & [adtl-opts]]
  (when route
    (let [[route-key-or-fn route-opts] route

          route-fn (if (keyword? route-key-or-fn)
                     (get @!registry route-key-or-fn)
                     route-key-or-fn)

          _ (when-not (fn? route-fn)
              (throw
                (ex-info "Invalid route function"
                  {:route route})))

          opts (merge
                 @!reg-opts
                 route-opts
                 adtl-opts)

          spec (route-fn opts)]

      {::spec spec
       ::opts opts
       ::id (ks/uuid)})))

(defn realize-route [route & [adtl-opts]]
  (when route
    (let [[route-key-or-fn route-opts] route

          route-fn (if (keyword? route-key-or-fn)
                     (get @!registry route-key-or-fn)
                     route-key-or-fn)

          _ (when-not (fn? route-fn)
              (throw
                (ex-info "Invalid route function"
                  {:route route})))

          opts (merge
                 @!reg-opts
                 route-opts
                 adtl-opts)

          spec (route-fn opts)

          {:keys [::spec ::opts]}
          (if (vector? spec)
            (realize-route spec opts)
            {::spec spec ::opts opts})]

      {::spec spec
       ::opts opts
       ::id (ks/uuid)})))

(defn render-route [route & [adtl-opts]]
  (when route
    (let [{:keys [::spec ::opts]} (realize-route route adtl-opts)]
      [(:render spec) opts])))

(defn render-realized-route [realized-route & [adtl-opts]]
  (let [render (->> realized-route
                    ::spec
                    :render)]
    (when render
      [render
       (merge
         (->> realized-route
              ::opts)
         adtl-opts)])))

(defn id [realized-route]
  (::id realized-route))

(defn wrap [render]
  (fn [_]
    {:render render}))

(defn realized-route-spec [rr]
  (::spec rr))

(def dummy-route [(fn [] {:render (fn [])})])

(defn opts [rr]
  (::opts rr))
