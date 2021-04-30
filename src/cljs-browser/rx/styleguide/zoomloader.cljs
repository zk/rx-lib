(ns rx.styleguide.zoomloader
  (:require [reagent.core :as r]
            [rx.css :as rc]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! alts! timeout pipe mult tap]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def image-href "/img/jetski.jpg")
(def vid-href "https://s3.amazonaws.com/zk-one/jetski.mp4")

(def path-coords
  ["M74.4823047,39.0518811 C75.5189357,38.8957743 78.4921077,39.1110589 79.7246375,39.4426583 C84.7516856,40.7333456 88.9757529,45.5257233 91.342863,52.5862415 C92.0468742,54.6788893 93.0243275,58.9039769 92.8488348,59.0600837 C92.7896571,59.1182412 92.1835953,59.1968047 91.4989699,59.2355764 C90.4235672,59.3141399 90.0715616,59.2355764 89.1522658,58.8050072 C87.4310094,57.9448892 86.3556068,56.2430186 86.022987,53.8371368 L85.886266,52.8005058 L85.3189758,53.5636948 C85.0057418,53.9748781 84.4180455,54.5421683 84.0272683,54.8156104 C81.8172853,56.2634247 79.8419727,56.0093685 78.1594879,54.1115991 C76.8494149,52.6260335 76.6341303,51.7261235 76.6341303,47.7357063 C76.6341303,44.5860214 76.5943383,44.2544219 76.2035611,43.5106188 C75.596479,42.3566526 74.3445634,41.3394074 72.5651496,40.4986751 L71,39.7558923 L72.4478144,39.4426583 C73.249775,39.2865515 74.1690707,39.1100385 74.4823047,39.0518811 Z"
   "M60.3602886,18.2641319 C63.3334606,17.9121263 69.5726324,17.9121263 72.6039618,18.2631116 C75.4597986,18.5957314 77.2596186,19.0069147 80.7602888,20.1608809 C91.2837259,23.5840079 97.4249483,29.0212191 99.4788245,36.6888194 C99.9287795,38.39069 99.9869369,38.977366 99.9869369,41.8332028 C100.006323,44.4155975 99.9083734,45.4328427 99.5757536,46.9582003 C99.0084634,49.5589606 98.1289595,52.1607412 97.013765,54.4686736 L96.0944692,56.4052145 L95.7230778,54.9380144 C94.7843961,51.0843182 92.8876471,46.683738 90.9898777,44.0238 C88.2126045,40.1119464 84.7302998,37.530572 80.7602888,36.4347632 C79.0196466,35.9460366 74.384396,35.9460366 72.5060124,36.4153774 C65.504672,38.1560196 60.7704517,42.9290116 59.0104236,49.9895298 C58.3645699,52.5321326 58.3839557,57.3826678 59.0298095,60.02322 C61.7489252,70.9955918 70.3939788,78.1346736 83.1273989,79.85593 C84.9261986,80.0906004 80.2123844,80.1303923 42.5804345,80.1497781 L0,80.1691639 L0.762168657,77.9387748 C3.14866463,70.9955918 7.1584676,62.3117666 10.9723718,55.7981324 C16.8993299,45.6664928 24.1945186,36.8061546 31.4713417,30.9383743 C33.8180457,29.0416252 38.239032,26.0490674 40.6642996,24.7185883 C46.6494152,21.4332026 53.9058322,19.0467067 60.3602886,18.2641319 Z"])


