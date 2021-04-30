(ns rx.browser.youtube
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.browser.components :as cmp]
            [rx.browser.ui :as ui]
            [rx.browser :as browser]
            [rx.theme :as th]
            [rx.icons.feather :as feather]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [dommy.core :as dommy]
            [cljs-http.client :as hc]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout sliding-buffer]
             :refer-macros [go go-loop]]))

(defonce !tag (atom nil))

(defonce !delayed-inits (atom []))

(defn attach-ready []
  (set! js/onYouTubeIframeAPIReady
        (fn []
          (doseq [f @!delayed-inits]
            (f))
          (reset! !delayed-inits []))))

(defn find-player [p-or-vid-state]
  (if (map? p-or-vid-state)
    (try
      (deref (:!player p-or-vid-state))
      (catch js/Error e nil))
    p-or-vid-state))
 
;; Player API

(defn play-video [p]
  (when-let [p (find-player p)]
    (.playVideo p)))

(defn pause-video [p]
  (when-let [p (find-player p)]
    (.pauseVideo p)))

(defn stop-video [p]
  (when p
    (.stopVideo p)))

(defn get-player-state [p]
  (when p
    (and
      (fn? (.-getPlayerState p))
      (.getPlayerState p))))

(defn seek-to [p-or-vid-state ms & [allow-seek-ahead?]]
  (when p-or-vid-state
    (let [p (find-player p-or-vid-state)]
      (try
        (.seekTo p (/ ms 1000) allow-seek-ahead?)
        true
        (catch js/Error e
          (prn e)
          (ks/prn "rx.browser.youtube: player not initialized")
          false)))))

(defn mute [p]
  (when p
    (.mute p)))

(defn unmute [p]
  (when p
    (.unmute p)))

(defn muted? [p]
  (when p
    (.isMuted p)))

(defn get-volume [p]
  (when p
    (.getVolume p)))

(defn set-volume
  ;; n [0 100]
  [p n]
  (when p
    (.setVolume p n)))

(defn set-size [p width height]
  (when p
    (.setSize p width height)))

(defn get-playback-rate [p]
  (when p
    (.getPlaybackRate p)))

(defn set-playback-rate [p n]
  (when p
    (try
      (.setPlaybackRate p n)
      (catch js/Error e
        (prn "Couldn't set playback rate")))))

(defn get-available-playback-rates [p]
  (when p
    (js->clj (.getAvailablePlaybackRates p))))

;; vid info

(defn get-duration [p]
  (when p
    (* 1000 (.getDuration p))))

(defn get-video-url [p]
  (when p
    (.getVideoUrl p)))

(defn get-video-data [p]
  (when p
    (js->clj (.getVideoData p) :keywordize-keys true)))

(defn get-video-embed-code [p]
  (when p
    (.getVideoEmbedCode p)))

(defn add-listener [p event f]
  (when p
    (.addEventListener p event f)))

(defn rem-listener [p event f]
  (when p
    (.removeEventListener p event f)))

;; playback status

(defn get-video-loaded-fraction [p]
  (when p
    (.getVideoLoadedFraction p)))

(defn round-to-resolution [yt-time]
  (when yt-time
    (ks/round (* 1000 yt-time))))

(defn get-current-time [p]
  (when p
    (try
      (round-to-resolution
        (.getCurrentTime p))
      (catch js/Error e
        (prn e)
        nil))))

(defn get-video-start-bytes [p]
  (when p
    (.getVideoStartBytes p)))

(defn get-video-bytes-loaded [p]
  (when p
    (.getVideoBytesLoaded p)))

(defn get-video-bytes-total [p]
  (when p
    (.getVideoBytesTotal p)))

(defn script-loaded? []
  (try
    (= 1 (.-loaded js/YT))
    (catch js/Error e false)))

(defn script-attached? []
  (or (script-loaded?)
      @!tag))

(defn attach-script []
  (let [tag (.createElement js/document "script")
        _ (set! (.-src tag) "https://www.youtube.com/iframe_api")
        first-script-node (.item (.getElementsByTagName js/document "script") 0)
        parent (.-parentNode first-script-node)]
    (.insertBefore parent tag first-script-node)
    (reset! !tag tag)))

(defn run-after-script-load [f]
  (if (script-loaded?)
    (f)
    (swap! !delayed-inits
           conj
           f)))

