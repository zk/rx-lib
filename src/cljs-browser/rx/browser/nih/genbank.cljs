(ns rx.browser.nih.genbank
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [clojure.string :as str]))

(defn extract-origin-text [sequence-text]
  (->> (str/split sequence-text #"\n")
       (drop-while #(not (str/starts-with? % "ORIGIN")))
       (take-while #(not (str/starts-with? % "//")))
       (interpose "\n")
       (apply str)))

(defn gb-origin [{:keys [sequence-text]}]
  (let [origin-text (extract-origin-text sequence-text)
        nucleotide-lines (->> (str/split origin-text #"\n")
                              (drop 1))
        nucleotide-data (->> nucleotide-lines
                             (map (fn [line]
                                    (let [line (str/trim line)
                                          [_
                                           starting-bp-index-str
                                           base-pairs-str]
                                          (re-find #"^(\d+)\s([atgcuATGCU\s]+)$" line)
                                          starting-bp-index (ks/parse-long starting-bp-index-str)
                                          base-pairs-str (-> base-pairs-str
                                                             str/trim)]
                                      {:starting-bp-index (ks/parse-long starting-bp-index-str)
                                       :base-pairs-str base-pairs-str}))))]
    [:div
     {:style {:overflow-y 'scroll
              :flex 1}}
     [:div
      {:style {:font-size 14}}
      #_(ks/pp-str nucleotide-data)
      (->> nucleotide-data
           (map (fn [{:keys [starting-bp-index
                             base-pairs-str]}]
                  [:div
                   {:key starting-bp-index
                    :style {:font-family "monospace"
                            :display 'flex
                            :flex-direction 'row}}
                   [:div {:style {:width 50
                                  :text-align 'right
                                  :padding-right 10}}
                    starting-bp-index]
                   [:div
                    base-pairs-str]])))
      #_(ks/pp-str nucleotide-data)]]))

(defn split-replacing
  "Splits string on a regular expression. Optional argument limit is
  the maximum number of splits. Not lazy. Returns vector of the splits."
  ([s re f]
   (split-replacing s re f 0))
  ([s re f limit]
   (str/discard-trailing-if-needed limit
     (if (identical? "/(?:)/" (str re))
       (str/split-with-empty-regex s limit)
       (loop [s s
              parts []]
         (let [m (re-find re s)]
           (if-not (nil? m)
             (let [index (.indexOf s m)]
               (recur
                 (.substring s (+ index (count m)))
                 (conj parts
                   (.substring s 0 index)
                   (f (.substring s index (+ index (count m)))))))
             (conj parts s))))))))

(defn sequence-text-detail [{:keys [sequence-text]}]
  (into
    [:pre
     {:style {:font-size 14
              :padding 10
              :flex 1}}]
    (-> sequence-text
        (split-replacing
          #"\d+\.\.\d+"
          (fn [s]
            [:a {:href "#"
                 :on-click (fn []
                             (prn "highlight base pairs" s))}
             s])))))

(defn sequence-viewer [opts]
  [:div {:style {:display 'flex
                 :flex-direction 'row
                 :overflow 'hidden
                 :flex 1}}
   [:div {:style {:width "50%"
                  :flex 1
                  :display 'flex}}
    [sequence-text-detail opts]]
   [:div {:style {:width "50%"
                  :flex 1
                  :display 'flex}}
    [gb-origin opts]]])

(comment

  (browser/<show-component!
    [sequence-viewer
     {:sequence-text
      (ks/slurp-cljs
        "resources/covid-19/sequence.gb")}])


  )
