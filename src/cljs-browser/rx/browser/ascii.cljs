(ns rx.browser.ascii
  (:require [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [reagent.core :as r]))

(defn line [{:keys [text]}]
  (r/create-class
    {:reagent-render
     (fn []
       [:svg {:viewBox "0 0 100 100"
              :width 100}
        [:text
         {:x 0 :y 0
          :transform-origin "50% 50%"
          :style {:font-size 130
                  :transform "translate3d(50%,50%,0)"}}
         text]])}))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {}
    [:h1 "ASCII"]
    (sg/example
      {:form
       [line {:text "h"}]})]])

(comment

  (browser/<set-root!
    [sg/standalone
     {:component sections}])

  
  )