(defn $image-loader [{:keys [loaded?]}]
  (let [img-width 1280
        img-height 720
        logo-width 100
        logo-height 100

        iwpx (/ logo-width img-width)
        ihpx (/ logo-height img-height)
        loaded? loaded?]
    [:div.flex-center
     {:style {:width "100%"
              :height "100%"
              :position 'relative}}
     [:svg {:xmlns "http://www.w3.org/2000/svg"
            :width "100%"
            :height "100%"
            :viewBox (str "0 0 " img-width " " img-height)
            :preserveAspectRatio "xMidYMid slice"
            :style {:display 'block}}
      [:clipPath {:id "mask"}
       (->> path-coords
            (map-indexed
              (fn [i points]
                (let [scale (if loaded?
                              2000
                              1)
                      offset-x (+ (if loaded?
                                    0 #_(* -5 scale)
                                    0)
                                  (- (/ img-width 2) logo-width))
                      offset-y (- (/ img-height 2) logo-height)]
                  [:path {:key i
                          :d points
                          :transform
                          (str
                            "matrix("
                            scale ","
                            "0,"
                            "0,"
                            scale ","
                            (+ offset-x
                               (-
                                 logo-width
                                 (* scale (/ logo-width 2))))
                            ","

                            (+ offset-y
                               (-
                                 logo-height
                                 (* scale (/ logo-height 2))))
                            ")")
                          :style (merge
                                   {:transform
                                    (str
                                      "matrix("
                                      scale ","
                                      "0,"
                                      "0,"
                                      scale ","
                                      (+ offset-x
                                         (-
                                           logo-width
                                           (* scale (/ logo-width 2))))
                                      ","

                                      (+ offset-y
                                         (-
                                           logo-height
                                           (* scale (/ logo-height 2))))
                                      ")")}
                                   (rc/transition "transform 1s ease-in"))}]))))]

      [:image {:xlinkHref image-href
               :width img-width
               :height img-height
               :clipPath "url(#mask)"
               :preserveAspectRatio "xMidYMid slice"
               :style {:width img-width
                       :height img-height}}]]]))

(defn $color-loader [{:keys [loaded?
                             color]}]
  (let [loaded? loaded?]
    [:div.flex-center
     {:style {:width "100%"
              :height "100%"
              :position 'relative}}
     [:svg {:xmlns "http://www.w3.org/2000/svg"
            :width "100%"
            :height "100%"
            :viewBox "0 0 100 100"
            :preserveAspectRatio "xMidYMid slice"
            :style {:display 'block}}
      [:clipPath {:id "mask"}
       (->> path-coords
            (map-indexed
              (fn [i points]
                (let [scale (if loaded?
                              200
                              0.08)]
                  [:path {:key i
                          :d points
                          :style (merge
                                   {:transform
                                    (str
                                      "matrix("
                                      scale ","
                                      "0,"
                                      "0,"
                                      scale ","
                                      (-
                                        50
                                        (* scale 50))
                                      ","
                                      (-
                                        50
                                        (* scale 50))
                                      ")")}
                                   (rc/transition "transform 1s ease-in"))}]))))]

      [:rect {:x 0 :y 0 :width 100 :height 100
              :clipPath "url(#mask)"
              :style {:fill (or color "black")}}]]]))

(defn $view []
  (let [!ui (r/atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go
           (<! (timeout 1000))
           (swap! !ui assoc :progress? true)
           (<! (timeout 400))
           (swap! !ui assoc :loaded? true)
           (<! (timeout 2000))
           (swap! !ui assoc :playing? true)))
       :reagent-render
       (fn []
         (let [{:keys [progress?
                       loaded?
                       playing?]} @!ui]
           [:div.fold-concious
            [:div.above-fold-static
             [$image-loader {:loaded? loaded?}]
             [:div.flex-center {:style {:position 'absolute
                                        :top 0
                                        :left 0
                                        :right 0
                                        :bottom 0}}
              [:div.text-center
               [:div {:style {:height 140
                              :width 114}}]

               #_[comps/$prog-bar-mock
                  {:loading? true
                   :done? progress?
                   :bar-color (rc/gr :900)
                   :bar-height 2
                   :style (merge
                            {:opacity (if loaded? 0 1)}
                            (rc/transition "opacity 0.2s ease"))}]]]
             [:div {:style (merge
                             {:position 'absolute
                              :top 0
                              :left 0
                              :right 0
                              :bottom 0
                              :opacity (if loaded? 1 0)
                              :transform "translate3d(0,0,0)"}
                             (rc/transition "opacity 1s ease 0.5s"))}

              #_[comps/$vidbg
                 {:mp4 vid-href
                  :playing? playing?
                  :loop? true
                  :muted? true
                  :poster image-href
                  :style {:position 'absolute
                          :top 0
                          :left 0
                          :right 0
                          :bottom 0
                          :color 'white

                          :display 'flex
                          :flex-direction 'column
                          :justify-content 'center
                          :align-items 'center
                          :transform "translate3d(0,0,0)"
                          :background-color "rgba(0,0,0,0.4)"}}
                 [:div.container
                  [:div.row
                   [:div.col-sm-8.offset-sm-2.col-md-6.offset-md-3
                    {:style {:font-size 23
                             :font-family rc/cardo
                             :font-weight 'bold
                             :line-height "150%"}}
                    [:div

                     [:div
                      {:style {:font-size 80}}
                      "“"]
                     [:p "In a reasonable world men would have treated these islands as precious possessions, a natural museum filled with beautiful and curious works of creation, valuable beyond price."]
                     [:p "Because nowhere in the world are they duplicated."]
                     [:p
                      [:i
                       "- Rachel Carson"]]

                     [:div {:style {:width "100%"
                                    :height 3
                                    :background-color 'white
                                    :margin-bottom rc/mdpx}}]

                     [:div
                      [:svg
                       {:width "75"
                        :height "75"
                        :viewBox "0 0 100 100"
                        :preserveAspectRatio "xMidYMid slice"
                        :style {:display 'block
                                :margin-left 'auto
                                :margin-right 'auto
                                :margin-bottom 0}}
                       (->> path-coords
                            (map-indexed
                              (fn [i points]
                                [:path {:key i
                                        :d points
                                        :style {:fill "#ffffff"
                                                :transform
                                                "scale(0.8)"
                                                :transform-origin "center center"}}])))]]]]]]]]]
            #_(when loaded?
                [:div.below-fold
                 (->> (range 100)
                      (map (fn [i]
                             [:div {:key i} "hello " i])))])]))})))

(defn handler [& args]
  (prn "args" args))
