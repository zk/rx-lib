(ns rx.browser.styleguide-main
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.css :as css]
            [rx.browser.forms :as forms]
            [rx.browser.ui :as ui]
            [rx.browser.jot-styleguide :as jot]
            [rx.browser.box-styleguide :as box]
            [rx.browser.tabs-styleguide :as tabs]
            [rx.browser.youtube-styleguide :as yt]
            [rx.browser.fast-list-styleguide :as fl]
            [rx.browser.modal-styleguide :as modalsg]
            [rx.browser.notebook-styleguide :as nb]
            [rx.browser.forms-styleguide :as forms-styleguide]
            [rx.browser.electron-build-styleguide :as ebs]
            [rx.browser.popover-styleguide :as popover]
            [rx.browser.components-styleguide :as cmp]
            [rx.browser.buttons-styleguide :as buttons]
            [rx.browser.theme-styleguide :as theme]
            [rx.browser.icons-styleguide :as icons]
            [rx.browser.css-in-cljs-styleguide :as css-in-cljs]
            [rx.browser.frame :as frame]
            [rx.browser.threejs :as threejs]
            [rx.browser.piano :as piano]
            [rx.browser.styleguide :as sg]
            [rx.browser.feather-icons :as fi]
            [rx.browser.modal :as modal]
            [rx.browser.ui-styleguide :as uisg]
            [rx.browser.frame :as frame]
            [rx.browser.opentype :as opentype]
            [rx.browser.canvas :as canvas]

            [scrubs-bingo.entry :as scrubs-bingo]
            [goog.object :as gobj]
            [dommy.core :as dommy
             :refer-macros [by-id]]
            [reitit.core :as rc]
            [reagent.core :as r]
            [clojure.string :as str]
            [cljs.core.async
             :refer [timeout <!]
             :refer-macros [go]]))

