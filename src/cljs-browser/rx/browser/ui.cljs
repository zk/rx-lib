(ns rx.browser.ui
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.css :as css]
            [rx.browser.css :as bcss]
            [rx.theme :as th]
            [reagent.core :as r]
            [clojure.core.async :as async
             :refer [go go-loop put! <! chan timeout
                     chan sliding-buffer]]))

(defn text [& args]
  (let [[opts & children] (ks/ensure-opts args)

        {:keys [scale style id]} opts
        
        {:keys [fg-color
                font-size
                font-weight
                font-family
                line-height
                letter-spacing]}

        (th/resolve opts
          [[:fg-color :color/text-primary]
           [:font-size
            (condp = scale
              "display" :type/display-font-size
              "headline" :type/headline-font-size
              "title" :type/title-font-size
              "subtext" :type/subtext-font-size
              "label" :type/label-font-size
              :type/body-font-size)]
           [:font-weight
            (condp = scale
              "display" :type/display-font-weight
              "headline" :type/headline-font-weight
              "title" :type/title-font-weight
              "subtext" :type/subtext-font-weight
              "label" :type/label-font-weight
              :type/body-font-weight)]
           [:font-family
            (condp = scale
              "display" :type/display-font-family
              "headline" :type/headline-font-family
              "title" :type/title-font-family
              "subtext" :type/subtext-font-family
              "label" :type/label-font-family
              :type/body-font-family)]
           [:line-height
            (condp = scale
              "display" :type/display-line-height
              "headline" :type/headline-line-height
              "title" :type/title-line-height
              "subtext" :type/subtext-line-height
              "label" :type/label-line-height
              :type/body-line-height)]
           [:letter-spacing
            (condp = scale
              "display" :type/display-letter-spacing
              "headline" :type/headline-letter-spacing
              "title" :type/title-letter-spacing
              "subtext" :type/subtext-letter-spacing
              "label" :type/label-letter-spacing
              :type/body-letter-spacing)]])]
    (into
      [(condp = scale
         "display" :h1
         "headline" :h2
         "title" :h3
         "subtext" :div
         "label" :span
         :p)
       (merge
         {:style (merge
                   {:color fg-color
                    :font-size font-size
                    :font-weight font-weight
                    :line-height (str line-height "px")
                    :font-family font-family
                    :letter-spacing letter-spacing
                    :margin-bottom 0
                    :padding 0}
                   style)}
         (when id {:id id}))]
      children)))

(defn group-base [_]
  (r/create-class
    {:reagent-render
     (fn [& args]
       (let [[opts & children] (ks/ensure-opts args)
             children (ks/unwrap-children children)
             {:keys [horizontal? gap style fill?]} opts
             gap (or gap 0)]
         (into
           [:div
            (merge
              (dissoc opts
                :horizontal?
                :align-items
                :form-state
                :justify-content
                :pad
                :gap)
              {:style style})]
           children)))}))

(defn g-css-rules []
  [[".rx-ui-g"
    ["&:hover"
     {:background-color "var(--hover-bg-color)"}]]])


;; HACK!
(bcss/set-style ::css-rules (g-css-rules))

