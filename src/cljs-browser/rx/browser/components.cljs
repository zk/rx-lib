(ns rx.browser.components
  (:require [clojure.string :as str]
            [rx.kitchen-sink :as ks]
            [rx.css :as rc]
            [rx.icons.feather :as feather]
            [rx.viewreg :as vr]
            [rx.view :as view]
            [rx.theme :as th]
            [rx.browser.popover :as po]
            [garden.units :as u]
            [garden.color :as co]
            [garden.core :as garden]
            [garden.stylesheet :as gs]
            [reagent.core :as r]
            [dommy.core :as dommy]
            [rx.browser.bnav :as bnav]
            [rx.browser :as browser]
            [cljs.reader :as reader]
            #_["react-window" :as rw]
            [cljs.core.async :as async
             :refer [<! >! chan close!
                     sliding-buffer put! take!
                     alts! timeout pipe mult tap]
             :refer-macros [go go-loop]]))

(defn transition [v]
  {:transition v
   :-webkit-transition (if (string? v)
                         (str/replace v #"transform" "-webkit-transform")
                         v)
   :-moz-transition v
   :-ms-transition v
   :-o-transition v})

(def css
  [(gs/at-keyframes
     :indprogress
     ["0%" {:transform "scaleX(0) translate3d(0,0,0)"
            :-webkit-backface-visibility "hidden"
            :-webkit-perspective "1000px"}]
     ["30%" {:transform "scaleX(0.6) translate3d(0,0,0)"
             :-webkit-backface-visibility "hidden"
             :-webkit-perspective "1000px"}]
     ["55%" {:transform "scaleX(0.75) translate3d(0,0,0)"
             :-webkit-backface-visibility "hidden"
             :-webkit-perspective "1000px"}]
     ["100%" {:transform "scaleX(1) translate3d(0,0,0)"
              :-webkit-backface-visibility "hidden"
              :-webkit-perspective "1000px"}])

   [:.prog-bar-mock
    {:transform-origin "left center"}]

   [:.prog-bar-mock-bar
    {:background-color 'green
     :transform-origin "left center"
     :transform "scaleX(0) translate3d(0,0,0)"
     :-webkit-backface-visibility "hidden"
     :-webkit-perspective "1000px"}
    {:animation "indprogress 20s ease infinite"}]

   [:.prog-bar-mock-done-bar
    {:transform "scaleX(0)"
     :transform-origin "left center"
     :background-color 'green}
    (transition "transform 0.2s ease-in")
    [:&.done
     {:transform "scaleX(1)"}]]])

(defn interpose-children [{:keys [separator] :as opts} children]
  (into
    [:div
     (dissoc opts :separator)]
    (->> children
         (remove nil?)
         (interpose separator))))

(defn vspacer [size]
  [:div {:style {:height size}}])

(defn list-separator [opts]
  (let [{:keys [:list/separator-color
                :list/separator-size]
         :or {separator-color "white"
              separator-size 1}}
        (th/get! opts)]
    [:div {:style {:height separator-size
                   :background-color separator-color}}]))

(defn $prog-bar-mock [_]
  (let [!ui (r/atom nil)
        !run (atom true)]
    (r/create-class
      {:reagent-render
       (fn [{:keys [loading? done? style stick-to
                    bar-color
                    bar-height]
             :or {bar-height 5}}]
         [:div.prog-bar-mock
          {:class (when done? "done")
           :style (merge
                    {:overflow 'hidden
                     :height bar-height}
                    (cond
                      (not stick-to)
                      {:width "100%"}

                      (= :top stick-to)
                      {:position 'absolute
                       :top 0
                       :left 0
                       :right 0}

                      (= :bot stick-to)
                      {:position 'absolute
                       :bottom 0
                       :left 0
                       :right 0})
                    style)}
          (when loading?
            [:div {:style {:position 'relative
                           :width "100%"
                           :height "100%"}}
             [:div.prog-bar-mock-bar
              {:style (merge
                        {:height (or bar-height 5)
                         :width "100%"}
                        (when bar-color
                          {:background-color bar-color}))}]
             [:div.prog-bar-mock-done-bar
              {:class (when done? "done")
               :style (merge
                        (when bar-color
                          {:background-color bar-color})
                        {:position 'absolute
                         :top 0
                         :left 0
                         :right 0
                         :bottom 0})}]])])})))

(defn $flipper [& args]
  #_(page/async-class
      (page/ensure-opts args)
      {:delay-fn
       (fn [[_ & old-c :as old] [_ & new-c :as new]]
         (when (not= old-c new-c)
           [[16
             (update
               old
               0
               merge
               {:stage :post})]
            [200
             (update
               old
               0
               merge
               {:stage :pre})]
            [50
             (update
               new
               0
               merge
               {:stage :vis})
             200]]))
       :reagent-render
       (fn [& args]
         (let [[{:keys [stage disabled? style]} & children] (page/ensure-opts args)]
           (let [stage (if disabled?
                         nil
                         stage)]
             [:div.flipper-viewbox
              {:class (when stage "updating")
               :style (merge
                        {:overflow 'hidden}
                        style)}
              (vec
                (concat
                  [:div
                   {:style (merge
                             {:transform "translate3d(0,0,0)"
                              :-webkit-backface-visibility "hidden"
                              :-webkit-perspective "1000px"}
                             (transition "transform 0.2s ease")
                             (when (= :post stage)
                               {:transform "translate3d(0,100%,0)"})
                             (when (= :pre stage)
                               (merge
                                 {:transform "translate3d(0,-100%,0)"}
                                 (transition 'none))))}]
                  children))])))}))

(defn $hamburger-menu
  [{:keys [open?
           color
           on-toggle
           line-width
           line-cap
           style
           size]
    :or {line-width 9
         line-cap :square
         color 'black
         size 25}}]
  (let [stroke-width line-width
        stroke-linecap (name line-cap)]
    [:div.hamburger
     {:class (when open? "open")
      :style (merge
               {:width size
                :height size
                :cursor 'pointer}
               style)
      :on-click (fn [e]
                  (.preventDefault e)
                  (on-toggle)
                  nil)}
     [:svg {:width "100%"
            :height "100%"
            :viewBox "0 0 100 100"
            :style {:display 'block}}
      [:line.line.top-line
       {:x1 10 :y1 24
        :x2 90 :y2 24
        :style (merge
                 (transition "transform 0.2s ease")
                 {:stroke-width stroke-width
                  :stroke-linecap stroke-linecap}
                 (when color
                   {:stroke color})
                 (when open?
                   {:transform ""})
                 (when open?
                   {:transform "rotate(45deg) translate3d(0,25%,0)"
                    :transform-origin "center"
                    :-webkit-backface-visibility "hidden"
                    :-webkit-perspective "1000px"}))}]

      [:line.line.mid-line
       {:x1 10 :y1 50
        :x2 90 :y2 50
        :style (merge
                 (transition "opacity 0.2s ease")
                 {:stroke-width (+ stroke-width 0.5)
                  :stroke-linecap stroke-linecap}
                 (when color
                   {:stroke color})
                 (when open?
                   {:opacity 0}))}]

      [:line.line.bot-line
       {:x1 10 :y1 75
        :x2 90 :y2 75
        :style (merge
                 (transition "transform 0.2s ease")
                 {:stroke-width stroke-width
                  :stroke-linecap stroke-linecap}
                 (when color
                   {:stroke color})
                 (when open?
                   {:transform "rotate(-45deg) translate3d(0,-25%,0)"
                    :transform-origin "center"
                    :-webkit-backface-visibility "hidden"
                    :-webkit-perspective "1000px"}))}]]]))

(defn hamburger-menu [& args] (apply $hamburger-menu args))

(defn copy-button-css
  [& [{:keys [rest-bg
              rest-fg

              active-bg
              active-fg

              hover-bg
              hover-fg]
       :or {rest-bg 'white
            rest-fg 'black

            hover-bg "#eee"
            hover-fg "#777"

            active-bg 'black
            active-fg 'white}}]]
  [:.copy-button
   (transition "background-color 1s ease")
   [:a
    {:padding "6px"}
    [:i
     (transition "color 0.1s ease")
     {:color rest-fg}]]
   [:&:hover [:i
              (transition "color 0.5s ease")
              {:color hover-fg}]]
   [:&.highlighted
    (transition "none")
    {:background-color active-bg}
    [:a [:i
         {:color active-fg}
         (transition "none")]]]])

(defn copy-to-clipboard [text]
  (let [ta (.createElement js/document "textarea")
        x (.-scrollX js/window)
        y (.-scrollY js/window)]
    (dommy/set-style! ta :position "absolute")
    (dommy/set-style! ta :bottom 0)
    (dommy/set-style! ta :opacity 0)
    (set! (.-value ta) text)
    (.appendChild
      (.-body js/document)
      ta)
    (.focus ta)
    (.scrollTo js/window x y)
    (.select ta)
    (.execCommand js/document "copy")
    (.removeChild
      (.-body js/document)
      ta)))

 (defn $copy-button [_]
   (let [!copied? (r/atom false)
         !hover? (r/atom nil)
         !ct (atom 0)]
     (r/create-class
       {#_:component-did-update
        #_(page/cdu-diff
            (fn [[{ot :text}] [{nt :text}]]
              (when (not= ot nt)
                (reset! !copied? false))))
        :reagent-render
        (fn [{:keys [text
                     style

                     rest-bg
                     rest-fg

                     active-bg
                     active-fg

                     hover-bg
                     hover-fg

                     icon-size
                     render-icon]
              :or {icon-size 22}}]
          [:div.copy-button.text-center
           {:class (when @!copied?
                     "highlighted")
            :style (merge
                     (transition "background-color 1s ease")
                     (if @!copied?
                       (merge
                         {:background-color active-bg}
                         (transition "none"))
                       {:background-color rest-bg})
                     style)
            :on-mouse-over
            (fn [])}
           [:div
            {:style {:display 'block
                     :padding "3px"
                     :line-height "50%"
                     :cursor 'pointer}
             :on-mouse-over
             (fn [e]
               (.preventDefault e)
               (reset! !copied? false)
               #_(reset! !hover? true)
               nil)
             :on-mouse-out
             (fn [e]
               (.preventDefault e)
               #_(reset! !hover? false)
               nil)
             :on-click
             (fn [e]
               (.preventDefault e)
               (.stopPropagation e)
               (copy-to-clipboard text)
               (reset! !copied? true)
               (go
                 (let [ct-val (ks/now)]
                   (reset! !ct ct-val)
                   (<! (timeout 100))
                   (when (= @!ct ct-val)
                     (reset! !copied? false))))
               nil)}
            (if render-icon
              [render-icon {:copied? @!copied?}]
              [:i.ion-ios-copy
               {:style (merge
                         (transition "color 0.5s ease")
                         (if @!copied?
                           (merge
                             {:color active-fg}
                             (transition "none"))
                           {:color rest-fg})
                         {:font-size icon-size
                          :margin 5})}])]])})))

(defn copy-button [_]
  (let [!copied? (r/atom false)
        !hover? (r/atom nil)
        !ct (atom 0)]
    (r/create-class
      {#_:component-did-update
       #_(page/cdu-diff
           (fn [[{ot :text}] [{nt :text}]]
             (when (not= ot nt)
               (reset! !copied? false))))
       :reagent-render
       (fn [{:keys [text
                    style

                    rest-bg
                    rest-fg

                    active-bg
                    active-fg

                    hover-bg
                    hover-fg

                    icon-size
                    render-icon]
             :or {icon-size 22}}]
         [:div.copy-button.text-center
          {:class (when @!copied?
                    "highlighted")
           :style (merge
                    (transition "background-color 1s ease")
                    {:display 'inline-block
                     :user-select 'none}
                    (if @!copied?
                      (merge
                        {:background-color active-bg}
                        (transition "none"))
                      {:background-color rest-bg})
                    style)
           :on-mouse-over
           (fn [])}
          [:div
           {:style {:display 'inline-block
                    :padding "3px"
                    :cursor 'pointer}
            :on-mouse-over
            (fn [e]
              (.preventDefault e)
              (reset! !copied? false)
              (reset! !hover? true)
              nil)
            :on-mouse-out
            (fn [e]
              (.preventDefault e)
              (reset! !hover? false)
              nil)
            :on-click
            (fn [e]
              (.preventDefault e)
              (.stopPropagation e)
              (copy-to-clipboard text)
              (reset! !copied? true)
              (go
                (let [ct-val (ks/now)]
                  (reset! !ct ct-val)
                  (<! (timeout 100))
                  (when (= @!ct ct-val)
                    (reset! !copied? false))))
              nil)}
           [po/view
            {:initial-visible? true
             :position :right-center
             :render-popover
             (fn []
               [:div
                {:style {:background-color 'black}}
                "hi"])
             :render-body
             (fn []
               (if render-icon
                 [render-icon {:copied? @!copied?}]
                 [feather/clipboard
                  {:color 'black
                   :size icon-size
                   :style {:opacity (if @!hover? 1 0.7)}}]))}]]])})))

(defn hover [& args]
  (let [[opts & children] (ks/ensure-opts args)
        !hover? (r/atom nil)
        !down? (r/atom nil)
        on-mouse-over (fn [] (reset! !hover? true))
        on-mouse-out (fn [] (reset! !hover? false))
        on-mouse-down (fn [e]
                        (prn "DOWN")
                        (reset! !down? true))
        on-mouse-up (fn [] (reset! !down? false))
        on-click (fn [e]
                   (prn "CL")
                   (reset! !down? false)
                   (when-let [f (:on-click opts)]
                     (f e)))
        
        {:keys [element]} opts]
    (r/create-class
      {:component-did-mount
       (fn []
         (.addEventListener js/window "mouseup" on-mouse-up)
         (.addEventListener js/window "touchend" on-mouse-up))
       :component-will-unmount
       (fn []
         (.removeEventListener js/window "mouseup" on-mouse-up)
         (.removeEventListener js/window "touchend" on-mouse-up))
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
                         style-down]} opts]
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
                   :on-touch-start on-mouse-down
                   :on-touch-end on-mouse-up
                   :on-click (fn [e]
                               (when-let [f (:on-click opts)]
                                 (f e)))}
                  {:style (merge
                            style
                            (when @!hover? style-over)
                            (when @!down? style-down))})]
               children))))})))

(defn $button [{:keys [title
                       on-action
                       style
                       style-over
                       style-down]}
               & children]
  (into
    [hover
     {:style (merge
               {:cursor 'pointer
                :font-weight 'bold
                :text-transform 'uppercase
                :display 'inline-block
                :padding 5}
               style)
      :style-over style-over
      :style-down style-down
      :on-click (fn [e]
                  (.preventDefault e)
                  (if on-action
                    (on-action e)
                    (println "No action set for button:" title))
                  nil)}
     title]
    children))

(def button $button)

(defn navbar [{:keys [nav
                      center
                      title
                      left
                      right
                      back-button?
                      button-style
                      style]}]
  [:div
   {:style (merge
             {:border-bottom "solid black 1px"
              :padding 10
              :display 'flex
              :flex-direction 'row
              :justify-content 'space-between
              :align-items 'center
              :background-color "#333"
              :color "rgba(255,255,255,0.5)"})}
   [:div
    {:style {:flex 1
             :flex-grow 1
             :flex-shrink 0
             :text-align 'left
             :white-space 'nowrap
             :overflow 'hidden
             :text-overflow 'ellipsis}}
    (or
      (when left
        [left {:nav nav}])
      (when (bnav/has-prev-view? nav)
        [$button {:title "BACK"
                  :on-action #(bnav/<pop nav)
                  :style button-style}]))]

   [:div
    {:style {:flex 1
             :flex-grow 1
             :flex-shrink 0
             :display 'flex
             :align-items 'center
             :justify-content 'center}}
    (if center
      [center {:nav nav}]
      (or title "notitle"))]

   [:div
    {:style {:flex 1
             :flex-grow 1
             :flex-shrink 0
             :text-align 'right
             :white-space 'nowrap
             :overflow 'hidden
             :text-overflow 'ellipsis}}
    (when right
      [right {:nav nav}])]])

(defn navroot [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    [:div
     {:style {:flex 1
              :display 'flex
              :flex-direction 'column
              :overflow 'hidden}}
     [navbar opts]
     (into
       [:div
        {:style (merge
                  {:flex 1
                   :overflow-y 'scroll
                   :display 'flex
                   :flex-direction 'column
                   :overflow-x 'hidden}
                  (:style opts))}]
       children)]))

(defn $countdown [{:keys [end-ts]}]
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
                     :padding 10
                     :text-align 'center
                     :margin-left 'auto
                     :margin-right 'auto}}

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
              #_[page/$interpose-children
                 {:separator [:div {:style {:width gutter-size}}]
                  :class "flex-center"}
                 (->> [d "days"
                       h "hours"
                       m "minutes"
                       s "seconds"]
                      (partition-all 2)
                      (map-indexed
                        (fn [i [n units]]
                          (let [value (ks/pad n 2)]
                            [:div {:key i
                                   :style {:width "25%"
                                           :text-align 'center}}
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

(defn $abl [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    (into
      [:a (merge
            {:target "_blank"
             :rel "noopener"}
            opts)]
      children)))

(defn $a [& args]
  (let [[{:keys [on-click] :as opts}
         & children]
        (ks/ensure-opts args)]
    (into
      [:a (merge
            {:href "#"}
            opts
            {:on-click (fn [e]
                         (.preventDefault e)
                         (when on-click
                           (on-click))
                         nil)})]
      children)))

(defn $popover [& args]
  (let [!ui (r/atom nil)
        !state (r/atom (assoc-in
                         (ks/ensure-opts args)
                         [0 :visible?]
                         false))
        !mouseover-timeout (atom nil)
        ch (chan 10)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go-loop []
           (when-let [next-state (<! ch)]
             (let [[{ov :visible?
                     oc :content
                     :as oo}]
                   @!state

                   [{nv :visible?
                     nc :content
                     nmo? :enable-mouse-over?
                     animate-content-transition?
                     :animate-content-transition?

                     :as no}]
                   next-state

                   ;; When vis controlled by mouse / touch, ignore
                   ;; :visible? from args change
                   nv (if nmo?
                        ov
                        nv)

                   next-state (assoc-in next-state [0 :visible?] nv)]

               (cond
                 (and (not= ov nv)
                      (not nv))
                 (do
                   (swap! !state assoc-in [0 :visible?] nv)
                   (<! (timeout 100))
                   (reset! !state next-state))

                 (and (not= oc nc)
                      ov
                      nv
                      animate-content-transition?)
                 (do
                   (swap! !state assoc-in [0 :visible?] false)
                   (<! (timeout 300))
                   (swap! !state
                     (fn [state]
                       (-> state
                           (assoc-in [0 :content] nc)
                           (assoc-in [0 :visible?] true)))))

                 :else (reset! !state next-state))
               (recur))))
         (put! ch (ks/ensure-opts args)))
       :component-will-unmount
       (fn [_]
         (close! ch))
       #_:component-did-update
       #_(page/cdu-diff
           (fn [[{ov :visible?
                  oemo :enable-mouse-over?
                  :as oo} :as old]
                [{nv :visible?
                  nemo :enable-mouse-over?
                  :as no} :as new]]
             (when (not= old new)
               (put! ch (page/ensure-opts new)))
             (when (and (not nemo)
                        (not= oemo nemo))
               (js/clearTimeout @!mouseover-timeout)
               (reset! !mouseover-timeout nil))))
       :reagent-render
       (fn [_ & _]
         (let [[opts body] @!state
               {:keys [position
                       style
                       width
                       border-color
                       pop-style
                       enable-mouse-over?
                       mouseover-delay
                       offset
                       offset-right
                       visible?
                       inline?
                       border-color
                       no-pad?]
                po-content :content
                :or {position :bot-center
                     color 'white
                     width "100%"
                     offset 0}}
               opts

               {:keys [slide-axis
                       slide-dist
                       top
                       bottom
                       left
                       right
                       tx
                       ty
                       h-align
                       v-align
                       carat-side]}

               (condp = position
                 :top-left {:slide-axis "translateY"
                            :slide-dist 5
                            :top (+ -3 offset)
                            :left 0
                            :tx "0"
                            :ty "-100%"
                            :h-align 'left
                            :carat-side :bot}

                 :top-center {:slide-axis "translateY"
                              :slide-dist 5
                              :top (+ -3 offset)
                              :left "50%"
                              :tx "-50%"
                              :ty "-100%"
                              :h-align 'center
                              :carat-side :bot}

                 :top-right {:slide-axis "translateY"
                             :slide-dist 5
                             :top (+ -3 offset)
                             :right 0
                             :tx "0"
                             :ty "-100%"
                             :h-align 'right
                             :carat-side :bot}

                 :bot-left {:slide-axis "translateY"
                            :slide-dist 5
                            :bottom (+ -3 offset)
                            :left 0
                            :tx "0"
                            :ty "100%"
                            :h-align 'left
                            :carat-side :top}

                 :bot-center {:slide-axis "translateY"
                              :slide-dist 5
                              :bottom (+ -3 offset)
                              :left "50%"
                              :tx "-50%"
                              :ty "100%"
                              :h-align 'center
                              :carat-side :top}

                 :bot-right {:slide-axis "translateY"
                             :slide-dist 5
                             :bottom (+ -3 offset)
                             :right (+ 0 offset-right)
                             :tx "0"
                             :ty "100%"
                             :h-align 'right
                             :carat-side :top}

                 :left-center {:slide-axis "translateX"
                               :slide-dist 5
                               :left (+ -3 offset)
                               :top "50%"
                               :tx "-100%"
                               :ty "-50%"
                               :carat-side :right
                               :v-align 'center}

                 :right-center {:slide-axis "translateX"
                                :slide-dist 5
                                :right (+ -3 offset)
                                :top "50%"
                                :tx "100%"
                                :ty "-50%"
                                :carat-side :left
                                :v-align 'center})

               h-align-margin (merge (when (= h-align 'left)
                                       {:margin-left 0})
                                (when (= h-align 'right)
                                  {:margin-right 0}))
               v-align-margin {}]
           [:div.popover-wrapper
            (merge
              {:style (merge
                        {:position 'relative}
                        (when inline?
                          {:display 'inline})
                        style)}
              (when enable-mouse-over?
                {:on-mouse-over
                 (fn [e]
                   (if mouseover-delay
                     (swap! !mouseover-timeout
                       (fn [to]
                         (when to
                           (js/clearTimeout to))
                         (js/setTimeout
                           (fn []
                             (swap! !state assoc-in [0 :visible?] true))
                           mouseover-delay)))
                     (swap! !state assoc-in [0 :visible?] true))
                   nil)
                 :on-mouse-out
                 (fn [e]
                   (when @!mouseover-timeout
                     (js/clearTimeout @!mouseover-timeout)
                     (reset! !mouseover-timeout nil))
                   (swap! !state assoc-in [0 :visible?] false)
                   nil)
                 :on-touch-end
                 (fn [e]
                   (swap! !state update-in [0 :visible?] not)
                   nil)}))
            body
            [:div
             {:style (merge
                       {:position 'absolute
                        :width width
                        :opacity (if visible?
                                   1
                                   0)
                        :z-index 2000
                        :pointer-events (if visible?
                                          'inherit
                                          'none)}
                       {:transform (str "translate("
                                        tx
                                        ","
                                        ty
                                        ")")}
                       (when top
                         {:top top})
                       (when bottom
                         {:bottom bottom})
                       (when left
                         {:left left})
                       (when right
                         {:right right})
                       (when h-align
                         {:text-align h-align})
                       (transition "opacity 0.1s ease"))}
             [:div
              {:style (merge
                        {:transform (if visible?
                                      "translateY(0)"
                                      "translateY(5px)")}
                        (transition "opacity 0.1s ease, transform 0.1s ease"))}
              (when (= :top carat-side)
                [:svg {:width "45px"
                       :height "12px"
                       :viewBox "0 0 100 100"
                       :preserveAspectRatio "none"
                       :style (merge
                                {:shape-rendering "geometricPrecision"
                                 :display 'block
                                 :margin-left 'auto
                                 :margin-right 'auto
                                 :padding 0}
                                h-align-margin)}
                 [:polygon
                  {:points "0,100 50,0 100,100"
                   :style {:fill 'black}}]
                 (when border-color
                   [:polyline
                    {:fill 'none
                     :stroke border-color
                     :stroke-width 6
                     :points "0,100 50,8 100,100"}])])
              [:div
               {:style {:display "flex"
                        :justify-content 'center
                        :align-items 'center}}
               (when (= :left carat-side)
                 [:svg {:width "6px"
                        :height "16px"
                        :viewBox "0 0 100 100"
                        :preserveAspectRatio "none"
                        :style (merge
                                 {:shape-rendering "geometricPrecision"
                                  :display 'block
                                  :margin-left 'auto
                                  :margin-right 'auto
                                  :padding 0}
                                 v-align-margin)}
                  [:polygon
                   {:points "0,50 100,100 100,0"
                    :style {:fill 'black}}
                   (when border-color
                     [:polyline
                      {:fill 'none
                       :stroke border-color
                       :stroke-width 6

                       :points "0,100 50,8 100,100"}])]])
               [:div
                {:on-touch-end (fn [e]
                                 (.stopPropagation e)
                                 nil)
                 :style (merge
                          {:flex 1
                           :z-index 2000
                           :border-radius 5
                           :position 'relative
                           :overflow 'hidden
                           :background-color 'black
                           :color 'white
                           :padding (when-not no-pad? "7px 10px")
                           :font-size 14
                           :text-align 'center
                           :box-shadow "0 4px 8px 0 rgba(0,0,0,0.12), 0 2px 4px 0 rgba(0,0,0,0.08)"}

                          (when border-color
                            {:border (str "solid " border-color " 1px")})
                          (when (= :top carat-side)
                            {:margin-top -5})
                          (when (= :bot carat-side)
                            {:margin-bottom -5})
                          pop-style)}
                po-content]
               (when (= :right carat-side)
                 [:svg {:width "6px"
                        :height "16px"
                        :viewBox "0 0 100 100"
                        :preserveAspectRatio "none"
                        :style (merge
                                 {:shape-rendering "geometricPrecision"
                                  :display 'block
                                  :margin-left 'auto
                                  :margin-right 'auto
                                  :padding 0}
                                 v-align-margin)}
                  [:polygon
                   {:points "0,0 0,100 100,50"
                    :style {:fill 'black}}
                   (when border-color
                     [:polyline
                      {:fill 'none
                       :stroke border-color
                       :stroke-width 6

                       :points "0,100 50,8 100,100"}])]])]
              (when (= :bot carat-side)
                [:svg {:width "45px"
                       :height "12px"
                       :viewBox "0 0 100 100"
                       :preserveAspectRatio "none"
                       :style (merge
                                {:shape-rendering "geometricPrecision"
                                 :display 'block
                                 :margin-left 'auto
                                 :margin-right 'auto
                                 :padding 0}
                                h-align-margin)}
                 [:polygon
                  {:points "0,0 50,100 100,0"
                   :style {:fill 'black}}
                  (when border-color
                    [:polyline
                     {:fill 'none
                      :stroke border-color
                      :stroke-width 6
                      :points "0,100 50,8 100,100"}])]])]]]))})))

(defn popover-control []
  (atom nil))

(defn show-popover [control]
  (let [{:keys [show-popover]} @control]
    (show-popover)))

(defn hide-popover [control]
  (let [{:keys [hide-popover]} @control]
    (when hide-popover
      (hide-popover))))

(defn $popover2 [{:keys [initial-visible?
                         dismiss-on-escape?
                         control]}]
  (let [!ui (r/atom nil)
        !vis? (r/atom initial-visible?)
        !mouseover-timeout (atom nil)
        control-map {:show-popover
                     (fn []
                       (reset! !vis? true))
                     :hide-popover
                     (fn []
                       (reset! !vis? false))
                     :toggle-popover
                     (fn []
                       (swap! !vis? not))}
        on-keydown (fn [e]
                     (when (= 27 (.. e -keyCode))
                       (reset! !vis? false)))]
    (when control
      (reset! control control-map))
    (r/create-class
      {:component-did-mount
       (fn [_]
         (when dismiss-on-escape?
           (dommy/listen!
             js/window
             :keydown
             on-keydown)))
       :component-will-unmount
       (fn [_]
         (dommy/unlisten!
           js/window
           :keydown
           on-keydown))
       :component-did-update
       (fn [_])
       :reagent-render
       (fn [opts]
         (let [{:keys [position
                       style
                       width
                       border-color
                       pop-style
                       enable-mouse-over?
                       mouseover-delay
                       offset
                       offset-right
                       visible?
                       inline?
                       border-color
                       no-pad?

                       render-body
                       render-popover]
                :or {position :bot-center
                     color 'white
                     offset 0}}

               opts

               visible? @!vis?


               {:keys [slide-axis
                       slide-dist
                       top
                       bottom
                       left
                       right
                       tx
                       ty
                       h-align
                       v-align
                       carat-side]}

               (condp = position
                 :top-left {:slide-axis "translateY"
                            :slide-dist 5
                            :top (+ -3 offset)
                            :left 0
                            :tx "0"
                            :ty "-100%"
                            :h-align 'left
                            :carat-side :bot}

                 :top-center {:slide-axis "translateY"
                              :slide-dist 5
                              :top (+ -3 offset)
                              :left "50%"
                              :tx "-50%"
                              :ty "-100%"
                              :h-align 'center
                              :carat-side :bot}

                 :top-right {:slide-axis "translateY"
                             :slide-dist 5
                             :top (+ -3 offset)
                             :right 0
                             :tx "0"
                             :ty "-100%"
                             :h-align 'right
                             :carat-side :bot}

                 :bot-left {:slide-axis "translateY"
                            :slide-dist 5
                            :bottom (+ -3 offset)
                            :left 0
                            :tx "0"
                            :ty "100%"
                            :h-align 'left
                            :carat-side :top}

                 :bot-center {:slide-axis "translateY"
                              :slide-dist 5
                              :bottom (+ -3 offset)
                              :left "50%"
                              :tx "-50%"
                              :ty "100%"
                              :h-align 'center
                              :carat-side :top}

                 :bot-right {:slide-axis "translateY"
                             :slide-dist 5
                             :bottom (+ -3 offset)
                             :right (+ 0 offset-right)
                             :tx "0"
                             :ty "100%"
                             :h-align 'right
                             :carat-side :top}

                 :left-center {:slide-axis "translateX"
                               :slide-dist 5
                               :left (+ -3 offset)
                               :top "50%"
                               :tx "-100%"
                               :ty "-50%"
                               :carat-side :right
                               :v-align 'center}

                 :right-center {:slide-axis "translateX"
                                :slide-dist 5
                                :right (+ -3 offset)
                                :top "50%"
                                :tx "100%"
                                :ty "-50%"
                                :carat-side :left
                                :v-align 'center})

               h-align-margin (merge (when (= h-align 'left)
                                       {:margin-left 0})
                                (when (= h-align 'right)
                                  {:margin-right 0}))
               v-align-margin {}]
           [:div.popover-wrapper
            (merge
              {:style (merge
                        {:position 'relative}
                        (when inline?
                          {:display 'inline})
                        style)})
            [render-body control-map]
            [:div
             {:style (merge
                       {:position 'absolute
                        :opacity (if visible?
                                   1
                                   0)
                        :z-index 2000
                        :pointer-events (if visible?
                                          'inherit
                                          'none)}
                       {:transform (str "translate("
                                        tx
                                        ","
                                        ty
                                        ")")}
                       (when top
                         {:top top})
                       (when bottom
                         {:bottom bottom})
                       (when left
                         {:left left})
                       (when right
                         {:right right})
                       (when h-align
                         {:text-align h-align})
                       (transition "opacity 0.1s ease"))}
             [:div
              {:style (merge
                        {:transform (if visible?
                                      "translateY(0)"
                                      "translateY(5px)")}
                        (transition "opacity 0.1s ease, transform 0.1s ease"))}
              (when (= :top carat-side)
                [:svg {:width "45px"
                       :height "12px"
                       :viewBox "0 0 100 100"
                       :preserveAspectRatio "none"
                       :style (merge
                                {:shape-rendering "geometricPrecision"
                                 :display 'block
                                 :margin-left 'auto
                                 :margin-right 'auto
                                 :padding 0}
                                h-align-margin)}
                 [:polygon
                  {:points "0,100 50,0 100,100"
                   :style {:fill 'black}}]
                 (when border-color
                   [:polyline
                    {:fill 'none
                     :stroke border-color
                     :stroke-width 6
                     :points "0,100 50,8 100,100"}])])
              [:div
               {:style {}}
               (when (= :left carat-side)
                 [:svg {:width "6px"
                        :height "16px"
                        :viewBox "0 0 100 100"
                        :preserveAspectRatio "none"
                        :style (merge
                                 {:shape-rendering "geometricPrecision"
                                  :display 'block
                                  :margin-left 'auto
                                  :margin-right 'auto
                                  :padding 0}
                                 v-align-margin)}
                  [:polygon
                   {:points "0,50 100,100 100,0"
                    :style {:fill 'black}}
                   (when border-color
                     [:polyline
                      {:fill 'none
                       :stroke border-color
                       :stroke-width 6

                       :points "0,100 50,8 100,100"}])]])
               [:div
                {:on-touch-end (fn [e]
                                 (.stopPropagation e)
                                 nil)
                 :style (merge
                          {:flex 1
                           :z-index 2000
                           :border-radius 5
                           :position 'relative
                           :background-color 'black
                           :color 'white
                           :overflow 'hidden
                           :box-shadow "0 4px 8px 0 rgba(0,0,0,0.12), 0 2px 4px 0 rgba(0,0,0,0.08)"}

                          (when border-color
                            {:border (str "solid " border-color " 1px")})
                          (when (= :top carat-side)
                            {:margin-top -5})
                          (when (= :bot carat-side)
                            {:margin-bottom -5})
                          pop-style)}
                [render-popover control-map]]
               (when (= :right carat-side)
                 [:svg {:width "6px"
                        :height "16px"
                        :viewBox "0 0 100 100"
                        :preserveAspectRatio "none"
                        :style (merge
                                 {:shape-rendering "geometricPrecision"
                                  :display 'block
                                  :margin-left 'auto
                                  :margin-right 'auto
                                  :padding 0}
                                 v-align-margin)}
                  [:polygon
                   {:points "0,0 0,100 100,50"
                    :style {:fill 'black}}
                   (when border-color
                     [:polyline
                      {:fill 'none
                       :stroke border-color
                       :stroke-width 6

                       :points "0,100 50,8 100,100"}])]])]
              (when (= :bot carat-side)
                [:svg {:width "45px"
                       :height "12px"
                       :viewBox "0 0 100 100"
                       :preserveAspectRatio "none"
                       :style (merge
                                {:shape-rendering "geometricPrecision"
                                 :display 'block
                                 :margin-left 'auto
                                 :margin-right 'auto
                                 :padding 0}
                                h-align-margin)}
                 [:polygon
                  {:points "0,0 50,100 100,0"
                   :style {:fill 'black}}
                  (when border-color
                    [:polyline
                     {:fill 'none
                      :stroke border-color
                      :stroke-width 6
                      :points "0,100 50,8 100,100"}])]])]]]))})))

(defn $video [{:keys [muted?]}]
  (let [!node (atom nil)
        set-muted (fn [muted?]
                    (when @!node
                      (if muted?
                        (.setAttribute @!node
                          "muted"
                          "muted")
                        (.removeAttribute @!node "muted"))
                      ))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (set-muted muted?))
       #_:component-did-update
       #_(page/cdu-diff
           (fn [[{op :playing?
                  om :muted?}]
                [{np :playing?
                  nm :muted?}]]

             (when (not= op np)
               (if np
                 (.play @!node)
                 (.pause @!node)))

             (when (not= om nm)
               (set-muted nm))))

       :reagent-render
       (fn [{:keys [webm mp4 ogg
                    autoplay? loop? controls?
                    muted? playinline?
                    style]
             :as opts}]
         (let [props (dissoc opts
                       :webm :mp4 :ogg
                       :autoplay? :loop? :controls?
                       :muted? :playinline?
                       :playing?)]
           [:video
            (merge
              {:ref #(when % (reset! !node %))}
              props
              (when autoplay?
                {:autoPlay "autoPlay"})
              (when loop?
                {:loop "loop"})
              (when controls?
                {:controls "controls"})
              (when muted?
                {:muted "muted"})
              (when playinline?
                {:playinline? playinline?}))
            "Your browser does not support HTML5 video. You should "
            [:a {:href "https://whatbrowser.org"} "consider updating"]
            "."
            (when webm
              [:source {:src webm :type "video/webm"}])
            (when ogg
              [:source {:src ogg :type "video/ogg"}])
            (when mp4
              [:source {:src mp4 :type "video/mp4"}])]))})))

(defn $vidbg [& args]
  (let [[{:keys []
          :as opts}
         & children] (ks/ensure-opts args)]
    (let [props (dissoc opts
                  :webm :mp4 :ogg
                  :autoplay? :loop? :controls?
                  :muted? :playinline?
                  :playing?
                  :buffered :crossorigin :height :width
                  :played :preload :poster
                  :src)]
      [:div.vidbg
       (merge
         props
         {:style {:position 'relative
                  :width "100%"
                  :height "100%"
                  :transform "translate3d(0,0,0)"
                  :-webkit-backface-visibility "hidden"
                  :-webkit-perspective "1000px"}})
       [:div
        {:style {:position 'absolute
                 :width "100%"
                 :height "100%"
                 :overflow 'hidden
                 :transform "translate3d(0,0,0)"
                 :-webkit-backface-visibility "hidden"
                 :-webkit-perspective "1000px"}}
        [:div
         {:style {:position 'relative
                  :width "100%"
                  :height "100%"
                  :transform "translate3d(0,0,0)"
                  :-webkit-backface-visibility "hidden"
                  :-webkit-perspective "1000px"}}

         [$video
          (merge
            (select-keys opts
              [:webm :mp4 :ogg
               :autoplay? :loop? :controls?
               :muted? :playinline?
               :playing?
               :buffered :crossorigin :height :width
               :played :preload :poster
               :src])
            {:style {:display 'block
                     :position 'absolute
                     :top "50%"
                     :left "50%"
                     :transform "translate3d(-50%,-50%,0)"
                     :-webkit-backface-visibility "hidden"
                     :-webkit-perspective "1000px"
                     :min-width "100%"
                     :min-height "100%"}})]]]
       (into
         [:div
          {:style (merge
                    {:z-index 100
                     :position 'relative
                     :width "100%"
                     :height "100%"
                     :transform "translate3d(0,0,0)"
                     :-webkit-backface-visibility "hidden"
                     :-webkit-perspective "1000px"}
                    (:style props))}]
         children)])))

(defn $cstack [{:keys [on-nav
                       view
                       initial-view
                       transition]
                :as opts}
               views]
  (let [!ui (r/atom {:view-key (or initial-view
                                   view
                                   (-> views
                                       first
                                       :key))
                     :visible? true})
        !rendered-keys (r/atom (->> [(or initial-view
                                         (-> views
                                             first
                                             :key))]
                                    (remove nil?)
                                    set))

        change-view (fn [k]
                      (when on-nav
                        (on-nav k))
                      (if (or (= :fade transition)
                              (= :quickfade transition))
                        (go
                          (swap! !ui assoc :visible? false)
                          (<! (timeout (if (= :quickfade transition)
                                         100
                                         200)))
                          (swap! !ui assoc
                            :view-key k
                            :visible? true)
                          (swap! !rendered-keys conj k))
                        (do
                          (swap! !ui assoc
                            :view-key k
                            :visible? true)
                          (swap! !rendered-keys conj k))))
        ]
    (r/create-class
      {#_:component-did-update
       #_(page/cdu-diff
           (fn [[{ov :view}] [{nv :view}]]
             (when (not= ov nv)
               (change-view nv))))
       :reagent-render
       (fn [{:keys [transition]} views]
         (let [views-lookup (->> views
                                 (map (fn [o]
                                        [(:key o) o]))
                                 (into {}))
               {:keys [view-key visible?]}
               @!ui]
           [:div
            {:style (merge
                      {:width "100%"
                       :height "100%"
                       :opacity (if visible? 1 0)
                       :position 'relative
                       :display 'flex}
                      #_(css/transition
                          (str "opacity "
                               (if (= :quickfade transition)
                                 "0.1s"
                                 "0.2s")
                               " ease")))}
            (->> @!rendered-keys
                 (map #(get views-lookup %))
                 (remove nil?)
                 (map (fn [{:keys [render render-opts key route]}]
                        (let [fore? (= key view-key)]
                          [:div
                           {:key key
                            :style {:position 'absolute
                                    :width "100%"
                                    :height "100%"
                                    :transform (str "translate3d("
                                                    (if fore? "0" "-200%")
                                                    ",0,0"
                                                    ")")
                                    :display 'flex}}
                           (if route
                             (vr/render-view route)
                             [render
                              (merge
                                render-opts
                                {:change-view change-view})])]))))]))})))

(defn $image [{:keys [src style] :as props}]
  (let [!node (atom nil)
        !ui (r/atom nil)]
    (r/create-class
      {:reagent-render
       (fn [{:keys [src style]}]
         (let [{:keys [loaded?]} @!ui]
           [:img (merge
                   {:ref #(reset! !node %)
                    :on-load (fn []
                               (swap! !ui assoc :loaded? true))}
                   props
                   {:style (merge
                             {:opacity (if loaded? 1 0)}
                             (transition "opacity 0.2s ease")
                             style)})]))})))

(def tiv-prop-keys [:ancestor
                    :direction
                    :offsets
                    :fire-on-rapid-scroll?
                    :throttle
                    :on-enter
                    :on-exit])

(defn scroll-node? [node direction]
  (when node
    (let [direction (or direction :vertical)
          style (.getComputedStyle js/window node)
          overflow-direc (if (= :vertical direction)
                           (.getPropertyValue style "overflow-y")
                           (.getPropertyValue style "overflow-x"))
          overflow (or overflow-direc
                       (.getPropertyValue style "overflow"))]
      (or (= "auto" overflow) (= "scroll" overflow)))))

(defn find-scrollable-ancestor [node direction]
  (loop [current-node node]
    (cond
      (= current-node (.-body js/document))
      js/window

      (scroll-node? current-node direction)
      current-node

      (.-parentNode current-node)
      (recur (.-parentNode current-node))

      :else current-node)))

(defn parse-offset-as-pixels [offset]
  (when offset
    (or (ks/parse-double offset)
        (ks/parse-double
          (subs
            offset
            0
            (- (count offset) 2))))))

(defn parse-offset-as-percent [offset]
  (when offset
    (when (str/includes? offset "%")
      (ks/parse-double (apply str (butlast offset))))))

(defn compute-offset-pixels [offset height]
  (or (parse-offset-as-pixels offset)
      (when-let [pct-offset (parse-offset-as-percent offset)]
        (* pct-offset height))))

(defn get-bounds [node ancestor direction offsets]
  (when (and node ancestor)
    (let [horizontal? (= :horizontal direction)
          {:keys [left top right bottom]} (dommy/bounding-client-rect node)

          wp-top (if horizontal? left top)
          wp-bot (if horizontal? right bottom)
          [context-height
           context-scroll-top]
          (if (= js/window ancestor)
            [(if horizontal?
               (.-innerWidth js/window)
               (.-innerHeight js/window))
             0]
            [(if horizontal?
               (.-offsetWidth ancestor)
               (.-offsetHeight ancestor))
             (if horizontal?
               (:left (dommy/bounding-client-rect ancestor))
               (:top (dommy/bounding-client-rect ancestor)))])

          {bot-offset :bot
           top-offset :top}
          offsets

          top-offset-px (compute-offset-pixels top-offset context-height)
          bot-offset-px (compute-offset-pixels bot-offset context-height)

          context-bot (+ context-scroll-top context-height)]

      {:wp-top wp-top
       :wp-bot wp-bot
       :viewport-top (+ context-scroll-top top-offset)
       :viewport-bot (- context-bot bot-offset-px)})))

(defn get-current-position [{:keys [wp-top wp-bot
                                    viewport-top viewport-bot]
                             :as bounds}]
  (cond
    (= 0 (- viewport-bot viewport-top)) ::invisible

    (or (<= viewport-top wp-top viewport-bot)
        (<= viewport-top wp-bot viewport-bot)
        (and (<= wp-top viewport-top)
             (>= wp-bot viewport-bot))) ::inside

    (< viewport-bot wp-top) ::below
    (< wp-top viewport-top) ::above

    :else ::invisible))

(defn handle-scroll [node
                     ancestor
                     direction
                     fire-on-rapid-scroll?
                     offsets
                     !prev-position
                     on-position-change
                     on-enter
                     on-exit]
  (when node
    (let [bounds (get-bounds node ancestor direction offsets)
          current-position (get-current-position bounds)
          prev-position @!prev-position

          on-position-change (or on-position-change
                                 (fn []))

          on-enter (or on-enter
                       (fn []))

          on-exit (or on-exit
                      (fn []))]
      (when (not= prev-position current-position)
        (reset! !prev-position current-position)
        (let [cb-arg (merge
                       {:current-position current-position
                        :previous-position prev-position}
                       bounds)]
          (when on-position-change
            (on-position-change cb-arg))

          (cond
            (= current-position ::inside)
            (on-enter cb-arg)

            (= prev-position ::inside)
            (on-exit cb-arg))

          (when (and fire-on-rapid-scroll?
                     (or (and (= prev-position ::below)
                              (= current-position ::above))
                         (and (= prev-position ::above)
                              (= current-position ::below))))
            (on-enter cb-arg)
            (on-exit cb-arg)))))))

;; Inspired by https://github.com/brigade/react-waypoint

(defn $track-in-view [& args]
  (let [prop-keys tiv-prop-keys
        [initial-opts & _] (ks/ensure-opts args)
        {:keys [ancestor
                direction
                offsets
                fire-on-rapid-scroll?
                on-position-change
                on-enter
                on-exit
                throttle]
         :or {throttle 200}} initial-opts

        throttle-ms throttle

        !node (atom nil)
        !ancestor (atom nil)
        !prev-position (atom nil)
        !on-position-change (atom on-position-change)
        !on-enter (atom on-enter)
        !on-exit (atom on-exit)

        on-scroll nil #_(page/throttle
                          (fn []
                            (handle-scroll
                              @!node
                              @!ancestor
                              direction
                              fire-on-rapid-scroll?
                              offsets
                              !prev-position
                              @!on-position-change
                              @!on-enter
                              @!on-exit))
                          throttle-ms)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (when-not @!node
           (ks/throw-str "Node not available at mount: " @!node))
         (reset! !ancestor
           (or
             ancestor
             (find-scrollable-ancestor
               @!node
               direction)))

         (dommy/listen! @!ancestor :scroll on-scroll)


         (on-scroll))

       #_:component-did-update
       #_(page/cdu-diff
           (fn [[{opc :on-position-change
                  oen :on-enter
                  oex :on-exit}]
                [{npc :on-position-change
                  nen :on-enter
                  nex :on-exit}]]
             (when (not (= opc npc))
               (reset! !on-position-change npc))
             (when (not (= oen nen))
               (reset! !on-enter nen))
             (when (not (= oex nex))
               (reset! !on-exit nex))))

       :component-will-unmount
       (fn [_]
         (dommy/unlisten! @!ancestor :scroll on-scroll))

       :reagent-render
       (fn [& args]
         (let [[opts & children] (ks/ensure-opts args)
               props (apply dissoc opts prop-keys)]
           (into
             [:div
              (merge
                {:ref #(reset! !node %)}
                props)]
             children)))})))

(defn $poller
  [{:keys [on-poll
           period
           active?]
    :or {period 1000
         on-poll (fn [& args])}}]
  (let [!run? (atom active?)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (go-loop []
           (<! (timeout 1000))
           (on-poll)
           (when @!run?
             (recur))))
       #_:component-did-update
       #_(page/cdu-diff
           (fn [[{na :active?}] [{oa :active?}]]
             (when (not= na oa)
               (reset! !run? na))))
       :component-will-unmount
       (fn [_]
         (reset! !run? false))
       :reagent-render
       (fn []
         [:span])})))

(defn htabs-control []
  (atom nil))

(defn show-tab [!c k]
  (let [{:keys [show-tab]} @!c]
    (when show-tab (show-tab k))))

(defn view [& args]
  (let [[{:keys [initial-key]}
         & [tab-specs]]
        (ks/ensure-opts args)
        !sel-key (r/atom (or initial-key
                             (-> tab-specs
                                 first
                                 :key)))
        !on-change (atom nil)
        show-tab (fn [k]
                   (let [last-sel-key @!sel-key]
                     (reset! !sel-key k)
                     (when @!on-change
                       (@!on-change k last-sel-key))))
        control-map
        {:show-tab show-tab}]
    (fn [& args]
      (let [[{:keys [tab-style
                     tab-container-style
                     selected-tab-style
                     transition
                     on-change
                     style
                     render-opts
                     control]}
             & [tab-specs]]
            (ks/ensure-opts args)
            sel-key @!sel-key
            sel-tab-spec (or (->> tab-specs
                                  (filter #(= sel-key (:key %)))
                                  first)
                             (first tab-specs))

            {:keys [render]} sel-tab-spec]
        (when control (reset! control control-map))
        (when on-change (reset! !on-change on-change))
        [:div.wrap
         {:style (merge
                   {:display 'flex
                    :flex-direction 'column
                    :flex 1
                    :overflow 'hidden}
                   style)}
         [:div
          {:style (merge
                    {:display 'flex
                     :flex-direction 'row
                     :flex-shrink 0
                     :border-bottom "solid black 1px"}
                    tab-container-style)}
          (->> tab-specs
               (map (fn [{:keys [key title]}]
                      [:div
                       {:key key
                        :style (merge
                                 {:padding 5
                                  :cursor 'pointer}
                                 tab-style
                                 (when (= sel-key key)
                                   (merge
                                     {:border-bottom "solid #ccc 4px"}
                                     selected-tab-style)))
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (show-tab key)
                                    nil)}
                       title])))]

         [:div
          {:style {:flex-grow 1
                   :position 'relative
                   :flex 1}}
          [:div
           {:style {:position 'absolute
                    :top 0
                    :left 0
                    :right 0
                    :bottom 0
                    :display 'flex}}
           [$cstack
            {:view @!sel-key
             :initial-view initial-key
             :transition transition}
            tab-specs]]]]))))

(defn $render-on-interval [{:keys [interval]}]
  (let [!run? (atom true)
        !interval (atom interval)
        !v (r/atom nil)]
    (r/create-class
      {:component-did-mount
       (fn []
         (go-loop []
           (<! (timeout @!interval))
           (swap! !v not)
           (when @!run?
             (recur))))
       :component-will-unmount
       (fn []
         (reset! !run? false))
       :reagent-render
       (fn [{:keys [interval render]}]
         @!v
         [:div {:key (rand)}
          [render ]])})))

(defn flat-list [{:keys [data
                         data-key
                         sort-key
                         sort-direction
                         render-item]}]
  [:div
   {:style {:flex-direction 'column
            :flex 1}}
   #_[page/$interpose-children
      {:separator [:div
                   {:style
                    {:height 1
                     :background-color "rgba(0,0,0,0.5)"}}]}
      (->> data
           (sort-by sort-key)
           (#(if (= :desc sort-direction)
               (reverse %)
               %))
           (map (fn [o]
                  [:div {:key (data-key o)}
                   [render-item o]])))]])

(defn list-item [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [style on-action]} opts]
    (into
      [:div
       (merge
         {:style
          (merge
            {:padding 5}
            (when on-action
              {:cursor 'pointer})
            style)}
         (when on-action
           {:class "hover-darken-bg-01"
            :on-click (fn [e]
                        (.preventDefault e)
                        (on-action e)
                        nil)}))]
      children)))

(defn $callout [{:keys [paras type]}]
  [:div.rx-callout
   {:style {:background-color "#f0f0f0"
            :padding 5 #_rc/smpx
            :border-radius 4}}
   [interpose-children
    {:separator [:div {:style {:height 50}}]}
    (->> paras
         (map
           (fn [s]
             (let [ss (if (string? s) [s] s)]
               (into
                 [:p {:style {:margin 0}}]
                 ss)))))]])

(defn searchbar
  [{:keys [initial-query
           on-change-text
           on-submit
           change-text-throttle
           autofocus?]
    :as opts}]
  (let [!input (atom nil)
        !text (r/atom nil)
        !focused? (r/atom nil)
        on-change (fn [v])
        #_(page/throttle
            (fn [text]
              (let [text (if (empty? text)
                           nil
                           text)]
                (when on-change-text
                  (on-change-text text))
                (or change-text-throttle 250)))
            0)]
    (fn [{:keys [loading? style]}]
      (let [{:keys [::bg-color
                    ::fg-color
                    ::clear-button-fg-color
                    ::clear-button-bg-color
                    ::clear-button-over-bg-color
                    ::clear-button-down-bg-color]}
            (th/des opts
              [[::bg-color :color/bg-0]
               [::fg-color :color/fg-0]
               [::clear-button-fg-color :color/fg-3]
               [::clear-button-over-bg-color :color/bg-3]
               [::clear-button-down-bg-color :color/bg-2]])

            pad-style (th/pad-padding opts {:vpad 4 :hpad 8})]
        [:div
         {:class (th/kw->class-name ::searchbar)
          :style (merge
                   {:display 'flex
                    :flex-direction 'row
                    :align-items 'center
                    :background-color bg-color}
                   style)}
         [:input (merge
                   {:autoFocus autofocus?
                    :type "text"
                    :ref #(when % (reset! !input %))
                    :default-value initial-query
                    :style (merge
                             {:background-color bg-color
                              :color fg-color
                              :border 0
                              :outline 'none
                              :width "100%"}
                             pad-style)
                    :on-change (fn [e]
                                 (let [v (.. e -target -value)]
                                   (on-change v)
                                   (reset! !text v)))
                    :on-key-down (fn [e]
                                   (when (= "Enter" (.-key e))
                                     (when on-submit
                                       (on-submit (.. e -target -value)))))
                    :on-focus (fn [e]
                                (reset! !focused? true))
                    :on-blur (fn [e]
                               (reset! !focused? false))}
                   (select-keys opts [:placeholder]))]
         (when loading?
           [:div
            {:style
             {:display 'flex
              :align-items 'center
              :justify-content 'center
              :padding-right (th/pad opts 1)}}
            [feather/loader {:size 16
                             :color clear-button-fg-color}]])

         [:div {:style {:width 20
                        :height 20}
                :on-click (fn [] (.focus @!input))}
          (when (or (not (empty? @!text))
                    @!focused?)
            [:div
             {:style {:display 'flex
                      :align-items 'center
                      :justify-content 'center
                      :padding-right (th/pad opts 1)}}
             [hover
              {:style {:display 'flex
                       :cursor 'pointer}
               :style-over {:background-color clear-button-over-bg-color}
               :style-down {:background-color clear-button-down-bg-color}
               :on-mouse-down (fn [e]
                                (.stopPropagation e)
                                (.preventDefault e)
                                (set! (.-value @!input) nil)
                                (on-change nil)
                                nil
                                #_(.clear @!input))}

              
              [feather/x
               {:size 20
                :color clear-button-fg-color}]]])]]))))


(defn $htabs [& args])

(defn input [opts]
  [:input
   (merge
     opts
     {:class (th/theme-class opts)
      :style (merge
               {:background-color 'transparent
                :border 'none
                :outline 0}
               (:style opts))})])

(defn group [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [gap
                horizontal?
                alignItems
                justifyContent
                style]}
        opts

        {:keys [baseline-space]}
        (th/resolve opts
          [[:baseline-space :space/baseline]])

        gap (or gap baseline-space)]
    (into
      [:div
       {:style (merge
                 {:display 'flex
                  :flexDirection (if horizontal? 'row 'column)
                  :alignItems alignItems
                  :justifyContent justifyContent}
                 style)}]
      (->> children
           (interpose
             [:div {:style (if horizontal?
                             {:width gap}
                             {:height gap})}])))))

(defn text [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [style]} opts

        {:keys [text-color]}
        (th/resolve opts
          [[:text-color :color/primary-text]])]
    (into
      [:span
       {:style (merge {:color text-color} style)}]
      children)))

(defn code [opts]
  (let [{:keys [style form]} opts
        {:keys [text-color]}
        (th/resolve opts
          [[:text-color
            :color/primary-text]])]
    (into
      [:pre
       {:style (merge {:color text-color} style)}]
      (ks/pp-str form))))

(defn example []
  (browser/<show-component!
    [group
     {:justifyContent "center"
      :style {:background-color 'red}}
     "hello world3"
     "foo"]))


(comment

  
  (swap! th/!default-theme
    merge
    {:space/baseline 4})

  (example)

  )

