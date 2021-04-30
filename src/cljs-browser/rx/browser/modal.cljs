(ns rx.browser.modal
  (:require [rx.kitchen-sink :as ks]
            [rx.view2 :as view]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [rx.theme :as th]
            [rx.browser.keybindings :as kb]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [dommy.core :as dommy]
            [goog.object :as gobj]
            [cljs.core.async :as async
             :refer [<! close! put! chan timeout]
             :refer-macros [go go-loop]]))

(defn <hide [{:keys [vis-ch
                     !realized-route
                     !result-ch
                     keybindings-id]}
             & [result]]
  (go
    (kb/remove-bindings! keybindings-id)
    (put! vis-ch false)
    (<! (timeout 250))
    (reset! !realized-route nil)
    (when @!result-ch
      (when result
        (put! @!result-ch result))
      (close! @!result-ch))))

(defn <show [{:keys [vis-ch
                     !realized-route
                     !result-ch
                     keybindings-id]
              :as state}
             route
             & [show-opts]]

  (when-not state
    (ks/throw-str "Modal state is nil"))

  (let [ch (chan)]
    
    (go
      (try
        (when @!result-ch
          (close! @!result-ch))
        (reset! !result-ch ch)
        (reset! !realized-route
          (<? (view/<realize-route route
                {::modal state})))

        (kb/add-bindings!
          keybindings-id
          (concat
            [{:key-name "Escape"
              :handler (fn []
                         (<hide state))}]
            (::kb/bindings @!realized-route)))

        (put! vis-ch true)
        (catch js/Error e
          (put! ch (anom/from-err e))
          (close! ch))))
    ch))

(defn create-state []
  {:!frame-visible? (r/atom nil)
   :!bg-visible? (r/atom nil)
   :!fg-visible? (r/atom nil)
   :!fg-tr-visible? (r/atom nil)
   :!realized-route (r/atom nil)
   :!result-ch (atom nil)
   :keybindings-id (str "modal-keybindings-" (ks/uuid))
   :vis-ch (chan)})

(def ctl create-state)

(def theme-rules
  [{:rule [:bg-color "rgba(0,0,0,0.8)"]
    :doc "Modal background color"}])

(defn realized-route-wrapper [realized-route opts]
  (let [!orig-focus-el (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn []
         (when-let [el (gobj/get js/document "activeElement")]
           (.blur el)
           (reset! !orig-focus-el el)))
       :reagent-render
       (fn []
         (view/render-view
           realized-route
           :render
           opts))})))

(defn modal-content-style [modal-component-opts
                           {:keys [!fg-visible?
                                   !fg-tr-visible?
                                   !realized-route]}]

  (let [{:keys [::fill-window?]} @!realized-route]

    (merge
      {:display 'flex
       :flex 1
       :z-index 1000
       :opacity (if @!fg-visible? 1 0)
       :transform
       (str "translate3d(0,"
            (if @!fg-tr-visible? 0 15) "px"
            ",0)")
       :transition "opacity 100ms ease, transform 100ms ease"}
      (when-not fill-window?
        {:align-items 'center
         :justify-content 'center}))))

(defn component [{:keys [state
                         with-state]
                  :as opts}]

  (let [{:keys [!frame-visible?
                !bg-visible?
                !fg-visible?
                !fg-tr-visible?
                !realized-route
                keybindings-id
                vis-ch]
         :as state}
        (or state (create-state))

        _ (when with-state
            (with-state state))

        cdu-ch (chan)

        !wrapper (atom nil)]

    (go-loop []
      (let [visible? (<! vis-ch)]
        (if visible?
          (do
            (reset! !frame-visible? true)
            (<! (timeout 17))
            (reset! !bg-visible? true)
            (reset! !fg-visible? true)
            (reset! !fg-tr-visible? true))
          (do
            (reset! !bg-visible? false)
            (reset! !fg-visible? false)
            (<! (timeout 150))
            (reset! !frame-visible? false)
            (reset! !fg-tr-visible? false)))
        (recur)))
    
    [(r/create-class
       {:component-did-mount
        (fn [])
        :component-will-unmount
        (fn [])
        :component-did-update
        (fn []
          #_(put! cdu-ch :update))
        :reagent-render
        (fn [{:keys [el-id] :as opts}]
          (let [rr @!realized-route
                {:keys [:bg-color
                        :bg-opacity]}
                (th/des opts theme-rules)

                {:keys [::fill-window?]} rr]
            [:div
             {:id el-id}
             (when @!frame-visible?
               [:div.modal-container
                {:style {:position 'fixed
                         :display 'flex
                         :z-index 999
                         :top 0
                         :left 0
                         :right 0
                         :bottom 0}}
                (when-not fill-window?
                  [:div.modal-background
                   {:style {:position 'absolute
                            :background-color bg-color
                            :top 0
                            :left 0
                            :right 0
                            :bottom 0
                            :z-index 999
                            :opacity (if @!bg-visible? 1 0)
                            :transition (str "opacity "
                                             150
                                             "ms ease")}}])
                [:div.modal-content
                 {:ref #(when % (reset! !wrapper %))
                  :on-click (fn [e]
                              #_(.stopPropagation e)
                              (when (= @!wrapper (.. e -target))
                                (<hide state))
                              nil)
                  :on-mouse-down
                  (fn [e]
                    #_(.stopPropagation e)
                    nil)
                  :on-mouse-up
                  (fn [e]
                    #_(.stopPropagation e)
                    nil)
                  :style (modal-content-style opts state)}
                 (when @!realized-route
                   [realized-route-wrapper @!realized-route {::state state}])]])]))})
     opts]))


(defonce default-state (create-state))

(defn global-component [opts]
  [component (merge
               {:el-id "rx-modal-global-component"
                :state default-state}
               opts)])

(defn <ensure-global-component []
  (let [ch (chan)
        el (.getElementById js/document "rx-modal-global-component")]
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

(<defn <show! [route & [opts]]
  (<! (<ensure-global-component))
  (<! (<show default-state route opts)))

(defn <hide! [& [result]]
  (<hide default-state result))

(defn <confirm [{:keys [title]}]
  (go
    (<! (<show!
          [(fn []
             {:render
              (fn []
                [:div
                 {:style {:flex 1
                          :display 'flex
                          :align-items 'center
                          :justify-content 'center}}
                 [ui/group
                  {:pad 20
                   :style {:background-color 'white}}
                  "HI"]])})]))))

(comment

  (<confirm
    {:title "HI"})

  )
