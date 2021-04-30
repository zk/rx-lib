(ns rx.browser.tabs
  (:require [rx.kitchen-sink :as ks]
            [rx.view :as view]
            [rx.browser.components :as cmp]
            [rx.theme :as th]
            [rx.icons.feather :as feather]
            [rx.browser.buttons :as btn]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]))

(defn <init-state [{:keys [!ids-order
                           !id->real-route
                           !sel-id]}
                   {:keys [initial-routes]}]
  (go
    (let [real-routes
          (->> initial-routes
               (map view/realize-route))]

      (swap! !ids-order
        into
        (->> real-routes
             (map view/id)
             vec))

      (swap! !id->real-route
        merge
        (->> real-routes
             (map (fn [m]
                    [(view/id m) m]))
             (into {})))
      
      (reset! !sel-id (or (view/id (first real-routes)) @!sel-id)))))

(defn create-state [& {:keys [initial-routes]}]
  {:!ids-order (r/atom [])
   :!id->real-route (r/atom nil)
   :!sel-id (r/atom nil)
   :instance-id (ks/uuid)})

(defn open [{:keys [!ids-order
                    !id->real-route
                    !sel-id
                    instance-id]
             :as tabs-state}
            route]

  (let [real-route (view/realize-route route)]
    (swap! !ids-order conj (view/id real-route))
    (swap! !id->real-route
      assoc
      (view/id real-route)
      real-route)
    (reset! !sel-id (view/id real-route))))

