(ns rx.viewreg
  (:require [rx.kitchen-sink :as ks]
            [reagent.core :as r]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :refer [timeout <!]
             :refer-macros [go go-loop]]))

(defonce !registry (atom nil))
(defonce !reg-opts (atom nil))

(defn set-opts! [opts]
  (reset! !reg-opts opts))

(defn register [m & [reg-opts]]
  (swap! !reg-opts merge reg-opts)
  (swap! !registry merge m))

(defn render-spec [view-spec-fn route-opts & [control-map]]
  (let [opts (merge @!reg-opts route-opts)
        {:keys [render] :as view-spec}
        (view-spec-fn (merge
                        opts
                        {:nav control-map}))]
    (when-not render
      (throw
        (ex-info (str "Invalid render fn in view spec: " view-spec)
          {:view-spec view-spec})))
    [render (merge
              {:key (ks/uuid)}
              opts)]))

(defn realize-view-spec [view-spec-fn route-opts control-map]
  (let [opts (merge @!reg-opts route-opts)]
    [(view-spec-fn
       (merge
         opts
         {:nav control-map}))
     opts]))

(defn render-key-route [registry route control-map]
  (let [route-key (first route)
        route-opts (second route)
        spec (get registry route-key)
        opts (merge
               route-opts)]

    (when-not route-key
      (throw
        (ex-info
          (str "No route key in route " route)
          {:route route
           :registry registry})))

    (when-not spec
      (throw
        (ex-info
          (str "No spec found for key " route-key)
          {:registry-keys (keys registry)})))

    (render-spec spec opts control-map)))

(defn render-fn-route [route control-map]
  (let [route-fn (first route)
        route-opts (second route)]

    (when-not route-fn
      (throw
        (ex-info
          (str "No route fn in route " route)
          {:route route})))

    (render-spec route-fn route-opts control-map)))

(defn render-route [registry route control-map]
  (if (keyword? (first route))
    (render-key-route registry route control-map)
    (render-fn-route route control-map)))

(defn route->view-spec [registry route control-map]
  (let [route-key (first route)
        route-opts (second route)
        spec (get registry route-key)
        opts (merge
               route-opts)]

    (when-not route-key
      (throw
        (ex-info
          (str "No route key in route " route)
          {:route route
           :registry registry})))

    (when-not spec
      (throw
        (ex-info
          (str "No spec found for key " route-key)
          {:registry-keys (keys registry)})))

    (realize-view-spec spec opts control-map)))

(defn view-spec+opts [route & [opts]]
  (when route
    (route->view-spec
      @!registry
      route
      opts)))

(defn render-view [route & [opts]]
  (when route
    (render-route
      @!registry
      route
      opts)))
