(ns rx.browser.buttons
  (:require [rx.kitchen-sink :as ks]
            [rx.theme :as th]
            [rx.browser.components :as cmp]
            [rx.browser.feather-icons :as fi]
            [rx.icons.feather :as feather]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [garden.color :as gc]
            [clojure.string :as str]))

(defn lighten [color amount]
  (try
    (let [color (cond
                  (and (string? color) (not (gc/hex? color)))
                  (gc/from-name color)

                  :else color)]
      (gc/as-hex (gc/lighten color amount)))
    (catch js/Error e nil)))

(defn darken [color amount]
  (try
    (let [color (cond
                  (and (string? color) (not (gc/hex? color)))
                  (gc/from-name color)

                  :else color)]
      (gc/as-hex (gc/darken color amount)))
    (catch js/Error e nil)))

(defn calculate-common-style [{:keys [style label icon] :as opts}]
  (let [pad-style (th/pad-padding opts
                    (if label
                      {:vpad 6
                       :hpad 12}
                      {:vpad 4
                       :hpad 5}))

        {:keys [border-radius]}
        (th/resolve opts
          [[:border-radius ::border-radius]])]
    (merge
      {:border-style 'solid
       :border-width 2
       :font-weight 'bold
       :box-shadow 'none
       :font-size 14
       :border-radius border-radius
       :transition "background-color 0.1s ease, border-color 0.1s ease, color 0.1s ease"}
      pad-style
      style)))

(defn apply-style-overrides [styles opts]
  (merge
    styles
    {:style (merge
              (:style styles)
              (calculate-common-style opts))
     :style-over (merge
                   (:style-over styles)
                   (:style-over opts))
     :style-down (merge
                   (:style-down styles)
                   (:style-down opts))
     :style-disabled (merge
                       (:style-disabled styles)
                       (:style-disabled opts))}))

