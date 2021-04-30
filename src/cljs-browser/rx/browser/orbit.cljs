(ns rx.browser.orbit
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as b]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            #_[pixijs :as pjs]))


;; body: mass scalar, velocity vec, acceleration vec

;; {:x 10 :y 10 :z 10}
;; {:x 20 :y 20 :z 20}

{:mass 20
 :radius 20
 :vel [10 10 10]
 :acc [0 0 0]}

(def PIXI nil #_ js/PIXI)

(defn draw [canvas-el {:keys [width height]}]
  (let [app (PIXI.Application.
              #js {:view canvas-el
                   :antialias false
                   :width width
                   :height height})
        stage (.-stage app)
        graphics (PIXI.Graphics.)]

    (.addChild stage graphics)

    (.lineStyle graphics 5 0x00ff00)
    (.beginFill graphics 0x00ff00)
    (.drawRect graphics 200 200 1 1)))

(defn canvas [{:keys [tick :as opts]}]
  (let [!dims (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [canvas-el (rdom/dom-node this)
               rect-obj (.getBoundingClientRect canvas-el)]
           (reset! !dims
             {:width (.-width rect-obj)
              :height (.-height rect-obj)
              :x (.-x rect-obj)
              :y (.-y rect-obj)})
           
           (draw canvas-el @!dims)))
       :reagent-render
       (fn []
         [:canvas
          (merge-with ks/deep-merge
            {:style {:background-color 'blue
                     :flex 1}}
            opts)])})))

(comment

  (b/<show-component!
    [:div {:style {:width "100%"
                   :height "100%"
                   :background-color 'red
                   :display 'flex}}
     [canvas]])

  

  )


