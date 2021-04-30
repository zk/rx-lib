(ns rx.browser.piano
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [rx.anom :as anom
             :refer-macros [go-anom <?]]
            #_[nsfw.util :as nu]
            [rx.browser :as browser]
            [rx.browser.tonejs :as tonejs]
            [rx.browser.buttons :as btn]
            [rx.browser.popbar :as popbar]
            [rx.browser.keybindings :as kb]
            [rx.browser.styleguide :as sg]
            [rx.browser.ui :as ui]
            [rx.css :as css]
            #_[nsfw.page :as page]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [rx.browser.feather-icons :as feather]
            [com.rpl.specter :as sp]
            #_[nsfw.components :as nc]
            [rx.theme :as th]
            [clojure.core.async
             :as async
             :refer [go <!]]))

(def SEQR_KEY_STRS
  #_["z" "x" "c" "v" "b" "n" "m" "," "."]
  (->> (range 10)
       (map inc)
       (map str)))

(def EMPTY_SEQ_STATE
  {:id->frame
   (->> (range 9)
        (map (fn [i]
               [i {:id i}]))
        (into {}))})

(def NOTES
  [{:note-str "C0", :freq 16.35, :wavelength 2109.89}
   {:note-str "C#0/Db0", :freq 17.32, :wavelength 1991.47}
   {:note-str "D0", :freq 18.35, :wavelength 1879.69}
   {:note-str "D#0/Eb0", :freq 19.45, :wavelength 1774.2}
   {:note-str "E0", :freq 20.6, :wavelength 1674.62}
   {:note-str "F0", :freq 21.83, :wavelength 1580.63}
   {:note-str "F#0/Gb0", :freq 23.12, :wavelength 1491.91}
   {:note-str "G0", :freq 24.5, :wavelength 1408.18}
   {:note-str "G#0/Ab0", :freq 25.96, :wavelength 1329.14}
   {:note-str "A0", :freq 27.5, :wavelength 1254.55}
   {:note-str "A#0/Bb0", :freq 29.14, :wavelength 1184.13}
   {:note-str "B0", :freq 30.87, :wavelength 1117.67}
   {:note-str "C1", :freq 32.7, :wavelength 1054.94}
   {:note-str "C#1/Db1", :freq 34.65, :wavelength 995.73}
   {:note-str "D1", :freq 36.71, :wavelength 939.85}
   {:note-str "D#1/Eb1", :freq 38.89, :wavelength 887.1}
   {:note-str "E1", :freq 41.2, :wavelength 837.31}
   {:note-str "F1", :freq 43.65, :wavelength 790.31}
   {:note-str "F#1/Gb1", :freq 46.25, :wavelength 745.96}
   {:note-str "G1", :freq 49, :wavelength 704.09}
   {:note-str "G#1/Ab1", :freq 51.91, :wavelength 664.57}
   {:note-str "A1", :freq 55, :wavelength 627.27}
   {:note-str "A#1/Bb1", :freq 58.27, :wavelength 592.07}
   {:note-str "B1", :freq 61.74, :wavelength 558.84}
   {:note-str "C2", :freq 65.41, :wavelength 527.47}
   {:note-str "C#2/Db2", :freq 69.3, :wavelength 497.87}
   {:note-str "D2", :freq 73.42, :wavelength 469.92}
   {:note-str "D#2/Eb2", :freq 77.78, :wavelength 443.55}
   {:note-str "E2", :freq 82.41, :wavelength 418.65}
   {:note-str "F2", :freq 87.31, :wavelength 395.16}
   {:note-str "F#2/Gb2", :freq 92.5, :wavelength 372.98}
   {:note-str "G2", :freq 98, :wavelength 352.04}
   {:note-str "G#2/Ab2", :freq 103.83, :wavelength 332.29}
   {:note-str "A2", :freq 110, :wavelength 313.64}
   {:note-str "A#2/Bb2", :freq 116.54, :wavelength 296.03}
   {:note-str "B2", :freq 123.47, :wavelength 279.42}
   {:note-str "C3", :freq 130.81, :wavelength 263.74}
   {:note-str "C#3/Db3", :freq 138.59, :wavelength 248.93}
   {:note-str "D3", :freq 146.83, :wavelength 234.96}
   {:note-str "D#3/Eb3", :freq 155.56, :wavelength 221.77}
   {:note-str "E3", :freq 164.81, :wavelength 209.33}
   {:note-str "F3", :freq 174.61, :wavelength 197.58}
   {:note-str "F#3/Gb3", :freq 185, :wavelength 186.49}
   {:note-str "G3", :freq 196, :wavelength 176.02}
   {:note-str "G#3/Ab3", :freq 207.65, :wavelength 166.14}
   {:note-str "A3", :freq 220, :wavelength 156.82}
   {:note-str "A#3/Bb3", :freq 233.08, :wavelength 148.02}
   {:note-str "B3", :freq 246.94, :wavelength 139.71}
   {:note-str "C4", :freq 261.63, :wavelength 131.87}
   {:note-str "C#4/Db4", :freq 277.18, :wavelength 124.47}
   {:note-str "D4", :freq 293.66, :wavelength 117.48}
   {:note-str "D#4/Eb4", :freq 311.13, :wavelength 110.89}
   {:note-str "E4", :freq 329.63, :wavelength 104.66}
   {:note-str "F4", :freq 349.23, :wavelength 98.79}
   {:note-str "F#4/Gb4", :freq 369.99, :wavelength 93.24}
   {:note-str "G4", :freq 392, :wavelength 88.01}
   {:note-str "G#4/Ab4", :freq 415.3, :wavelength 83.07}
   {:note-str "A4", :freq 440, :wavelength 78.41}
   {:note-str "A#4/Bb4", :freq 466.16, :wavelength 74.01}
   {:note-str "B4", :freq 493.88, :wavelength 69.85}
   {:note-str "C5", :freq 523.25, :wavelength 65.93}
   {:note-str "C#5/Db5", :freq 554.37, :wavelength 62.23}
   {:note-str "D5", :freq 587.33, :wavelength 58.74}
   {:note-str "D#5/Eb5", :freq 622.25, :wavelength 55.44}
   {:note-str "E5", :freq 659.25, :wavelength 52.33}
   {:note-str "F5", :freq 698.46, :wavelength 49.39}
   {:note-str "F#5/Gb5", :freq 739.99, :wavelength 46.62}
   {:note-str "G5", :freq 783.99, :wavelength 44.01}
   {:note-str "G#5/Ab5", :freq 830.61, :wavelength 41.54}
   {:note-str "A5", :freq 880, :wavelength 39.2}
   {:note-str "A#5/Bb5", :freq 932.33, :wavelength 37}
   {:note-str "B5", :freq 987.77, :wavelength 34.93}
   {:note-str "C6", :freq 1046.5, :wavelength 32.97}
   {:note-str "C#6/Db6", :freq 1108.73, :wavelength 31.12}
   {:note-str "D6", :freq 1174.66, :wavelength 29.37}
   {:note-str "D#6/Eb6", :freq 1244.51, :wavelength 27.72}
   {:note-str "E6", :freq 1318.51, :wavelength 26.17}
   {:note-str "F6", :freq 1396.91, :wavelength 24.7}
   {:note-str "F#6/Gb6", :freq 1479.98, :wavelength 23.31}
   {:note-str "G6", :freq 1567.98, :wavelength 22}
   {:note-str "G#6/Ab6", :freq 1661.22, :wavelength 20.77}
   {:note-str "A6", :freq 1760, :wavelength 19.6}
   {:note-str "A#6/Bb6", :freq 1864.66, :wavelength 18.5}
   {:note-str "B6", :freq 1975.53, :wavelength 17.46}
   {:note-str "C7", :freq 2093, :wavelength 16.48}
   {:note-str "C#7/Db7", :freq 2217.46, :wavelength 15.56}
   {:note-str "D7", :freq 2349.32, :wavelength 14.69}
   {:note-str "D#7/Eb7", :freq 2489.02, :wavelength 13.86}
   {:note-str "E7", :freq 2637.02, :wavelength 13.08}
   {:note-str "F7", :freq 2793.83, :wavelength 12.35}
   {:note-str "F#7/Gb7", :freq 2959.96, :wavelength 11.66}
   {:note-str "G7", :freq 3135.96, :wavelength 11}
   {:note-str "G#7/Ab7", :freq 3322.44, :wavelength 10.38}
   {:note-str "A7", :freq 3520, :wavelength 9.8}
   {:note-str "A#7/Bb7", :freq 3729.31, :wavelength 9.25}
   {:note-str "B7", :freq 3951.07, :wavelength 8.73}
   {:note-str "C8", :freq 4186.01, :wavelength 8.24}
   {:note-str "C#8/Db8", :freq 4434.92, :wavelength 7.78}
   {:note-str "D8", :freq 4698.63, :wavelength 7.34}
   {:note-str "D#8/Eb8", :freq 4978.03, :wavelength 6.93}
   {:note-str "E8", :freq 5274.04, :wavelength 6.54}
   {:note-str "F8", :freq 5587.65, :wavelength 6.17}
   {:note-str "F#8/Gb8", :freq 5919.91, :wavelength 5.83}
   {:note-str "G8", :freq 6271.93, :wavelength 5.5}
   {:note-str "G#8/Ab8", :freq 6644.88, :wavelength 5.19}
   {:note-str "A8", :freq 7040, :wavelength 4.9}
   {:note-str "A#8/Bb8", :freq 7458.62, :wavelength 4.63}
   {:note-str "B8", :freq 7902.13, :wavelength 4.37}])

