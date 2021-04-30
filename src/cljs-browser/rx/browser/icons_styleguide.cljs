(ns rx.browser.icons-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.feather-icons :as feather]
            [rx.browser.styleguide :as sg]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.buttons :as btn]
            [rx.browser.popover :as po]
            [rx.browser.forms :as forms]
            [reagent.core :as r]))

(def headings
  [[::icons :h1 "Icons"]
   [::feather :h2 "Feather"]])

(defn sections []
  (let [!icon-size (r/atom 20)]
    (r/create-class
      {:reagent-render
       (fn []
         [sg/sections
          [sg/section
           [sg/heading headings ::icons]
           [:p "Rx comes bundled with several svg-based icon sets."]]
          [sg/section
           [sg/heading headings ::feather]
           [:p "See namespace " [:code "rx.browser.feather-icons"] "."]
           [:h3 "Example"]
           [ui/group
            {:horizontal? true
             :gap 8}
            [sg/checkerboard
             [feather/settings
              {:size 100
               :color "blue"
               :stroke-width 0.5}]]
            [sg/code-block
             (ks/pp-str
               '[r.b.f-i/settings
                 {:size 100
                  :color "blue"
                  :stroke-width 0.5}])]]
           [ui/group
            {:horizontal? true
             :gap 8}
            [sg/checkerboard
             [feather/feather
              {:size 30
               :style {:fill "gold"
                       :stroke "maroon"
                       :stroke-width 2}}]]
            [sg/code-block
             (ks/pp-str
               '[r.b.f-i/feather
                 {:size 30
                  :style {:fill "gold"
                          :stroke "maroon"
                          :stroke-width 2}}])]]
           [ui/group
            {:horizontal? true
             :justify-content 'space-between}
            [:h3 "All Icons"]
            [po/view
             {:position :left-center
              :esc-enabled? true
              :render-body
              (fn [po]
                [btn/bare
                 {:icon {:set "feather"
                         :color "black"
                         :name "more-horizontal"}
                  :on-click (fn []
                              (po/toggle po))}])
              :popover [ui/group
                        {:style {:width 300}}
                        [ui/group
                         {:horizontal? true
                          :gap 12
                          :vpad 4}
                         [forms/label {:text "Icon Size"}]
                         [forms/range
                          {:style {:flex 1}
                           :!val !icon-size
                           :min 24
                           :max 100}]]]}]]
           [ui/group
            {:gap 12}
            (into
              [:div
               {:style {:flex-direction 'row
                        :display 'flex
                        :flex-wrap 'wrap}}]
              (->> feather/all-icons
                   (map (fn [{:keys [icon-name icon-fn]}]
                          [:div
                           {:style {:padding 5}}
                           [po/view
                            {:show-on-over? true
                             :position :right-center
                             :over-delay 0
                             :body [icon-fn {:pointer-events 'all
                                             :size @!icon-size}]
                             :popover [ui/group
                                       {:pad 8
                                        :style {:white-space 'nowrap
                                                :line-height "100%"}}
                                       icon-name]}]]))))]]])})))

(comment

  (browser/<set-root!
    [sg/standalone {:component sections
                    :headings headings}])


  )


