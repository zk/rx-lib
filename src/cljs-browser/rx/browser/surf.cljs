(ns rx.browser.surf
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.pixijs :as pj]
            [rx.browser.plotlyjs :as ply]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [clojure.core.async :as async
             :refer [go go-loop timeout <! chan sliding-buffer
                     close! alts! put!]]))

(defn beach [{:keys [x y width height]
              :as opts}]
  #_[pj/rectangle
   (merge
     {:fill 0xffff00}
     opts)])

(defn ocean [{:keys [x y width height]
              :as opts}]
  #_[pj/rectangle
   (merge
     {:fill 0x0000ff}
     opts)])

(defn environment [{:keys [width height x y]}]
  #_[pj/container
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

(defn ocean-grid [{:keys [rows cols
                          cell-width
                          cell-height]}]
  {::row-count rows
   ::col-count cols
   ::cell-width cell-width
   ::cell-height cell-height
   ::cells (into {}
             (for [r (range rows)
                   c (range cols)]
               [[r c] {}]))})


(defn ocean-depth [c r t _]
  (- c)
  #_(-
      (cond
        (> c 50)
        (+ (- c) 50)
        :else
        (- (- 50 (+ (- c) 50)) 50))
      20))

(defn depth-scaling [c r t grid]
  (let [depth (ocean-depth c r t grid)
        x (/ (- c 50) 50)]
    (+
      (* (/
           (+ (* -50 x x)
              (* x)
              50)
           50)
        4)
      1)
    1))

#_(defn freq-mult [c r t grid]
  (let [x (/ (- c 50) 50)]
    (+
      (* (/
           (+ (* -50 x x)
              (* x)
              50)
           50)
        4)
      1)))

(defn freq-mult [c r t grid]
  (let [x (/ (- c 50) 50)]
    (if (or (< c 20)
            (> c 80))
      1
      (- (+
           (* (/
                (+ (* -50 x x)
                   (* x)
                   50)
                50)
             8)
           1)
         5))))

(defn wave-fn [c r t grid]
  (let [depth (ocean-depth c r t grid)
        amplitude (+ 3 0)
        wl 20
        base-freq (/ 1 wl)
        integral-fcdc (->> (range 0 c)
                           (map (fn [c]
                                  (*
                                    (freq-mult c r t grid)
                                    base-freq
                                    0.5)))
                           (reduce +))
        
        y (+ (*
               amplitude
               (ks/sin
                 (+ (* 2 ks/PI integral-fcdc)
                    (/ (- t) 200)))))]
    y))

(defn height-to-wavelength [c r t grid]
  (let [wl 20
        freq-mult (freq-mult c r t grid)
        frequency (* (/ 1 wl) freq-mult)
        wl (/ 1 frequency)
        amp (wave-fn c r t grid)]
    (/ amp wl)))

#_(defn wave-fn-parts []
  (let [initial-amplitude 10
        initial-wavelength 14
        initial-frequency (/ 1 initial-wavelength)]
    [{:title "cos(x)"
      :amplitude
      (fn [c r t]
        (*
          initial-amplitude
          (ks/sin
            (* 2 ks/PI initial-frequency c))))
      :slope
      (fn [c r t]
        1 #_(ks/cos (* 2 ks/PI initial-frequency c)))}
     #_{:title "cos(x)"
        :amplitude (fn [c r t]
                     (+ 
                       (* 0.5 (ks/sin (* 1 (- c (/ t div)))))
                       (* 0.5 (ks/sin (/ r 8)))))
        :slope
        (fn [c r t]
          (ks/cos (- c (/ t div))))
        #_(fn [c r t]
            (+ 
              (* 0.5 (ks/cos (* 1 (- c (/ t div)))))
              (- 0.5 (* 0.5 (ks/sin (/ r 8))))))
        #_(fn [c r t]
            (+ 
              (* 0.5 (ks/cos (* 1 (- c (/ t div)))))
              (* 0.5 (ks/sin (/ r 8)))))}
     #_{:title "cos(x/2)"
        :amplitude (fn [c r t]
                     (+ 
                       (* 0.5 (ks/cos (* 0.5 (- c (/ t div)))))
                       (* 0.5 (ks/sin (/ r 8)))))}
     #_{:title "cos(x)"
        :amplitude (fn [c r t]
                     (+ 
                       (* 0.5 (ks/sin (* 0.33 (- c (/ t div)))))
                       (* 0.5 (ks/sin (/ r 8)))))}]))



