(ns rx.browser.popover-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.browser.forms :as forms]
            [rx.browser.buttons :as bts]
            [rx.browser.popover :as po]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [rx.theme :as th]))

(def headings
  [[::popover :h1 "Popover"]
   [::example :h2 "Example"]
   [::usage :h2 "Usage"]
   [::real-world-examples :h2 "Real World Examples"]
   [::state-space :h2 "State Space"]])

(def options
  [[:body "Component or component vector of popover body content. Component is passed popover state for use with API functions like show, hide, and toggle."]
   [:popover "Component or component vector of pop up content.  Component is passed popover state for use with API functions like show, hide, and toggle."]
   [:style "CSS style for body wrapper div"]
   [:position
    (into
      [:span
       "Tooltip posititon. One of "]
      (->> po/position-keys
           (map (fn [k]
                  [:code (pr-str k)]))
           (interpose ", ")))]
   [:inline? [:span "Set popover body wrapper to "
              [:code "display: inline-block"]]]
   [:show-on-over? "Show popover on mouse over."]
   [:over-delay "Mouse over show delay in ms."]
   [:caret-inset [:span "Inset in pixels from position edge. Only applies to non-centered positions (e.g. not " [:code ":left-center"] ")."]]
   [:caret-base "Size in pixels of the caret triangle's base. Would be width when the caret is on the top or bottom, and height when the caret is on the left or right side."]
   [:caret-altitude "Size in pixels of the caret triangle's altitutde."]])

(defn real-world-examples []
  [sg/section
   [sg/heading headings ::real-world-examples]
   [:h3 "Tooltip"]
   (sg/example
     {:form
      [po/view
       {:initial-visible? true
        :inline? true
        :body [:div "hello world"]
        :popover [:div "HI"]}]})
   [:h3 "Dropdown Menu"]
   (sg/example
     {:form
      [po/view
       {:inline? true
        :position :right-center
        :esc-enabled? true
        :body (fn [po]
                [:div
                 {:style {:cursor 'pointer}
                  :on-click (fn []
                              (po/toggle po))}
                 "Menu"])
        :popover [ui/group
                  {:gap 4}
                  (->> ["Open" "Move" "Close"]
                       (map (fn [s]
                              [bts/bare
                               {:label s}])))]}]})

   [:h3 "With Input"]
   (sg/example
     {:form
      [po/view
       {:inline? true
        :initial-visible? false
        :esc-enabled? true
        :position :right-center
        :render-body
        (fn [po-state]
          [:div
           {:style {:cursor 'pointer}
            :on-click (fn []
                        (po/toggle po-state))}
           "Edit Title"])

        :popover-pad 16
        :render-popover
        (fn [po]
          [forms/form
           {:gap 12
            :on-submit (fn [d]
                         (prn d)
                         (po/hide po))
            :prop-validations
            {:title #(when (empty? %)
                       "Can't be empty!")}}
           [forms/group
            {:data-key :title
             :gap 8}
            [forms/label {:text "Title"}]
            [forms/text
             {:autofocus? true
              :style {:background-color "#333"
                      :border-color "#333"
                      :color 'white}}]
            [forms/error-text {:style {:color "#f88"}}]]
           [forms/group
            {:justify-content 'space-between
             :horizontal? "true"}
            [bts/bare {:label "Cancel"
                       :on-click (fn [] (po/hide po))}]
            [forms/submit-button {:label "Update"}]]])}]})])

(defn state-space []
  [sg/section
   [sg/heading headings ::state-space]
   (sg/example
     {:form
      [ui/group
       {:horizontal? true
        :style {:flex-wrap 'wrap}}
       (->> [:top-left :top-center :top-right
             :bot-left :bot-center :bot-right
             :left-top
             :left-center
             :left-bot
             :right-top
             :right-center
             :right-bot]
            (map (fn [position]
                   [:div {:style {:padding "40px 100px"}}
                    [po/view
                     {:initial-visible? true
                      :inline? true
                      :position position
                      :body [:div "hello"]
                      :popover [:div
                                {:style {:white-space 'nowrap}}
                                (str position)]}]])))]})])

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1913/signal-war-768.jpg"
             :height 300}}
    [sg/heading headings ::popover]
    [:p "Tooltips, a place to put contextual information for your users"]
    [:p "Popovers can be placed at the major cardinal directions."]
    [:p "Popovers will avoid screen edges where possible."]]
   [sg/section
    [sg/heading headings ::example]
    (sg/example
      {:form [po/view
              {:style {:display 'inline-block}
               :initial-visible? true
               :show-on-over? show-on-over?
               :over-delay over-delay
               :position position
               :inline? true
               :body
               [:div "Open"]
               :popover
               [:div
                {:style {:padding 5
                         :line-height "100%"}}
                "Popover"]}]
       :initial {:position :right-center
                 :show-on-over? false
                 :over-delay 0}
       :options
       [[:position :form]
        [:show-on-over? :boolean]
        [:over-delay :long]]})

    (sg/example
      {:form
       [po/view
        {:inline? true
         :position :right-center
         :initial-visible? true
         :theme {:bg-color "red"
                 :fg-color "yellow"
                 :box-shadow 'none}
         :body
         [:div "Themed"]
         :popover
         [:div
          {:style {:padding 5
                   :line-height "100%"}}
          "Popover"]}]})

    (sg/example
      {:form
       [po/view
        {:inline? true
         :position :right-top
         :show-on-over? true
         :over-delay 0
         :popover-pad 12
         :body
         [:div "Longer Content"]
         :popover
         [:div
          {:style {:width 300}}
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et."]}]})

    (sg/example
      {:form [po/view
              {:inline? true
               :position :right-center
               :show-on-over? true
               :over-delay 0
               :body
               [:div "Mouseover"]
               :popover
               [:div
                {:style {:padding 5
                         :line-height "100%"}}
                "Popover"]}]})

    (sg/example
      {:form [po/view
              {:position :right-center
               :inline? true
               :render-body
               (fn [po-state]
                 [:div
                  {:on-click (fn []
                               (po/toggle po-state))}
                  "Click to Toggle"])
               
               :popover
               [:div
                {:style {:padding 5
                         :line-height "100%"}}
                "Popover"]}]})]
   [sg/section
    [sg/heading headings ::usage]
    [:h3 "Options"]
    [sg/options-list
     {:options options}]
    [:h3 "Theming"]
    [sg/theme-rules-list
     {:theme-rules po/theme-docs}]]
   [real-world-examples]
   [state-space]])

(comment

  (browser/<set-root!
    [sg/standalone {:component sections
                    :headings headings}])

  (browser/<set-root!
    [sg/standalone {:component state-space}])

  (browser/<set-root!
    [sg/standalone {:component real-world-examples}])

  )