(defn create-player [node
                     {:keys [video-id
                             width height
                             autoplay?
                             player-vars

                             on-ready
                             on-state-change]
                      :or [width 500
                           height 300]}]
  (let [p (js/YT.Player.
            node
            (clj->js
              {:height height
               :width width
               :playerVars player-vars
               ;;:videoId video-id
               :events {:onReady on-ready
                        :onStateChange on-state-change}}))]
    #_(.loadVideoById
        p
        #js {:videoId video-id
             :startSeconds 40})
    p))

(defn playing? [p]
  (= (get-player-state p) 1))

(defn toggle-play-pause [p]
  (if (playing? p)
    (pause-video p)
    (play-video p)))

(defn toggle-play-stop [p]
  (if (playing? p)
    (stop-video p)
    (play-video p)))

(defn gather-props [player]
  {:playing? (playing? player)
   :time (get-current-time player)
   :current-time (get-current-time player)
   :duration (get-duration player)})

(defn update-player-opts [p
                          {:keys [] :as old}
                          {:keys [playback-rate] :as new}]
  (when p
    (when (or (not= (:width old) (:width new))
              (not= (:height old) (:height new)))
      (set-size p (:width new) (:height new)))

    (set-playback-rate
      p
      (or playback-rate 1))))

(defn update-playing [p playing-flag?]
  (when p
    (when-not (= (playing? p) playing-flag?)
      (if playing-flag?
        (play-video p)
        (pause-video p)))))

(defn handle-loop [p [^long low ^long high]]
  (when (and low high)
    (let [^long ms (get-current-time p)]
      (when ms
        (when (> ms high)
          (seek-to p low))
        (when (< ms low)
          (seek-to p low))))))

(defn cdu-diff [f]
  (fn [this [_ & old-args]]
    (let [new-args (rest (r/argv this))]
      (f old-args new-args this))))

(defn create-state []
  {:!player (atom nil)
   :!video-id (atom nil)
   :!video-id->stats (r/atom nil)
   :!initialize-video (atom nil)})

(defn current-stats [{:keys [!video-id->stats !video-id]}]
  (get @!video-id->stats @!video-id))

(defn !get-current-time [state]
  (:current-time (current-stats state)))

(defn !get-current-pct [state]
  (let [{:keys [current-time duration]}
        (current-stats state)]
    (/ current-time duration)))

(defn seek-to-pct [{:keys [!player]} pct]
  (let [duration (get-duration @!player)
        ms (* pct duration)]
    (seek-to @!player ms)))

(defn has-video-id? [vid-state]
  @(:!video-id vid-state))

(defn vid-status-cursor [{:keys [!video-id->stats]} video-id]
  (r/cursor !video-id->stats [video-id]))

(defn load-and-play-video-id [{:keys [!player
                                      !initialize-video
                                      !video-id]}
                              video-id
                              start-ms]
  (when-let [f @!initialize-video]
    #_(f {:video-id video-id})
    (reset! !video-id video-id)
    (.cueVideoById
      @!player
      (clj->js
        {:videoId video-id
         :startSeconds (/ (or start-ms 0) 1000)}))
    (play-video @!player)))

