(ns rx.styleguide.sgcomponents
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [rx.kitchen-sink :as ks]
            [rx.browser.components :as bc]
            #_[rx.browser.modal2 :as modal]
            #_[nsfw.popbar :as popbar]
            #_[nsfw.mobile-nav :as mn]
            [rx.browser.youtube :as yt]
            [rx.styleguide.common :as com]
            [rx.css :as css]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn $popover []
  #_[com/$section
     {:title "Popover"
      :id "popover"
      :var 'nsfw.comps/$popover}
     [:p "Useful for tooltips, etc"]

     [com/$subsection
      {:title "Example"}
      (let [style {:width 80
                   :cursor 'pointer
                   :font-size 14}
            content "pop"]
        [:div
         [:div.flex-center
          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :top-left
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":top-left"]]]

          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :top-center
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":top-center"]]]

          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :top-right
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":top-right"]]]]

         [:div.flex-center
          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :left-center
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":left-center"]]]

          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :right-center
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":right-center"]]]]

         [:div.flex-center
          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :bot-left
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":bot-left"]]]

          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":bot-center"]]]

          [com/$checkerboard
           {:class "mg-xs"}
           [bc/$popover
            {:content content
             :position :bot-right
             :enable-mouse-over? true}
            [:div.text-center
             {:style style}
             ":bot-right"]]]]])]
     [com/$subsection
      {:title "Options"}
      [com/$opts
       {:position {:desc [:span
                          "Position of popover relative to `children`. One of "
                          [:span.code
                           (->> [:bot-center
                                 :bot-left
                                 :bot-right
                                 :top-center
                                 :top-left
                                 :top-right
                                 :left-center
                                 :right-center]
                                (map pr-str)
                                (interpose " | ")
                                (apply str))]
                          "."]}
        :width "Num in px. Forces width of popover, otherwise defaults to width of children."
        :enable-mouse-over? [:span
                             "Show on mouse hover / mobile touch? Overrides "
                             [:span.code ":visible?"]
                             "."]
        :border-color "Popup outline color, includes carat"
        :style "Top level wrapper style"
        :pop-style "Popover style"
        :offset "Offset in px of popover. Positive values move it closer, negative move it away."
        :visible? "Show popover"}]]])