(defn pages []
  [{:title "Intro"
    :path "/"
    :render (fn [{:keys [::browser/window-width
                         ::browser/window-height]}]
              [sg/section-container
               [sg/section-intro
                {:image
                 {:url "https://www.oldbookillustrations.com/wp-content/high-res/n-d-after-1887/switchboard-operator-768.jpg"
                  :height 250}}
                [:h1 {:id "rx-overview"} "Intro"]
                [:p "Rx is a collection of tools for building, verifying, and deploying high quality applications across a variety of platforms."]
                [:p "The repository can be found "
                 [:a {:href "https://github.com/zk/rx"} "here"]
                 "."]
                [:h2 {:id "intro-projects-built-with-rx"} "Projects Built with Rx"]
                [:ul
                 {:style {:list-style-type 'disc
                          :padding-left 20}}
                 [:li [:a {:href "https://canterapp.com"} "Canter"] " -- Scheduling and client communication app for Farriers"]
                 [:li [:a {:href "https://www.youtube.com/watch?v=-LyP0cf59ag"} "Myers Song Exploder"]
                  " -- Slice & dice youtube videos to learn your favorite songs."]
                 [:li [:a {:href "https://ftl.ai/"} "ftl.ai"]
                  " -- Stellar crypto testnet dev tool"]
                 [:li [:a {:href "https://inspo.cc"} "inspo.cc"]
                  " -- Inspiration albums from r/ffa"]
                 [:li
                  [:a {:href "https://clojuredocs.org"} "ClojureDocs"]
                  " -- Community-powered Clojure documentation and examples"]
                 [:li
                  [:a {:href "https://www.youtube.com/watch?v=YbQb_8EdfU8&feature=youtu.be"} "pair.io"]
                  " -- One-click cloud development environments for pair programming"]]]])}
   {:title "Theming"
    :path "/theming"
    :render theme/sections}
   {:title "CSS in cljs"
    :path "/css-in-cljs"
    :headings css-in-cljs/headings
    :render css-in-cljs/sections}
   {:title "Frame"
    :path "/frame"
    :headings frame/headings
    :render frame/sections}
   {:title "Buttons"
    :path "/buttons"
    :headings buttons/headings
    :render buttons/sections}
   {:title "Icons"
    :path "/icons"
    :headings icons/headings
    :render icons/sections}
   #_{:title "Components"
      :path "/components"
      :render cmp/sections}
   {:title "Popover"
    :path "/popover"
    :headings popover/headings
    :render popover/sections}
   {:title "Forms"
    :path "/forms"
    :headings forms-styleguide/headings
    :render forms-styleguide/sections}
   {:title "Canvas"
    :path "/canvas"
    :headings canvas/headings
    :render canvas/sections}
   {:title "Jot"
    :path "/jot"
    :headings jot/headings
    :render jot/sections}
   {:title "Box"
    :path "/box"
    :render box/sections}
   {:title "Tabs"
    :path "/tabs"
    :render tabs/sections}
   {:title "ThreeJS"
    :path "/threejs"
    :render threejs/sections}
   {:title "YouTube"
    :path "/youtube"
    :headings yt/headings
    :render yt/sections}
   {:title "Fast List"
    :path "/fast-list"
    :render fl/sections}
   {:title "Modal"
    :path "/modal"
    :render modalsg/sections}
   {:title "Notebook"
    :path "/notebook"
    :render nb/sections}
   {:title "Electron Build"
    :path "/electron-build"
    :render ebs/sections}
   {:title "OpenType"
    :path "/open-type"
    :headings opentype/headings
    :render opentype/sections}
   {:title "Piano"
    :path "/piano"
    :render piano/styleguide-sections}
   {:title "FDRF Bingo"
    :path "/fdrf-bingo"
    :on-click #(scrubs-bingo/<init)}])

(defn sidebar-title []
  [:div
   {:style {:font-family "'Roboto Condensed'"
            :font-weight 'bold
            :text-transform 'uppercase}}
   [:div
    {:style {:font-size 33
             :line-height "100%"
             :letter-spacing 1
             :margin-left -1}}
    "Hey ZK"]
   [:div
    {:style {:line-height "100%"}}
    "Rx Styleguide"]])

(defn browser-sidebar [{:keys [on-choose-page
                               current-page
                               on-choose-toc-id
                               pages]}]
  [:div {:style {:padding 30
                 :overflow-y 'scroll
                 :width 200}}
   [sidebar-title]
   [:div {:style {:height 10}}]
   (->> pages
        (map-indexed
          (fn [i {:keys [title path render headings on-click] :as page}]
            [:div
             {:key i
              :style {:font-size 14
                      :margin-bottom 5}}
             [:a
              {:key title
               :href "#"
               :style {:color (if (= page current-page)
                                "black"
                                "#686868")}
               :on-click (fn [_]
                           (if on-click
                             (on-click)
                             (on-choose-page page)))}
              [:strong title]]
             #_(if (= page current-page)
                 (let [toc (sg/parse-toc
                             ((:render current-page))
                             headings)]
                   (when-not (empty? toc)
                     [:div
                      {:style {:position 'relative}}
                      #_ [:div
                          {:style {:position 'absolute
                                   :top 2
                                   :left -2
                                   :transform "translate3d(-100%,0,0)"}}
                          [fi/chevron-right {:size 19}]]
                      [sg/toc
                       {:toc toc
                        :on-choose-id on-choose-toc-id}]]))
                 [:a
                  {:key title
                   :href "#"
                   :style {:color "#636363"}
                   :on-click (fn [_]
                               (if on-click
                                 (on-click)
                                 (on-choose-page page)))}
                  [:strong title]])]))
        doall)])

(defn mobile-nav-view
  [{:keys [pages
           current-page
           on-choose-toc-id
           on-choose-page
           ::modal/modal]}]
  {:render
   (fn []
     [:div
      {:style {:flex 1
               :background-color 'black
               :color 'white
               :padding 25
               :overflow-y 'scroll
               :height "100%"}}
      (->> pages
           (map-indexed
             (fn [i {:keys [title path render on-click] :as page}]
               [:div
                {:key i
                 :style {:font-size 30
                         :margin-bottom 5}}
                [:a
                 {:key title
                  :href "#"
                  :style {:color "white"}
                  :on-click (fn []
                              (if on-click
                                (on-click)
                                (on-choose-page page)))}
                 [:strong title]]]))
           doall)])})

(defn mobile-header [opts]
  [ui/group
   {:horizontal? true
    :pad 8
    :gap 8
    :on-click (fn []
                (modal/<show!
                  [mobile-nav-view opts]))
    :style {:font-family "'Roboto Condensed'"
            :font-weight 'bold
            :text-transform 'uppercase
            :align-items 'center
            :display 'flex
            :position 'fixed
            :background-color 'white
            :padding-left 10
            :padding-right 10
            :top 0
            :left 0
            :right 0
            :line-height 0}}
   [:div
    {}
    [fi/menu
     {:size 21}]]
   [:div "Hey ZK"]
   [:div "/"]
   [:div "Rx Styleguide"]])

(defn fixed-sidebar-layout [{:keys [::browser/window-width
                                    ::browser/window-height
                                    content-width
                                    sidebar-width
                                    mobile-header
                                    render-mobile-header
                                    sidebar
                                    render-sidebar
                                    content
                                    render-content
                                    render-right
                                    right-width
                                    right]
                             :as opts}]
  (let [content-width (or content-width 720)
        sidebar-width (or sidebar-width 200)
        right-width (or right-width 200)
        collapse-width (+ content-width sidebar-width right-width)
        side-gap (max 0 (/ (- window-width (+ content-width
                                              sidebar-width
                                              right-width)) 2))
        content-left (if (> window-width collapse-width)
                       (+ side-gap sidebar-width)
                       0)]
    [:div
     {:style {:flex 1
              :width "100%"}}
     
     [ui/group
      {:horizontal? true
       :style (merge
                {:margin-right side-gap}
                (when (<= window-width collapse-width)
                  {:padding-top 20}))}
      (when (> window-width collapse-width)
        [:div
         {:style {:position 'fixed
                  :top 0
                  :left side-gap
                  :width sidebar-width
                  :bottom 0
                  :overflow-y 'scroll}}
         (or sidebar
             (and render-sidebar
                  [render-sidebar opts]))])
      [:div
       {:style
        (merge
          {:flex 1
           :overflow 'hidden
           :max-width content-width
           :margin-left content-left})}
       (or content
           (and render-content
                [render-content opts]))]
      (when (> window-width collapse-width)
        [:div
         {:style {:position 'fixed
                  :top 0
                  :right side-gap
                  :width right-width
                  :bottom 0
                  :overflow-y 'scroll}}
         (or right
             (and render-right
                  [render-right opts]))])]

     (when (<= window-width collapse-width)
       (or mobile-header
           (and render-mobile-header
                [render-mobile-header opts])))]))

(defn right-sidebar [{:keys [current-page
                             on-choose-toc-key]}]
  (let [headings (:headings current-page)]
    [ui/group
     {:gap 4
      :style {:margin-top 24}}
     (->> headings
          (map sg/normalize-heading)
          (map (fn [[k el-key title]]
                 [:a
                  {:href "#"
                   :on-click (fn [e]
                               (.preventDefault e)
                               (on-choose-toc-key k))
                   :style {:color "#353535"
                           :font-size 14
                           :margin-left (condp = el-key
                                          :h2 0
                                          :h3 10
                                          0)
                           :font-weight (condp = el-key
                                          :h2 "normal"
                                          :h3 "normal"
                                          "bold")}}
                  title])))]))

(defn root [{:keys [pages]}]
  (let [router (rc/router
                 (->> pages
                      (map (fn [{:keys [path] :as page}]
                             [path page]))))

        !current-page (r/atom
                        (-> (rc/match-by-path
                              router
                              (browser/location-pathname))
                            :data))

        !main-scroll-el (atom nil)


        scroll-to-id (fn [id {:keys [instant?]}]
                       (when id
                         (let [el (.getElementById js/document id)]
                           (when el
                             (let [scrollable-parent
                                   (browser/find-scrollable-ancestor
                                     el
                                     :vertical)
                                   {:keys [top]} (dommy/bounding-client-rect el)]
                               (.scrollTo
                                 scrollable-parent
                                 (clj->js
                                   {:top (- (.-offsetTop el) 50)
                                    :left 0
                                    :behavior (if instant? "auto" "smooth")}))

                               (browser/replace-state-hash id))))))

        handle-choose-toc-key
        (fn [k opts]
          (when k
            (let [id (sg/heading-key->id k)]
              (scroll-to-id id opts))))

        on-choose-page (fn [{:keys [path] :as page}]
                         (reset! !current-page page)
                         (browser/history-push-state path)
                         (.scrollTo @!main-scroll-el (clj->js {:top 0}))
                         (modal/<hide!)
                         nil)]
    (r/create-class
      (-> {:component-did-mount
           (fn []
             (let [hash (gobj/get
                          (gobj/get
                            js/window
                            "location")
                          "hash")]
               (when hash
                 (go
                   (<! (timeout 250))
                   (scroll-to-id
                     (str/replace hash "#" "")
                     {:instant? true})))))
           :reagent-render
           (fn [{:keys [::browser/window-width] :as opts}]
             (let [content-width 720]
               [:div
                [fixed-sidebar-layout
                 (merge
                   opts
                   {:content-width content-width
                    :sidebar-width 200
                    :mobile-header
                    [mobile-header
                     {:pages pages
                      :current-page @!current-page
                      :on-choose-page on-choose-page}]
                    
                    :sidebar
                    [browser-sidebar
                     {:pages pages
                      :current-page @!current-page
                      :on-choose-page on-choose-page}]

                    :right
                    [right-sidebar
                     {:current-page @!current-page
                      :on-choose-toc-key handle-choose-toc-key}]

                    :content
                    [:div
                     {:ref #(when %
                              (reset! !main-scroll-el
                                (browser/find-scrollable-ancestor % :vertical)))
                      :style {:padding-top (if (> window-width content-width) 20 60)
                              :padding-left (if (> window-width content-width) 30 10)
                              :padding-right (if (> window-width content-width) 30 10)
                              :padding-bottom 40}}
                     (when (:render @!current-page)
                       [(:render @!current-page)])]})]
                [modal/global-component]]))}
          browser/bind-window-resize))))

(defn init []

  (css/set-style-text
    "css-reset"
    (ks/slurp-cljs
      "./resources/rx-css/rx-css-reset.css"))
  
  (css/set-style-text
    "fonts"
    (str
      "@import url('https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,400;0,500;0,900;1,500;1,700&display=swap');\n"
      "@import url('https://fonts.googleapis.com/css2?family=Roboto+Condensed:wght@400;700&display=swap');"))
  (css/set-dev-style
    (concat
      forms/css-rules))
  (browser/<show-component!
    [root {:pages (pages)}]))

#_(init)

(comment

  (init)

  (go
    (init)
    (<! (modal/<hide!))
    (modal/<show! [mobile-nav-view {:pages (pages)}]))

  )



