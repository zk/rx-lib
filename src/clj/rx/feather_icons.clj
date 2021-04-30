(ns rx.feather-icons
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [rx.kitchen-sink :as ks]
            [com.rpl.specter :as s]
            [hickory.core :as h]
            [hickory.render :as hr]))

(def icons-dir "/Users/zk/code/rx/resources/feather-icons/svg")

(defn svg-str-to-clj [svg-str]
  (->> svg-str
       h/parse-fragment
       (map h/as-hiccup)))

(defn file-name->name [file-name]
  (-> file-name
      (str/split #"/")
      last
      (str/split #"\.")
      first))

(defn create-def [{:keys [file-name
                          svg]}]
  (let [[tag opts & kids :as svg-form] svg
        name (file-name->name file-name)
        svg-form (vec
                   (concat
                     [tag
                      (merge
                        (dissoc opts :viewbox)
                        {:width 'size
                         :height 'size
                         :stroke 'color
                         :style 'style
                         :stroke-width 'stroke-width
                         :view-box (:viewbox opts)
                         :pointer-events "none"})]
                     (->> kids
                          (remove #(and (string? %)
                                        (re-find #"\n.*" %))))))]
    (str
      "(defn "
      name
      " [{:keys [size color style stroke-width] :or {size 24 color \"currentColor\" stroke-width 2}}]\n"
      (->> (ks/pp-str svg-form)
           (#(str/split % #"\n"))
           (map #(str "  " %))
           (interpose "\n")
           (apply str))
      ")")))

(defn feather-gen-source []
  (let [icon-file-names
        (->> (io/as-file icons-dir)
             file-seq
             (map str)
             sort
             (filter #(str/includes? % ".svg")))

        icon-defs (->> icon-file-names
                       (map (fn [file-name]
                              {:file-name file-name
                               :svg (slurp file-name)}))
                       (map (fn [o]
                              (update o :svg #(first (svg-str-to-clj %)))))
                       (map create-def)
                       (interpose "\n\n")
                       (apply str))

        ns-decl (str "(ns rx.browser.feather-icons (:refer-clojure :exclude [list repeat map type hash filter shuffle key divide]))")

        icon-list (str
                    "(def all-icons\n"
                    (->> icon-file-names
                         (map file-name->name)
                         (mapv (fn [name]
                                 (let [sym (symbol name)]
                                   {:icon-name name
                                    :icon-fn sym})))
                         ks/pp-str)
                    ")")

        icon-lookup (str
                      "(def lookup\n"
                      (->> icon-file-names
                           (map file-name->name)
                           (map (fn [name]
                                  (let [sym (symbol name)]
                                    [name sym])))
                           (into {})
                           ks/pp-str)
                      ")")]

    (str
      ns-decl
      "\n\n"
      icon-defs
      "\n\n"
      icon-list
      "\n\n"
      icon-lookup)))


(defn path->name [s]
  (.getName (java.io.File. s)))

#_(* (/ 180 24) 2)

(defn export []
  (spit
    "./src/cljs-browser/rx/browser/feather_icons.cljs"
    (feather-gen-source)))

(comment


  (ks/spy (take 1 ))

  (hr/hiccup-to-html
    )
  
  (export)

  )