(defn $prog-bar-mock []
  (let [!ui (r/atom nil)]
    (fn []
      [com/$section
       {:title "Prog Bar Mock"
        :id "prog-bar-mock"
        :var 'nsfw.comps/$prog-bar-mock}
       [:p "Mock progress bar. Looks deterministic but isn't. Max 20 seconds loading time."]

       [com/$subsection
        {:title "Example"}
        (let [style {:width 100
                     :cursor 'pointer}
              content "pop"]
          [:div.code
           "hello world"]
          [com/$checkerboard
           [:div.flex-center
            {:style
             {:width "100%"
              :height 100
              :background-color (css/lb :200)
              :position 'relative}}
            [bc/$prog-bar-mock
             {:loading? (:loading? @!ui)
              :done? (:done? @!ui)
              :stick-to :top}]

            [bc/$popover
             {:content "Completes after 3 seconds."
              :enable-mouse-over? true}
             [:button.btn-primary-alt
              {:on-click (fn [e]
                           (.preventDefault e)
                           (go
                             (swap! !ui assoc
                               :loading? false
                               :done? false)
                             (<! (timeout 16))
                             (swap! !ui assoc :loading? true)
                             (<! (timeout 3000))
                             (swap! !ui assoc :done? true))
                           nil)}
              "Start Loading"]]]])]
       [com/$subsection {:title "Options"}
        [com/$opts
         {:loading? "Start progress"
          :done? "Complete progress. Animates bar to completion."
          :style [:span "Top level container style. Class "
                  [:span.code ".prog-bar-mock"]]
          :stick-to [:span
                     [:span.code "nil | :top | :bot"]
                     ". Optional, if specified prog bar container must be "
                     [:span.code "position: relative"]
                     "."]
          :bar-height "Bar height in px"
          :bar-color "Bar color"}]]])))

(defn $flipper []
  (let [!run? (atom true)
        !quotes (r/atom
                  (cycle
                    (partition 2
                      ["CIA Realizes It’s Been Using Black Highlighters All These Years"
                       "https://politics.theonion.com/cia-realizes-its-been-using-black-highlighters-all-thes-1819568147"
                       "Jurisprudence Fetishist Gets Off On Technicality"
                       "https://www.theonion.com/jurisprudence-fetishist-gets-off-on-technicality-1819586446"
                       "Man Unknowingly Purchases Lifetime Supply of Condoms"
                       "https://local.theonion.com/man-unknowingly-purchases-lifetime-supply-of-condoms-1819591526"
                       "Sometimes I Feel Like I'm The Only One Trying To Gentrify This Neighborhood"
                       "https://local.theonion.com/sometimes-i-feel-like-im-the-only-one-trying-to-gentrif-1819584310"
                       "Man Says ‘Fuck It,’ Eats Lunch At 10:58 A.M."
                       "https://local.theonion.com/man-says-fuck-it-eats-lunch-at-10-58-a-m-1819574888"])))]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go-loop []
           (when @!run?
             (swap! !quotes rest)
             (<! (timeout 3000))
             (recur))))
       :component-will-unmount
       (fn [_]
         (reset! !run? false))
       :reagent-render
       (fn []
         [com/$section
          {:title "Flipper"
           :id "flipper"
           :var 'nsfw.comps/$flipper}
          [:p "Automatically manage and show content transitions through animation. Use to show the user that important information has changed."]
          [com/$subsection
           {:title "Example"}
           [:div.code
            "{:disabled? nil}"]
           [com/$checkerboard
            {:style {:min-height 100}}
            [bc/$flipper
             [:a {:href (second (first @!quotes))}
              (ffirst @!quotes)]]]

           [:div.code
            "{:disabled? true}"]
           [com/$checkerboard
            {:style {:min-height 100}}
            [bc/$flipper
             {:disabled? true}
             [:a {:href (second (first @!quotes))}
              (ffirst @!quotes)]]]]
          [com/$subsection
           {:title "Syntax"}
           [:pre
            "[comps/$flipper\n  \"Hello World\"]"]
           [:pre
            "[comps/$flipper\n  {:disabled? true}\n  \"Hello World\"]"]]

          [com/$subsection {:title "Options"}
           [com/$opts
            {:disabled? "Prevent content transition animation"}]]])})))

(defn $hamburger-menu []
  (let [!open? (r/atom nil)
        !run? (r/atom true)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go-loop []
           (when @!run?
             (swap! !open? not)
             (<! (timeout 2000))
             (recur))))
       :component-will-unmount
       (fn [_]
         (reset! !run? false))
       :reagent-render
       (fn []
         [com/$section
          {:title "Hamburger Menu"
           :id "hamburger-menu"
           :var 'nsfw.comps/$hamburger-menu}

          [com/$subsection {:title "Example"}
           [:div.row
            [:div.col-sm-4
             [:div.code "{:open? nil}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:open? false}]]]
            [:div.col-sm-4
             [:div.code "{:open? true}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:open? true}]]]

            [:div.col-sm-4
             [:div.code "{:color \"red\"}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:color (css/red2 :500)}]]]
            [:div.col-sm-4
             [:div.code "{:line-cap :round}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:line-cap :round}]]]
            [:div.col-sm-4
             [:div.code "{:line-width 15}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:line-width 15}]]]

            [:div.col-sm-4
             [:div.code "{:size 40}"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:size 40}]]]

            [:div.col-sm-4
             [:div "Transition"]
             [com/$checkerboard
              [bc/$hamburger-menu
               {:open? @!open?}]]]]]

          [com/$subsection {:title "Options"}
           [com/$opts
            {:open? "Lines vs cross"
             :line-cap [:span
                        "Menu lines endcap style. One of "
                        [:span.code.bold ":butt | :round (default) | :square"]]
             :line-width "Line thickness in px"
             :color "Line color"
             :size "Width & height in px. Default 25px."
             :style "Container style"}]]])})))

(defn $copy-button []
  [com/$section
   {:title "Copy Button"
    :id "copy-button"
    :var 'nsfw.comps/$copy-button}
   [:p "Styling can be done either by passing options directly to the component, or by CSS. See " [:span.code "nsfw.comps/copy-button-css"] " which generates the necessary styles and takes identical properties to the component call."]
   [:p "Styling by component is useful when you have a small number of disparately styled copy buttons, styling by css is useful when you have many copy links styled the same."]
   [com/$subsection {:title "Example"}
    [com/$checkerboard
     [bc/$copy-button]]]
   [com/$subsection {:title "Options"}
    [com/$opts
     {:text "Text to copy"
      :style "Container style"
      :bg-color "At rest background color"
      :fg-color "At rest icon color"
      :active-bg-color [:span "Background color when pressed. Also styleable with CSS."]
      :active-fg-color [:span "Foreground color when pressed. Also styleable with CSS."]}]]])

(defn $countdown []
  #_[com/$section
     {:title "Countdown"
      :id "countdown"
      :var 'nsfw.comps/$countdown}
     [com/$subsection {:title "Example"}
      [:div
       [:div.code "{:end-ts (+ (ks/now) (* 1000 60 60 24 3.5))}"]
       [com/$checkerboard
        [bc/$countdown {:end-ts (+ (ks/now) (* 1000 60 60 24 3.5))}]]]
      [:div
       [:div.code "{:size :sm}"]
       [com/$checkerboard
        [bc/$countdown
         {:end-ts (+ (ks/now) (* 1000 60 60 24 3.5))
          :size :sm}]]]
      [:div
       [:div.code "{:title \"Hello World\"}"]
       [com/$checkerboard
        [bc/$countdown
         {:title "Hello World"
          :end-ts (+ (ks/now) (* 1000 60 60 24 3.5))}]]]
      [com/$subsection
       {:title "Options"}
       [com/$opts
        {:end-ts "Countdown to date / time in UNIX ms timestamp"
         :size [:span.code ":sm (default) | :md"]
         :gutter-size "Width between counters"}]]]])

(defn $modal []
  #_[:div
     [com/$section
      {:title "Modal"
       :id "modal"}
      [:ul
       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   #_(modal/<show [::modal-content]))}
          [:span.code "(modal/<show [:route-key])"]]
         [:pre
          "[modal/$global
  {:initial-route [...]
   :initial-visible? bool}]"]]]]]])

(defn $popbar []
  #_[:div
     [com/$section
      {:title "Popbar"
       :var nil #_'nsfw.popbar/$container
       :id "popbar"}
      [:ul
       [:li
        [bc/$a {:on-click
                (fn []
                  (popbar/show [:div.pad-md "Hello Modal"]))}
         [:span.code "(popbar/show [:div.pad-md \"Hello Modal\"])"]]]
       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   (popbar/show :on-bot-view))}
          [:span.code "(popbar/show :on-bot-view)"]]]]
       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   (popbar/show :on-top-view))}
          [:span.code "(popbar/show :on-top-view)"]]]]
       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   (popbar/show :on-right-view))}
          [:span.code "(popbar/show :on-right-view)"]]]]
       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   (popbar/show :on-left-view))}
          [:span.code "(popbar/show :on-left-view)"]]]]

       [:li
        [:div
         [bc/$a {:on-click
                 (fn []
                   (popbar/hide))}
          [:span.code "(popbar/hide)"]]]]]
      [com/$subsection
       {:title "Example Code"}
       [:pre
        "[popbar/$container\n [{:key :on-bot-view\n   :comp  [:div.pad-md\n           {:style {:background-color (css/lb :500)\n                    :color 'white\n                    :width \"100%\"\n                    :height \"100%\"}}\n           \"Hello Bot\"]}\n  {:key :on-top-view\n   :stick-to :top\n   :comp  [:div.pad-md\n           {:style {:background-color (css/lb :500)\n                    :color 'white\n                    :width \"100%\"\n                    :height \"100%\"}}\n           \"Hello Top\"]}\n  {:key :on-right-view\n   :stick-to :right\n   :comp  [:div.pad-md\n           {:style {:background-color (css/lb :500)\n                    :color 'white\n                    :width \"100%\"\n                    :height \"100%\"}}\n           \"Hello Right\"]}\n  {:key :on-left-view\n   :stick-to :left\n   :comp  [:div.pad-md\n           {:style {:background-color (css/lb :500)\n                    :color 'white\n                    :width \"100%\"\n                    :height \"100%\"}}\n           \"Hello Left\"]}]]"]]
      [com/$opts {}
       {:stick-to [:span.code ":bot (default) | :top | :left | :right"]
        :visible? "Show popbar"
        :initial-view "Initial view in popbar. View key, form, or function."
        :initial-args "Initial args passed to view function."
        :on-visible-change [:span "Fn called when visibility changes. "
                            [:span.code "(on-visible-change visible?)"]]}]]
     [popbar/$container
      [{:key :on-bot-view
        :comp  [:div.pad-md
                {:style {:background-color (css/lb :500)
                         :color 'white
                         :width "100%"
                         :height "100%"}}
                "Hello Bot"]}
       {:key :on-top-view
        :stick-to :top
        :comp  [:div.pad-md
                {:style {:background-color (css/lb :500)
                         :color 'white
                         :width "100%"
                         :height "100%"}}
                "Hello Top"]}
       {:key :on-right-view
        :stick-to :right
        :comp  [:div.pad-md
                {:style {:background-color (css/lb :500)
                         :color 'white
                         :width "100%"
                         :height "100%"}}
                "Hello Right"]}
       {:key :on-left-view
        :stick-to :left
        :comp  [:div.pad-md
                {:style {:background-color (css/lb :500)
                         :color 'white
                         :width "100%"
                         :height "100%"}}
                "Hello Left"]}]]])