(defn close-tab [{:keys [!ids-order
                         !sel-id]}
                 realized-route]

  (swap! !ids-order
    (fn [ids-order]
      (->> ids-order
           (remove #(= % (view/id realized-route)))
           vec)))
  (when (= @!sel-id (view/id realized-route))
    (reset! !sel-id (first @!ids-order)))
  (when-let [f (->> realized-route
                    :rx.view/spec
                    :tab-did-close)]
    (f (:rx.view/opts realized-route))))

(defn focus-tab [{:keys [!sel-id]} real-route]
  (reset! !sel-id (view/id real-route)))

(defn tabs-in-order [{:keys [!ids-order
                             !id->real-route]}]
  (->> @!ids-order
       (mapv (fn [id]
               (get @!id->real-route id)))))

(defn focus-tab-index [state index]
  (focus-tab state (get (tabs-in-order state) index)))

(defn close-current-tab [state]
  (close-tab state (get @(:!id->real-route state) @(:!sel-id state))))

(defn tabs-render-separator [{:keys [render-separator] :as opts}]
  (when render-separator
    [render-separator opts]))

(defn render-default-tab [{:keys [title
                                  render-title
                                  tab-width
                                  style-tab
                                  style-selected-tab
                                  style-tab-title
                                  style-selected-tab-title
                                  selected?
                                  on-choose
                                  on-close
                                  vertical?
                                  closeable?] :as opts}]
  [:div.rx-browser-tabs-tab
   {:style (merge
             {:cursor 'pointer
              :display 'flex
              :flex-shrink 1
              :flex-direction 'row
              :align-items 'center
              :overflow 'hidden
              :padding "4px 10px"
              :user-select 'none}
             (if closeable?
               {:padding "4px 8px 4px 10px"})
             (if vertical?
               {:border-left "solid transparent 2px"}
               {:border-bottom "solid transparent 2px"})
             (when selected?
               (if vertical?
                 {:border-left "solid transparent 2px"}
                 {:border-bottom "solid black 2px"}))
             (when tab-width
               {:width tab-width})
             style-tab
             (when selected?
               style-selected-tab))
    :key (ks/uuid)
    :on-click on-choose}
   [:div
    {:style {:display 'flex
             :flex-shrink 1
             :flex-grow 1
             :overflow 'hidden
             :white-space 'nowrap
             :text-overflow 'ellipsis}}
      
    (cond
      render-title [render-title opts]
      title [ui/text
             {:scale "subtext"
              :style (merge
                       {:text-shadow
                        (if selected?
                          "0 0 .2px black, 0 0 .2px black"
                          'none)
                        :white-space 'nowrap
                        :overflow 'hidden
                        :text-overflow 'ellipsis
                        :flex 1}
                       style-tab-title
                       (when selected?
                         style-selected-tab-title))}
             title]
      :else (throw
              (ex-info "Missing tab render fn on view spec"
                opts)))]
   (when closeable?
     [:div {:style {:width 2}}])
   (when closeable?
     [btn/bare
      {:icon {:set "feather"
              :name "x"
              :color "#555"
              :size 14}
       :style {:padding 0
               :border 'none
               :align-self 'center}
       :on-click on-close}])])

(defn tabs-render-tab [{:keys [render-tab] :as opts}]
  (if render-tab
    [render-tab opts]
    [render-default-tab opts]))

(defn render-default-content-separator [{:keys [vertical?]}]
  [:div.rx-browser-tabs-separator
   {:style (merge
             {:background-color "#ccc"}
             (if vertical?
               {:width 1
                :height "100%"}
               {:height 1
                :width "100%"}))}])

(defn tabs-render-content-separator [{:keys [render-content-separator]
                                      :as opts}]
  (if render-content-separator
    [render-content-separator opts]
    [render-default-content-separator opts]))

(defn view [{:keys [on-comp] :as opts}]
  (let [{:keys [!sel-id
                !ids-order
                !id->real-route]
         :as state}
        (or (:state opts)
            (create-state opts))

        on-choose-tab (fn [realized-route]
                        (when (get
                                (set @!ids-order)
                                (view/id realized-route))
                          (when (not= (view/id realized-route)
                                      @!sel-id)
                            (when @!sel-id
                              (when-let [f (->> @!sel-id
                                                (get @!id->real-route)
                                                :rx.view/spec
                                                :tab-did-blur)]
                                (f (:rx.view/opts realized-route))))
                            (reset! !sel-id
                              (view/id realized-route))
                            (when-let [f (->> realized-route
                                              :rx.view/spec
                                              :tab-did-focus)]
                              (f (:rx.view/opts realized-route))))))
        on-close-tab (fn [realized-route]
                       (close-tab state realized-route))]

    (<init-state state opts)

    (when on-comp
      (on-comp state))

    (r/create-class
      {:reagent-render
       (fn [{:keys [vertical?
                    tabs-closeable?
                    style
                    theme-disabled?
                    tab-width]
             :as opts}]
         [:div.rx-browser-tabs-view
          {:style (merge
                    {:flex 1
                     :display 'flex
                     :flex-direction 'column
                     :width "100%"
                     :height "100%"}
                    (when vertical?
                      {:flex-direction 'row})
                    style)}

          (into
            [ui/group
             (merge
               {:class "rx-browser-tabs-tabs-container"
                :style {:min-width tab-width}}
               (when-not vertical?
                 {:horizontal? true}))]
            (->> @!ids-order
                 (map #(get @!id->real-route %))
                 (map-indexed
                   (fn [i rr]
                     (merge
                       opts
                       {:title (-> rr ::view/spec ::title)
                        :render-title (or (-> rr ::view/spec ::render)
                                          (-> rr ::view/spec ::render-title))
                        :selected? (= (view/id rr) @!sel-id)
                        :closeable? (if (not (nil? (::closeable? (::view/spec rr))))
                                      (::closeable? (::view/spec rr))
                                      tabs-closeable?)
                        :on-choose (partial on-choose-tab rr)
                        :on-close (partial on-close-tab rr)
                        :width-pct (/ 1 (count @!ids-order))
                        :first? (= 0 i)
                        :last? (= (max (dec (count @!ids-order)) 0)
                                  i)})))
                 (map (fn [opts]
                        [tabs-render-tab opts]))
                 (interpose [tabs-render-separator opts])
                 doall))
          [tabs-render-content-separator opts]
          [:div.rx-browser-tabs-content
           {:style {:flex 1
                    :position 'relative
                    :overflow 'hidden
                    :display 'flex
                    :height "100%"
                    :width "100%"}}
           (->> @!ids-order
                (map #(get @!id->real-route %))
                (map (fn [rr]
                       (let [selected? (= (view/id rr) @!sel-id)]
                         [:div
                          {:key (view/id rr)
                           :style (merge
                                    {:position 'absolute
                                     :top 0
                                     :left 0
                                     :right 0
                                     :bottom 0
                                     :overflow 'hidden
                                     :transform (str "translate3d("
                                                     (if selected?
                                                       "0"
                                                       "-10000%")
                                                     ","
                                                     (if selected?
                                                       "0"
                                                       "-10000%")
                                                     ",0)")}
                                    (when selected?
                                      {:position 'relative
                                       :top nil
                                       :bottom nil
                                       :left nil
                                       :right nil
                                       :width "100%"
                                       :height "100%"})
                                    #_(when (and content-space (not bare?))
                                        {:padding content-space}))}
                          (view/render-realized-route rr)])))
                doall)]])})))
