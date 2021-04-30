(ns rx.browser.threejs
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.styleguide :as sg]
            [reagent.core :as r]
            #_[react-three-fiber
               :refer [Canvas]]
            [clojure.core.async :as async
             :refer [go go-loop timeout <! chan sliding-buffer
                     close! alts! put!]]))

(def canvas-component
  #_(r/adapt-react-class Canvas))

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

(defn create-state []
  (let [run-ch (chan (sliding-buffer 1))
        !on-tick (atom nil)
        !running? (atom nil)
        !listeners (atom nil)
        !ref (atom nil)]
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

#_(defn bind-window-focus [{:keys [!listeners] :as state}]
  (when !listeners
    (let [on-focus (fn []
                     (start-loop state))
          on-blur (fn []
                    (stop-loop state))]
      (.addEventListener js/window "focus" on-focus)
      (.addEventListener js/window "blur" on-blur)
      (reset! !listeners
        {:on-focus on-focus
         :on-blur on-blur}))))

#_(defn unbind-window-focus [{:keys [!listeners] :as state}]
  (when !listeners
    (.removeEventListener js/window
      "focus" (:on-focus @!listeners))
    (.removeEventListener js/window
      "blur" (:on-blur @!listeners))))

(defn canvas [{:keys [state initial-paused?]}]
  (let [state (or state (create-state))
        {:keys [!run? !on-tick]} state]
    (r/create-class
      {:component-did-mount
       (fn []
         (when-not initial-paused?
           (start-loop state)))
       :component-will-unmount
       (fn []
         (stop-loop state))
       :reagent-render
       (fn [& args]
         (let [[opts & children] (ks/ensure-opts args)]
           (reset! !on-tick (:on-tick opts))
           (into
             [canvas-component
              (dissoc opts :on-tick)]
             children)))})))

(defn client-coords [e]
  (when e
    [(.-clientX e)
     (.-clientY e)]))

(defn test-comp []
  (let [!on? (r/atom false)
        !rot (r/atom [0.5 0 0])
        state (create-state)]
    (r/create-class
      {:reagent-render
       (fn []
         [canvas
          {:state state
           :on-tick (fn []
                      (swap! !rot
                        (fn [[x y z]]
                          [(+ x 0.02)
                           (+ y 0.02)
                           (+ z 0.02)])))
           :on-mouse-over (fn []
                            #_(start-loop state))
           :on-mouse-out (fn []
                           #_(stop-loop state))
           :camera {:fov 30}}
          [:ambientLight
           {:intensity 0.8}]
          [:pointLight {:position [10 10 10]}]
          [:mesh
           {:position [0 0 0]
            :scale [1 1 1]
            :rotation @!rot
            :on-click (fn []
                        (swap! !on? not))}
           [:boxBufferGeometry {:attach "geometry" :args [1 1 1]}]
           (if @!on?
             [:meshStandardMaterial
              {:attach "material"
               :color "red"}]
             [:meshStandardMaterial
              {:attach "material"
               :color "green"}])]])})))

(defn test-2-comp []
  [canvas
   {:on-tick (fn [])
    :on-mouse-over (fn [])
    :on-mouse-out (fn [])
    :camera {:fov 30}}
   [:ambientLight
    {:intensity 0.8}]
   [:pointLight {:position [10 10 10]}]
   [:mesh
    {:position [0 0 0]
     :scale [1 1 1]}
    [:boxBufferGeometry {:attach "geometry" :args [1 1 1]}]
    [:meshStandardMaterial
     {:attach "material"
      :color "green"}]]])

(defn sections []
  [sg/sections
   [sg/section
    [:h1 {:id "threejs"} "ThreeJS"]
    [:p "3D rendering provided by "
     [:a {:href "https://threejs.org/"} "ThreeJS"]
     " and "
     [:a {:href "https://github.com/react-spring/react-three-fiber"} "react-three-fiber"]
     "."]
    [sg/checkerboard
     [test-comp]
     [ui/text
      {:scale "subtext"
       :style {:color "#aaa"
               :user-select 'none}}
      "click the box"]]]])


(comment

  (.-Canvas react-three-fiber)

  (js/console.log Canvas)

  Canvas
  
  (browser/<show-component! [test-comp])

  (browser/<set-root!
    [sg/standalone {:component sections}])
  
  )
