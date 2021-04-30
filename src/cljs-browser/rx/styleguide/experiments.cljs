(ns rx.styleguide.experiments
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [rx.css :as css]
            [reagent.core :as r]
            [rx.browser.components :as bc]
            [rx.browser.youtube :as yt]
            [dommy.core :as dommy]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! take! alts! timeout pipe mult tap]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def !state (r/atom
              (let [t1 (ks/uuid)
                    f1 (ks/uuid)
                    f2 (ks/uuid)]
                {:duration 30000
                 :pixels-per-ms 0.01
                 :loop [10000 12000]
                 :tracks {:id->obj {t1 {:id t1
                                        :title "Base"
                                        :features-order [f1 f2]}}
                          :order [t1]}

                 :features {:id->obj {f1 {:id f1
                                          :time-index 1234}
                                      f2 {:id f2
                                          :time-index 5000}}}
                 :order [t1]})))

(def LOAD_PERSISTED? false)

(def STATE_DEFAULTS
  (let [id (ks/uuid)]
    {:pixels-per-ms 0.01

     :tracks {:id->obj {id {:id id
                            :title "Base"}}
              :order [id]}}))

(def TEST_PLANS
  [{:title "Clean Slate"
    :state {}}
   {:title "Someone New"
    :state {:features
            {:id->obj
             {"f5123994803345c0aa137631aa266b01"
              {:id "f5123994803345c0aa137631aa266b01", :time-index 1234},
              "cfadff2f2566440e8d66c9efd3db86ba"
              {:id "cfadff2f2566440e8d66c9efd3db86ba", :time-index 5000}}},
            :current-board
            {:features
             {:id->obj
              {"f5123994803345c0aa137631aa266b01"
               {:id "f5123994803345c0aa137631aa266b01", :time-index 1234},
               "cfadff2f2566440e8d66c9efd3db86ba"
               {:id "cfadff2f2566440e8d66c9efd3db86ba", :time-index 5000}}},
             :current-time 30816,
             :video-id "bPJSsAr2iu0",
             :time 0,
             :tracks
             {:id->obj
              {"6919ae6a28834f239924ce43c2ea244d"
               {:id "6919ae6a28834f239924ce43c2ea244d",
                :title "Base",
                :features-order
                ["f5123994803345c0aa137631aa266b01"
                 "cfadff2f2566440e8d66c9efd3db86ba"]}},
              :order ["6919ae6a28834f239924ce43c2ea244d"]},
             :duration 243000,
             :loop [29180 34300],
             :playing? true,
             :pixels-per-ms 0.01},
            :current-time 44958,
            :video-id "bPJSsAr2iu0",
            :time 0,
            :tracks
            {:id->obj
             {"6919ae6a28834f239924ce43c2ea244d"
              {:id "6919ae6a28834f239924ce43c2ea244d",
               :title "Base",
               :features-order
               ["f5123994803345c0aa137631aa266b01"
                "cfadff2f2566440e8d66c9efd3db86ba"]}},
             :order ["6919ae6a28834f239924ce43c2ea244d"]},
            :duration 243000,
            :loops
            [{:id "656e2c2b790449a5ab9e3fac46da6f0a",
              :earlier 29190,
              :later 34300}
             {:id "11dd043386574103a8d4a05c85613555",
              :earlier 44115,
              :later 46551}],
            :loop [44115 46551],
            :playing? true,
            :road-scroll-ms 28999.999999999985,
            :pixels-per-ms 0.026000000000000013}}
   {:title "One Track"
    :state (let [t1 (ks/uuid)
                 f1 (ks/uuid)
                 f2 (ks/uuid)]
             {:duration 30000
              :loop [29190 34300]
              :playing? true
              :pixels-per-ms 0.01
              :tracks {:id->obj {t1 {:id t1
                                     :title "Base"
                                     :features-order [f1 f2]}}
                       :order [t1]}
              :features {:id->obj {f1 {:id f1
                                       :time-index 1234}
                                   f2 {:id f2
                                       :time-index 5000}}}})}])


;; max resolution is millisecond

(defn $track [opts track]
  [:div
   [:pre (ks/pp-str track)]])

