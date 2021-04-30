(ns rx.browser.components-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.styleguide :as sg]
            [rx.browser.components :as cmp]
            [rx.theme :as th]))

(defn sections []
  [:div
   [sg/section
    [sg/section-image
     {:url "https://www.oldbookillustrations.com/wp-content/high-res/1880/corliss-gear-cutting-machine-768.jpg"
      :style {:float 'right
              :width 400}}]
    [:h1 {:id "components"} "Common Components"]
    [:p ""]
    [:h3 "Namespace"]
    [:code "rx.browser.components"]
    #_[cmp/$copy-button {:text "rx.browser.components"}]
    [:div {:style {:clear 'both}}]]

   [sg/section
    [:h2 {:id "forms-controls"} "Controls"]
    [sg/checkerboard
     [cmp/copy-button {:text "test"}]]]])
