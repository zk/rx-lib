(ns rx.browser.popper
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.core.async
             :as async
             :refer [go <! put! chan timeout close! sliding-buffer]]))

(defn create-state [{:keys [comp initial-visible?
                            pop-id]}]
  (or comp
      (let [vis-ch (chan (sliding-buffer 1))
            !body-layout (r/atom nil)]
        {:!body-layout !body-layout
         :!container-ref (atom nil)
         :!popper-ref (atom nil)
         :!transition-state (r/atom :hidden)
         :vis-ch vis-ch
         :!return-ch (atom nil)
         :!mouseover-timeout (atom nil)
         :pop-id (or pop-id (ks/uuid))
         :!disabled-interactions (r/atom #{})
         :!esc-listener (atom nil)
         :!click-listener (atom nil)
         :on-layout (fn [layout]
                      (when layout
                        (reset! !body-layout layout)))})))

(defn <transition [{:keys [vis-ch !return-ch]} action-key]
  (let [new-return-ch (chan)]
    (when vis-ch
      (when (and !return-ch @!return-ch)
        (close! @!return-ch))

      (reset! !return-ch new-return-ch)
      (put! vis-ch action-key))
    new-return-ch))

(defn <show [state]
  (<transition state :show))

(def show <show)

(defn <hide [state]
  (<transition state :hide))

(def hide <hide)

(defn <toggle [{:keys [!transition-state] :as state}]
  (if (get #{:showing :shown} @!transition-state)
    (<hide state)
    (<show state)))

(def toggle <toggle)

(defn disable-interaction [{:keys [!disabled-interactions]} k]
  (swap! !disabled-interactions
    conj k))

(defn enable-interaction [{:keys [!disabled-interactions]} k]
  (swap! !disabled-interactions
    disj k))

(defn po-render-body
  [{:keys [render-body body] :as opts} state]
  (or body
      (and render-body
           [render-body state])))

(defn calc-popper-position [opts cl pl]
  (let [{:keys [offset]} opts]
    (merge
      (->>
        (condp = (:position opts)
          :right-top
          {:top (:top cl)
           :left (+ (:left cl) (:width cl) offset)}

          :right-bottom
          {:top (- (+ (:top cl) (:height cl)) (:height pl))
           :left (+ (:left cl) (:width cl) offset)}

          :left-center
          {:top (- (+ (:top cl) (/ (:height cl) 2))
                   (/ (:height pl) 2))
           :left (- (:left cl) (:width pl) offset)}

          :left-top
          {:top (:top cl)
           :left (- (:left cl) (:width pl) offset)}

          :left-bottom
          {:top (- (+ (:top cl) (:height cl)) (:height pl))
           :left (- (:left cl) (:width pl) offset)}

          :top-left
          {:top (- (:top cl) (:height pl) offset)
           :left (:left cl)}

          :top-center
          {:top (- (:top cl) (:height pl) offset)
           :left (- (+ (:left cl) (/ (:width cl) 2))
                    (/ (:width pl) 2))}

          :top-right
          {:top (- (:top cl) (:height pl) offset)
           :left (- (+ (:left cl) (:width cl))
                    (:width pl))}

          :bottom-left
          {:top (+ (:top cl) (:height cl) offset)
           :left (:left cl)}

          :bottom-center
          {:top (+ (:top cl) (:height cl) offset)
           :left (- (+ (:left cl) (/ (:width cl) 2))
                    (/ (:width pl) 2))}

          :bottom-right
          {:top (+ (:top cl) (:height cl) offset)
           :left (- (+ (:left cl) (:width cl))
                    (:width pl))}


          ;; default right center
          {:top (- (+ (:top cl) (/ (:height cl) 2))
                   (/ (:height pl) 2))
           :left (+ (:left cl) (:width cl) offset)})
        (map (fn [[k v]]
               [k (ks/round v)]))
        (into {}))
      {:position 'absolute})))

(defn calc-popper-rect [{:keys [body-width? body-height?]} cl pl]
  (merge
    (when body-width?
      {:width (:width cl)})
    (when body-height?
      {:height (:height cl)})))

(defn popper-transition-style [{:keys [transition-style] :as opts}
                               {:keys [!transition-state]}]
  (merge
    {:transition "transform 0.15s ease, opacity 0.1s ease"
     :transform (if (get #{:showing :shown} @!transition-state)
                  "translateY(0)"
                  "translateY(3px)")
     :opacity (if (get #{:showing :shown} @!transition-state)
                1
                0)
     :pointer-events (if (get #{:showing :shown :hiding} @!transition-state)
                       'auto
                       'none)
     #_:display
     #_(if (get #{:showing :shown :hiding} @!transition-state)
         'block
         'none)}
    (when transition-style
      (transition-style @!transition-state))))

(defn po-popper-mouse-wrapper-opts [{:keys [:mouse-enabled?
                                     :show-on-mouseover?
                                     :hide-on-mouseout?

                                     :mouse-delay
                                     :mouseover-delay
                                     :mouseout-delay

                                     :click-enabled?]}
                             {:keys [vis-ch
                                     !mouseover-timeout
                                     !disabled-interactions]
                              :as state}]
  (let [show-on-mouseover? (or mouse-enabled?
                               show-on-mouseover?)
        hide-on-mouseout? (or mouse-enabled?
                              hide-on-mouseout?)
        mouseover-delay (or mouseover-delay
                            mouse-delay
                            0)
        mouseout-delay (or mouseout-delay
                           mouse-delay
                           0)]
    (merge
      (when hide-on-mouseout?
        {:on-mouse-over (fn []
                          (js/clearTimeout @!mouseover-timeout))
         :on-mouse-out (fn []
                         (js/clearTimeout @!mouseover-timeout)
                         (when-not (get @!disabled-interactions :mouseout)
                           (reset! !mouseover-timeout
                             (js/setTimeout
                               (fn []
                                 (<hide state))
                               mouseout-delay))))})
      (when show-on-mouseover?
        {:on-mouse-over (fn []
                          (js/clearTimeout @!mouseover-timeout)
                          (when-not (get @!disabled-interactions :mouseover)
                            (reset! !mouseover-timeout
                              (js/setTimeout
                                (fn []
                                  (<show state))
                                mouseover-delay))))}))))

(defn po-render-popper [_]
  (let [!layout (r/atom nil)]
    (r/create-class
      (-> {:on-layout
           (fn [layout]
             (reset! !layout layout))
           :reagent-render
           (fn [_ {:keys [render-popper popper position popper-style] :as opts} state]
             (when (or popper render-popper)
               (let [{:keys [!body-layout
                             !popper-ref]} state
                     cl @!body-layout
                     pl @!layout]
                 [:div
                  (merge
                    (po-popper-mouse-wrapper-opts opts state)
                    {:ref #(when % (reset! !popper-ref %))
                     :style (merge
                              (calc-popper-position opts cl pl)
                              (calc-popper-rect opts cl pl)
                              (popper-transition-style opts state)
                              popper-style)})
                  (or popper
                      (and render-popper
                           [render-popper state]))])))}
          browser/bind-layout))))

(defn update-layout [{:keys [!body-layout
                             !container-ref
                             pop-id]}
                     layout]
  (when layout
    (reset! !body-layout layout)))

(defn measure-and-update-container-layout
  [{:keys [!container-ref]
    :as state}]
  (let [layout (when @!container-ref
                 (browser/bounding-client-rect @!container-ref))]
    (when-not (empty? layout)
      (update-layout state layout))))

(declare tear-down-popper)

(defn init-transition-loop
  [{:keys [transition-duration
           tear-down-on-hide?]
    :or {transition-duration 0}}
   {:keys [vis-ch !return-ch
           !transition-state
           !container-ref
           on-layout]
    :as state}]
  (go
    (try
      (loop [action (<! vis-ch)]
        (condp = action
          :show
          (when-not (= :shown @!transition-state)
            (measure-and-update-container-layout state)
            (when @!container-ref
              (browser/add-layout-listener
                @!container-ref
                on-layout))
            (reset! !transition-state :showing)
            (<! (timeout transition-duration))
            (reset! !transition-state :shown)
            (when @!return-ch
              (close! @!return-ch)
              (reset! !return-ch nil)))
          
          :hide
          (when-not (= :hidden @!transition-state)
            (when @!container-ref
              (browser/remove-layout-listener
                @!container-ref
                on-layout))
            (reset! !transition-state :hiding)
            (<! (timeout transition-duration))
            (reset! !transition-state :hidden)
            (when tear-down-on-hide?
              (tear-down-popper state))
            (when @!return-ch
              (close! @!return-ch)
              (reset! !return-ch nil))))
        (recur (<! vis-ch)))
      (catch js/Error e
        (prn e)
        (when @!return-ch
          (put! @!return-ch
            (anom/from-err e))
          (close! @!return-ch))))))

(defn po-mouse-wrapper-opts [{:keys [:mouse-enabled?
                                     :show-on-mouseover?
                                     :hide-on-mouseout?

                                     :mouse-delay
                                     :mouseover-delay
                                     :mouseout-delay

                                     :click-enabled?]}
                             {:keys [vis-ch
                                     !mouseover-timeout
                                     !disabled-interactions]
                              :as state}]
  (let [show-on-mouseover? (or mouse-enabled? show-on-mouseover?)
        hide-on-mouseout? (or mouse-enabled? hide-on-mouseout?)
        mouseover-delay (or mouseover-delay mouse-delay 0)
        mouseout-delay (or mouseout-delay
                           mouse-delay
                           mouseover-delay
                           0)]
    (merge
      (when click-enabled?
        {:on-click (fn []
                     (js/clearTimeout @!mouseover-timeout)
                     (when-not (get @!disabled-interactions :click)
                       (<toggle state)))})
      (when hide-on-mouseout?
        {:on-mouse-over (fn []
                          (js/clearTimeout @!mouseover-timeout))
         :on-mouse-out (fn []
                         (js/clearTimeout @!mouseover-timeout)
                         (when-not (get @!disabled-interactions :mouseout)
                           (reset! !mouseover-timeout
                             (js/setTimeout
                               (fn []
                                 (<hide state))
                               mouseout-delay))))})
      (when show-on-mouseover?
        {:on-mouse-over (fn []
                          (js/clearTimeout @!mouseover-timeout)
                          (when-not (get @!disabled-interactions :mouseover)
                            (reset! !mouseover-timeout
                              (js/setTimeout
                                (fn []
                                  (<show state))
                                mouseover-delay))))}))))

(def !poppers (r/atom {}))


(comment
  
  (reset! !poppers {})

  (count @!poppers)

  )

(defn popper-container []
  (into
    [:div {:style {:position 'absolute
                   :pointer-events 'none
                   :overflow 'hidden
                   :top 0
                   :left 0
                   :right 0
                   :bottom 0}}]

    
    (->> @!poppers
         (map (fn [[id {:keys [opts state]}]]
                (when #(get #{:showing :shown :hiding} @(:!transition-state state))
                  [po-render-popper
                   {:key id}
                   opts
                   state])))
         doall)))

(defn recreate-popper-container []
  (let [existing-container (.getElementById js/document "rx-popper-container")
        _ (when existing-container
            (rdom/unmount-component-at-node existing-container)
            (.remove existing-container))
        body js/document.body
        container (js/document.createElement "div")]
    (.setAttribute container "id" "rx-popper-container")
    (.appendChild body container)
    (rdom/render
      [popper-container]
      container)))

(defn update-popper! [id comp]
  (swap! !poppers assoc id comp)
  (let [popper-container (or (.getElementById js/document "rx-popper-container")
                             (recreate-popper-container))]))

(defn remove-popper! [id]
  (swap! !poppers dissoc id))

(defn update-handle-esc [{:keys [hide-on-esc?]}
                         {:keys [!esc-listener
                                 pop-id]
                          :as state}]
  (when-not @!esc-listener
    (reset! !esc-listener
      (fn [e]
        (when (= (.-key e) "Escape")
          (<hide state)))))
  (if hide-on-esc?
    (.addEventListener js/window "keydown" @!esc-listener)
    (.removeEventListener js/window "keydown" @!esc-listener)))

(defn update-handle-click [{:keys [hide-on-click?]}
                           {:keys [!click-listener
                                   !popper-ref
                                   pop-id]
                            :as state}]
  (when-not @!click-listener
    (reset! !click-listener
      (fn [e]
        (when-not (browser/ancestor?
                    @!popper-ref
                    (.. e -target))
          (<hide state)))))
  (if hide-on-click?
    (do
      (.addEventListener js/document "click" @!click-listener)
      (.addEventListener js/document "contextmenu" @!click-listener))
    (do
      (.removeEventListener js/document "click" @!click-listener)
      (.removeEventListener js/document "contextmenu" @!click-listener))))

(defn set-up-popper [opts]
  (let [state (create-state opts)
        {:keys [!container-ref vis-ch]} state

        pop-id (:pop-id state)]
    (init-transition-loop opts state)
    (when (:initial-visible? opts)
      (put! vis-ch :show))
    (update-popper!
      pop-id {:opts opts
              :state state})
    (update-handle-esc opts state)
    (update-handle-click opts state)
    state))

(defn tear-down-popper [{:keys [pop-id] :as state}]
  (remove-popper! pop-id)
  (update-handle-esc {:hide-on-esc? false} state)
  (update-handle-click {:hide-on-click? false} state))

(defn wrap [opts]
  (let [state (set-up-popper opts)
        {:keys [!container-ref vis-ch pop-id]} state

        update-container-ref
        (fn [ref]
          (when ref (reset! !container-ref ref)))]
    
    (r/create-class
      (->
        {:component-did-update
         (browser/cdu-diff
           (fn [_ [opts]]
             (update-popper!
               pop-id
               {:opts opts
                :state state})
             (update-handle-esc opts state)
             (update-handle-click opts state)))
         :component-will-unmount
         (fn []
           (tear-down-popper state))
         :on-layout
         (partial update-layout state)
         :reagent-render
         (fn [{:keys [style]
               :as opts}]
           [:div.popper-wrapper
            (merge
              {:ref update-container-ref}
              (po-mouse-wrapper-opts opts state)
              {:style (merge
                        {:display 'flex}
                        style)})
            (po-render-body opts state)])}
        (browser/bind-comp state)))))

(defn <show-at [position opts]
  (let [opts (merge opts
               {:initial-visible? true
                :tear-down-on-hide? true})
        {:keys [vis-ch] :as state}
        (set-up-popper opts)]
    
    (update-layout state position)

    (when (:initial-visible? opts)
      (put! vis-ch :show))))

(comment

  (recreate-popper-container)

  (reset! !poppers {})

  (ks/spy @!poppers)

  (count @!poppers)

  

  )
