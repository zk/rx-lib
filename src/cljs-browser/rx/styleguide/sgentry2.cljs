(ns rx.styleguide.sgentry2
  (:require [clojure.string :as str]
            [rx.kitchen-sink :as ks]
            [cljfmt.core :as cf]
            [rx.css :as css]
            [rx.browser :as browser]
            [rx.browser.bnav :as bnav]
            [rx.browser.ui :as rui]
            [rx.viewreg :as vr]
            [rx.styleguide.common :as com]
            [rx.styleguide.sgui :as ui]
            [rx.styleguide.sgcomponents :as components]
            [rx.styleguide.zoomloader :as zoomloader]
            [rx.styleguide.experiments :as exp]
            [rx.styleguide.guitar :as guitar]
            [rx.styleguide.datascript :as datascript]
            [reagent.core :as r]
            [dommy.core :as dommy]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def experiments
  [{:key :zoomloader
    :title "Zoom Loader"
    :href "/sg/zoom-loader"
    :view zoomloader/$view}

   #_{:key :yt
      :title "YT"
      :href "/sg/yt"
      :route-paths [["/sg/yt"]
                    [["/sg/yt/" :video-id]]]
      :view exp/$yt}

   {:key :datascript
    :title "Datascript"
    :href "/sg/datascript"
    :route-paths [["/sg/datascript"]]
    :view datascript/$view}

   {:key :guitar
    :title "Guitar"
    :href "/sg/guitar"
    :route-paths [["/sg/guitar"]]
    :view guitar/$view}])

(defn $experiments-nav []
  [:div.mg-bot-lg
   [:h3.sg-header
    "Experiments "
    [:span.sg-num
     ".03"]]
   [:ul.bare
    [:li [:a {:href "/sg/zoomloader"} "Zoom Loader"]]]])

(def navs
  [{:title "Introduction"
    :href "/"
    :route-key ::root}
   ui/nav
   components/nav
   {:title "Experiments"
    :links experiments}])

(defn $navbar [{:keys [on-route]}]
  [:div.sg-navbar
   (->> navs
        (map-indexed
          (fn [i {:keys [title href links route-key]}]
            [:div.mg-bot-lg
             {:key title}
             [:a (merge
                   (when href
                     {:href href})
                   (when route-key
                     {:on-click (fn [e]
                                  (.preventDefault e)
                                  (on-route [route-key])
                                  nil)}))
              [:h3.sg-header
               title
               " "
               [:span.sg-num
                (ks/sformat "%02d" i) "."]]]
             (when links
               [:ul.bare
                (->> links
                     (map (fn [{:keys [title href route]}]
                            [:li
                             {:key title}
                             [:a
                              (merge
                                (when href
                                  {:href "#"})
                                (when route
                                  {:on-click (fn [e]
                                               (.preventDefault e)
                                               (on-route route)
                                               nil)}))
                              title]])))])])))])

(defn $sg-content []
  [:div
   [:div.container
    [:div.row
     [:div.col-sm-12]]
    [:div.row
     [:div.col-sm-12
      [:div.pad-md
       [ui/$content]
       [components/$content]]]]]])