(defn $feature [_ _]
  (let [size 1]
    [:div.feature
     {:style {:width size
              :height "100%"
              :background-color 'black}}]))


(defn on-drag [{:keys [on-click
                       on-down
                       on-up
                       on-move]}]
  (let [!m (atom nil)]
    (letfn [(internal-move [e]
              (.preventDefault e)
              (when on-move
                (on-move
                  {:x (.-pageX e)
                   :y (.-pageY e)}
                  e))
              nil)
            (internal-mu [e]
              (.preventDefault e)
              (let [start (:start @!m)
                    {:keys [x y]} start
                    up-x (.-pageX e)
                    up-y (.-pageY e)
                    dx (- up-x x)
                    dy (- up-y y)]
                (if (and (<= (ks/abs dx) 3)
                         (<= (ks/abs dy) 3))
                  (when on-click
                    (on-click e))
                  (when on-up
                    (on-up
                      {:sx x
                       :sy y
                       :ex up-x
                       :ey up-y
                       :dx dx
                       :dy dy}))))
              (dommy/unlisten!
                js/window
                :mouseup
                internal-mu)
              (dommy/unlisten!
                js/window
                :mousemove
                internal-move)
              nil)
            (internal-md [e]
              (.preventDefault e)
              (swap! !m assoc
                     :start {:x (.-pageX e)
                             :y (.-pageY e)})
              (dommy/listen!
               js/window
               :mouseup
               internal-mu)
              (dommy/listen!
                js/window
                :mousemove
                internal-move)
              (when on-down
                (on-down e))
              nil)]
      {:on-mouse-down internal-md})))

(defn $timeline [_ _]
  (let [!ref (atom nil)]
    (fn [{:keys [on-time
                 on-loop]}
         {:keys [current-time
                 duration
                 pixels-per-ms
                 loop]}]
      [:div
       (merge
         {:ref #(when % (reset! !ref %))
          :style {:width (* pixels-per-ms duration)
                  :margin-top css/xspx
                  :margin-bottom css/xspx
                  :position 'relative
                  :height 14
                  :user-select 'none}}
         (on-drag
           {:on-click (fn [e]
                        (.preventDefault e)
                        (when on-time
                          (let [ancestor nil #_
                                (bc/find-scrollable-ancestor
                                  @!ref
                                  :horizontal)]
                            (on-time
                              (ks/round
                                (/ (+
                                     (.-clientX e)
                                     (.-scrollLeft ancestor)
                                     (- (:left (dommy/bounding-client-rect ancestor))))
                                  pixels-per-ms)))))
                        nil)

            :on-up
            (fn [{:keys [dx dy sx sy ex ey]}]
              (let [ancestor nil #_(bc/find-scrollable-ancestor
                                     @!ref
                                     :horizontal)
                    low (ks/round
                          (/ (+
                               (if (< sx ex)
                                 sx
                                 ex)

                               (.-scrollLeft ancestor)
                               (- (:left (dommy/bounding-client-rect ancestor))))
                            pixels-per-ms))

                    high (ks/round
                           (/ (+
                                (if (> sx ex)
                                  sx
                                  ex)
                                (.-scrollLeft ancestor)
                                (- (:left (dommy/bounding-client-rect ancestor))))
                             pixels-per-ms))]
                (on-loop [low high])))}))
       (->> (range 0 duration 5000)
            (map (fn [i]
                   [:div {:key i
                          :style {:position 'absolute
                                  :left (* pixels-per-ms i)
                                  :top 0
                                  :bottom 0
                                  :padding-left 2
                                  :border-left "solid #ccc 1px"
                                  :font-size "10px"}}
                    (/ i 1000) "s"])))



       (when loop
         [:div {:style {:height "100%"
                        :left (* pixels-per-ms (first loop))
                        :width (- (* pixels-per-ms (second loop))
                                  (* pixels-per-ms (first loop)))
                        :position 'absolute
                        :background-color "rgba(0,0,0,0.1)"}}])

       #_(when (first loop)
           [:div {:style {:height "100%"
                          :width (* pixels-per-ms (first loop))
                          :left 0
                          :position 'absolute
                          :background-color "rgba(0,0,0,0.1)"}}])

       #_(when (second loop)
           [:div {:style {:height "100%"
                          :left (* pixels-per-ms (second loop))
                          :right 0
                          :position 'absolute
                          :background-color "rgba(0,0,0,0.1)"}}])

       ])))

(defn $bpm [_ _]
  (let [!ref (atom nil)]
    (fn [{:keys [on-time
                 on-loop]}
         {:keys [current-time
                 duration
                 pixels-per-ms]}]
      (let [bpm 120
            bps (/ bpm 60)
            bpms (* bps 1000)]
        [:div
         (merge
           {:ref #(when % (reset! !ref %))
            :style {:width (* pixels-per-ms duration)
                    :margin-top css/xspx
                    :margin-bottom css/xspx
                    :position 'relative
                    :height 14
                    :user-select 'none}})
         (->> (range 0 duration bpms)
              (map (fn [i]
                     [:div {:key i
                            :style {:position 'absolute
                                    :left (* pixels-per-ms i)
                                    :top 0
                                    :bottom 0
                                    :padding-left 2
                                    :border-left "solid #ccc 1px"
                                    :font-size "10px"}}])))]))))


(defn $keyboard []
  [:div.flex-left])

(defn handle-road-scroll [!state left-px]
  (let [{:keys [pixels-per-ms]} @!state
        scroll-ms (/ left-px pixels-per-ms)]
    (swap! !state
      assoc
      :road-scroll-ms
      scroll-ms)))

(defn $tracks [_ {:keys [road-scroll-ms
                         pixels-per-ms]}])


(defn save-video-state [!state]
  #_(page/ls-set
      (str "yt:v1:"
           (:video-id @!state))
      (select-keys
        @!state
        [:video-id
         :duration
         :loop
         :tracks
         :features])))

(def keybindings
  {})


(defn handle-loop [p [^long low ^long high]]
  (when (and low high)
    (let [^long ms (yt/get-current-time p)]
      (when (> ms high)
        (yt/seek-to p low))
      (when (< ms low)
        (yt/seek-to p low)))))

(defn delete-feature [!state track-id feature-id]
  #_(update-in))

(defn $yt [{:keys [video-id]}]
  (let [!p (r/atom nil)

        !marks (r/atom [])

        !current-time (r/atom 0)

        !ui (r/atom {})

        on-key (fn [e]
                 (let [kc (.-keyCode e)]
                   (when-let [handler (get keybindings kc)]
                     (.preventDefault e)
                     (handler @!p !state))
                   (prn kc)))]

    (r/create-class
      {:component-did-mount
       (fn [e]
         (dommy/listen! js/window :keydown on-key)
         #_(th/init!
             {:component-selectors [:#cljs-entry]
              :console {:position :top-right}
              :state-refs {:!state !state}
              :plans TEST_PLANS})
         (reset! !state (merge
                          STATE_DEFAULTS
                          {:video-id video-id}
                          (when (and LOAD_PERSISTED?
                                     video-id)
                            #_(page/ls-get (str "yt:v1:" video-id)))
                          nil
                          #_(:state (th/selected-plan!)))))
       :component-will-unmount
       (fn [e]
         (dommy/unlisten! js/window :keydown on-key))
       :reagent-render
       (fn []
         (let [board @!state]
           [:div
            [:div.flex-left
             [yt/video
              {:video-id (or video-id "bPJSsAr2iu0")
               :playing? (:playing? @!state)

               :on-ready (fn [p]
                           (reset! !p p))

               :on-time
               (fn [_ sec]
                 (swap! !state assoc :current-time sec)
                 (handle-loop @!p (:loop @!state)))
               :throttle-time 50
               :on-props (fn [_ props]

                           (swap! !state merge props))}]
             [:div {:style {:flex 1}}
              [:div
               (:playback-rate @!state)]
              [:div
               [:a {:href "#"
                    :on-click (fn [e]
                                (.preventDefault e)
                                nil)}
                "Save Loop"]]]]
            [$tracks
             {:on-seek-to
              (fn [ms]
                (swap! !state assoc :loop nil)
                (yt/seek-to @!p ms))
              :on-loop (fn [loop]
                         (yt/seek-to @!p (first loop))
                         (swap! !state assoc :loop loop))}
             @!state]

            [:div
             [:div.flex-center
              [:div
               (let [{:keys [m s ms]
                      :or {m 0 s 0 ms 0}}
                     (ks/time-delta-parts
                       (:current-time @!state))]
                 (str m " " (ks/pad s 2) " " (ks/pad ms 3)))]]]

            [:div]
            [:div
             ]]))})))

