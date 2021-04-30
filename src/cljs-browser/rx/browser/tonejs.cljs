(ns rx.browser.tonejs
  (:require
   [rx.kitchen-sink :as ks]
   [rx.browser :as browser]
   [rx.anom :as anom]
   [tonejs]
   #_[tone :refer
      [Transport Part Synth Sampler Envelope Master
       Volume]]
   [clojure.core.async
    :as async
    :refer [go <! timeout put! close! chan]]))


(def Tone (try
            js/Tone
            (catch js/Error e
              (println "Couldn't load Tone.js")
              nil)))

(def Volume (.-Volume Tone))
(def Transport (.-Transport Tone))
(def Synth (.-Synth Tone))
(def PolySynth (.-PolySynth Tone))

(def GLOBAL_VOLUME (Volume. -7.5))

(defn set-global-volume [volume]
  (set! (.-value (.-volume GLOBAL_VOLUME)) volume))

(defn get-global-volume []
  (.-value (.-volume GLOBAL_VOLUME)))

(def key->file-frag
  {"C" "C"
   "C#" "Cs"
   "D" "D"
   "D#" "Ds"
   "E" "E"
   "F" "F"
   "F#" "Fs"
   "G" "G"
   "G#" "Gs"
   "A" "A"
   "A#" "As"
   "B" "B"})

(def order
  ["C"
   "Cs"
   "D"
   "Ds"
   "E"
   "F"
   "Fs"
   "G"
   "Gs"
   "A"
   "As"
   "B"])

(defn note-range [start end]
  (let [start-note (first start)
        start-octave (ks/parse-long (apply str (rest start)))

        end-note (first end)
        end-octave (ks/parse-long (apply str (rest end)))

        delta (- (inc end-octave) start-octave)]

    (conj
      (->> (cycle order)
           (drop-while #(not (= start-note %)))
           (take (* delta (count order)))
           (map
             (fn [i note-frag]
               (str note-frag i))
             (->> (range start-octave (inc end-octave))
                  (mapcat (fn [i]
                            (repeat 12 i)))))

           (take-while #(not= end %))
           vec)
      end)))

(defn create-sample-map [{:keys [uri-base ext
                                 low-octave
                                 high-octave]
                          :or
                          {low-octave 3
                           high-octave 5}}]
  (->> key->file-frag
       (map (fn [[note-str sample-file-frag]]
              (->> (range low-octave high-octave)
                   (map (fn [i]
                          [(str note-str i)
                           (str uri-base sample-file-frag i "." ext)]))
                   (into {}))))
       (reduce merge)))

(defn <instrument [opts]
  (let [ch (chan)]
    (try
      (let [sample-map (create-sample-map opts)
            !sample-map (atom nil)
            sampler nil #_ (Sampler.
                             (clj->js sample-map)
                             (clj->js
                               {:onload
                                (fn []
                                  (put! ch @!sample-map)
                                  (close! ch))
                                :onerror (fn [e]
                                           (put! ch (anom/from-err e {:var #'<instrument}))
                                           (close! ch))
                         
                                :release 0.2}))

            envelope nil #_(Envelope. 0 0 1 0.2)]

        (set! (.-releaseCurve envelope) "exponential")

        (.chain sampler GLOBAL_VOLUME nil #_Master)

        (reset! !sample-map
          (merge
            opts
            {:sample-map sample-map
             :sampler-obj sampler})))
      (catch js/Error e
        (put! ch (anom/from-err e {:var #'<instrument}))
        (close! ch)))
    ch))

(defn <piano [& [opts]]
  (<instrument
    (merge
      {:uri-base "http://localhost:5000/target/rx-browser/dev/tonejs-samples/piano/"
       :ext "mp3"
       :low-octave 0
       :high-octave 6}
      opts)))

(defn trigger-attack-release [{:keys [sampler-obj]} note duration & [time]]
  (assert sampler-obj)
  (.triggerAttackRelease
    sampler-obj
    note
    duration
    time))

(defn <start []
  (ks/<promise (.start Tone)))

(defn play-note [instrument
                 {:keys [freq duration shape]
                  :or {shape :sine
                       freq 440}}]

  (trigger-attack-release
    instrument
    freq
    "4n"
    (.. Tone -context -currentTime)))

(defn trigger-attack [{:keys [freq duration shape]
                       :or {shape :sine
                            freq 440}}]

  (.triggerAttack
    (fn [])
    freq
    (.. Tone -context -currentTime)))

(defn trigger-release [{:keys [freq duration shape]
                        :or {shape :sine
                             freq 440}}]
  (.triggerRelease
    (fn [])
    freq
    (.. Tone -context -currentTime)))


(def default-synth (.toDestination (PolySynth.)))

(defn trigger-ar! [note duration]
  (.triggerAttackRelease
    default-synth
    note duration))

(comment

  


  (go
    (let [instrument
          (ks/spy
            (<! (<instrument
                  {:uri-base "http://localhost:5000/target/rx-browser/dev/tonejs-samples/piano/"
                   :ext "mp3"
                   :low-octave 0
                   :high-octave 6})))]
      (trigger-attack-release
        instrument
        "C3"
        "1n")))

  (ks/spy
    (sample-map
      {:uri-base "http://localhost:5000/target/rx-browser/dev/tonejs-samples/piano/"
       :ext "mp3"}))

  (.triggerAttackRelease)

  (let [synth (.toDestination (Synth.))]
    (.triggerAttackRelease
      synth
      "C4" "32n"))


  (.start Tone)

  tone

  (trigger-ar! "B5" "32n")

  (browser/<show-component!
    [:button
     {:on-click (fn [] (.start tone))}
     "Click"])

  )