(defn calculate-solid-styles [opts]
  (let [{:keys [::color-intent-default
                ::color-intent-danger
                ::color-intent-none
                :fg-color]}
        (th/resolve opts
          [[:fg-color ::button-fg-color "white"]])

        main-color (or
                     (-> opts :style :background-color)
                     (condp = (:intent opts)
                       "danger" color-intent-danger
                       "none" color-intent-none
                       color-intent-default))]
    (-> {:style {:background-color main-color
                 :border-style 'solid
                 :border-color main-color
                 :border-width 2
                 :color fg-color
                 :font-weight 'bold
                 :box-shadow 'none
                 :font-size 14
                 :transition "background-color 0.1s ease, border-color 0.1s ease, color 0.1s ease"}
         :style-over {:background-color main-color #_(darken main-color 10)
                      :border-color main-color #_(darken main-color 10)}
         :style-down {:background-color main-color #_(darken main-color 15)
                      :border-color main-color #_(darken main-color 15)}
         :style-disabled {:background-color main-color #_(lighten main-color 25)
                          :border-color main-color #_(lighten main-color 25)}}
        (apply-style-overrides opts))))

(defn -button [{:keys [label
                       icon
                       type
                       on-click
                       disabled?
                       interaction-state
                       style
                       class]
                :as opts}
               {:keys [style
                       style-over
                       style-down
                       style-disabled]}]
  (let [style (merge
                {:outline 0
                 :cursor 'pointer
                 :line-height 1
                 :transition "background-color 0.1s ease, border-color 0.1s ease, color 0.1s ease"}
                (when icon
                  {:line-height 0})
                #_{:align-self 'flex-start}
                style)

        style-disabled (merge
                         style-disabled
                         {:cursor "not-allowed"})


        {:keys [icon-margin]}
        (th/resolve opts
          [[:icon-margin ::button-icon-margin 4]])]

    [cmp/hover
     (merge
       {:element :button
        :class class
        :disabled (when disabled? "disabled")
        :style (merge
                 #_{:align-self 'flex-start}
                 style
                 (if disabled?
                   style-disabled
                   (condp = interaction-state
                     "over" style-over
                     "down" style-down
                     nil)))
        :style-over style-over
        :style-down style-down
        :type (or type "button")
        
        :on-click (fn [e]
                    (when on-click
                      (on-click))
                    nil)})
     [ui/group
      {:gap icon-margin
       :horizontal? true
       :align-items 'center
       :style {:justify-content 'center}}
      (when icon
        [(if-let [comp (:comp icon)]
           comp
           (fi/lookup (:name icon)))
         (merge
           {:color (:color style)
            :size (if label 14 18)}
           (dissoc icon :name :set))])
      (when label label)]]))

(defn solid [opts]
  (-button opts
    (calculate-solid-styles opts)))

(defn calculate-hollow-styles [{:keys [style] :as opts}]
  #_(let [{:keys [::color-intent-default
                  ::color-intent-danger
                  ::color-intent-none
                  :border-color
                  :fg-color]}
          (th/resolve opts
            [[:border-color "#bbb"]])

          pad-style (th/pad-padding opts
                      {:vpad 6 :hpad 12})

          main-color (condp = (:intent opts)
                       "danger" color-intent-danger
                       "none" color-intent-none
                       color-intent-default)
          darker-color (darken main-color 10)
          darker-border-color (darken border-color 5)
        
          darkest-color (darken main-color 20)
          darkest-border-color (darken border-color 20)
        
          lighter-color (lighten main-color 20)
          lighter-border-color (lighten border-color 20)]
      (-> {:style {:background-color 'transparent
                   :color main-color
                   :border-style 'solid
                   :border-color border-color
                   :border-width 2
                   :font-weight 'bold
                   :font-size 14
                   :transition "background-color 0.1s ease, border-color 0.1s ease, color 0.1s ease"}
           :style-over
           {:color darker-color
            :border-color darker-border-color}
       
           :style-down
           {:color darkest-color
            :border-color darkest-border-color}
       
           :style-disabled
           {:color lighter-color
            :border-color lighter-border-color}}
          (apply-style-overrides opts))))

(defn hollow [opts]
  (-button opts
    (calculate-hollow-styles opts)))

(defn calculate-bare-styles [{:keys [style] :as opts}]
  (let [{:keys [::color-intent-default
                ::color-intent-danger
                ::color-intent-none
                :fg-color]}
        (th/resolve opts)

        main-color (condp = (:intent opts)
                     "danger" color-intent-danger
                     "none" color-intent-none
                     color-intent-default)
        
        darker-color main-color #_(darken main-color 10)
        
        darkest-color main-color #_(darken main-color 20)

        lighter-color main-color #_(lighten main-color 20)]
    (-> {:style (merge
                  {:background-color 'transparent
                   :color main-color
                   :border-style 'solid
                   :border-width 2
                   :border-color 'transparent
                   :font-weight 'bold
                   :font-size 14
                   :transition "background-color 0.1s ease, border-color 0.1s ease, color 0.1s ease"}
                  style)
         :style-over
         {:color darker-color
          :background-color "rgba(0,0,0,0.05)"}
       
         :style-down
         {:color darkest-color
          :background-color "rgba(0,0,0,0.1)"}
       
         :style-disabled
         {:color lighter-color}}
        (apply-style-overrides opts))))

(defn bare [opts]
  (-button opts
    (calculate-bare-styles opts)))

(defn test-pattern [opts]
  (let [theme (:theme opts)
        types [solid hollow bare]
        intents ["default" "danger" "none"]
        states ["null" "over" "down"]]
    [ui/group
     {:gap 16}
     (for [type types]
       [ui/group
        {:gap 20}
        (for [intent intents]
          (into
            [ui/group
             {:gap 16
              :horizontal? true
              :style {:padding 8
                      :margin 8
                      :background-color (:color/bg0 theme)}}]
            (concat
              (for [state states]
                [type
                 {:label "Press Me"
                  :theme theme
                  :interaction-state state
                  :intent intent}])
              [[type
                {:label "Press Me"
                 :theme theme
                 :intent intent
                 :disabled? true}]
               [type
                {:theme theme
                 :icon {:set "feather"
                        :name "settings"}}]
               [type
                {:label "Press Me"
                 :theme theme
                 :icon {:set "feather"
                        :name "settings"}}]])))])]))

(defn button [{:keys [before
                      label
                      after
                      icon
                      link?
                      on-click
                      style
                      hover-style
                      active-style
                      disabled-style
                      disabled?
                      gap
                      justify
                      align
                      class
                      href]
               :as opts}]
  (let [padding (th/pad-padding opts)
        default-button-style
        {:outline 0}]
    (into
      [ui/hover
       (merge
         {:element (if href :a :button)
          :on-click on-click
          :style (merge
                   padding
                   default-button-style
                   {:width "auto"
                    :height "auto"
                    :display 'flex
                    :flex-direction 'row
                    :align-items 'center
                    :justify-content 'center}
                   style
                   (when disabled?
                     disabled-style))
          :style-over hover-style
          :style-down active-style
          :disabled disabled?
          :class class}
         (select-keys opts [:active? :hover? :down? :href :target]))]
      (->> [(when icon
              [(if-let [comp (:comp icon)]
                 comp
                 (fi/lookup (:name icon)))
               (merge
                 {:color (:color style)
                  :size (if label 14 18)}
                 (dissoc icon :name :set))])
            before label after]
           (remove nil?)
           (interpose
             [:div {:style {:width (or gap 8)}}])))))

(defn test-basic []
  (let [aligns ["left" "center" "right"]
        justify ["left" "center" "right"]
        labels ["basic"]
        before [nil [fi/settings {}]]
        opts [{:label "Default"
               :pad 10}
              {:vpad 10
               :hpad 20
               :style {:background-color 'red
                       :color 'white
                       :border-radius 4}
               :hover-style {:background-color 'green}
               :active-style {:background-color 'blue}
               :label "RGB"}
              {:pad 10
               :label "Hammer"
               :style {:box-shadow "0px 2px 0px black"}
               :hover-style {:margin-top -3
                             :box-shadow "0px 5px 0px black"
                             :margin-bottom 3}
               :active-style {:margin-top 1
                              :box-shadow "0px 1px 0px black"
                              :margin-bottom -1}
               :on-click prn}
              {:label "Substack"
               :style {:padding "14px 20px"
                       :border-radius 4
                       :border "solid #ccc 1px"
                       :background-color 'transparent
                       :transition "background-color 1s ease"}
               :hover-style {:background-color "#f0f0f0"}}

              {:pad 10
               :label "Disabled"
               :style {:box-shadow "0px 2px 0px black"}
               :hover-style {:margin-top -3
                             :box-shadow "0px 5px 0px black"
                             :margin-bottom 3}
               :active-style {:margin-top 1
                              :box-shadow "0px 1px 0px black"
                              :margin-bottom -1}
               :on-click prn
               :disabled? true}
              ]]
    (browser/<show-component!
      [ui/g
       {:gap 20
        :style {:padding 30}}
       (for [opts opts]
         [:div
          [button opts]])])))

(comment

  (test-basic)

  (test-pattern)

  )