(defn $video []
  [com/$section
   {:title "Video"
    :id "video"}
   [com/$subsection
    {:title "Example"}
    [bc/$video
     {:mp4 "https://www.bukwild.com/uploads/encodes/zp8rhXS0PVRK7maZdr6ZgZTU7tO4MOub/mp4.mp4"
      :controls? true
      :style {:width "100%"
              :min-height 350}}]]
   [com/$subsection
    {:title "Options"}
    [com/$opts
     {:mp4 "mp4 url"
      :webm "WEBM url"
      :ogg "Ogg url"
      :autoplay? "Autoplay?"
      :loop? "Loop?"
      :controls? "Controls?"
      :muted? "Muted?"
      :playinline? "Playinline?"}]]])

(defn $vidbg []
  (let [!switch? (r/atom false)
        !run? (atom true)
        !in-view? (r/atom false)
        blue "#0C3DEA"
        red (css/red2 :A700)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go-loop []
           (when @!run?
             (<! (timeout 1000))
             (swap! !switch? not)
             (recur))))
       :component-will-unmount
       (fn [_]
         (reset! !run? false))
       :reagent-render
       (fn []
         [com/$section
          {:title "VidBg"
           :id "vidbg"}
          [com/$subsection
           {:title "Example"}
           [bc/$track-in-view
            {:on-enter (fn []
                         (reset! !in-view? true))
             :on-exit (fn []
                        (reset! !in-view? false))}
            [bc/$vidbg
             {:playing? @!in-view?
              :loop? true
              :mp4 "https://www.bukwild.com/uploads/encodes/zp8rhXS0PVRK7maZdr6ZgZTU7tO4MOub/mp4.mp4"
              :style {:width "100%"
                      :height 500}}
             [:div.pad-md.flex-center
              {:style {:background-color "rgba(0,0,0,0.4)"
                       :width "100%"
                       :height "100%"}}
              [:div
               {:style {:color 'white}}
               [:h1.impact.bold.text-center
                {:style {:letter-spacing 1
                         :line-height "140%"
                         :font-size 60
                         :text-transform 'uppercase
                         :margin-bottom css/mdpx}}
                "Great Artists "
                [:span
                 {:style {:background-color (if @!switch?
                                              red
                                              blue)
                          :padding (str #_css/xspx " " #_ css/smpx)
                          :color 'white}}
                 "Steal"]]
               [:div.text-center
                #_[page/$interpose-children
                   {:separator " | "
                    :style {:line-height "1.6em"
                            :color 'white}}
                   (->> ["Fonts In Use" "https://fontsinuse.com"
                         "Dribbble" "https://dribbble.com/"
                         "Behance Interaction" "https://www.behance.net/galleries/8/Interaction"
                         "pages.xyz" "https://pages.xyz"
                         "Really Good Emails" "https://reallygoodemails.com/"
                         "9 Squares" "http://9-squares.tumblr.com/"]
                        (partition 2)
                        (map-indexed
                          (fn [i [title href]]
                            [bc/$abl
                             {:href href
                              :key i
                              :style {:white-space 'nowrap
                                      :color 'white}}
                             title])))]]]]]]]

          [com/$subsection {:title "Options"}
           [com/$opts {:webm "WEBM url"
                       :mp4 "MP4 url"
                       :ogg "Ogg Url"
                       :loop? "Loop?"}]]])})))

(defn $mobile-nav []
  [:div
   [com/$section
    {:title "Mobile Nav"
     :id "mobile-nav"}
    [:p "Built in mobile nav container and hamburger menu."]
    [com/$subsection {:title "Hamburger"}
     [:div.bold "nsfw.comps/$hamburger-menu"]
     [com/$checkerboard
      [:div.shadow-sm
       {:style {:width "100%"}}

       [:div.flex-right.pad-sm
        {:style {:width "100%"
                 :border-bottom "solid #ccc 1px"}}
        #_[mn/$hamburger-menu]]]]]

    [com/$subsection {:title "Container"}
     [:div.bold "nsfw.comps/$container"]]]])

(defn $track-in-view []
  (let [!over? (r/atom false)]
    (fn []
      [com/$section
       {:title "Track In View"
        :id "track-in-view"}
       [com/$subsection
        {:title "Example"}
        [com/$checkerboard
         [:div.pad-sm.bold
          "In View: " (pr-str @!over?)]
         [:div
          {:style {:overflow-y 'scroll
                   :height 300
                   :width "100%"}}

          (->> (range 8)
               (map (fn [i]
                      [:div.pad-sm.mg-sm.text-center
                       {:key i
                        :style {:background-color 'white}}
                       "Before " i])))

          [bc/$track-in-view
           {:fire-on-rapid-scroll? true
            :throttle 16
            :on-enter
            (fn []
              (reset! !over? true))
            :on-exit
            (fn []
              (reset! !over? false))
            :class "pad-sm mg-sm text-center"
            :style {:background-color 'green
                    :color 'white}}
           "Tracking Here"]

          (->> (range 8)
               (map (fn [i]
                      [:div.pad-sm.mg-sm.text-center
                       {:key i
                        :style {:background-color 'white}}
                       "After " i])))]]]])))


(defn $youtube-video []
  [com/$section
   {:title "Youtube Video"
    :id "youtube-video"
    :var 'nsfw.youtube/$video}


   [:p [:a {:href "https://developers.google.com/youtube/iframe_api_reference"} "API Reference"]]

   [:pre "[yt/$video
  {:video-id \"3vC5TsSyNjU\"
   :width \"100%\"
   :height \"100%\"}]"]
   [com/$checkerboard
    {:style {:height 300}}
    [yt/video {:video-id "3vC5TsSyNjU"
               :width "100%"
               :height "100%"}]]
   [com/$subsection
    {:title "Options"}
    [com/$opts
     {:video-id [:div
                 "Youtube Video ID (ex "
                 [:span.code "3vC5TsSyNjU"]
                 ")"]

      :width "width"
      :height "height"

      :player-vars [:a {:href "https://developers.google.com/youtube/player_parameters?playerVersion=HTML5"}
                    "Player Vars"]

      :on-player "[player] Called with created player object"
      :on-playing "[player]"
      :on-paused "[player]"
      :on-ready "[player event]"
      :on-state-change "[player event]"
      :on-props "[player props]"

      :on-time "[player current-time-ms]"
      :throttle-time "Throttle current time check in ms"}]]

   [com/$subsection
    {:title "API"}
    [com/$opts
     {'(playing? player) "Is video playing?"}]]])

(defonce !view (r/atom :one))

(comment

  (reset! !view :one)
  (reset! !view :two)

  )

(defn $htabs []
  [com/$section
   {:title "Horizontal Tabs (htabs)"
    :id "htabs"}
   [com/$subsection
    {:title "Example"}
    [:div
     {:style {:height 300
              :border "solid #eee 1px"
              :display 'flex}}
     [bc/$htabs
      {}
      [{:key :one
        :title "One"
        :render (fn []
                  [:div
                   {:style {:background-color 'red
                            :padding 5
                            :color 'white
                            :flex 1}}
                   "ONE"])}
       {:key :two
        :title "Two"
        :render (fn []
                  [:div
                   {:style {:background-color 'blue
                            :padding 5
                            :width "100%"
                            :height "100%"
                            :color 'white}}
                   "TWO"])}]]]]
   #_[com/$subsection
      {:title "Options"}
      [com/$opts
       {:mp4 "mp4 url"
        :webm "WEBM url"
        :ogg "Ogg url"
        :autoplay? "Autoplay?"
        :loop? "Loop?"
        :controls? "Controls?"
        :muted? "Muted?"
        :playinline? "Playinline?"}]]])

(def nav
  {:title "Components"
   :href "/sg/components"
   :route-key ::route
   :links [{:href "/sg/components#video"
            :title "Video"}
           {:href "/sg/components#vidbg"
            :title "VidBg"}
           {:href "/sg/components#youtube-video"
            :title "Youtube Video"}
           {:href "/sg/components#popover"
            :title "Popover"}
           {:href "/sg/components#prog-bar-mock"
            :title "Prog Bar Mock"}
           {:href "/sg/components#flipper"
            :title "Flipper"}
           {:href "/sg/components#hamburger-menu"
            :title "Hamburger"}
           {:href "/sg/components#copy-button"
            :title "Copy Button"}
           {:href "/sg/components#countdown"
            :title "Countdown"}
           {:href "/sg/components#modal"
            :title "Modal"}
           {:href "/sg/components#popbar"
            :title "Popbar"}
           {:href "/sg/components#mobile-nav"
            :title "Mobile Nav"}
           {:href "/sg/components#track-in-view"
            :title "Track In View"}]})

(defn $content []
  [:div.pad-lg
   [$htabs]
   [$video]
   [$vidbg]
   [$youtube-video]
   [$popover]
   [$prog-bar-mock]
   [$flipper]
   [$hamburger-menu]
   [$copy-button]
   [$countdown]
   [$modal]
   [$popbar]
   [$mobile-nav]
   [$track-in-view]])

(defn modal-content-view [{:keys []}]
  {:render
   (fn [{:keys [dismiss]}]
     [:div
      {:style {:padding 40
               :background-color 'black
               :border-radius 8
               :color 'white}}
      "Hello World"
      ])})

(defn views []
  {::modal-content modal-content-view})

