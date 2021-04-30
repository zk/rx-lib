(ns rx.browser.view-stack
  (:require [rx.kitchen-sink :as ks]
            [rx.view :as view]
            [reagent.core :as r]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]])
  (:refer-clojure :exclude [pop]))

(defn gen-control [{:keys [initial-routes]}]
  (let [!id->realized-route (r/atom {})
        !ids-order (r/atom [])
        
        control {:!id->realized-route !id->realized-route
                 :!ids-order !ids-order}]


    ;; view stack control (and built up state) needs to
    ;; be supplied when realizing a route
    
    (loop [routes initial-routes]
      (if (empty? routes)
        nil
        (let [rr (view/realize-route (first routes) {:view-stack control})]
          (swap! !id->realized-route assoc (view/id rr) rr)
          (swap! !ids-order conj (view/id rr))
          (recur (rest routes)))))

    control))

(defn <pop [{:keys [!ids-order !id->realized-route]}]
  (go
    (let [id (last @!ids-order)]
      (swap! !ids-order clojure.core/pop)
      (swap! !id->realized-route dissoc id))))

(defn <push [{:keys [!ids-order !id->realized-route]
             :as control} route]
  (go
    (let [rr (view/realize-route route {:view-stack control})]
      (swap! !id->realized-route assoc (view/id rr) rr)
      (swap! !ids-order conj (view/id rr)))))

(defn view [{:keys [initial-routes
                    control]
             :as opts}]
  (let [{:keys [!id->realized-route
                !ids-order]}
        (or control
            (gen-control opts))]
    (r/create-class
      {:reagent-render
       (fn []
         (let [id->realized-route @!id->realized-route
               ids-order @!ids-order]
           [:div
            {:style {:flex 1
                     :position 'relative}}
            (->> ids-order
                 (map
                   (fn [id]
                     (let [rr (get id->realized-route id)]
                       [:div
                        {:key (view/id rr)
                         :style {:position 'absolute
                                 :top 0
                                 :left 0
                                 :right 0
                                 :bottom 0
                                 :display 'flex
                                 :flex-direction 'column}}
                        (view/render-realized-route rr)]))))]))})))


