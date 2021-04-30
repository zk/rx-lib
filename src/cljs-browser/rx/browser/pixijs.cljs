(ns rx.browser.pixijs
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.styleguide :as sg]
            [reagent.core :as r]
            #_["pixi.js" :as pixi
               :refer [Graphics]]
            #_["@inlet/react-pixi" :as react-pixi
               :refer [Stage Sprite Text PixiComponent Container]]
            [clojure.core.async :as async
             :refer [go go-loop timeout <! chan sliding-buffer
                     close! alts! put!]]))

(def stage-component
  nil #_(r/adapt-react-class Stage))
(def sprite
  nil #_(r/adapt-react-class Sprite))
(def text
  nil #_(r/adapt-react-class Text))
(def container
  nil #_(r/adapt-react-class Container))

(def Rectangle
  nil
  #_(PixiComponent
    "Rectangle"
    (clj->js
      {:create (fn [props] (Graphics.))
       :applyProps (fn [instance oldProps newProps]
                     (.clear instance)
                     (.beginFill instance (.-fill newProps))
                     (.drawRect instance
                       (.-x newProps)
                       (.-y newProps)
                       (.-width newProps)
                       (.-height newProps))
                     (.endFill instance))})))

(def rectangle
  nil
  #_(r/adapt-react-class Rectangle))


(defn <raf []
  (let [ch (chan)]
    (.requestAnimationFrame
      js/window
      (fn []
        (close! ch)))
    ch))

(defn running? [{:keys [!running?]}]
  (when !running? @!running?))

(defn start-loop [{:keys [run-ch]}]
  (put! run-ch true))

(defn stop-loop [{:keys [run-ch]}]
  (put! run-ch false))

(defn toggle-loop [{:keys [run-ch] :as state}]
  (put! run-ch (not (running? state))))

(defn create-state [& [opts]]
  (let [run-ch (chan (sliding-buffer 1))
        !on-tick (atom (:on-tick opts))
        !running? (atom false)
        !listeners (atom nil)
        !ref (atom nil)]

    (when (:loop? opts)
      (put! run-ch true))
    
    (go-loop []

      (loop []
        (let [run? (<! run-ch)]
          (reset! !running? run?)
          (when-not run? (recur))))

      (loop []
        (let [[v ch] (alts! [run-ch (<raf)])]
          (when (or (not= ch run-ch)
                    (and (= ch run-ch) v))
            (when @!on-tick
              (@!on-tick))
            (recur))))

      (recur))
    {:run-ch run-ch
     :!running? !running?
     :!on-tick !on-tick}))

(defn set-on-tick [{:keys [!on-tick]} f]
  (reset! !on-tick f))

(defn stage [& args]
  (let [[opts & children] (ks/ensure-opts args)
        children (ks/unwrap-children children)

        {:keys [state on-state]} opts
        !state (atom (or state (create-state opts)))]

    (when on-state
      (on-state @!state))

    (r/create-class
      {:component-will-unmount
       (fn []
         (stop-loop @!state))
       :reagent-render
       (fn [opts]
         (into
           [stage-component
            (dissoc opts
              :loop?)]
           children))})))

(defn beach [{:keys [x y width height]
              :as opts}]
  [rectangle
   (merge
     {:fill 0xffff00}
     opts)])

(defn ocean [{:keys [x y width height]
              :as opts}]
  [rectangle
   (merge
     {:fill 0x0000ff}
     opts)])

(defn environment [{:keys [width height x y]}]
  [container
   {:x x
    :y y}
   [ocean
    {:x 0
     :y 0
     :width (* 0.75 width)
     :height height}]
   [beach
    {:x (* 0.75 width)
     :y 0
     :width (* 0.25 width)
     :height height}]])



(defn test-stuff []
  (browser/<show-component!
    [stage
     {:on-tick (fn [])
      :loop? false}
     [environment
      {:x 100
       :y 100
       :width 500
       :height 200}]
     ]))


(comment

  (ocean-grid {:rows 10 :cols 5})

  

  (test-stuff)
  

  (js/console.log pixi)

  )
