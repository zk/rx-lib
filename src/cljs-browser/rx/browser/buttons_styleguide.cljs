(ns rx.browser.buttons-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.browser.forms :as forms]
            [rx.browser.buttons :as bts]
            [rx.browser.components :as cmp]
            [rx.theme :as th]
            [clojure.string :as str]))

(def headings
  [[::buttons :h1 "Buttons"]
   [::example :h2 "Example"]
   [::button-types :h2 "Button Types"]
   [::solid :h3 "Solid"]
   [::hollow :h3 "Hollow"]
   [::bare :h3 "Bare"]
   [::theming-examples :h2 "Theming Examples"]])

(def button-options
  [[:label "Text content of button"]
   [:icon "Map of icon opts"]
   [:intent "One of 'basic' (default), 'warning', or 'error'"]
   [:disbled? "Bool"]
   [:on-press "Handler for button press / fire"]
   [:style "Default state style"]
   [:style-over "Mouseover state style"]
   [:style-down "Mousedown / active state style"]])

(defn theming-examples []
  [sg/section
   [sg/heading headings ::theming-examples]
   [:p "Ultimately, button UI is highly dependent on the design of your app, so it's difficult to provide specific values for things like background color or border radius."]
   [:p "Here are some examples of how to control the look of your buttons."]
   (sg/example
     {:form
      [sg/group
       {:gap 12
        :horizontal? true
        :style {:flex-wrap 'wrap}}
       [bts/solid
        {:label "hello"
         :theme {:color/intent-default "blue"}}]
       [bts/solid
        {:label "hello"
         :style {:border-color "black"
                 :background-color "blue"
                 :box-shadow "0px 2px #888"}
         :style-over {:box-shadow "0px 3px #888"
                      :transform "translate3d(0,-1px,0)"
                      :border-color "black"}

         :style-down {:border-color "black"
                      :box-shadow "0px 1px #888"
                      :transform "translate3d(0,1px,0)"}}]

       [bts/solid
        {:label "hello"
         :theme {:color/intent-default "blue"}
         :style {:font-size 20
                 :padding "20px 40px 20px 40px"
                 :border-radius 999}}]]})])

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1886/prison-window-768.jpg"
             :height 400}}
    [sg/heading headings ::buttons]
    [:p "Three flavors of buttons: solid, hollow, and bare."]
    [:h3 "Namespace"]
    [:code "rx.browser.buttons"]]
   [sg/section
    [sg/heading headings ::example]
    (sg/example
      {:form [bts/solid {:label label
                         :icon icon
                         :disabled? disabled?
                         :intent intent
                         :on-click (fn []
                                     (js/alert "You clicked!"))}]
       :initial {:label "Click Me"
                 :icon {:set "feather"
                        :name "alert-circle"
                        :stroke-width 3}
                 :intent "default"}
       :options
       [[:label :string]
        [:disabled? :boolean]
        [:icon :form]
        [:intent :string]]})]
    
   [sg/section
    {:gap 16}
    [sg/heading headings ::button-types]
    [sg/heading headings ::solid]
    (sg/example
      {:form (let [themes [{:title "Light"
                            :theme {:color/intent-default "#596CDD"
                                    :color/intent-danger "#C73A3D"
                                    :color/intent-none "#333"}}]
                   disableds [false true]
                   kinds ["solid"]
                   intents ["default" "warning" "error"]
                   states ["null" "over" "down"]]
               (into
                 [cmp/group
                  {:gap 16}]
                 (for [{:keys [title theme icon]} themes]
                   [cmp/group
                    {:gap 4}
                    [cmp/text
                     {:style {:margin-left 8}}
                     title " Theme"]
                    (into
                      [cmp/group
                       {:horizontal? true
                        :gap 8}]
                      (concat
                        (for [state states]
                          [cmp/group
                           [cmp/text
                            {:style {:margin-left 8}}
                            state]
                           (into
                             [cmp/group
                              {:gap 8
                               :style {:padding 8
                                       :background-color
                                       (:color/bg0 theme)}}]
                             (for [kind kinds]
                               [(condp = kind
                                  "hollow" bts/hollow
                                  "bare" bts/bare
                                  bts/solid)
                                {:label title
                                 :icon icon
                                 :kind kind
                                 :theme theme
                                 :interaction-state state}]))])
                        [[cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "disabled"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:label title
                                :icon icon
                                :kind kind
                                :theme theme
                                :disabled? true}]))]
                         [cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "icon"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:icon {:set "feather"
                                       :name "settings"}
                                :kind kind
                                :theme theme}]))]]))])))})
    [:h3 "Options"]
    [sg/options-list
     {:options button-options}]]

   [sg/section
    {:gap 16}
    [sg/heading headings ::hollow]
    (sg/example
      {:form (let [themes [{:title "Light"
                            :theme {:color/intent-default "#596CDD"
                                    :color/intent-danger "#C73A3D"
                                    :color/intent-none "#333"}}]
                   disableds [false true]
                   kinds ["hollow"]
                   intents ["default" "warning" "error"]
                   states ["null" "over" "down"]]
               (into
                 [cmp/group
                  {:gap 16}]
                 (for [{:keys [title theme icon]} themes]
                   [cmp/group
                    {:gap 4}
                    [cmp/text
                     {:style {:margin-left 8}}
                     title " Theme"]
                    (into
                      [cmp/group
                       {:horizontal? true
                        :gap 8}]
                      (concat
                        (for [state states]
                          [cmp/group
                           [cmp/text
                            {:style {:margin-left 8}}
                            state]
                           (into
                             [cmp/group
                              {:gap 8
                               :style {:padding 8
                                       :background-color
                                       (:color/bg0 theme)}}]
                             (for [kind kinds]
                               [(condp = kind
                                  "hollow" bts/hollow
                                  "bare" bts/bare
                                  bts/solid)
                                {:label title
                                 :icon icon
                                 :kind kind
                                 :theme theme
                                 :interaction-state state}]))])
                        [[cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "disabled"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:label title
                                :icon icon
                                :kind kind
                                :theme theme
                                :disabled? true}]))]
                         [cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "icon"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:icon {:set "feather"
                                       :name "settings"}
                                :kind kind
                                :theme theme}]))]]))])))})
    [:h3 {:id "buttons-hollow-options"} "Options"]
    [sg/options-list
     {:options button-options}]]
    
   [sg/section
    {:gap 16}
    [sg/heading headings ::bare]
    (sg/example
      {:form (let [themes [{:title "Light"
                            :theme {:color/intent-default "#596CDD"
                                    :color/intent-danger "#C73A3D"
                                    :color/intent-none "#333"}}]
                   disableds [false true]
                   kinds ["bare"]
                   intents ["default" "warning" "error"]
                   states ["null" "over" "down"]]
               (into
                 [cmp/group
                  {:gap 16}]
                 (for [{:keys [title theme icon]} themes]
                   [cmp/group
                    {:gap 4}
                    [cmp/text
                     {:style {:margin-left 8}}
                     title " Theme"]
                    (into
                      [cmp/group
                       {:horizontal? true
                        :gap 8}]
                      (concat
                        (for [state states]
                          [cmp/group
                           [cmp/text
                            {:style {:margin-left 8}}
                            state]
                           (into
                             [cmp/group
                              {:gap 8
                               :style {:padding 8
                                       :background-color
                                       (:color/bg0 theme)}}]
                             (for [kind kinds]
                               [(condp = kind
                                  "hollow" bts/hollow
                                  "bare" bts/bare
                                  bts/solid)
                                {:label title
                                 :icon icon
                                 :kind kind
                                 :theme theme
                                 :interaction-state state}]))])
                        [[cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "disabled"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:label title
                                :icon icon
                                :kind kind
                                :theme theme
                                :disabled? true}]))]
                         [cmp/group
                          [cmp/text
                           {:style {:margin-left 8}}
                           "icon"]
                          (into
                            [cmp/group
                             {:gap 8
                              :style {:padding 8
                                      :background-color
                                      (:color/bg0 theme)}}]
                            (for [kind kinds]
                              [(condp = kind
                                 "hollow" bts/hollow
                                 "bare" bts/bare
                                 bts/solid)
                               {:icon {:set "feather"
                                       :name "settings"}
                                :kind kind
                                :theme theme}]))]]))])))})
    [:h3 {:id "buttons-bare-options"} "Options"]
    [sg/options-list
     {:options button-options}]

    [theming-examples]]])



(comment

  (browser/<set-root!
    [sg/standalone {:component sections
                    :headings headings}])

  (browser/<set-root!
    [sg/standalone {:component theming-examples}])

  )
