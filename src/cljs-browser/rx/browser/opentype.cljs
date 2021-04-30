(ns rx.browser.opentype
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.canvas :as canvas]
            [rx.annie :as annie]
            [rx.browser.forms :as forms]
            [rx.browser.ui :as ui]
            [rx.browser.buttons :as btn]
            [rx.browser.styleguide :as sg]
            [reagent.core :as r]
            #_["opentype.js" :as opentype]
            [clojure.core.async :as async
             :refer [go <! chan put! close! timeout sliding-buffer alts!]]))

(defn <load [url]
  (let [ch (chan)]
    #_(opentype/load
        url
        (fn [err font]
          (when (or err font)
            (put! ch (or err font)))
          (close! ch)))
    ch))

(defn command-obj->clj [co]
  (merge
    {:type (.-type co)}
    (when (.-x co)
      {:x (.-x co)})
    (when (.-y co)
      {:y (.-y co)})))

(defn bounding-box-obj->clj [bbo]
  {:x1 (.-x1 bbo)
   :y1 (.-y1 bbo)
   :x2 (.-x2 bbo)
   :y2 (.-y2 bbo)})

(defn path-obj->clj [po]
  {:stroke (.-stroke po)
   :stroke-width (.-strokeWidth po)
   :fill (.-fill po)
   :box (bounding-box-obj->clj (.getBoundingBox po))
   :commands (->> (.-commands po)
                  (mapv command-obj->clj))})

(defn opentype-example []
  (let [!scale (r/atom 0.5)
        !font (r/atom nil)]

    (go
      (reset! !font (<! (<load "https://p197.p4.n0.cdn.getcloudapp.com/items/xQuYPKqd/RobotoMono-VariableFont_wght.ttf?v=9e06bf8e4155ad3a942a9ff38f59fbc4"))))
    
    (r/create-class
      {:reagent-render
       (fn []
         (sg/example
           {:form
            [ui/group
             {:gap 8}
             [canvas/canvas
              {:style {:width "100%"
                       :height 200}
               :render (fn [{:keys [::canvas/ctx
                                    ::canvas/width
                                    ::canvas/height] :as c}]
                         (when @!font
                           (let [font @!font
                                 text "Hey ZK"
                                 x 0
                                 y (* height 0.8)
                                 font-size (+ (* height @!scale) 50)]
                             (.clearRect ctx 0 0 1000 1000)
                             (.draw font ctx text x y font-size)
                             (.drawMetrics font ctx text x y font-size)
                             (.drawPoints font ctx text x y font-size))))}]
             [ui/group
              {:horizontal? true
               :gap 8}
              [btn/bare
               {:icon {:set "feather"
                       :name "chevrons-left"}
                :on-click
                (fn []
                  (annie/spring
                    (atom nil)
                    @!scale
                    0
                    (fn [v]
                      (reset! !scale v))))}]
              
              [forms/range
               {:min 0
                :max 1
                :step 0.001
                :!val !scale
                :style {:flex 1}}]
              [btn/bare
               {:icon {:set "feather"
                       :name "chevrons-right"}
                :on-click
                (fn []
                  (annie/spring
                    (atom nil)
                    @!scale
                    1
                    (fn [v]
                      (reset! !scale v))))}]]]}))})))


(defn test-component []
  (browser/<show-component!
    [:div {:style {:display 'flex
                   :flex 1
                   :align-items 'center
                   :justify-content 'center}}
     #_[canvas]]))

(def headings
  [[::opentype :h1 "OpenType"]
   [::example :h2 "Example"]])

(defn sections []
  [sg/section-container
   [sg/section-intro
    {}
    [sg/heading headings ::opentype]
    [:p
     "Bindings and helpers to "
     [:a {:href "https://github.com/opentypejs/opentype.js"}
      "OpenType.js"]
     ", which helps in creating, measuring, transforming, and displaying OpenType fonts."]]
   [sg/section-intro
    [sg/heading headings ::example]
    [opentype-example]]])

(comment

  (go
    (def font (<! (<load "https://p197.p4.n0.cdn.getcloudapp.com/items/xQuYPKqd/RobotoMono-VariableFont_wght.ttf?v=9e06bf8e4155ad3a942a9ff38f59fbc4"))))

  (ks/clog font)

  (ks/clog (.getPath font "z" 0 0))

  (ks/pp (path-obj->clj (.getPath font "e" 0 0)))

  (test-component)

  (browser/<set-root!
    [sg/standalone
     {:component sections
      :headings headings}])

  

  )

