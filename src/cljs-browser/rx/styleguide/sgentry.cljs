(ns rx.styleguide.sgentry
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            #_[nsfw.mobile-nav :as mn]
            [cljfmt.core :as cf]
            [rx.css :as css]
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

   {:key :yt
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
    :href "/"}
   ui/nav
   components/nav
   {:title "Experiments"
    :links experiments}])

(defn $navbar []
  [:div.sg-navbar
   (->> navs
        (map-indexed
         (fn [i {:keys [title href links]}]
           [:div.mg-bot-lg
            {:key title}
            [:a (when href {:href href})
             [:h3.sg-header
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
                            [:a {:href href} title]])))])])))])

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


(defn $standalone []
  [:div
   {:style {:flex 1
            :display 'flex}
    :on-click (fn [e]
                (prn "click" )
                (.log js/console (.-target e))
                (.preventDefault e))}
   [$page {}]])

(defn handle-root [route-params !state]
  [$page
   [:div.pad-md
    {:id "intro"
     :style (merge
              {:width "100%"
               :height "100%"
               :position 'absolute
               :color 'white
               :top 0
               :left 0}
              #_css/flex-vcenter)}
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
    #_(page/dispatch-current-path
        {:routes
         ["" (vec
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

         :actions
         (concat
           [{:key :root
             :view handle-root
             :attach-to :#cljs-entry}

            {:key :ui
             :view handle-ui
             :attach-to :#cljs-entry}

            {:key :components
             :view handle-components
             :attach-to :#cljs-entry}]
           (->> experiments
                (map (fn [{:keys [key view handler]}]
                       {:key key
                        :view view
                        :attach-to :#cljs-entry
                        :handler handler})))
           [{:key :not-found
             :view (fn [route-params !state]
                     [:div {:style {:padding 40}}
                      [:h1 "Soft 404"]])
             :attach-to :#cljs-entry}])
         :context !state})))