(defn start-wave [{:keys [::row-count ::col-count ::cells]
                   :as grid}]
  (merge
    grid
    {::wave-fn wave-fn
     ::wave-start-ts (ks/now)}))

(defn tick-ocean [{:keys [::row-count ::col-count
                          ::cells
                          ::cell-width
                          ::cell-height
                          ::wave-fn
                          ::slope-fn
                          ::wave-start-ts]
                   :as grid}
                  t]
  (merge
    grid
    {::cells
     (into
       {}
       (for [r (range row-count)
             c (range col-count)]
         (let [cur-coord [r c]
               last-cell (get-in grid [::cells cur-coord])
               amp (wave-fn
                     (* c cell-width)
                     (* r cell-height)
                     (- t wave-start-ts)
                     grid)
               htl (height-to-wavelength
                     (* c cell-width)
                     (* r cell-height)
                     (- t wave-start-ts)
                     grid)
               cell-content
               {:amp amp
                :htl htl}]
           [cur-coord cell-content])))}))

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

(defn debug-ocean [& [{:keys [initial-grid
                              on-focus-row]
                       :as opts}]]
  (let [!grid (r/atom initial-grid)
        !focused-row (r/atom nil)
        state (create-state
                (merge
                  opts
                  {:on-tick
                   (fn []
                     (swap! !grid tick-ocean (ks/now)))}))
        on-focus-row (fn [row]
                       (reset! !focused-row row)
                       (when on-focus-row
                         (on-focus-row row)))]
    (r/create-class
      {:component-did-mount
       (fn []
         (start-loop state))
       :component-will-unmount
       (fn []
         (stop-loop state))
       :reagent-render
       (fn []
         (let [grid @!grid
               {:keys [::row-count ::col-count]} grid]
           [ui/group
            {:gap 20}
            [ui/group
             {:horizontal? true
              :style {:margin 10}}
             (->> (range col-count)
                  (map (fn [c]
                         [ui/group
                          (->> (range row-count)
                               (map (fn [r]
                                      (let [cell (get-in grid [::cells [r c]])
                                            v (* 255 (- (:htl cell) (/ 1 7)))
                                            color (str "rgb(" v "," v "," v ")")]
                                        [:div {:style (merge
                                                        {:width 10
                                                         :height 10
                                                         :border "solid black 1px"
                                                         :display 'flex
                                                         :flex-direction 'column
                                                         :justify-content 'center
                                                         :align-items 'center
                                                         :font-size 8
                                                         :background-color color}
                                                        (if (= @!focused-row r)
                                                          {:border "solid yellow 1px"}))
                                               :on-click #(when on-focus-row
                                                            (on-focus-row r))}
                                         
                                         #_[:div
                                            c "," r]
                                         #_[:div
                                            {:style {:font-size 10}}
                                            (ks/pp-str (get-in grid [::cells [r c]]))]])))
                               doall)])))]]))})))

(defn plot-2d-wave-fn [{:keys [grid]}]
  (let [{:keys [::col-count ::row-count
                ::wave-fn]} grid]
    [ui/group
     {:flex 1}
     [ui/text {:scale "title"} "Waveform"]
     [ply/plot
      {:data
       (concat
         [{:x (range 0 100 0.5)
           :y (->> (range 0 100 0.5)
                   (map (fn [x]
                          (wave-fn x 0 0))))
           :type "scatter"
           :showscale false
           :mode "lines+markers"
           :marker {:color "red"
                    :opacity 0}}])
       :layout {:showlegend false
                :height 200
                :margin {:50 0 :r 0 :t 50 :b 50}}
       :config
       {:displayModeBar false
        :responsive true
        :title "wave"}}]]))

