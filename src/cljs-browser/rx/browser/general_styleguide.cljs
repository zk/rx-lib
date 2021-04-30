(ns rx.browser.general-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.styleguide :as sg]
            [rx.browser.feather-icons :as fi]
            [rx.browser.components :as cmp]))

(def headings
  [[::general :h1 "General"]
   [::expander :h2 "Expander"]])

(defn expander-section []
  [ui/group
   {:gap 16}
   [sg/heading headings ::expander]
   [:p "Provides a way to display collapsable content."]
   (sg/example
     {:form
      [ui/expander
       {:label [:div "Expander Label"]
        :content [:div "Expander Content"]}]})
   (sg/example
     {:form
      [ui/expander
       {:render-label
        (fn [{:keys [closed?]}]
          [cmp/hover
           {:style {:padding 4}
            :style-over {:background-color "rgba(0,0,0,0.05"}}
           [ui/group
            {:horizontal? true
             :justify-content 'space-between
             :align-items 'center}
            [ui/text "Custom Label"]
            (if closed?
              [fi/chevron-down]
              [fi/chevron-up])]])
        :content [ui/group {:pad 8
                            :style {:background-color "black"
                                    :color "white"}}
                  "Expander Content"]}]})])

(defn sections []
  [ui/section
   [sg/heading headings ::general]
   [:p "Bunch o' stuff"]
   [expander-section]])

(comment

  (browser/<set-root!
    [sg/standalone {:component sections
                    :headings headings}])

  )


