(ns rx.jot.css)

(defn indent-level->ol-style-type [level]
  (nth (cycle ["decimal" "lower-latin" "upper-latin" "lower-roman" "upper-roman"]) (or level 0)))

(defn rules []
  [[:.jot-block.last-block
    {:margin-bottom "0 !important"}]
   (into
     [:ol.jot-block]
     (->> (range 9)
          (map (fn [i]
                 [(str "li.indent"
                       i
                       ":before")
                  {:content
                   (str "counter(indent"
                        i
                        ","
                        (indent-level->ol-style-type i)
                        ") \". \"")}]))))])