(defn white-key? [note]
  (when note
    (not (str/includes? (:note-str note) "/"))))

(def NOTE-STR->NOTE
  (->> NOTES
       (mapcat (fn [n]
                 (if (white-key? n)
                   [[(:note-str n) n]]
                   (concat
                     [[(:note-str n) n]]
                     (->> (str/split (:note-str n) "/")
                          (map str/trim)
                          (map (fn [s]
                                 [s n])))))))
       (into {})))


(defn take-until
  "Take from coll up to and including the first item that satisfies pred."
  [pred coll]
  (lazy-seq
    (when-let [coll (seq coll)]
      (let [x (first coll)]
        (cons x (when-not (pred x)
                  (take-until pred (rest coll))))))))

(defn slice-notes [from-note-str to-note-str]
  (->> NOTES
       (drop-while #(not= from-note-str (:note-str %)))
       (take-until #(= to-note-str (:note-str %)))))


(defn notes-add-layout [notes]
  (when (and notes (not (empty? notes)))
    (->> notes
         rest
         (reduce
           (fn [out-notes cur]
             (let [prev (last out-notes)
                   prev-index (or (:index prev)
                                  -0.5)]
               (conj
                 out-notes
                 (merge
                   cur
                   (if (and (white-key? prev)
                            (white-key? cur))
                     {:index (+ prev-index 1)}
                     {:index (+ prev-index 0.5)})
                   (if (white-key? cur)
                     {:key-type :white}
                     {:key-type :black})))))
           [(merge
              (first notes)
              {:index (if (white-key? (first notes))
                        0
                        0.5)
               :key-type (if (white-key? (first notes))
                           :white
                           :black)})]))))

(defn take-scale [notes]
  (->> [0 2 4 5 7 9 11 12]
       (map #(nth notes %))))


(def KEY_STRS
  ["C"
   "C#/Db"
   "D"
   "D#/Eb"
   "E"
   "F"
   "F#/Gb"
   "G"
   "G#/Ab"
   "A"
   "A#/Bb"
   "B"])

(def ROMAN_NUMERAL_LOOKUP
  (->> KEY_STRS
       (map (fn [key-str]
              [key-str
               (->> KEY_STRS
                    cycle
                    (drop-while #(not= key-str %))
                    take-scale
                    (map
                      (fn [numeral key-str]
                        [key-str numeral])
                      ["I" "II" "III" "IV" "V" "VI" "VII"])
                    (into {}))]))
       (into {})))

(defn note-str-without-octave [note-str]
  (when note-str
    (str/replace note-str #"\d" "")))

(defn roman-numeral-for [key-str note]
  (get-in ROMAN_NUMERAL_LOOKUP [key-str (note-str-without-octave (:note-str note))]))

(defn root-note? [key-str note]
  (= key-str (note-str-without-octave (:note-str note))))

(defn $sequencer [{:keys [frames

                          on-play
                          on-delete-frames
                          on-select-frame
                          on-clear-selection
                          on-delete-selection]}]
  (let [selected-frames (filter :selected? frames)
        has-selected? (not (empty? selected-frames))]
    [:div
     {:style {:display 'flex
              :flex-direction 'row
              :align-items 'center}}
     (->> frames
          (map
            (fn [key-str {:keys [chord id selected? playing?] :as frame}]
              [:div
               {:key id
                :style {:width 25
                        :height 20
                        :background-color (cond
                                            playing? css/light-blue
                                            selected? css/green
                                            :else 'white)
                        :margin-right 3
                        :display 'flex
                        :flex-direction 'row
                        :align-items 'center
                        :justify-content 'center
                        :border-radius 3}
                :on-click (fn [e]
                            (.preventDefault e)
                            (when on-select-frame
                              (on-select-frame
                                id
                                {:cherry? (.-ctrlKey e)
                                 :range? (.-shiftKey e)}))
                            nil)}
               [:div
                {:style {:font-size 10
                         :user-select 'none
                         :line-height "80%"
                         :color css/black
                         #_css/dark-grey
                         :font-weight 'bold
                         :transition "transform 0.2s ease, opacity 0.2s ease"
                         :transform (if (and chord (not (empty? chord)))
                                      "scale(1.4)"
                                      "scale(1.1)")
                         :opacity (if (and chord (not (empty? chord)))
                                    1
                                    0.5)}}
                key-str]

               #_(when (and chord
                            (not (empty? chord)))
                   [:div {:style {:width 9
                                  :height 9
                                  :border-radius 4.5
                                  :margin 3
                                  :background-color 'black}}])])
            SEQR_KEY_STRS)
          doall)
     
     [btn/bare
      {:on-click on-clear-selection
       :disabled? (not has-selected?)
       :label [feather/x
               {:size 16}]}]

     [btn/bare
      {:on-click on-delete-selection
       :disabled? (not has-selected?)
       :label [feather/trash
               {:size 16
                :style {:line-height "100%"}}]}]]))

(defn $controls [{:keys [on-change-key
                         on-change-volume
                         on-select-frame
                         seq-state]
                  :or {initial-volume 50}}]
  (let [!ui (r/atom {:volume (tonejs/get-global-volume)})]
    (r/create-class
      {:component-did-update
       (browser/cdu-diff
         (fn [[{oss :seq-state}]
              [{nss :seq-state}]]
           (when (not=
                   (:volume oss)
                   (:volume nss))
             (swap! !ui assoc :volume (or (:volume nss) 0)))))
       :reagent-render
       (fn [{:keys [seq-state
                    on-clear-selection
                    on-delete-selection]}]
         (let [{:keys [volume]} @!ui
               {:keys [scale-key]} seq-state]
           [:div.piano-controls
            [browser/interpose-children
             {:separator [:div {:style {:width 10}}]
              :style {:display 'flex
                      :flex-direction 'row
                      :align-items 'center}}
             [[:div
               {:style {:display 'flex
                        :flex-direction 'row
                        :justify-content 'center
                        :align-items 'center
                        :margin-right 10}}
               [:select.btn-dark
                {:on-change (fn [e]
                              (let [value (.. e -target -value)]
                                (when on-change-key
                                  (on-change-key value))))}
                [:option
                 (merge
                   {:value nil}
                   (when-not scale-key
                     {:selected "selected"}))
                 "Key"]
                (->> KEY_STRS
                     (map (fn [key-str]
                            [:option
                             (merge
                               {:key key-str
                                :value key-str
                                :style {:font-size 13}}
                               (when (= key-str scale-key)
                                 {:selected "selected"}))
                             key-str])))]]

              [:div
               {:style {:display 'flex
                        :flex-direction 'row
                        :justify-content 'center
                        :align-items 'center}}
               [:label
                {:style {:margin 0
                         :line-height "100%"}}
                "Vol"]
               [:div {:style {:width 5}}]
               [:input {:type "range"
                        :min -25
                        :max 0
                        :value volume
                        :on-change (fn [e]
                                     (let [volume (ks/parse-long (.. e -target -value))]
                                       (tonejs/set-global-volume volume)
                                       (swap! !ui assoc
                                         :volume
                                         volume)
                                       (when on-change-volume
                                         (on-change-volume volume))))
                        :style {:width 100}}]]
              [:div
               {:style {:display 'flex
                        :margin-left 10
                        :flex-direction 'row
                        :justify-content 'center
                        :align-items 'center}}
               [:label
                {:style {:margin 0
                         :line-height "100%"}}
                "Seq"]
               [:div {:style {:width 5}}]
               [$sequencer (merge
                             {:on-select-frame on-select-frame
                              :on-clear-selection on-clear-selection
                              :on-delete-selection on-delete-selection
                              :frames (->> seq-state
                                           :id->frame
                                           (sort-by first)
                                           (map second))})]]]]]))})))

(def keyboard-theme-docs
  [{:doc "White keys font color"
    :type ::th/css-color
    :rule [:nat-keys-fg-color ::nat-keys-fg-color "black"]}
   {:doc "White keys bg color"
    :type ::th/css-color
    :rule [:nat-keys-bg-color ::nat-keys-bg-color "white"]}
   {:doc "Black keys font color"
    :type ::th/css-color
    :rule [:acci-keys-fg-color ::acci-keys-fg-color "white"]}
   {:doc "Black keys bg color"
    :type ::th/css-color
    :rule [:acci-keys-bg-color ::acci-keys-bg-color "black"]}])

(defn $keyboard [{:keys [range
                         scale-key
                         on-click-note
                         style
                         highlighted-notes
                         playing-notes
                         instrument]
                  :as opts}]

  (let [white-key-width 40
        black-key-width 40
        notes-with-layout (vec
                            (notes-add-layout
                              (slice-notes
                                (first range)
                                (second range))))
        num-white-notes (->> notes-with-layout
                             (filter #(= :white (:key-type %)))
                             count)

        highlighted-notes-set (set highlighted-notes)
        playing-notes-set (set playing-notes)

        {:keys [nat-keys-fg-color
                nat-keys-bg-color
                acci-keys-fg-color
                acci-keys-bg-color]}
        (th/resolve opts
          (map :rule keyboard-theme-docs))]

    [:div {:style (merge
                    {:background-color 'white
                     :width (* num-white-notes white-key-width)
                     :height "100%"
                     :position 'relative}
                    style)}
     (->> notes-with-layout
          (map (fn [{:keys [index key-type freq note-str] :as note}]
                 (let [highlighted? (get highlighted-notes-set note-str)
                       playing? (get playing-notes-set note-str)
                       white-key? (= key-type :white)
                       labels (concat
                                [(roman-numeral-for
                                   scale-key
                                   note)]
                                (str/split note-str #"/"))
                       border-color (if white-key?
                                      "black"
                                      "white")
                       border-radius (when (not white-key?)
                                       4)]
                   [:div
                    {:key index
                     :style (merge
                              {:position 'absolute
                               :left  (str
                                        (* 100
                                          (/ index num-white-notes))
                                        "%")
                               :width (if white-key?
                                        (str
                                          (/ 100 num-white-notes)
                                          "%")
                                        (str
                                          (/ 100 num-white-notes)
                                          "%"))



                               :top 0
                               :bottom  (if white-key?
                                          0
                                          "40%")

                               :border-top 'none}

                              (when (not white-key?)
                                {:z-index 10}))
                     :on-mouse-down (fn [e]
                                      (.preventDefault e)
                                      (tonejs/play-note
                                        instrument
                                        {:freq freq})
                                      (on-click-note note)
                                      nil)}
                    [:div
                     {:style {:position 'relative
                              :width "100%"
                              :height "100%"}}
                     [:div {:style (merge
                                     {:position 'absolute
                                      :top 0
                                      :left 0
                                      :right 0
                                      :bottom 0
                                      :background-color
                                      (cond
                                        playing? css/light-blue
                                        highlighted? css/green
                                        :else (if (and scale-key
                                                       (root-note? scale-key note))
                                                (if white-key?
                                                  css/light-grey
                                                  css/dark-grey)
                                                (if white-key?
                                                  nat-keys-bg-color
                                                  acci-keys-bg-color)))}
                                     (when (not white-key?)
                                       {:margin-left 2
                                        :margin-right 2})
                                     (when white-key?
                                       {:border (str "solid " border-color " 1px")})
                                     (when border-radius
                                       {:border-bottom-left-radius border-radius
                                        :border-bottom-right-radius border-radius}))}]
                     [:div
                      {:style {:position 'absolute
                               :bottom 0
                               :left 0
                               :right 0
                               :text-align 'center
                               :padding-bottom 4}}
                      (->> labels
                           (map-indexed
                             (fn [i s]
                               [:div
                                {:key i
                                 :style {:font-size 11
                                         :color (if white-key?
                                                  nat-keys-fg-color
                                                  'white)}}
                                s])))]]]))))]))

(defn toggle-frame-note [{:keys [chord] :as seq-frame}
                         note-str]
  (assoc seq-frame
         :chord
         (if (get (-> chord set) note-str)
           (vec (disj (-> chord set) note-str))
           (vec (conj (-> chord set) note-str)))))

(defn frames-in-order [seq-data]
  (->> seq-data
       :id->frame
       (sort-by first)
       (map second)))

(defn $initial-scroll
  [{:keys [pct horizontal?]}]
  (r/create-class
    {:component-did-mount
     (fn [this]
       (let [node (rdom/dom-node this)]
         (set! (.-scrollLeft node) (/ (.-scrollWidth node) 4))))
     :reagent-render
     (fn [{:keys [horizontal?
                  hide-scrollbar?
                  style]}
          & children]
       (vec
         (concat
           [:div
            {:class (when hide-scrollbar? "noscrollbar")
             :style (merge
                      (when horizontal?
                        {:overflow-x 'scroll}
                        {:overflow-y 'scroll})
                      style)}]
           children)))}))

(defn frame-trigger-attack
  [{:keys [!seq-state]} {:keys [chord id] :as frame}]

  (when-not id
    (ks/throw-str "Frame id required for " frame))

  (doseq [note-str chord]
    (tonejs/trigger-attack
      {:freq note-str}))

  (swap! !seq-state
         update-in
         [:id->frame id]
         assoc
         :playing?
         true))

(defn frame-trigger-release

  [{:keys [!seq-state]} {:keys [chord id] :as frame}]

  (when-not id
    (ks/throw-str "Frame id required for " frame))

  (doseq [note-str chord]
    (tonejs/trigger-release
      {:freq note-str}))

  (swap! !seq-state
         update-in
         [:id->frame id]
         assoc
         :playing?
         false))

(defn create-state [] EMPTY_SEQ_STATE)

(defn play-seq-frame [instrument {:keys [chord]}]
  (doseq [note-str chord]
    (let [note (get NOTE-STR->NOTE note-str)]
      (tonejs/play-note instrument note))))

(defn view [{:keys [state
                    on-state
                    enable-keybindings?]}]
  (let [!state (r/atom
                 (or state
                     (create-state)))
        !instrument (r/atom nil)

        on-clear-selection
        (fn []
          (swap! !state
            (fn [state]
              (sp/transform
                [:id->frame sp/MAP-VALS]
                #(assoc % :selected? false)
                state))))

        on-delete-selection
        (fn []
          (swap! !state
            (fn [state]
              (sp/transform
                [:id->frame
                 sp/MAP-VALS
                 (sp/pred :selected?)]
                (fn [m]
                  (assoc m :chord nil))
                state))))

        keybindings
        (concat
          (->> (range 9)
               (map (fn [i]
                      {:key-name (str (inc i))
                       :handler
                       (fn []
                         (swap! !state
                           (fn [state]
                             (sp/transform
                               [:id->frame sp/MAP-VALS]
                               #(assoc % :selected?
                                  (= i (:id %)))
                               state)))
                         (play-seq-frame
                           @!instrument
                           (get-in
                             @!state
                             [:id->frame i])))})))
          [{:key-name "Escape"
            :handler on-clear-selection}
           {:key-name "Backspace"
            :handler on-delete-selection}])]

    (when on-state
      (on-state @!state))

    (go
      (let [res (<! (tonejs/<piano
                      (ks/spy
                        {:uri-base
                         ;; hack
                         (str
                           (browser/location-root)
                           (if (browser/dev-host?)
                             "/target/rx-browser/dev/tonejs-samples/piano/"
                             "/tonejs-samples/piano/"))})))]
        (if (anom/? res)
          (ks/pp res)
          (reset! !instrument res))))

    (r/create-class
      {:component-did-mount
       (fn []
         (when enable-keybindings?
           (kb/add-bindings! ::keybindings keybindings)))
       :component-will-unmount
       (fn []
         (kb/remove-bindings! ::keybindings))
       :component-did-update
       (browser/cdu-diff
         (fn [[{oss :sequencer-state
                old-enable-keybindings?
                :enable-keybindings?}]
              [{nss :sequencer-state
                new-enable-keybindings?
                :enable-keybindings?}]]
           (when (not= oss nss)
             (reset! !state (or nss EMPTY_SEQ_STATE)))

           (when (not= old-enable-keybindings?
                       new-enable-keybindings?)
             (if new-enable-keybindings?
               (kb/add-bindings! ::keybindings keybindings)
               (kb/remove-bindings! ::keybindings)))))
       :reagent-render
       (fn [{:keys [on-change-sequencer
                    hide-controls?
                    style]
             :as opts}]
         (let [{:keys [scale-key]} @!state]
           [:div
            {:style (merge
                      {:width "100%"
                       :height "100%"
                       :display 'flex
                       :flex-direction 'column}
                      style)}
            (when-not hide-controls?
              [$controls
               {:seq-state @!state
                :on-clear-selection on-clear-selection
                :on-delete-selection on-delete-selection

                :on-change-key
                (fn [key]
                  (swap! !state assoc :scale-key key)
                  (when on-change-sequencer
                    (on-change-sequencer @!state)))

                :on-change-volume
                (fn [volume]
                  (swap! !state assoc :volume volume)
                  (when on-change-sequencer
                    (on-change-sequencer @!state)))

                :on-select-frame
                (fn [id {:keys [range?]}]
                  (let [current-selection
                        (->> @!state
                             :id->frame
                             (map second)
                             (filter :selected?))
                        ids (conj (map :id current-selection) id)
                        high-id (apply max ids)
                        low-id (apply min ids)

                        selected-ids-set (if range?
                                           (set (range low-id (inc high-id)))
                                           #{id})]


                    (swap! !state
                      (fn [state]
                        (sp/transform
                          [:id->frame sp/MAP-VALS]
                          #(assoc % :selected? (get selected-ids-set (:id %)))
                          state))))

                  (when on-change-sequencer
                    (on-change-sequencer @!state))

                  (doseq [note-str (->> @!state
                                        :id->frame
                                        (map second)
                                        (filter :selected?)
                                        (map :chord)
                                        (reduce concat))]
                    (let [note (get NOTE-STR->NOTE note-str)]
                      (tonejs/play-note @!instrument note)))
                  nil)}])
            [:div {:style {:height 5}}]
            [:div
             {:style {:flex-grow 1
                      :position 'relative
                      :overflow 'hidden}}
             [$initial-scroll
              {:pct 0.5
               :horizontal? true
               :hide-scrollbar? true
               :style {:position 'absolute
                       :top 0
                       :left 0
                       :right 0
                       :bottom 0}}
              [$keyboard
               {:theme (:theme opts)
                :range ["A0" "C8"]
                :scale-key scale-key
                :instrument @!instrument
                :highlighted-notes (->> @!state
                                        :id->frame
                                        (map second)
                                        (filter :selected?)
                                        (map :chord)
                                        (reduce concat))
                :playing-notes (->> @!state
                                    :id->frame
                                    (map second)
                                    (filter :playing?)
                                    (map :chord)
                                    (reduce concat))
                :on-click-note
                (fn [{:keys [note-str]}]
                  (swap! !state
                    update
                    :id->frame
                    (fn [id->frame]
                      (->> id->frame
                           (map (fn [[id {:keys [selected?]
                                          :as seq-frame}]]
                                  [id (if selected?
                                        (toggle-frame-note
                                          seq-frame
                                          note-str)
                                        seq-frame)]))
                           (into {}))))
                  (when on-change-sequencer
                    (on-change-sequencer @!state)))}]]]]))})))

(defn styleguide-sections []
  [sg/section-container
   [:div
    [sg/section-image
     {:url "https://www.oldbookillustrations.com/wp-content/high-res/1879/know-weakness-768.jpg"
      :style {:width 250
              :float 'right}}]
    [sg/section
     {:style {:display 'block}}
     
     [:h1 {:id "piano"} "Piano"]
     [:div {:style {:clear 'both}}]]]
   [sg/section
    [ui/group
     {:gap 20}
     [ui/group
      [:h2 {:id "piano-examples"} "Examples"]
      (sg/example
        {:form [view
                {:enable-keybindings? true
                 :style {:height 200}}]})]
     [ui/group
      [:h3 "Hidden Controls"]
      (sg/example
        {:form [view {:hide-controls? true
                      :style {:height 200}}]})]]]
   [sg/section
    [:h2 {:id "piano-usage"} "Usage"]
    [sg/options-list
     {:options
      [[:enable-keybindings? "Enable keybindings for activating sequencer frames (keys 1-9), escape to defocus sequencer, and backspace to clear sequencer frame."]
       [:hide-controls? "Hide top controls"]
       [:state [:span "Component state, create with "
                [:code "r.b.p/create-state"]]]
       [:on-state "Callback, passed state for use with api"]]}]]])

(defn test-stuff []
  (do
    (popbar/<hide!)
    (browser/<set-root!
      [sg/standalone
       {:component styleguide-sections}])))


(comment

  (browser/<show-component! [view])

  (browser/<set-root!
    [sg/standalone
     {:component styleguide-sections}])

  (test-stuff)

  )