(defn $page [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    [:div.full-size
     [:div.mag-left
      [:div.mag-sidebar
       [:div.sg-sidebar
        [:div.pad-sm
         [:div.mg-bot-lg
          [:h1.mg-none
           {:style (merge
                     {:font-size 36
                      :font-weight 'bold
                      :font-family css/impact-font
                      :letter-spacing 1
                      :text-transform 'uppercase})}
           "Hey ZK"]
          [:h2.mg-none
           {:style {:font-size 17.5
                    :font-weight 'bold
                    :font-family css/impact-font
                    :text-transform 'uppercase}}
           "Rx Styleguide"]]
         [$navbar]]]]
      (into
        [:div.mag-content]
        children)]

     [:div.mobile-navbar.visible-xs
      {:style {:position 'fixed
               :top css/mdpx
               :right css/mdpx}}
      #_[mn/$hamburger-menu]]

     #_[mn/$container
        {:style {:height "80%"}}
        [:div.full-size.flex-column
         {:style {:background-color 'black}}
         [:div.flex-right.pad-md
          [:a {:href "#"
               :style {:color 'white}
               :on-click (fn [e]
                           (.preventDefault e)
                           (mn/hide)
                           nil)}
           [:div.flex-center
            {:style {:width 24
                     :height 24}}
            [:i.ion-close
             {:style {:font-size 24}}]]]]
         [:div.scroll-y.pad-md
          {:style {:flex 1}}
          (->> navs
               (map-indexed
                 (fn [i {:keys [title href links]}]
                   [:div.mg-bot-lg
                    {:key title}
                    [:a (when href {:href href})
                     [:h3.sg-header
                      {:style {:color 'white}}
                      title
                      " "
                      [:span.sg-num
                       (ks/sformat "%02d" i) "."]]]
                    (when links
                      [:ul.bare
                       (->> links
                            (map (fn [{:keys [title href]}]
                                   [:li
                                    {:key title}
                                    [:a.pad-sm
                                     {:href href
                                      :on-click (fn [e]
                                                  (mn/hide)
                                                  nil)
                                      :style {:display 'block
                                              :color 'white}} title]])))])])))]]]]))

(defonce !route (r/atom [::root]))


(defonce !scroller (atom nil))

#_(prn @!scroller)


(defn scroll-to-target-id [root-el target-id]
  (when (and root-el target-id)
    (let [el (dommy/sel1 root-el (str "#" target-id))]
      (when el
        (.scrollIntoView el #js {:behavior "auto"})))))

#_(scroll-to @!scroller "units")

#_(.scrollTo
    @!scroller
    #js {:top 10000
         :left 0
         :behavior "smooth"})

(defonce !scroll-offsets (atom {}))

(defn $render-route [{:keys [route]}]
  (let [!route (r/atom route)
        !scroller (atom nil)
        handle-update-route
        (fn [or nr]
          (go
            (when (not= or nr)
              (reset! !route nr)
              (<! (timeout 17))
              (let [[_ {:keys [section-id]}] nr]
                (if section-id
                  (scroll-to-target-id
                    @!scroller
                    section-id)
                  (when @!scroller
                    (.scrollTo
                      @!scroller
                      #js {:top 0 :behavior "auto"})))))))

        on-scroll (fn [e]
                    (swap! !scroll-offsets
                      assoc
                      route
                      (.. e -target -scrollTop)))]
    (r/create-class
      {:component-did-mount
       (fn []
         (set! (.-scrollTop @!scroller)
           (or (get @!scroll-offsets route) 0))
         (dommy/listen! @!scroller :scroll on-scroll))
       :component-will-unmount
       (fn []
         (dommy/unlisten! @!scroller :scroll on-scroll))
       :component-did-update
       (browser/cdu-diff
         (fn [[{or :route}] [{nr :route}]]
           (handle-update-route or nr)))
       :reagent-render
       (fn [{:keys [route]}]
         [:div
          {:ref #(reset! !scroller %)
           :style {:width "100%"
                   :height "100%"
                   :position 'absolute
                   :top 0
                   :left 0
                   :overflow-y 'scroll
                   :overflow-x 'hidden}}
          (vr/render-route
            @vr/!registry
            @!route
            nil)])})))

(defn $standalone [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    [:div.full-size
     [:div.mag-left
      [:div.mag-sidebar
       [:div.sg-sidebar
        [:div.pad-sm
         [:div.mg-bot-lg
          [:h1.mg-none
           {:style (merge
                     {:font-size 36
                      :font-weight 'bold
                      :font-family css/impact-font
                      :letter-spacing 1
                      :text-transform 'uppercase})}
           "Hey ZK"]
          [:h2.mg-none
           {:style {:font-size 17.5
                    :font-weight 'bold
                    :font-family css/impact-font
                    :text-transform 'uppercase}}
           "Rx Styleguide"]]
         [$navbar
          {:on-route #(reset! !route %)}]]]]
      [:div.mag-content
       {:style {:background-color 'white}}
       [$render-route {:route @!route}]]]

     [:div.mobile-navbar.visible-xs
      {:style {:position 'fixed
               :top css/mdpx
               :right css/mdpx}}
      #_[mn/$hamburger-menu]]

     #_[mn/$container
        {:style {:height "80%"}}
        [:div.full-size.flex-column
         {:style {:background-color 'black}}
         [:div.flex-right.pad-md
          [:a {:href "#"
               :style {:color 'white}
               :on-click (fn [e]
                           (.preventDefault e)
                           (mn/hide)
                           nil)}
           [:div.flex-center
            {:style {:width 24
                     :height 24}}
            [:i.ion-close
             {:style {:font-size 24}}]]]]
         [:div.scroll-y.pad-md
          {:style {:flex 1}}
          (->> navs
               (map-indexed
                 (fn [i {:keys [title href links]}]
                   [:div.mg-bot-lg
                    {:key title}
                    [:a (when href {:href href})
                     [:h3.sg-header
                      {:style {:color 'white}}
                      title
                      " "
                      [:span.sg-num
                       (ks/sformat "%02d" i) "."]]]
                    (when links
                      [:ul.bare
                       (->> links
                            (map (fn [{:keys [title href]}]
                                   [:li
                                    {:key title}
                                    [:a.pad-sm
                                     {:href href
                                      :on-click (fn [e]
                                                  (mn/hide)
                                                  nil)
                                      :style {:display 'block
                                              :color 'white}} title]])))])])))]]]]))

(defn views []
  (merge
    {::root
     (fn []
       {:render
        (fn []
          [rui/group
           {:flex 1
            :align-items 'center
            :justify-content 'center}])})
     :rx.styleguide.sgui/route
     (fn []
       {:render (fn []
                  [ui/$content])})
     :rx.styleguide.sgcomponents/route
     (fn []
       {:render (fn []
                  [components/$content])})}
    (components/views)))

(defn handle-root [route-params !state]
  [$page
   [rui/group
    {:align-items 'center
     :justify-content 'center}
    [:h1.text-center.bold
     {:style {:font-size 32
              :font-weight 'bold
              :letter-spacing 1
              :color 'black
              :line-height "140%"}}
     "Hi"]]])

(defn handle-ui [route-params !state]
  [$page
   [ui/$content]])

(defn handle-components [route-params !state]
  [$page
   [components/$content]])

(defn init [env]
  (let [!state (r/atom env)]
    (browser/dispatch-current-location
      {:mount-selector :#rx-root-mount-point
       :routes ["" (vec
                     (concat
                       [["/" :root]
                        ["/sg" :root]
                        ["/sg/ui" :ui]
                        ["/sg/components" :components]]

                       (->> experiments
                            (mapcat
                              (fn [{:keys [key route-paths view href]}]
                                (if route-paths
                                  (->> route-paths
                                       (map (fn [route-path]
                                              (vec
                                                (concat
                                                  route-path
                                                  [key]))))
                                       vec)
                                  [[href key]]))))

                       [[true :not-found]]))]
       :comps (merge
                {:root handle-root
                 :ui handle-ui
                 :components handle-components
                 :not-found (fn [route-params !state]
                              [:div {:style {:padding 40}}
                               [:h1 "Soft 404"]])}
                (->> experiments
                     (map (fn [{:keys [key view handler]}]
                            [key view]))
                     (into {})))
       :comp-opts {:!state !state}})))


(comment

  (init)

  )
