(ns rx.browser.youtube-otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.youtube :as yt]
            [reagent.core :as r]))

(defonce !timestamp (atom 0))
(defonce !video (r/atom nil))
(defonce !seek-request (r/atom nil))

(defn base []
  (browser/<set-root!
    [(fn []
       {:render
        (fn []
          [:div
           {:key "foo"
            :style {:flex 1
                    :display 'flex}}
           [yt/search-pane
            {:on-choose-video
             (fn [video]
               (when (not= (::yt/id video)
                           (::yt/id @!video))
                 (reset! !video video)
                 (reset! !seek-request
                   {:ms 0
                    :ts (ks/now)})))
             :recent-videos []}]
           [:div
            {:style {:position 'absolute
                     :bottom 10
                     :left 10
                     :overflow 'hidden
                     :border-radius 4}}
            [yt/video
             {:video-id (::yt/video-id @!video)
              :initial-playing? true
              :seek-request @!seek-request
              :on-time (fn [p t]
                         (reset! !timestamp t))
              :start-seconds (/ @!timestamp 1000)}]]])})]))



(comment

  (base)
  
  )