(defn video [{:keys [on-player
                     on-playing
                     on-paused
                     on-ready
                     on-cued
                     on-ended
                     on-state-change
                     on-props
                     
                     on-time
                     throttle-time
                     start-seconds
                     playback-rate
                     initial-playing?
                     
                     loop
                     style
                     state
                     on-state]
              :or {on-playing (fn [])
                   on-paused (fn [])}
              :as opts}]
  (when-not style
    (ks/pn "No style provided, resulting video will have a width and height of 0"))
  (let [debug? true

        {:keys [!player
                !video-id
                !video-id->stats
                !initialize-video]
         :as state}
        (or state (create-state))

        _ (when on-state
            (on-state state))

        !node (atom nil)

        !runs (atom {})
        !current-run-id (atom nil)

        !loop (atom loop)

        !opts (atom opts)

        internal-on-cued (fn []
                           (update-player-opts @!player nil @!opts)
                           (when initial-playing?
                             (play-video @!player))
                           (when on-cued (on-cued)))

        internal-on-ended (fn []
                            (when on-ended
                              (on-ended)))

        internal-on-playing
        (fn []
          (when debug?
            (ks/prn "internal-on-playing"))
          (let [next-run-id (ks/uuid)
                last-run-id @!current-run-id]
            (swap!
              !runs
              (fn [runs]
                (-> runs
                    (assoc last-run-id nil)
                    (assoc next-run-id true))))

            (reset! !current-run-id next-run-id)

            (go-loop []
              (when (get @!runs next-run-id)
                (when on-time
                  (on-time
                    @!player
                    (get-current-time @!player)))
                (swap! !video-id->stats
                  update
                  @!video-id
                  merge
                  (when-let [t (get-current-time @!player)]
                    {:current-time t})
                  {:playing? (playing? @!player)})
                (when @!loop
                  (handle-loop @!player @!loop))
                (<! (timeout (or throttle-time 200)))
                (recur))))

          (when on-playing
            (on-playing @!player)))

        internal-on-paused
        (fn []
          (when debug?
            (ks/prn "internal-on-paused"))
          (when @!current-run-id
            (swap! !runs assoc @!current-run-id nil))

          (when on-paused
            (on-paused @!player)))

        initialize-video (fn [opts]
                           (when debug?
                             (ks/prn "initialize-video" opts))
                           (reset! !player
                             (create-player
                               @!node
                               (merge
                                 (select-keys
                                   opts
                                   [:width
                                    :height
                                    :video-id
                                    :player-vars])
                                 
                                 {:on-ready
                                  (fn [e]
                                    (when debug?
                                      (ks/prn "on-ready"))
                                    (let [{:keys [playback-rate]} opts]
                                      (.cueVideoById
                                        @!player
                                        (clj->js
                                          {:videoId (:video-id opts)
                                           :startSeconds
                                           (or (:start-seconds opts) 0)}))

                                      (when on-ready
                                        (when debug?
                                          (ks/prn "init on ready"))
                                        (on-ready @!player e))
                                      (when-let [props (gather-props @!player)]
                                        (swap! !video-id->stats
                                          update
                                          @!video-id
                                          merge
                                          props)
                                        (when on-props
                                          (on-props
                                            @!player
                                            props)))))

                                  :on-state-change
                                  (fn [e]

                                    (let [state (.-data e)]
                                      (when debug?
                                        (ks/prn "init on state change" state))
                                      (condp = state
                                        -1 nil ; unstarted
                                        0 (internal-on-ended) ; ended
                                        1 (internal-on-playing)
                                        2 (internal-on-paused)
                                        3 nil ; buffering
                                        5 (internal-on-cued) ; cued
                                        nil))


                                    (when on-state-change
                                      (on-state-change e))


                                    (when-let [props (gather-props @!player)]
                                      (swap! !video-id->stats
                                        update
                                        @!video-id
                                        merge
                                        props)
                                      (when on-props
                                        (on-props
                                          @!player
                                          props))))})))

                           (when on-player
                             (when @!current-run-id
                               (swap! !runs assoc @!current-run-id nil))
                             (on-player @!player)))

        _ (reset! !initialize-video initialize-video)]

    (r/create-class
      {:component-did-mount
       (fn [_]
         (when-not (script-attached?)
           (attach-script))
         (attach-ready)
         (run-after-script-load
           (fn []
             (initialize-video opts))))

       :component-will-unmount
       (fn [_]
         (when @!player
           (.destroy @!player))
         (when (and @!tag
                    (.-parentNode @!tag))
           (try
             (.removeChild
               (.-parentNode @!tag)
               @!tag)
             (catch js/Error e
               (prn "Error removing yt script from parent")
               nil)))
         (when @!current-run-id
           (swap! !runs assoc @!current-run-id nil)))

       :component-did-update
       (cdu-diff
         (fn [[{old-loop :loop
                old-pbr :playback-rate
                old-playing? :playing?
                old-seek-request :seek-request
                :as old-opts}]
              [{new-loop :loop
                new-pbr :playback-rate
                new-playing? :playing?
                new-seek-request :seek-request
                :as new-opts}]]

           (reset! !opts new-opts)

           (run-after-script-load
             (fn []
               (if (not= (:video-id old-opts)
                         (:video-id new-opts))
                 (initialize-video new-opts))

               (when (not= old-loop new-loop)
                 (reset! !loop new-loop))))

           (when (not= old-pbr new-pbr)
             (set-playback-rate
               @!player
               new-pbr))

           (when (not= old-playing? new-playing?)
             (if new-playing?
               (play-video @!player)
               (pause-video @!player)))

           #_(when (not= old-seek-request new-seek-request)
               (seek-to @!player (:ms new-seek-request)))

           (set-size
             @!player
             (:width new-opts)
             (:height new-opts))


           #_(run-after-script-load
               (fn []
                 (when (not= (:playing? old-opts) (:playing? new-opts))
                   (update-playing
                     @!player
                     (:playing? new-opts)))))))

       :reagent-render
       (fn [{:keys [video-id width height]}]
         [:div.yt-vid-wrapper
          {:key video-id
           :style style}
          [:div.yt-vid
           {:ref #(when % (reset! !node %))
            :style {:width "100%"
                    :height "100%"}}]])})))

