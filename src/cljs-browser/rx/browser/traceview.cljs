(ns rx.browser.traceview
  (:require [reagent.core :as r]
            [rx.kitchen-sink :as ks]
            [rx.browser.components :as cmp]
            [rx.browser.tabs :as tabs]
            [rx.theme :as th]
            [rx.view :as view]
            [rx.trace :as trace]
            [rx.browser.logview :as lv]
            [rx.browser.fast-list :as fl]
            #_[rx.log-ingress :as ingress]
            [clojure.set :as set]
            [dommy.core :as dommy]
            [cljs.core.async :as async
             :refer [chan <! timeout close! put!]
             :refer-macros [go]]))

(defn render-frame [_ {:keys [:rx.trace/frame-id]}]
  (let [!run? (atom true)
        !count (r/atom 0)]
    (r/create-class
      {:component-did-mount
       (fn []
         (go
           (loop []
             (<! (timeout 1000))
             (swap! !count inc)
             (when @!run?
               (recur)))))
       :component-will-unmount
       (fn []
         (reset! !run? false))
       :reagent-render
       (fn 
         [{:keys [on-choose-frame
                  !detail-frame
                  offset-ts
                  last-item?] :as opts}
          {:keys [:rx.trace/var
                  :rx.trace/title
                  :rx.trace/created-ts]
           :as frame}]

         (let [{:keys [::selected-bg-color]}
               (th/des opts
                 [[::selected-bg-color :color/bg-2]])]
           [:div
            {:on-click (fn [e]
                         (.preventDefault e)
                         (on-choose-frame frame)
                         nil)
             :style (merge
                      {:cursor 'pointer
                       :display 'flex
                       :flex-direction 'row
                       :justify-content 'space-between
                       :font-family "monospace"
                       :font-size 14
                       :padding (th/pad opts 1)}
                      (when (= @!detail-frame frame)
                        {:background-color selected-bg-color}))}
            (if (or title var)
              [cmp/interpose-children
               {:separator [:div {:style {:width 10}}]
                :style {:display 'flex
                        :flex-direction 'row
                        :justify-content 'flex-start
                        :overflow 'hidden
                        :flex-wrap 'nowrap
                        :align-items 'center}}
               (->> [(when title
                       [:div
                        {:style {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis
                                 :flex 1}}
                        title])
                     (when var
                       [:div
                        {:style {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis
                                 :flex 1
                                 :flex-shrink 1}}
                        (:ns var) "/" (:name var)])]
                    (remove nil?))]
              [:div
               {:style {:white-space 'nowrap
                        :overflow 'hidden
                        :text-overflow 'ellipsis}}
               (pr-str frame)])
            

            [:div {:style {:width (th/pad opts 1)}}]
            [:div
             {:style {:white-space 'nowrap
                      :flex-shrink 0}}
             (when @!count nil)
             (ks/date-format
               created-ts
               "hh:mm:ss.SSS aa")
             " / "
             (if last-item?
               (let [delta (- (ks/now) created-ts)]
                 (if (< delta (* 1000 60))
                   (str (ks/round (/ delta 1000)) "s")
                   (ks/ms-delta-desc-short delta)))
               (let [delta (- offset-ts created-ts)]
                 (if (< delta (* 1000 60))
                   (str "+" (ks/round (/ delta 1000)) "s")
                   (ks/ms-delta-desc-short delta))))]]))})))

(defn trace-list [{:keys [initial-frames
                          control]
                   :as opts}]
  (let [{:keys [::fg-color
                ::bg-color
                ::separator-color
                ::selected-bg-color]}
        (th/des opts
          [[::fg-color :color/fg-1]
           [::bg-color :color/bg-0]
           [::separator-color :list/separator-color]
           [::selected-bg-color :color/bg-2]])]
    [fl/view
     {:control control
      :initial-items initial-frames
      :style {:background-color bg-color
              :flex 1
              :color fg-color}
      :render-item (fn [frame-opts frame]
                     [render-frame
                      (merge
                        opts
                        frame-opts)
                      frame])}]))

(defn frame-detail [{:keys [frame
                            on-choose-frame]
                     :as opts}]
  (let [{:keys [::fg-color
                ::bg-color
                ::separator-color
                ::secondary-bg-color]}
        (th/des opts
          [[::fg-color :color/fg-1]
           [::bg-color :color/bg-0]
           [::secondary-bg-color :color/bg-1]
           [::separator-color :list/separator-color]])]
    {:render
     (fn []
       [:div
        {:style {:background-color bg-color
                 :color fg-color
                 :flex 1
                 :display 'flex
                 :flex-direction 'column}}
        [:div
         {:style {:padding (th/pad opts 1)
                  :overflow 'scroll
                  :flex 1}}
         [lv/render-clj frame nil]]
        [:div
         [cmp/list-separator opts]
         [:div
          {:style {:padding (th/pad opts 1)
                   :background-color secondary-bg-color}}
          [cmp/$button
           {:title "Copy"
            :style {:display 'inline}
            :on-action (fn []
                         (cmp/copy-to-clipboard
                           (ks/pp-str frame)))}]]]])}))

(defn frame-match-query? [frame {:keys [:rx.trace/match-group-regex]}]
  (if match-group-regex
    (re-find (re-pattern match-group-regex)
      (:rx.trace/group-id frame))
    true))

(defn side-by-side [{:keys [initial-frames
                            initial-detail-frame
                            :rx.trace/group-id
                            :rx.trace/match-group-regex]
                     :as opts}]

  (let [{:keys [::fg-color
                ::bg-color
                ::separator-color]}
        (th/des opts
          [[::fg-color :color/fg-1]
           [::bg-color "green"]
           [::separator-color :list/separator-color]])


        list-ctl (fl/create-state
                   {:id-key :rx.trace/frame-id
                    :sort-key :rx.trace/created-ts})

        !detail-frame (r/atom nil)

        !offset-ts (r/atom (ks/now))

        on-log (fn [le]
                 (let [frame (trace/log-entry->trace le)]
                   (when (>
                           (:rx.trace/created-ts frame)
                           @!offset-ts)
                     (reset! !offset-ts (:rx.trace/created-ts frame)))
                   (when-not @!detail-frame
                     (reset! !detail-frame frame))
                   (when (frame-match-query? frame opts)
                     (fl/append-items list-ctl [frame]))))]

    {:render
     (fn []
       (r/create-class
         {:component-did-mount
          (fn []
            (when initial-frames
              (reset! !offset-ts (->> initial-frames
                                      last
                                      :rx.trace/created-ts))
              (fl/append-items list-ctl initial-frames))
            #_(ingress/listen! on-log))
          :component-will-unmount
          (fn []
            #_(ingress/unlisten! on-log))
          :reagent-render
          (fn []
            [:div
             {:style {:display 'flex
                      :flex-direction 'row
                      :flex 1
                      :overflow-x 'hidden
                      :background-color 'red}}
             [:div
              {:style {:flex 1
                       :width "50%"
                       :display 'flex
                       :overflow 'hidden}}
              [trace-list
               (merge
                 opts
                 {:control list-ctl
                  :!detail-frame !detail-frame
                  :offset-ts @!offset-ts
                  :on-choose-frame
                  (fn [frame]
                    (reset! !detail-frame frame))})]]
             
             [:div {:style
                    {:width 1
                     :background-color separator-color}}]
             
             [:div
              {:style {:flex 1
                       :width "50%"
                       :display 'flex
                       :overflow 'hidden}}
              (view/render-route
                [frame-detail
                 (merge
                   opts
                   {:frame @!detail-frame})])]])}))

     :rx.tabs/render
     (fn []
       [:div
        (or group-id
            match-group-regex
            "All")])}))

(defn trace-tabs [opts]
  (let [!rendered-group-ids (atom #{})
        tab-control (tabs/create-state)]

    {:render
     (fn []
       (r/create-class
         {:component-did-mount
          (fn []
            (swap! !rendered-group-ids
              conj
              (:rx.trace/group-id (first (:initial-frames opts)))))
          :reagent-render
          (fn []
            [tabs/view
             (merge
               {:control tab-control
                :vertical? true
                :tab-width 100}
               {:initial-routes
                (concat
                  [[side-by-side opts]]
                  (->> opts
                       :match-group-regexs
                       (map (fn [s]
                              [side-by-side
                               (merge
                                 opts
                                 {:rx.trace/match-group-regex s})]))))})])}))
     :rx.tabs/render (fn []
                       [:div "trace tabs"])}))
