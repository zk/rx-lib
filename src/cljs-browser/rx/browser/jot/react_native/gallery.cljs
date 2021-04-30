(ns rx.browser.jot.react-native.gallery
  (:require [rx.kitchen-sink :as ks]
            [rx.jot :as jot]
            [dommy.core :as dommy]))

(defn block-view
  [{:keys [::jot/block
           ::on-choose-media]}]
  {:render
   (fn []
     [:div
      (->> block
           :media
           (map (fn [{:keys [uri] :as media}]
                  [:div {:key uri}
                   [:img {:src uri
                          :style {:width "100%"}
                          :on-mouse-down (fn [e]
                                           (.preventDefault e)
                                           (.stopPropagation e)
                                           nil)
                          :on-click
                          (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e)
                            (when on-choose-media
                              (on-choose-media
                                media
                                (dommy/bounding-client-rect
                                  (.. e -target))))
                            nil)}]])))])})
