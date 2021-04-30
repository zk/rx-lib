(ns rx.jvm.icons.material-icons-import
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

(defn svg-str-to-clj [svg-str]
  (->> svg-str
       h/parse-fragment
       (map h/as-hiccup)
       first))

(defn extract-data [path]
  (let [parts (reverse (str/split path #"/"))
        [_ theme name category] parts

        theme (str/replace theme "materialicons" "")]
    {:theme theme
     :name name
     :category category
     :path path
     :svg-str (slurp path)}))

(defn clean-svg-str [s]
  (-> s
      (str/replace "<path d=\"M0 0h24v24H0z\" fill=\"none\"/>" "")
      (str/replace "<path d=\"M0 0h24v24H0V0z\" fill=\"none\"/>" "")))

(defn gen-icon [icon-data]
  (merge
    icon-data
    {:fn-str (create-fn (svg-str-to-clj (clean-svg-str (:svg-str icon-data))))}))

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

(defn gen-theme-defns []
  (let [themes ["filled" "outlined" "round" "sharp" "twotone"]]
    (->> themes
         (map (fn [theme]
                (str
                  "(defn " theme " [opts] ((get " theme "-theme (:name opts)) opts))")))
         (interpose "\n\n")
         (apply str))))

(defn generate-icon-cljc [{:keys [repo-path
                                  output-path
                                  namespace]}]
  (let [src-path (str repo-path "/src")
        icons (->> src-path
                   io/file
                   file-seq
                   (map str)
                   (filter #(str/ends-with? % "24px.svg"))
                   (map extract-data)
                   #_(filter #(= "book" (:name %)))
                   (map gen-icon))

        clj-src (str
                  "(ns " namespace ")\n\n"
                  (gen-themes icons)
                  "\n\n"
                  (gen-theme-defns))]
    (spit output-path clj-src)))

(comment

  (time
    (generate-icon-cljc
      {:repo-path "/Users/zk/code/material-design-icons"
       :output-path "src/cljc/rx/icons/material.cljc"
       :namespace 'rx.icons.material}))

  (ks/pp
    (gen-theme-defns))

  #_(ks/pp (gen-icon (extract-data "/Users/zk/code/material-design-icons/src/action/book/materialicons/24px.svg")))

  )

