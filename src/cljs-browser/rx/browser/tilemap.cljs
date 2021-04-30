(ns rx.browser.tilemap
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.tilemap :as tm]
            [dommy.core :as dommy]
            [reagent.core :as r]
            [cljs.core.async :as async
             :refer [chan put! <! close!]
             :refer-macros [go go-loop]]))

(defn render-layer [{:keys [::tm/type]}]
  [:rect
   {:width 1
    :height 1
    :fill (condp = type
            :ocean "blue"
            :beach "yellow")}])

(defn tilemap-display []
  (let [!canvas (atom nil)
        !opts (atom nil)
        set-ref #(when % (reset! !canvas %))
        internal-on-drag
        (fn [{:keys [sx sy ex ey lx ly]}]
          (when (:on-drag @!opts)
            (let [{canvas-width :width
                   canvas-height :height}
                  (dommy/bounding-client-rect @!canvas)

                  {:keys [map-width map-height on-drag]} @!opts

                  sx-pct (/ sx canvas-width)
                  sy-pct (/ sy canvas-height)

                  ex-pct (/ ex canvas-width)
                  ey-pct (/ ey canvas-height)

                  lx-pct (/ lx canvas-width)
                  ly-pct (/ ly canvas-height)

                  sx (ks/floor (* sx-pct map-width))
                  sy (ks/floor (* sy-pct map-height))

                  ex (ks/floor (* ex-pct map-width))
                  ey (ks/floor (* ey-pct map-height))

                  lx (ks/floor (* lx-pct map-width))
                  ly (ks/floor (* ly-pct map-height))]

              (on-drag {:sx sx :sy sy
                        :ex ex :ey ey
                        :lx lx :ly ly}))))
        
        internal-on-move
        (fn [{:keys [x y]}]
          (when (:on-move @!opts)
            (let [{canvas-width :width
                   canvas-height :height}
                  (dommy/bounding-client-rect @!canvas)

                  {:keys [map-width map-height on-move]} @!opts

                  x-pct (/ x canvas-width)
                  y-pct (/ y canvas-height)

                  xm (ks/floor (* x-pct map-width))
                  ym (ks/floor (* y-pct map-height))]
              (on-move
                {:x xm :y ym}))))

        internal-on-click
        (fn [e]
          (when (:on-click @!opts)
            (let [x (.-clientX e)
                  y (.-clientY e)
                  {canvas-width :width
                   canvas-height :height}
                  (dommy/bounding-client-rect @!canvas)

                  {:keys [map-width map-height]} @!opts

                  x-pct (/ x canvas-width)
                  y-pct (/ y canvas-height)

                  xm (ks/floor (* x-pct map-width))
                  ym (ks/floor (* y-pct map-height))]
              ((:on-click @!opts)
               {:x xm :y ym}))))]
    
    (r/create-class
      {:reagent-render
       (fn [{:keys [tilemap
                    map-width
                    map-height]
             :or {map-width 100
                  map-height 100}
             :as opts}]
         (reset! !opts opts)
         (let [{:keys [::tm/coord->block
                       ::tm/highlight]}
               tilemap]
           
           [:svg
            (merge
              {:ref set-ref
               :style {:width 500
                       :height 500}
               :viewBox (str "0 0 " map-width " " map-height)}
              (browser/drag-handler
                {:on-drag internal-on-drag
                 :on-move internal-on-move
                 :on-click internal-on-click}))
            [:g
             (->> coord->block
                  (map (fn [[coord layers]]
                         [:g
                          {:key (str coord)}
                          (->> layers
                               (map-indexed
                                 (fn [i layer]
                                   [:g {:key i
                                        :transform
                                        (str
                                          "translate("
                                          (first coord)
                                          ","
                                          (second coord)
                                          ")")}
                                    (render-layer layer)])))])))]
            [:g
             (->> highlight
                  ::tm/coords
                  (map-indexed
                    (fn [i coord]
                      [:g {:key i
                           :transform
                           (str
                             "translate("
                             (first coord)
                             ","
                             (second coord)
                             ")")}
                       [:rect
                        {:width 1
                         :height 1
                         :stroke "black"
                         :fill "rgba(0,0,0,0.1)"
                         :stroke-width 0
                         :stroke-location "inside"}]])))]]))})))

(defn root []
  (let [!tilemap
        (r/atom {::tm/coord->block
                 {[0 0] [{::layer-index 0
                          ::tm/type :ocean}]
                  [1 0] [{::tm/layer-index 0
                          ::tm/type :beach}]}
                            
                 ::tm/highlight {::tm/coords [[0 0]]}})]
    (r/create-class
      {:reagent-render
       (fn []
         [tilemap-display
          {:tilemap @!tilemap
           :map-width 112
           :map-height 112
           :on-move (fn [{:keys [x y]}]
                      (swap! !tilemap
                        assoc
                        ::tm/highlight
                        {::tm/coords [[x y]]}))
           :on-click (fn [{:keys [x y]}]
                       (prn x y)
                       (swap! !tilemap
                         assoc-in
                         [::tm/coord->block [x y]]
                         [{::tm/type :beach}]))

           :on-drag (fn [{:keys [lx ly ex ey]}]
                      (let [coords
                            (if (and (not= ex lx)
                                     (not= ey ly))
                              (let [slope (/ (- ey ly) (- ex lx))
                                    intercept (/
                                                (- (* ex ly)
                                                   (* lx ey))
                                                (- ex lx))]
                                (->> (range lx (+ lx (inc (- ex lx))))
                                     (map (fn [x]
                                            [x (ks/round (+ (* slope x) intercept))]))))
                              [[ex ey]])]

                        (swap! !tilemap
                          update
                          ::tm/coord->block
                          merge
                          (->> coords
                                (map (fn [coord]
                                       [coord [{::tm/type :beach}]]))
                                (into {})))))}])})))

(range 0)
(defn init []
  (go
    (let []
      (browser/<show-component! [root]))))

(comment

  (init)
  
  )
