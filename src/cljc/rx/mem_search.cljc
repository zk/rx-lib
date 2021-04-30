(ns rx.mem-search
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [clj-fuzzy.porter :as porter]))

;; Need full text search

(defn tokenize [s]
  (->> (str/split s #"\b+")
       (map str/trim)
       (map str/lower-case)
       (map porter/stem)
       (remove empty?)))

(defn add-doc-to-index [index {:keys [id text]}]
  (let [words (tokenize text)]
    (->> words
         (reduce
           (fn [accu word]
             (update
               accu
               word
               (fn [v]
                 (conj (set v) id))))
           index))))

(defn add-docs-to-index [index docs]
  (reduce
    (fn [accu doc]
      (add-doc-to-index accu doc))
    index
    docs))

(comment

  

  (ks/pp
    (add-docs-to-index
      {}
      [{:id "doc-123"
        :text "The quick brown fox jumps over the lazy dog"}
       {:id "doc-456"
        :text "Hello world brown"}]))

  )
