(ns rx.browser.youtube-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg
             :refer-macros [example]]
            [rx.browser.youtube :as yt]
            [rx.browser.ui :as ui]
            [reagent.core :as r]))

(def headings
  [[::youtube :h1 "YouTube"]
   [::example :h2 "Example"]
   [::video-component :h2 "Video Component"]
   [::scrubber-component :h2 "Scrubber Component"]])

(defn scrubber-ex [{:keys [title] :as opts}]
  (let [state (yt/create-state)

        opts (merge
               opts
               {:state state
                :throttle-time 50})]
    
    (r/create-class
      {:reagent-render
       (fn []
         [ui/group
          [:h3 title]
          (sg/example
            {:form
             [ui/group
              {:gap 8}
              [yt/video
               (merge
                 (dissoc opts :theme)
                 {:style {:height 250}})]
              [yt/scrubber opts]]})])})))

(defn scrubber []
  (let [state (yt/create-state)]
    (r/create-class
      {:reagent-render
       (fn []
         [sg/section
          [sg/heading headings ::scrubber-component]
          [scrubber-ex
           {:title "Default"
            :video-id "Cihr9OWsEuk"}]
          [sg/section
           [:h3 {:id "youtube-scrubber-options"} "Options"]
           [sg/options-list
            {:options
             [[:hide-tick? "Hide current time tick"]
              [:disable-fixed-width-numeric? [:span
                                              "Scrubber has the css prop-val combo "
                                              [:code [:a {:href "https://developer.mozilla.org/en-US/docs/Web/CSS/font-variant-numeric"
                                                          :target "_blank"} "font-variant-numeric: tabular-nums"]]
                                              " set to force fixed-width rendering of digits for the current time value. This prevents the shifting / glitching effect resulting from rendering slightly different widths in quick successing. May not work with all font families. Set to true to disable."]]]}]]
          [scrubber-ex
           {:title "Hidden Tick"
            :video-id "QrR_gm6RqCo"
            :hide-tick? true}]
          [scrubber-ex
           {:title "Themed"
            :video-id "oQyFFTh_YGc"
            :theme {:bg-color "black"
                    :fg-color "white"
                    :tick-color "white"}}]
          [scrubber-ex
           {:title "Tick Inset"
            :video-id "GOFiGClu0uk"
            :theme {:tick-inset 3}}]
          [:h3 {:id "youtube-scrubber-theming"} "Theming"]
          [sg/theme-rules-list
           {:theme-rules
            yt/scrubber-theme-docs}]])})))

;; LdyabrdFMC8

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1883/telephonoscopic-news-768.jpg"
             :height 280}}
    [sg/heading headings ::youtube]
    [:p "Rx makes embedding YouTube videos easy, and adds some powerful functionality on top, like looping."]
    [:p "The YouTube API js will be added to the page automatically if not found."]]
   [sg/section
    [sg/heading headings ::example]
    (sg/example
      {:form [yt/video
              {:video-id video-id
               :loop loop
               :style {:height 400}}]
       :initial {:video-id "89Oc1UE7SS4"}
       :options
       [[:loop :form]
        [:video-id :string]]})]
   [sg/section
    [:h2 {:id "yt-component-options"} "Component Options"]
    [sg/options-list
     {:options
      [[:video-id "YouTube video id"]
       [:loop [:span "Vector denoting starting and ending timestamps in ms. Ex: " [:code "{:loop [1000 2000]}"] "."]]
       [:on-ready "Called with player object and ready event"]
       [:on-playing "Called when video starts playing, passed player object"]
       [:on-paused "Called when video is paused, passed player object"]
       [:on-time [:span "Called frequently with player object and current time when playing. See " [:code ":throttle-time"] "."]]
       [:throttle-time [:span "Rate (ms) at which " [:code ":on-time"] " will be called when playing."]]
       [:state [:span "Player state, used in api calls to imperitively control the YouTube player instance. Player state can be created outside of the video component via " [:code "yt/create-state"] "."]]
       [:on-state "Function called with internally created vid state."]]}]]
   [scrubber]])


(comment

  (browser/<set-root!
    [sg/standalone
     {:component scrubber}])

  (browser/<set-root!
    [sg/standalone
     {:component sections}])
  
  )
