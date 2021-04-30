(ns rx.browser.sidebar
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [rx.icons.feather :as feather]
            [rx.viewreg :as vr]
            [garden.units :as u]
            [garden.color :as co]
            [garden.core :as garden]
            [garden.stylesheet :as gs]
            [rx.browser.components :as bc]
            [reagent.core :as r]
            [dommy.core :as dommy]
            [rx.browser.bnav :as bnav]
            [cljs.reader :as reader]
            [cljs.core.async :as async
             :refer [<! >! chan close!
                     sliding-buffer put! take!
                     alts! timeout pipe mult tap]
             :refer-macros [go go-loop]]))

(defonce !tabs (r/atom []))

(defn gen-control []
  {:!tabs (r/atom nil)})

(defn <open-tab [{:keys [!tabs]} route]
  (let [control (bnav/gen-control)
        [view-spec opts]
        (vr/route->view-spec
          @vr/!registry
          route
          nil)]
    (swap! !tabs conj
      (merge
        view-spec
        {:key (keyword (gensym))
         :title (or (:title view-spec) "no title")
         :subtitle (:subtitle view-spec)
         :closable? true
         :on-close (fn [{:keys [key]}]
                     (swap! !tabs
                       (fn [tabs]
                         (->> tabs
                              (remove
                                #(= key (:key %)))
                              vec))))
         :render (fn []
                   [bnav/$container
                    {:control control
                     :initial-routes [route]}])}))))

(defn $component [& args]
  (let [[{:keys [initial-key
                 initial-tabs
                 tab-style
                 selected-tab-style
                 control
                 nav]}
         & [tab-specs]]
        (ks/ensure-opts args)
        !sel-key (r/atom (or
                           initial-key
                           (-> nav
                               first
                               :key)))
        !on-change (atom nil)
        show-tab (fn [k]
                   (let [last-sel-key @!sel-key]
                     (reset! !sel-key k)
                     (when @!on-change
                       (@!on-change k last-sel-key))))
        control (or control (gen-control))
        {:keys [!tabs]} control

        render-tab (fn [{:keys [key
                                title
                                subtitle
                                closable?
                                on-close]
                         :as tab-spec}]
                     (let [sel? (= @!sel-key key)]
                       [:div.cursor-pointer.hover-lighten-bg-01
                        {:key key
                         :style {:flex-direction 'row
                                 :display 'flex
                                 :align-items 'center
                                 :justify-content 'space-between}
                         :on-click (fn [e]
                                     (.preventDefault e)
                                     (show-tab key)
                                     nil)}
                        [:div
                         {:style (merge
                                   {:display 'flex
                                    :flex-direction 'column
                                    :overflow 'hidden
                                    :color "#aaa"
                                    :padding 5}
                                   {:border-left
                                    (str
                                      "solid "
                                      (if sel?
                                        "#333"
                                        "transparent")
                                      " 5px")}
                                   tab-style
                                   (when sel?
                                     (merge
                                       {:color "white"}
                                       selected-tab-style))
                                   (when closable?
                                     {:font-size 12}))}
                         [:div
                          {:style (merge
                                    {:cursor 'pointer
                                     :text-overflow 'ellipsis
                                     :overflow 'hidden}
                                    (when closable?
                                      {:font-size 12}))}
                          title]
                         (when subtitle
                           [:div
                            {:style {:font-size 10}}
                            subtitle])]

                        (when closable?
                          [:div.hover-lighten-bg-02.cursor-pointer
                           {:style {:display 'flex
                                    :align-items 'center
                                    :justify-content 'center
                                    :margin-right 5}
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (on-close tab-spec)
                                        nil)}
                           [feather/x
                            {:size 16
                             :color "rgba(255,255,255,0.5)"}]])]))]
    (fn [& args]
      (let [[{:keys [tab-style
                     tab-container-style
                     selected-tab-style
                     content-container-style
                     transition
                     on-change
                     style
                     render-opts
                     control
                     nav
                     tabs]}]
            (ks/ensure-opts args)

            sel-key @!sel-key
            sel-tab-spec (or (->> tab-specs
                                  (filter #(= sel-key (:key %)))
                                  first)
                             (first tab-specs))

            {:keys [render]} sel-tab-spec

            tab-specs (concat nav @!tabs)]
        (when on-change (reset! !on-change on-change))
        [:div.wrap
         {:style (merge
                   {:display 'flex
                    :flex-direction 'row
                    :flex 1
                    :overflow 'hidden}
                   style)}
         [:div
          {:style (merge
                    {:display 'flex
                     :flex-direction 'column
                     :flex-shrink 0
                     :flex-grow 0}
                    tab-container-style)}
          [:div {:style {:height 5}}]
          [:div
           [:div
            {:style {:padding 5
                     :padding-left 10
                     :font-size 13
                     :color "rgba(255,255,255,0.3)"
                     :font-weight 'bold
                     :text-transform 'uppercase}}
            "Nav"]
           (->> nav
                (map render-tab)
                doall)]

          (when (> (count @!tabs) 0)
            [:div
             {:style {:margin-top 10}}
             [:div
              {:style {:padding 5
                       :padding-left 10
                       :font-size 13
                       :color "rgba(255,255,255,0.3)"
                       :font-weight 'bold
                       :text-transform 'uppercase}}
              "Tabs"]
             (->> @!tabs
                  (map render-tab)
                  doall)])]

         [:div
          {:style (merge
                    {:flex-grow 1
                     :position 'relative
                     :flex 1}
                    content-container-style)}
          [:div
           {:style {:position 'absolute
                    :top 0
                    :left 0
                    :right 0
                    :bottom 0
                    :display 'flex}}
           [bc/$cstack
            {:view @!sel-key
             :initial-view initial-key
             :transition transition}
            tab-specs]]]]))))