(defn <videos-by-ids [{:keys [::api-key]}
                      video-ids
                      & [{:keys [limit offset]
                          :or {limit 10}}]]
  (go
    (let [res (<! (hc/get
                    "https://www.googleapis.com/youtube/v3/videos"
                    {:query-params {:key api-key
                                    :part "snippet"
                                    :maxResults limit
                                    :id (->> video-ids
                                             (interpose ",")
                                             (apply str))}}))]
      (if (:success res)
        [(:body res)]
        [nil res]))))

(defn <yt-api-snippet-search [{:keys [::api-key]}
                              q
                              & [{:keys [limit offset]
                                  :or {limit 10
                                       offset 0}}]]
  (go
    (let [res (<! (hc/get
                    "https://www.googleapis.com/youtube/v3/search"
                    {:query-params {:key api-key
                                    :part "snippet"
                                    :maxResults limit
                                    :q q}}))]
      (if (:success res)
        {:rx.res/data (:body res)}
        {:rx.res/anom
         {:rx.anom/description
          (-> res
              :body
              str)}
         :http/response res}))))

(defn yt-thumb->thumbnail [{:keys [default medium high]}]
  (merge
    default
    {:md medium
     :lg high}))

(defn yt-video->video [ytv]
  (let [{:keys [snippet]} ytv
        {:keys [publishedAt channelId title
                description
                thumbnails
                channelTitle]} snippet]
    {::id (str "youtube-" (-> ytv :id :videoId))
     ::published-ts (->> ytv
                         :snippet
                         :publishedAt
                         ks/from-iso8601)
     ::description description
     ::thumbnail (yt-thumb->thumbnail thumbnails)
     ::title title
     ::video-id (-> ytv :id :videoId)
     ::channel-id (->> ytv
                       :snippet
                       :channelId)}))

(defn search-response->videos [yt-resp]
  (->> yt-resp
       res/data
       :items
       (filter #(= "youtube#video"
                   (-> % :id :kind)))
       (map yt-video->video)))

(defn unescape-html [s]
  (let [dp (js/DOMParser.)
        doc (.parseFromString dp s "text/html")]
    (.. doc -documentElement -textContent)))

