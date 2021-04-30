(ns rx.browser.popover
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.theme :as th]
            [reagent.core :as r]
            [clojure.string :as str]
            [cljs.core.async :as async
             :refer [<! put! close! timeout chan sliding-buffer]
             :refer-macros [go]]))

(def theme-docs
  [{:rule [:bg-color ::bg-color :color/text-primary]
    :doc "Popover background color"}
   {:rule [:fg-color ::fg-color :color/bg-0]
    :doc "Popover foreground / text color"}
   {:rule [:box-shadow
           ::box-shadow
           "0 4px 8px 0 rgba(0,0,0,0.12), 0 2px 4px 0 rgba(0,0,0,0.08)"]}
   {:rule [:border-radius ::border-radius 4]
    :doc "Popover border radius"}
   {:rule [:popover-vpad ::popover-vpad 4]
    :doc "Popover top / bottom padding"}
   {:rule [:popover-hpad ::popover-hpad 12]
    :doc "Popover left / right padding"}
   {:rule [:popover-pad ::popover-pad]
    :doc "Popover padding, overrides vpad & hpad"}])

(defn transition [v]
  {:transition v
   :-webkit-transition (if (string? v)
                         (str/replace v #"transform" "-webkit-transform")
                         v)
   :-moz-transition v
   :-ms-transition v
   :-o-transition v})

(defn create-state []
  {:!ui (r/atom nil)
   :!vis? (r/atom nil)
   :!visible-state (r/atom :hidden)
   :vis-ch (chan (sliding-buffer 1))
   :!mouseover-timeout (atom nil)
   :!esc-handler (atom nil)
   :!esc-enabled? (atom nil)})

(defn show [{:keys [vis-ch]}]
  (put! vis-ch :shown))

(defn hide [{:keys [vis-ch]}]
  (let [ch (chan)]
    (put! vis-ch [:hidden ch])
    ch))

(defn toggle [{:keys [vis-ch !visible-state]}]
  (if (= :shown @!visible-state)
    (put! vis-ch :hidden)
    (put! vis-ch :shown)))

(defn po-render-body [{:keys [render-body body]} state]
  (let [component
        (cond
          render-body [render-body state]
          (fn? body) [body state]
          (vector? body) body
          :else nil)]
    component))

(defn po-render-popover [{:keys [render-popover popover]} state]
  (let [component
        (cond
          render-popover [render-popover state]
          (fn? popover) [popover state]
          (vector? popover) popover
          :else nil)]
    component))

(def position-keys
  [:top-left
   :top-center
   :top-right 
   :bot-left 
   :bot-center
   :bot-right
   :left-center
   :right-center])

(defn render-top-caret [{:keys [position] :as opts}]
  (when (get #{:bot-left :bot-center :bot-right} position)
    (let [{:keys [caret-inset caret-base caret-altitude]} opts
          {:keys [:bg-color]}
          (th/resolve opts
            [[:bg-color ::popover-bg-color "black"]])]
      [:svg {:width caret-base
             :height caret-altitude
             :viewBox "0 0 100 100"
             :preserveAspectRatio "none"
             :style (merge
                      {:shape-rendering "geometricPrecision"
                       :display 'block
                       :margin-left (if (= :bot-left position) caret-inset 'auto)
                       :margin-right (if (= :bot-right position) caret-inset 'auto)
                       :padding 0})}
       [:polygon
        {:points "0,100 50,0 100,100"
         :style {:fill bg-color}}]])))

(defn render-bot-caret [{:keys [position] :as opts}]
  (when (get #{:top-left :top-center :top-right} position)
    (let [{:keys [caret-inset caret-base caret-altitude]} opts
          {:keys [:bg-color]}
          (th/resolve opts
            [[:bg-color ::popover-bg-color "black"]])]
      [:svg {:width caret-base
             :height caret-altitude
             :viewBox "0 0 100 100"
             :preserveAspectRatio "none"
             :style (merge
                      {:shape-rendering "geometricPrecision"
                       :display 'block
                       :margin-left (if (= :top-left position) caret-inset 'auto)
                       :margin-right (if (= :top-right position) caret-inset 'auto)
                       :padding 0})}
       [:polygon
        {:points "0,0 50,100 100,0"
         :style {:fill bg-color}}]])))

(defn render-left-caret [{:keys [position] :as opts}]
  (when (get #{:right-center :right-top :right-bot} position)
    (let [{:keys [caret-inset caret-base caret-altitude]} opts
          {:keys [:bg-color]}
          (th/resolve opts
            [[:bg-color ::popover-bg-color "black"]])]
      [:svg {:width caret-altitude
             :height caret-base
             :viewBox "0 0 100 100"
             :preserveAspectRatio "none"
             :style {:shape-rendering "geometricPrecision"
                     :display 'block
                     :margin-top (if (= :right-top position) caret-inset 'auto)
                     :margin-bottom (if (= :right-bot position) caret-inset 'auto)
                     :padding 0
                     :transform "translate3d(1px,0,0)"}}
       [:polygon
        {:points "0,50 100,100 100,0"
         :style {:fill bg-color}}]])))

(defn render-right-caret [{:keys [position] :as opts}]
  (when (get #{:left-center :left-top :left-bot} position)
    (let [{:keys [caret-inset caret-base caret-altitude]} opts
          {:keys [:bg-color]}
          (th/resolve opts
            [[:bg-color ::popover-bg-color "black"]])]
      [:svg {:width caret-altitude
             :height caret-base
             :viewBox "0 0 100 100"
             :preserveAspectRatio "none"
             :style (merge
                      {:shape-rendering "geometricPrecision"
                       :display 'block
                       :margin-top (if (= :left-top position) caret-inset 'auto)
                       :margin-bottom (if (= :left-bot position) caret-inset 'auto)
                       :padding 0
                       :transform "translate3d(-1px,0,0)"})}
       [:polygon
        {:points "0,0 0,100 100,50"
         :style {:fill bg-color}}]])))

(defn popover-content-wrapper-style [{:keys [position offset offset-right]
                                      :as opts}]
  (let [{:keys [top
                bottom
                left
                right
                tx
                ty
                h-align
                v-align]}

        (condp = position
          :top-left {:top (+ -3 offset)
                     :left 0
                     :tx "0"
                     :ty "-100%"
                     :h-align 'left}

          :top-center {:top (+ -3 offset)
                       :left "50%"
                       :tx "-50%"
                       :ty "-100%"
                       :h-align 'center}

          :top-right {:top (+ -3 offset)
                      :right 0
                      :tx "0"
                      :ty "-100%"
                      :h-align 'right}

          :bot-left {:bottom (+ -3 offset)
                     :left 0
                     :tx "0"
                     :ty "100%"
                     :h-align 'left}

          :bot-center {:bottom (+ -3 offset)
                       :left "50%"
                       :tx "-50%"
                       :ty "100%"
                       :h-align 'center}

          :bot-right {:bottom (+ -3 offset)
                      :right (+ 0 offset-right)
                      :tx "0"
                      :ty "100%"
                      :h-align 'right}

          :left-top {:left (+ -3 offset)
                     :top 0
                     :tx "-100%"
                     :ty "0"
                     :v-align 'flex-start}

          :left-center {:left (+ -3 offset)
                        :top "50%"
                        :tx "-100%"
                        :ty "-50%"
                        :v-align 'center}

          :left-bot {:left (+ -3 offset)
                     :bottom 0
                     :tx "-100%"
                     :ty "0"
                     :v-align 'flex-end}

          :right-top {:right (+ -3 offset)
                      :top 0
                      :tx "100%"
                      :ty "0"
                      :v-align 'flex-start}

          :right-center {:right (+ -3 offset)
                         :top "50%"
                         :tx "100%"
                         :ty "-50%"
                         :v-align 'center}

          :right-bot {:right (+ -3 offset)
                      :bottom 0
                      :tx "100%"
                      :ty "0"
                      :v-align 'flex-end})]
    (merge
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
        {:text-align h-align}))))

(def default-opts
  {:position :right-center
   :caret-inset 5
   :caret-base 20
   :caret-altitude 8})

(defn popover-content-style [opts]
  (let [{:keys [border-radius
                bg-color
                fg-color
                popover-vpad
                popover-hpad
                popover-pad
                box-shadow]}
        
        (th/resolve opts
          (->> theme-docs (map :rule)))

        pad-style (th/pad-padding
                    {}
                    {:vpad popover-vpad
                     :hpad popover-hpad
                     :pad popover-pad})]
    (merge
      {:flex 1
       :z-index 9999
       :border-radius border-radius
       :position 'relative
       :background-color bg-color
       :color fg-color
       ;;:overflow 'hidden
       :box-shadow box-shadow}
      pad-style
      (:popover-content-style opts))))

(defn <transition-vis-state [{:keys [!visible-state
                                     !esc-handler
                                     vis-ch
                                     !esc-enabled?]}
                             new-state]
  (go
    (when (not= new-state @!visible-state)
      (cond
        (= :shown new-state)
        (do
          (reset! !visible-state :showing)
          (<! (browser/<raf))
          (when @!esc-enabled?
            (reset! !esc-handler
              (fn [e]
                (when (= 27 (.. e -keyCode))
                  (put! vis-ch :hidden))))
            (.addEventListener js/window
              "keydown"
              @!esc-handler))
          (reset! !visible-state :shown))

        (= :hidden new-state)
        (do
          (when @!esc-enabled?
            (.removeEventListener js/window
              "keydown"
              @!esc-handler)
            (reset! !esc-handler nil))
          (reset! !visible-state :hiding)
          (<! (timeout 300))
          (reset! !visible-state :hidden))))))

(defn left-right-caret-wrapper-style [opts]
  (let [{:keys [position]} opts]
    {:display 'flex
     :flex-direction 'row
     :align-items
     (cond
       (get #{:left-top :right-top} position)
       'flex-start
       (get #{:left-bot :right-bot} position)
       'flex-end
       :else 'center)}))

(defn view [{:keys [initial-visible?
                    state
                    esc-enabled?]}]
  (let [!ui (r/atom nil)

        state (or state (create-state))

        {:keys [!ui !mouseover-timeout
                vis-ch !visible-state
                !esc-enabled?]}
        state

        cid (ks/uuid)]

    (reset! !esc-enabled? esc-enabled?)

    (put! vis-ch (if initial-visible? :shown :hidden))
    
    (go
      (loop []
        (let [new-vis-state (<! vis-ch)
              [new-vis-state done-ch]
              (if (vector? new-vis-state)
                new-vis-state
                [new-vis-state (chan)])]
          (<! (<transition-vis-state state new-vis-state))
          (close! done-ch)
          (recur))))
    
    (r/create-class
      {:reagent-render
       (fn [opts]
         (let [opts (merge
                      default-opts
                      opts)

               {:keys [position
                       width
                       border-color
                       show-on-over?
                       show-on-click?
                       over-delay
                       offset
                       offset-right
                       caret-inset
                       visible?
                       inline?
                       no-pad?
                       esc-enabled?

                       style]
                :or {position :bot-center
                     color 'white
                     offset 0
                     caret-inset 10}} opts

               {:keys [bg-color
                       fg-color
                       border-radius]}
               (th/resolve opts
                 (->> theme-docs
                      (mapv :rule)))

               visible? (= :shown @!visible-state)]

           (reset! !esc-enabled? esc-enabled?)
           
           [:div.popover-container
            (merge
              {:style (merge
                        {:position 'relative
                         :height "100%"
                         :flex 0}
                        (when inline?
                          {:display 'inline-block
                           :align-self 'flex-start})
                        style)}
              (when show-on-over?
                {:on-mouse-over
                 (fn []
                   (reset! !mouseover-timeout
                     (js/setTimeout
                       (fn []
                         (put! vis-ch :shown))
                       (or over-delay 500))))
                 :on-mouse-out
                 (fn []
                   (js/clearTimeout @!mouseover-timeout)
                   (reset! !mouseover-timeout nil)
                   (put! vis-ch :hidden))}))

            (po-render-body opts state)

            (when (get #{:shown :showing :hiding} @!visible-state)
              (when-let [popover-comp (po-render-popover opts state)]
                [:div.popover-content-wrapper
                 {:style (merge
                           {:position 'absolute
                            :opacity (if visible? 1 0)
                            :z-index 9999
                            :pointer-events (if visible? 'inherit 'none)}
                           (popover-content-wrapper-style opts)
                           (transition "opacity 0.1s ease")
                           (:popover-content-wrapper-style opts))}
                 [:div.popover-transition-wrapper
                  {:style (merge
                            {:transform (if visible?
                                          "translateY(0)"
                                          "translateY(5px)")}
                            (transition "opacity 0.1s ease, transform 0.1s ease"))}
                  (render-top-caret opts)
                  [:div.popover-left-right-caret-wrapper
                   {:style (left-right-caret-wrapper-style opts)}
                   (render-left-caret opts)
                   [:div.popover-content
                    {:on-touch-end (fn [e] (.stopPropagation e) nil)
                     :style (popover-content-style opts)}
                    popover-comp]
                   (render-right-caret opts)]
                  (render-bot-caret opts)]]))]))})))
