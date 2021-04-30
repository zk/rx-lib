(ns rx.browser.annie-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.annie :as ani]
            [rx.browser :as browser]
            [rx.browser.canvas :as canv]
            [rx.browser.ui :as ui]
            [rx.browser.buttons :as btn]
            [rx.browser.styleguide :as sg]
            [reagent.core :as r]))

(def headings
  [[::annie :h1 "Annie"]
   [::example :h2 "Example"]])


(def !v (r/atom 0))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {}
    [sg/heading headings ::annie]
    [:p "Pure clojure animation library."]]
   [sg/section
    (sg/example
      {:form
       [ui/group
        {:gap 8}
        [canv/canvas
         {:style {:width "100%"
                  :height 200}
          :on-click (fn []
                      (prn "CLICk"))
          :render (fn [{:keys [::canv/width
                               ::canv/height]
                        :as c}]
                    (canv/clear-rect c 0 0 width height)
                    (canv/fill-style c "red")
                    (canv/fill-rect c
                      (max 0 (- (* @!v width) (/ height 2)))
                      (/ height 4)
                      (/ height 2)
                      (/ height 2)))}]
        [ui/group
         {:horizontal? true}
         [btn/bare
          {:icon {:set "feather"
                  :name "chevrons-left"}
           :on-click (fn []
                       (ani/spring
                         (atom nil)
                         @!v 0
                         (fn [v]
                           (reset! !v v))))}]
         [btn/bare
          {:icon {:set "feather"
                  :name "chevrons-right"}
           :on-click (fn []
                       (ani/spring
                         (atom nil)
                         @!v 1
                         (fn [v]
                           (reset! !v v))))}]]]})]])

(comment

  (browser/<set-root!
    [sg/standalone {:headings headings
                    :component sections}])

  
  )