#_(defn g [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [horizontal? gap fill? align-start?]
         :or {gap 0}} opts
        pad-style (th/pad-padding opts)]
    (into
      [group-base
       (merge
         opts
         {:class (str (:class opts)
                      " rx-ui-g "
                      (if horizontal?
                        "rx-ui-g-horizontal"
                        "rx-ui-g-vertical"))
          :style (merge
                   pad-style
                   {"--flex-direction" 'column}
                   (when horizontal?
                     {"--flex-direction" 'row})
                   (select-keys opts css/flex-keys)
                   (if horizontal?
                     {"--child-margin-right" (str gap "px")}
                     {"--child-margin-bottom"
                      (ks/spy (str "var(--child-margin-bottom, " gap "px)"))
                      #_(ks/spy (str "var(--child-margin-bottom)"))})
                   (when fill?
                     {"--child-flex" 1})
                   (when align-start?
                     {"--child-align-self" 'flex-start})
                   (:style opts))})]
      children)))

(defn hover [& args]
  (let [[opts & children] (ks/ensure-opts args)
        !hover? (r/atom nil)
        !down? (r/atom nil)
        on-mouse-over (fn []
                        (reset! !hover? true))
        on-mouse-out (fn [] (reset! !hover? false))
        on-mouse-down (fn [e]
                        (reset! !down? true))
        on-mouse-up (fn [] (reset! !down? false))
        on-touch-start (fn []
                         (reset! !hover? false)
                         (reset! !down? true))

        on-touch-end (fn []
                       (reset! !hover? false)
                       (reset! !down? false))

        {:keys [element]} opts]
    (r/create-class
      {:component-did-mount
       (fn []
         (.addEventListener js/window "mouseup" on-mouse-up)
         (.addEventListener js/window "touchend" on-touch-end))
       :component-will-unmount
       (fn []
         (.removeEventListener js/window "mouseup" on-mouse-up)
         (.removeEventListener js/window "touchend" on-touch-end))
       :component-did-update
       (browser/cdu-diff
         (fn [_ [{:keys [disabled]}]]
           (when disabled
             (reset! !hover? false)
             (reset! !down? false))))
       :reagent-render
       (fn [& args]
         (let [[opts & children] (ks/ensure-opts args)]
           (let [{:keys [style
                         style-over
                         style-down
                         down? hover? active?]} opts]
             (into
               [(or element :div)
                (merge
                  (dissoc
                    opts
                    :style
                    :style-down
                    :style-over)
                  {:on-mouse-over on-mouse-over
                   :on-mouse-out on-mouse-out
                   :on-mouse-down (fn [e]
                                    (on-mouse-down e)
                                    (when-let [f (:on-mouse-down opts)]
                                      (f e)))
                   :on-touch-start on-touch-start
                   :on-click (fn [e]
                               (when-let [f (:on-click opts)]
                                 (f e)))}
                  {:style (merge
                            style
                            (when (or @!hover? hover?) style-over)
                            (when (or @!down? down? active?) style-down))})]
               children))))})))

(defn g [opts]
  (let [bcss (bcss/comp-state)]
    (r/create-class
      (->
        {:reagent-render
         (fn [& args]
           (let [[opts & children] (ks/ensure-opts args)
                 children (ks/unwrap-children children)
                 {:keys [horizontal? gap style]} opts
                 pad-style (th/pad-padding opts)]
             (bcss/update-css! bcss
               (concat
                 [(merge
                    {:display 'flex
                     :flex-direction 'column}
                    (if horizontal?
                      {:flex-direction 'row})
                    (select-keys opts css/flex-keys)
                    pad-style)]
                 (:css opts)))
             (into
               [:div
                (merge
                  (dissoc opts
                    :horizontal?
                    :align-items
                    :form-state
                    :justify-content
                    :css
                    :pad
                    :gap)
                  {:style style}
                  (bcss/el-opts bcss
                    (merge
                      opts
                      {:class (->> [(:class opts)
                                    "rx-ui-g"]
                                   (interpose " ")
                                   (apply str))})))]
               (->> children
                    (interpose
                      [:div {:style (if horizontal?
                                      {:width gap}
                                      {:height gap})}])))))}
        (browser/bind-lifecycle-callbacks opts)))))

(def group g)

(defn section [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [title]} opts
        {:keys [gap]} (th/resolve opts [[:gap
                                         ::section-gap
                                         8]])
        children (ks/unwrap-children children)]
    (into
      [group
       {:gap gap}
       [text {:scale "title"} title]]
      children)))

(defn test-group []
  [group {:pad (* 4 5)
          :gap 20}
   (->> ["Vertical" "Horizontal"]
        (map (fn [s]
               [section
                {:title s
                 :gap 8}
                [group
                 {:gap 8
                  :horizontal? (= "Horizontal" s)}
                 (->> ["display"
                       "headline"
                       "title"
                       "subtext"
                       "label"]
                      (map (fn [scale]
                             [text {:scale scale} scale])))]])))])

(defn countdown [{:keys [end-ts]}]
  (let [!remaining (r/atom (- end-ts (ks/now)))
        !run? (atom true)]
    (r/create-class
      {:component-did-mount
       (fn []
         (go-loop []
           (reset! !remaining (- end-ts (ks/now)))
           (<! (timeout (if (and (< @!remaining (* 1000 60))
                                 (>= @!remaining 0))
                          16
                          250)))
           (when @!run?
             (recur))))
       :component-will-unmount
       (fn []
         (reset! !run? false))
       :reagent-render
       (fn [{:keys [size title gutter-size]
             :or {size :md
                  gutter-size 20}}]
         (let [font-size (condp = size
                           :sm 30
                           :md 50)]
           [:div
            {:style {:color 'black
                     :font-size font-size
                     :letter-spacing 1
                     :text-align 'center
                     :font-variant-numeric 'tabular-nums}}

            (when title
              [:h4 {:style {:font-weight '700
                            :text-transform 'upppercase
                            :font-size 12
                            :margin-bottom 10
                            :letter-spacing 1}}
               title])
            (let [{:keys [d h m s ms]}
                  (if (> @!remaining 0)
                    (ks/time-delta-parts @!remaining)
                    {:d 0 :h 0 :m 0 :s 0})]
              [group
               {:horizontal? true
                :gap gutter-size
                :style {:display 'flex}}
               (->> [d "days"
                     h "hours"
                     m "minutes"
                     s "seconds"]
                    (partition-all 2)
                    (map-indexed
                      (fn [i [n units]]
                        (let [value (ks/pad n 2)]
                          [:div {:key i
                                 :style {:text-align 'center}}
                           [:div
                            {:style {:font-size font-size
                                     :line-height "100%"}}

                            value]
                           [:div
                            {:style {:font-size (if (= :sm size)
                                                  10
                                                  14)
                                     :color "rgba(0,0,0,0.6)"}}
                            units]]))))])]))})))

(defn expander [{:keys [initial-open?]}]
  (let [!opts (atom nil)
        !open-state (r/atom (if initial-open? :open :closed))
        open-ch (chan (sliding-buffer 1))
        state {:!open-state !open-state
               :open-ch open-ch}

        on-click (fn [e]
                   (if (= :open @!open-state)
                     (put! open-ch :close)
                     (put! open-ch :open))
                   nil)]
    
    (go
      (loop []
        (let [next-state (<! open-ch)
              {:keys [transition-duration]
               :or {transition-duration 0}}
              @!opts]
          (if (= next-state :open)
            (do
              (reset! !open-state :opening)
              (<! (timeout transition-duration))
              (reset! !open-state :open))
            (do
              (reset! !open-state :closing)
              (<! (timeout transition-duration))
              (reset! !open-state :closed)))
          (recur))))
    
    (r/create-class
      {:reagent-render
       (fn [{:keys [label render-label
                    content render-content
                    style
                    transition-duration]
             :as opts}]
         (let [opts (merge
                      opts
                      {:open? (= :open @!open-state)
                       :opening? (= :opening @!open-state)
                       :closing? (= :closing @!open-state)
                       :closed? (= :closed @!open-state)})]
           [group
            {:style style}
            [:div
             {:style {:cursor 'pointer
                      :user-select 'none}
              :on-click on-click}
             (cond
               label label
               render-label [render-label opts])]

            [:div
             {:style {:height (if (get #{:closed} @!open-state)
                                0
                                'auto)
                      :overflow 'hidden
                      :transition (str "opacity " transition-duration "ms ease")
                      :opacity (if (get #{:open :opening} @!open-state)
                                 1
                                 0)}}
             (when (get #{:opening :open :closing} @!open-state)
               (cond
                 content content
                 render-content [render-content opts]))]]))})))

(defn list-container [& args]
  (let [[opts & children] (ks/ensure-opts args)
        children (ks/unwrap-children children)

        {:keys [layer]
         :or {layer 0}} opts
        
        {:keys [separator-color]}
        (th/resolve opts
          [[:separator-color (keyword
                               "color"
                               (str "bg-" (inc layer)))]])]
    (into
      [group
       opts]
      (concat
        [[:div {:style {:height 1
                        :background-color separator-color}}]]
        (->> children
             (interpose
               [:div {:style {:height 1
                              :background-color separator-color}}]))
        [[:div {:style {:height 1
                        :background-color separator-color}}]]))))

(defn clickable [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    (into
      [g
       (merge
         {:on-click (:on-click opts)
          :style (merge
                   {:cursor 'pointer}
                   (:style opts))
          :css [{:user-select 'none}
                ["&:hover" {:opacity 0.8}]
                ["&:active" {:opacity 1}]]}
         opts)]
      children)))

(defn panel [& args]
  (let [[opts & args] (ks/ensure-opts args)
        
        {:keys [layer]
         :or {layer 0}} opts
        
        {:keys [bg-color
                fg-color]}
        (th/resolve
          opts
          [[:bg-color (keyword
                        "color"
                        (str "bg-" layer))]
           [:fg-color :color/fg-0]])]
    
    (into
      [g
       (merge opts
         {:style (merge
                   {:color fg-color
                    :background-color bg-color}
                   (:style opts))})]
      args)))

(defn separator [{:keys [layer size style]
                  :or {layer 0
                       size 1}
                  :as opts}]
  (let [{:keys [bg-color]}
        (th/resolve opts
          [[:bg-color (keyword
                        "color"
                        (str "bg-" (inc layer)))]])]
    [:div
     {:style (merge
               {:height size
                :background-color bg-color}
               style)}]))

(defn label [& [opts & children :as args]]
  (let [[{:keys [text style] :as opts} & children] (ks/ensure-opts args)
        children (ks/unwrap-children children)]
    (into
      [:label
       {:style (merge
                 {:font-weight 'bold
                  :font-size 12}
                 style)}
       text]
      children)))

(defn test-g []
  (browser/<show-component!
    [g
     {:gap 20
      :pad 30}
     [g {:gap 12
         :pad 8
         :style {:background-color "#eee"}}
      [:div
       "HERE"]
      [:div
       "two"]]
     [g {:horizontal? true
         :gap 12
         :style {:background-color "#eee"}}
      (->> (range 100)
           (map (fn [i]
                  [:div
                   {:key i
                    :style {:width 30
                            :height 30
                            :background-color "#ddd"}}
                   i])))]
     [g {:horizontal? true
         :gap 12
         :fill? true
         :center? true
         :style {:height 100}}
      [:div "before"]
      (->> (range 3)
           (map (fn [i]
                  [:div
                   {:style {:width 30
                            :background-color "#ddd"}}
                   [:div i]
                   [:div i]])))
      [:div "after"]]


     [g
      {:gap 30}
      [:div "Level 0 -- Gap 30"]
      [g
       {:gap 12}
       [:div "Level 1 -- Gap 12"]
       [:div "12"]
       [:div "12"]
       [:div "12"]
       [:div "12"]]
      [g
       {:gap 12}
       [:div "Level 1 -- Gap 12"]
       [:div "12"]
       [:div "12"]
       [:div "12"]
       [:div "12"]]]]))



(comment

  (test-g)


  )