(defn video-grid [{:keys [columns
                          on-select-video
                          on-remove-video
                          title-style
                          ellipsis-title?
                          show-remove?
                          videos]
                   :as opts
                   :or {columns 5}}]
  (let [{:keys [::bg-color
                ::fg-color]}
        (th/des opts
          [[::bg-color :color/bg-0
            ::fg-color :color/fg-1]])]
    [:div
     {:style {:display 'flex
              :flex-wrap 'wrap
              :align-items 'flex-start
              :justify-content 'flex-start}}
     (->> videos
          (map (fn [{:keys [::id
                            ::thumbnail
                            ::title]
                     :as video}]
                 [:div
                  {:key id
                   :style {:padding 7.5
                           :width (str (/ 100 columns) "%")
                           :margin-bottom 10
                           :cursor 'pointer}}
                  [:div
                   {:style {:position 'relative}
                    :on-click (fn [e]
                                (.preventDefault e)
                                (on-select-video video)
                                nil)}
                   [:div {:style {:width "100%"
                                  :border-radius 4
                                  :overflow 'hidden
                                  :padding-top "55%"
                                  :background-color bg-color
                                  :background-size 'cover
                                  :background-position "center center"
                                  :background-image (str "url('"
                                                         (-> thumbnail
                                                             :md
                                                             :url)
                                                         "')")}}]
                   [:div {:style {:height 5}}]
                   [:div
                    {:style (merge
                              {:color fg-color

                               :font-size 13}
                              (when ellipsis-title?
                                {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis})
                              title-style)}
                    (unescape-html title)]

                   (when show-remove?
                     [:div {:style {:position 'absolute
                                    :top 0
                                    :right 0
                                    :width 25
                                    :height 25
                                    :background-color bg-color
                                    :border-radius "0px 4px 0px 4px"
                                    :overflow 'hidden}}

                      [:button.btn-dark-warning.flex-center
                       {:on-click (fn [e]
                                    (.preventDefault e)
                                    (.stopPropagation e)
                                    (on-remove-video video)
                                    nil)
                        :style {:width "100%"
                                :height "100%"
                                :padding 0}}
                       [feather/x {:size 15}]]])]])))]))

(defn youtube-url? [s]
  (and s
       (or
         (str/starts-with? s "https://youtube.com")
         (str/starts-with? s "https://www.youtube.com"))))

(defn search-pane [{:keys [initial-query
                           on-choose-video
                           on-youtube-url
                           recent-videos]
                    :as opts}]
  (let [!search-videos (r/atom nil)
        query-ch (chan (sliding-buffer 1))
        !loading? (r/atom nil)]
    (go-loop []
      (let [query (<! query-ch)]
        (reset! !loading? true)
        (if (= :empty query)
          (reset! !search-videos nil)
          (let [res (<! (<yt-api-snippet-search
                          {::api-key "AIzaSyA-oO0O4ZFIUotjtsfqJKRwBV4_q8fQ-Sk"}
                          query))]
            (if (res/anom res)
              (js/alert (ks/pp-str res))
              (let [videos (-> res
                               search-response->videos
                               ks/spy)]
                (reset! !search-videos videos)))))
        (reset! !loading? false)
        (recur)))
    (fn []
      [:div
       {:style {:display 'flex
                :flex 1
                :flex-direction 'column
                :overflow 'hidden}}
       [cmp/searchbar
        (merge
          opts
          {:loading? @!loading?
           :on-submit (fn [t]
                        (if (youtube-url? t)
                          (on-youtube-url t)
                          (put! query-ch t)))
           :placeholder "Search Youtube Videos"})]
       [:div
        {:style {:flex-grow 1
                 :overflow-y 'scroll}}
        (let [videos @!search-videos]
          [:div
           {:style {:padding 0
                    :flex 1}}
           (cond
             @!search-videos
             [video-grid
              {:columns 4
               :on-select-video
               (fn [video]
                 (on-choose-video video))
               :videos @!search-videos}]

             recent-videos
             [video-grid
              (merge
                opts
                {:columns 4
                 :ellipsis-title? true
                 :show-remove? true
                 :videos recent-videos
                 :on-select-video
                 (fn [video]
                   (on-choose-video video))})]
             :else
             [:div])])]])))

(defn url->video-id [s]
  (when s
    (second
      (re-find
        #"\?v=([^&\s]+)"
        s))))

(defn format-time [n]
  (when n
    (str (ks/sformat "%.2f" (/ n 1000)) "s")))

(def scrubber-theme-docs
  [{:doc "Scrubber background color"
    :rule [:bg-color ::bg-color "white"]}
   {:doc "Tick line color"
    :rule [:tick-color ::tick-color "#555"]}
   {:doc "Time text color"
    :rule [:fg-color ::fg-color "#555"]}
   {:doc "Time text font size"
    :rule [:font-size ::tick-font-size 12]}
   {:doc "Time text font family"
    :rule [:font-family ::tick-font-family]}
   {:doc "Space between tick and time text"
    :rule [:time-inset ::time-inset 5]}
   {:doc "Tick line top / bottom inset in px"
    :rule [:tick-inset ::tick-inset 0]}
   {:doc [:span "Cursor when over scrubber. Defaults to " [:code "col-resize"]]
    :rule [:cursor ::cursor "col-resize"]}])

(defn scrubber [{:keys [state]}]
  (let [!rect (r/atom nil)
        !time-ref (atom nil)
        !time-rect (r/atom nil)
        on-time-ref (fn [r]
                      (when r (reset! !time-ref r)))
        on-choose-pct (fn [pct]
                        (seek-to-pct state pct))
        on-drag (fn [{:keys [start-client-x delta-x]}]
                  (let [{:keys [left width]} @!rect]
                    (on-choose-pct
                      (/
                        (- (+ start-client-x delta-x) left)
                        width))))
        on-down (fn [{:keys [client-x client-y]}]
                  (let [{:keys [left width]} @!rect]
                    (on-choose-pct (/ (- client-x left) width))))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (reset! !rect (dommy/bounding-client-rect (rdom/dom-node this))))
       :component-did-update
       (fn [this]
         (let [new-rect (dommy/bounding-client-rect (rdom/dom-node this))
               new-time-rect (select-keys
                               (dommy/bounding-client-rect @!time-ref)
                               [:width])]
           (when (not= @!rect new-rect)
             (reset! !rect new-rect))
           (when (not= @!time-rect new-time-rect)
             (reset! !time-rect new-time-rect))))
       :reagent-render
       (fn [{:keys [hide-tick?
                    disable-fixed-width-numeric?] :as opts}]
         (let [{:keys [tick-color
                       fg-color
                       bg-color
                       font-size
                       font-family
                       tick-inset
                       time-inset
                       cursor]}
               (th/resolve opts
                 (map :rule scrubber-theme-docs))]
           [:div
            (merge
              {:style {:background-color bg-color
                       :height 20
                       :position 'relative
                       :cursor cursor
                       :user-select 'none}}
              (browser/drag-handler
                {:on-drag on-drag
                 :on-down on-down}))
            
            (when-not hide-tick?
              [:div {:style
                     {:display 'inline-block
                      :position 'absolute
                      :top tick-inset
                      :bottom tick-inset
                      :left 0
                      :transform
                      (str
                        "translate3d("
                        (* (:width @!rect) (!get-current-pct state)) "px"
                        ",0,0)")}}
               [:div
                {:style
                 {:width 1
                  :background-color tick-color
                  :height "100%"}}]])
            
            (when-not hide-tick?
              [:div {:style
                     {:display 'flex
                      :flex-direction 'row
                      :position 'absolute
                      :align-items 'center
                      :top 0
                      :left time-inset
                      :bottom 0
                      :transform
                      (str
                        "translate3d("
                        (ks/clamp
                          (*
                            (:width @!rect)
                            (!get-current-pct state))
                          0 (- (:width @!rect)
                               (:width @!time-rect)
                               (* 2 time-inset))) "px"
                        ",0,0)")}}
               [:div
                {:ref on-time-ref
                 :style (merge
                          {:color fg-color
                           :font-size 12
                           :font-family font-family
                           :line-height "100%"}
                          (when-not disable-fixed-width-numeric?
                            {:font-variant-numeric 'tabular-nums}))}
                (format-time (!get-current-time state))]])]))})))

(comment

  (go
    (-> (<! (<yt-api-snippet-search
              {::api-key "AIzaSyA-oO0O4ZFIUotjtsfqJKRwBV4_q8fQ-Sk"}
              "amy winehouse valerie"))
        search-response->videos
        ks/pp))


  '({:rx.browser.youtube/id "youtube-izkqPdVAdL4",
     :rx.browser.youtube/published-ts 1345186813000,
     :rx.browser.youtube/description
     "John Mayer's official music video for 'Something Like Olivia (Acoustic)'. Click to listen to John Mayer on Spotify: http://smarturl.it/JMayerSpotify?IQid=JMayerSLO ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/izkqPdVAdL4/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/izkqPdVAdL4/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/izkqPdVAdL4/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia (Official Acoustic Performance)",
     :rx.browser.youtube/video-id "izkqPdVAdL4",
     :rx.browser.youtube/channel-id
     {:publishedAt "2012-08-17T07:00:13.000Z",
      :channelId "UC9KhB07HSEtWISy_LFWwHzw",
      :title
      "John Mayer - Something Like Olivia (Official Acoustic Performance)",
      :description
      "John Mayer's official music video for 'Something Like Olivia (Acoustic)'. Click to listen to John Mayer on Spotify: http://smarturl.it/JMayerSpotify?IQid=JMayerSLO ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/izkqPdVAdL4/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/izkqPdVAdL4/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/izkqPdVAdL4/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "johnmayerVEVO",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-ng6-bBzIZEE",
     :rx.browser.youtube/published-ts 1377554428000,
     :rx.browser.youtube/description
     "Music video by John Mayer performing Something Like Olivia (Live on Letterman). (C) 2013 CBS Interactive.",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia (Live on Letterman)",
     :rx.browser.youtube/video-id "ng6-bBzIZEE",
     :rx.browser.youtube/channel-id
     {:publishedAt "2013-08-26T22:00:28.000Z",
      :channelId "UC9KhB07HSEtWISy_LFWwHzw",
      :title "John Mayer - Something Like Olivia (Live on Letterman)",
      :description
      "Music video by John Mayer performing Something Like Olivia (Live on Letterman). (C) 2013 CBS Interactive.",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/ng6-bBzIZEE/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "johnmayerVEVO",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-mTxpqtbCft8",
     :rx.browser.youtube/published-ts 1339013124000,
     :rx.browser.youtube/description
     "This is from John Mayers latest album, Born and Raised. By his album on itunes, totaly worth it! Lyrics: Well Olivia is taken But a look like hers can be found from ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/mTxpqtbCft8/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/mTxpqtbCft8/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/mTxpqtbCft8/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia (Lyrics)",
     :rx.browser.youtube/video-id "mTxpqtbCft8",
     :rx.browser.youtube/channel-id
     {:publishedAt "2012-06-06T20:05:24.000Z",
      :channelId "UCBvKGjS3FDLMt46ZB_G8jIQ",
      :title "John Mayer - Something Like Olivia (Lyrics)",
      :description
      "This is from John Mayers latest album, Born and Raised. By his album on itunes, totaly worth it! Lyrics: Well Olivia is taken But a look like hers can be found from ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/mTxpqtbCft8/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/mTxpqtbCft8/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/mTxpqtbCft8/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "KavZ",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube--k_KlCqAZ-I",
     :rx.browser.youtube/published-ts 1362643224000,
     :rx.browser.youtube/description
     "Music video by John Mayer performing Something Like Olivia. (c) 2012 Columbia Records, a Division of Sony Music Entertainment.",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia (Genero.tv Competition Winner)",
     :rx.browser.youtube/video-id "-k_KlCqAZ-I",
     :rx.browser.youtube/channel-id
     {:publishedAt "2013-03-07T08:00:24.000Z",
      :channelId "UC9KhB07HSEtWISy_LFWwHzw",
      :title
      "John Mayer - Something Like Olivia (Genero.tv Competition Winner)",
      :description
      "Music video by John Mayer performing Something Like Olivia. (c) 2012 Columbia Records, a Division of Sony Music Entertainment.",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/-k_KlCqAZ-I/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "johnmayerVEVO",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-kOkrvIU-LbA",
     :rx.browser.youtube/published-ts 1498578374000,
     :rx.browser.youtube/description
     "John Mayer performs Something Like Olivia live at the Village Studios in Los Angeles with music producer Don Was on the G+ Hangout. All material Copyright of ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia - Live from Village Studios",
     :rx.browser.youtube/video-id "kOkrvIU-LbA",
     :rx.browser.youtube/channel-id
     {:publishedAt "2017-06-27T15:46:14.000Z",
      :channelId "UCV6-QTZ1DR8jundLrpDBujg",
      :title
      "John Mayer - Something Like Olivia - Live from Village Studios",
      :description
      "John Mayer performs Something Like Olivia live at the Village Studios in Los Angeles with music producer Don Was on the G+ Hangout. All material Copyright of ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/kOkrvIU-LbA/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "Michael O'Connor",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-chLFi2cFxzo",
     :rx.browser.youtube/published-ts 1340607607000,
     :rx.browser.youtube/description
     "John Mayer's official music video for 'Queen Of California (Acoustic)'. Click to listen to John Mayer on Spotify: http://smarturl.it/JMayerSpotify?IQid=JMayerQoC ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/chLFi2cFxzo/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/chLFi2cFxzo/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/chLFi2cFxzo/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Queen Of California (Acoustic)",
     :rx.browser.youtube/video-id "chLFi2cFxzo",
     :rx.browser.youtube/channel-id
     {:publishedAt "2012-06-25T07:00:07.000Z",
      :channelId "UC9KhB07HSEtWISy_LFWwHzw",
      :title "John Mayer - Queen Of California (Acoustic)",
      :description
      "John Mayer's official music video for 'Queen Of California (Acoustic)'. Click to listen to John Mayer on Spotify: http://smarturl.it/JMayerSpotify?IQid=JMayerQoC ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/chLFi2cFxzo/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/chLFi2cFxzo/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/chLFi2cFxzo/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "johnmayerVEVO",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-TO4OKr6NOCg",
     :rx.browser.youtube/published-ts 1340742295000,
     :rx.browser.youtube/description "John Mayer.",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something like Olivia (Lyrics)",
     :rx.browser.youtube/video-id "TO4OKr6NOCg",
     :rx.browser.youtube/channel-id
     {:publishedAt "2012-06-26T20:24:55.000Z",
      :channelId "UCrZNRo2kLSwgRuame5cIAow",
      :title "John Mayer - Something like Olivia (Lyrics)",
      :description "John Mayer.",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/TO4OKr6NOCg/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "Tri Padukan Purba",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-vKliAHZqdxo",
     :rx.browser.youtube/published-ts 1511007391000,
     :rx.browser.youtube/description
     "Help with this and over 1000 more free lessons: https://www.justinguitar.com In this Something Like Olivia Acoustic guitar lesson tutorial, we're going to learn the ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/vKliAHZqdxo/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/vKliAHZqdxo/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/vKliAHZqdxo/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - Something Like Olivia Guitar Lesson Acoustic - Chords Strumming JustinGuitar",
     :rx.browser.youtube/video-id "vKliAHZqdxo",
     :rx.browser.youtube/channel-id
     {:publishedAt "2017-11-18T12:16:31.000Z",
      :channelId "UCcZd_G62wtsCXd-b7OhQdlw",
      :title
      "John Mayer - Something Like Olivia Guitar Lesson Acoustic - Chords Strumming JustinGuitar",
      :description
      "Help with this and over 1000 more free lessons: https://www.justinguitar.com In this Something Like Olivia Acoustic guitar lesson tutorial, we're going to learn the ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/vKliAHZqdxo/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/vKliAHZqdxo/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/vKliAHZqdxo/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "JustinGuitar Songs",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-6aubXZ_k2QI",
     :rx.browser.youtube/published-ts 1360733376000,
     :rx.browser.youtube/description
     "Mary Desmond was featured in a video submitted for a contest to be the official video for John Mayer's song \"Something Like Olivia\". This video was selected as ...",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - &quot;Something Like Olivia&quot; (Official Video Finalist)",
     :rx.browser.youtube/video-id "6aubXZ_k2QI",
     :rx.browser.youtube/channel-id
     {:publishedAt "2013-02-13T05:29:36.000Z",
      :channelId "UCfO0-ls7C2qlKqD3izf1-5A",
      :title
      "John Mayer - &quot;Something Like Olivia&quot; (Official Video Finalist)",
      :description
      "Mary Desmond was featured in a video submitted for a contest to be the official video for John Mayer's song \"Something Like Olivia\". This video was selected as ...",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/6aubXZ_k2QI/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "Mary Desmond",
      :liveBroadcastContent "none"}}
    {:rx.browser.youtube/id "youtube-O46Ag7QUVbI",
     :rx.browser.youtube/published-ts 1364938702000,
     :rx.browser.youtube/description "",
     :rx.browser.youtube/thumbnail
     {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/default.jpg",
      :width 120,
      :height 90,
      :md
      {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/mqdefault.jpg",
       :width 320,
       :height 180},
      :lg
      {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/hqdefault.jpg",
       :width 480,
       :height 360}},
     :rx.browser.youtube/title
     "John Mayer - &#39;Something Like Olivia&#39;  [Ellen Degeneres 04/02/13]",
     :rx.browser.youtube/video-id "O46Ag7QUVbI",
     :rx.browser.youtube/channel-id
     {:publishedAt "2013-04-02T21:38:22.000Z",
      :channelId "UCZlMODEr4o4dFOA5Unhhjlg",
      :title
      "John Mayer - &#39;Something Like Olivia&#39;  [Ellen Degeneres 04/02/13]",
      :description "",
      :thumbnails
      {:default
       {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/default.jpg",
        :width 120,
        :height 90},
       :medium
       {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/mqdefault.jpg",
        :width 320,
        :height 180},
       :high
       {:url "https://i.ytimg.com/vi/O46Ag7QUVbI/hqdefault.jpg",
        :width 480,
        :height 360}},
      :channelTitle "tavarinho123", 
      :liveBroadcastContent "none"}}))