(defn plot-2d-depth-fn [{:keys [grid]}]
  (let [{:keys [::col-count ::row-count]} grid]
    [ui/group
     {:flex 1}
     [ui/text {:scale "title"} "Depth"]
     [ply/plot
      {:data
       (concat
         [{:x (range 0 100 0.5)
           :y (->> (range 0 100 0.5)
                   (map (fn [x]
                          (ocean-depth x 0 0 grid))))
           :type "scatter"
           :showscale false
           :mode "lines+markers"
           :marker {:color "red"
                    :opacity 0}}])
       :layout {:showlegend false
                :height 200
                :margin {:50 0 :r 0 :t 50 :b 50}}
       :config
       {:displayModeBar false
        :responsive true
        :title "wave"}}]]))

(defn plot-2d-depth-scaling [{:keys [grid]}]
  (let [{:keys [::col-count ::row-count]} grid]
    [ui/group
     {:flex 1}
     [ui/text {:scale "title"} "Freq Mult"]
     [ply/plot
      {:data
       (concat
         [{:x (range 0 100 0.5)
           :y (->> (range 0 100 0.5)
                   (map (fn [x]
                          (freq-mult x 0 0 grid))))
           :type "scatter"
           :showscale false
           :mode "lines+markers"
           :marker {:color "red"
                    :opacity 0}}])
       :layout {:showlegend false
                :height 200
                :margin {:50 0 :r 0 :t 50 :b 50}}
       :config
       {:displayModeBar false
        :responsive true
        :title "wave"}}]]))

(defn plot-2d-htw [{:keys [grid]}]
  (let [{:keys [::col-count ::row-count]} grid]
    [ui/group
     {:flex 1}
     [ui/text {:scale "title"} "Height to Wavelength"]
     [ply/plot
      {:data
       (concat
         [{:x (range 0 100 0.5)
           :y (->> (range 0 100 0.5)
                   (map (fn [x]
                          (height-to-wavelength x 0 0 grid))))
           :type "scatter"
           :showscale false
           :mode "lines+markers"
           :marker {:color "red"
                    :opacity 0}}
          {:x (range 0 100 0.5)
           :y (->> (range 0 100 0.5)
                   (map (fn [_] (/ 1 7))))
           :type "scatter"
           :showscale false
           :mode "lines+markers"
           :marker {:color "black"
                    :opacity 0}}])
       :layout {:showlegend false
                :height 200
                :margin {:50 0 :r 0 :t 50 :b 50}}
       :config
       {:displayModeBar false
        :responsive true
        :title "wave"}}]]))

(defn test-stuff2 []
  (let [grid (-> (ocean-grid {:rows 5
                              :cols 100
                              :cell-width 1
                              :cell-height 1})
                 start-wave
                 (tick-ocean (ks/now)))]
    [ui/group
     {:horizontal? false}
     [ui/group
      {:flex 1}
      [debug-ocean
       {:initial-grid grid}]
      #_[ui/group
         {:gap 20
          :flex 1
          :horizontal? true}
         [ui/group
          {:flex 1
           :style {:padding 10}}
          [plot-wave-fn {:grid grid}]]]]
     [ui/group
      {:flex 1
       :style {:padding 10}
       :gap 12}
      
      [plot-2d-depth-fn {:grid grid}]
      [plot-2d-depth-scaling {:grid grid}]
      [plot-2d-htw {:grid grid}]
      [plot-2d-wave-fn {:grid grid}]]]))


(comment

  (browser/<show-component!
    [test-stuff2])

  (browser/<show-component!
    [:div "HI"])

  
  

  )




