(ns rx.browser.popbar
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [rx.kitchen-sink :as ks]
            [rx.view :as view]
            #_[nsfw.css :as nc]
            #_[nsfw.page :as page]
            [dommy.core :as dommy]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))




(defn cdu-diff [f]
  (fn [this [_ & old-args]]
    (let [new-args (rest (r/argv this))]
      (f old-args new-args))))

(defn inject-args [view args]
  (if args
    (if (vector? view)
      (let [[el & tail] view]
        (if (map? (first tail))
          (vec
            (concat
              [el (merge (first tail) args)]
              (rest tail)))
          view))
      view)
    view))

(defn $container [& args]
  (let [[{:keys [visible?
                 on-visible-change
                 !state
                 container-id]}]
        (ks/ensure-opts args)]
    (let [!ui (r/atom {:anim-state (if visible?
                                     :post-in
                                     :post-out)})
          on-keydown (fn [e]
                       (when (= 27 (.. e -keyCode))
                         (swap! !state assoc :visible? false)))]
      (r/create-class
        {:component-did-mount
         (fn [_]
           (dommy/listen!
             js/window
             :keydown
             on-keydown)
           (add-watch !state :anims
             (fn [_ _ {ov? :visible?} {nv? :visible?}]
               (when (not= ov? nv?)
                 (when on-visible-change
                   (on-visible-change nv?))
                 (if nv?
                   (go
                     (swap! !ui assoc :anim-state :pre-in)
                     (<! (timeout 17))
                     (swap! !ui assoc :anim-state :post-in))
                   (go
                     (swap! !ui assoc :anim-state :pre-out)
                     (<! (timeout 300))
                     (swap! !ui assoc :anim-state :post-out)))))))
         :component-will-unmount
         (fn [_]
           (dommy/unlisten!
             js/window
             :keydown
             on-keydown)
           (remove-watch !state :anims))
         :reagent-render
         (fn [& args]
           (let [[{:keys [style
                          stick-to]
                   :or {stick-to :bot}}
                  views]
                 (ks/ensure-opts args)

                 {:keys [anim-state]} @!ui
                 view-lookup (ks/lookup-map :key views)

                 realized-route (:realized-route @!state)

                 {:keys [::stick-to]
                  :or {stick-to :bot}}
                 (view/realized-route-spec realized-route)

                 visible? (get @!state :visible?)

                 wrap-style
                 (condp = stick-to
                   :bot {:left 0
                         :right 0
                         :bottom 0
                         :transform
                         (str "translate3d(0,"
                              (if visible? 0 15)
                              "px,0)")}
                   
                   :right {:top 0
                           :right 0
                           :bottom 0
                           :transform
                           (str "translate3d("
                                (if visible? 0 15)
                                "px,0,0)")}

                   :left {:top 0
                          :left 0
                          :bottom 0
                          :transform
                          (str "translate3d("
                               (if visible? 0 -15)
                               "px,0,0)")}
                   
                   :top {:left 0
                         :right 0
                         :top 0
                         :transform
                         (str "translate3d(0,"
                              (if visible? 0 -15)
                              "px,0)")})]

             [:div.popbar-container
              {:id container-id
               :style (merge
                        {:position 'fixed
                         :opacity (if visible? 1 0)
                         :pointer-events (if visible? "auto" "none")
                         :display 'flex
                         :z-index 1000}
                        wrap-style
                        {:transition  "opacity 0.15s ease, transform 0.15s ease"}
                        style)
               :on-scroll (fn [e]
                            (.stopPropagation e))}
              (view/render-realized-route realized-route)]))}))))

(defonce !default (r/atom nil))

(defn global-component [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    (vec
      (concat
        [$container
         (merge
           {:!state !default
            :container-id "popbar-global-component"}
           opts)]
        children))))

(defn <ensure-global-component []
  (let [ch (chan)
        el (.getElementById js/document "popbar-global-component")]
    (if el
      (close! ch)
      (let [parent (.createElement js/document "div")
            body (.-body js/document)]
        (.appendChild body parent)
        (rdom/render
          [global-component]
          parent
          (fn []
            (close! ch)))))
    ch))

(defn gen-show [!state]
  (fn [route]
    (go
      (<! (<ensure-global-component))
      (if (:visible? @!state)
        (do
          (swap! !state assoc :visible? false)
          (<! (timeout 250))
          (swap! !state assoc
            :visible? true
            :realized-route (view/realize-route route {::popbar !state})))
        (swap! !state assoc
          :visible? true
          :realized-route (view/realize-route route)))
      (<! (timeout 250)))))

(defn gen-hide [!state]
  (fn []
    (go
      (swap! !state assoc
        :visible? false)
      (do
        (<! (timeout 300))
        (swap! !state assoc
          :realized-route nil)))))

(defn gen-visible? [!state]
  (fn []
    (:visible? @!state)))

(def <show! (gen-show !default))

(def <hide! (gen-hide !default))

(def visible? (gen-visible? !default))
