(ns rx.jvm.icons.streamline-icons-import
  (:require [rx.kitchen-sink :as ks]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.core :as h]))

(defn create-fn [svg-hic]
  (let [[tag opts & kids :as svg-form] svg-hic
        svg-form (vec
                   (concat
                     [tag
                      (merge
                        (dissoc opts :viewbox)
                        {:width 'size
                         :height 'size
                         :fill 'color
                         :style 'style
                         :view-box (:viewbox opts)
                         :pointer-events "none"})]
                     (->> kids
                          (remove #(and (string? %)
                                        (re-find #"\n.*" %))))))]
    (str
      "(fn "
      "[{:keys [size color style] :or {size 24 color \"currentColor\"}}]\n"
      (->> (ks/pp-str svg-form)
           (#(str/split % #"\n"))
           (map #(str "  " %))
           (interpose "\n")
           (apply str))
      ")")))

(defn create-defn [fn-name svg-hic]
  (let [[tag opts & kids :as svg-form] svg-hic
        svg-form (vec
                   (concat
                     [tag
                      (merge
                        (dissoc opts :viewbox)
                        {:width 'size
                         :height 'size
                         :style 'style
                         :fill 'color
                         :stroke 'color
                         :view-box (:viewbox opts)
                         :pointer-events "none"})]
                     (->> kids
                          (remove #(and (string? %)
                                        (re-find #"\n.*" %))))))]
    (str
      "(defn " fn-name " "
      "[{:keys [size color style] :or {size 24 color \"currentColor\"}}]\n"
      (->> (ks/pp-str svg-form)
           (#(str/split % #"\n"))
           (map #(str "  " %))
           (interpose "\n")
           (apply str))
      ")")))

(defn svg-str-to-clj [svg-str]
  (->> svg-str
       h/parse-fragment
       (map h/as-hiccup)
       first))

(defn extract-data [path]
  (let [parts (reverse (str/split path #"/"))
        [name & _] parts

        name (-> name
                 (str/replace "streamline-icon-" "")
                 (str/replace #"@.*" ""))]
    {:name name
     :path path
     :svg-str (slurp path)}))

(defn clean-svg-str [s]
  (-> s
      (str/replace "<path d=\"M0 0h24v24H0z\" fill=\"none\"/>" "")
      (str/replace "<path d=\"M0 0h24v24H0V0z\" fill=\"none\"/>" "")
      (str/replace "stroke=\"currentColor\"" "")
      (str/replace "fill=\"currentColor\"" "")
      (str/replace "stroke=\"#000000\"" "")
      (str/replace "fill=\"#000000\"" "")))

(defn gen-icon [icon-data]
  (merge
    icon-data
    {:defn-str (create-defn
                 (:name icon-data)
                 (svg-str-to-clj (clean-svg-str (:svg-str icon-data))))}))

(defn gen-themes [icons]
  (->> icons
       (group-by :theme)
       (sort-by first)
       (map (fn [[theme icons]]
              (str
                "(def " (if (empty? theme)
                          "filled"
                          theme)
                "-theme\n"
                "{\n"
                (->> icons
                     (map (fn [{:keys [name fn-str]}]
                            (str "\"" name "\" \n" fn-str)))
                     (interpose "\n\n")
                     (apply str))
                "\n})")))
       (interpose "\n\n")
       (apply str)))

(defn icons-defns [icons]
  (->> icons
       (map :defn-str)
       (interpose "\n\n")
       (apply str)))

(defn gen-theme-defns []
  (let [themes ["filled" "outlined" "round" "sharp" "twotone"]]
    (->> themes
         (map (fn [theme]
                (str
                  "(defn " theme " [opts] ((get " theme "-theme (:name opts)) opts))")))
         (interpose "\n\n")
         (apply str))))

(defn generate-icon-cljc [{:keys [svgs-dir
                                  output-path
                                  namespace]}]
  (let [icons (->> svgs-dir
                   io/file
                   file-seq
                   (map str)
                   (filter #(str/ends-with? % ".svg"))
                   (map extract-data)
                   #_(filter #(= "book" (:name %)))
                   (map gen-icon))

        clj-src (str
                  "(ns " namespace ")\n\n"
                  (icons-defns icons))]

    (spit output-path clj-src)))

(comment

  (time
    (generate-icon-cljc
      {:svgs-dir "/Users/zk/code/rx/resources/papers/icons"
       :output-path "src/cljc/zel/streamline_icons.cljc"
       :namespace 'zel.streamline-icons}))

  (ks/pp
    (gen-theme-defns))

  #_(ks/pp (gen-icon (extract-data "/Users/zk/code/material-design-icons/src/action/book/materialicons/24px.svg")))

  )

