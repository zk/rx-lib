(ns rx.jvm.icons.ionicons-import
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
                         :stroke 'color
                         :style 'style
                         :stroke-width 'stroke-width
                         :view-box (:viewbox opts)
                         :pointer-events "none"})]
                     (->> kids
                          (remove #(and (string? %)
                                        (re-find #"\n.*" %))))))]
    (str
      "(fn "
      "[{:keys [size color style stroke-width] :or {size 24 color \"currentColor\"}}]\n"
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
       (drop-while #(not= :svg (first %)))
       first))


#_(svg-str-to-clj
  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!-- Generator: Adobe Illustrator 24.3.0, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->\n<svg version=\"1.1\" id=\"icons\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\"\n\t viewBox=\"0 0 512 512\" style=\"enable-background:new 0 0 512 512;\" xml:space=\"preserve\">\n<path d=\"M96,208H48c-8.8,0-16-7.2-16-16s7.2-16,16-16h48c8.8,0,16,7.2,16,16S104.8,208,96,208z\"/>\n<path d=\"M124.1,140.1c-4.2,0-8.3-1.7-11.3-4.7l-33.9-33.9c-6.2-6.2-6.2-16.4,0-22.6s16.4-6.2,22.6,0l33.9,33.9\n\tc6.3,6.2,6.3,16.4,0,22.6C132.4,138.4,128.4,140.1,124.1,140.1z\"/>\n<path d=\"M192,112c-8.8,0-16-7.2-16-16V48c0-8.8,7.2-16,16-16s16,7.2,16,16v48C208,104.8,200.8,112,192,112z\"/>\n<path d=\"M259.9,140.1c-8.8,0-16-7.2-16-16c0-4.2,1.7-8.3,4.7-11.3l33.9-33.9c6.2-6.2,16.4-6.2,22.6,0c6.2,6.2,6.2,16.4,0,22.6\n\tl-33.9,33.9C268.2,138.4,264.1,140.1,259.9,140.1z\"/>\n<path d=\"M90.2,309.8c-8.8,0-16-7.2-16-16c0-4.2,1.7-8.3,4.7-11.3l33.9-33.9c6.2-6.2,16.4-6.2,22.6,0s6.2,16.4,0,22.6l-33.9,33.9\n\tC98.5,308.1,94.4,309.8,90.2,309.8z\"/>\n<path d=\"M234.2,167c-18.4-18.7-48.5-19-67.2-0.7s-19,48.5-0.7,67.2c0.2,0.2,0.5,0.5,0.7,0.7l39.5,39.5c3.1,3.1,8.2,3.1,11.3,0\n\tl55.9-55.9c3.1-3.1,3.1-8.2,0-11.3L234.2,167z\"/>\n<path d=\"M457,389.8L307.6,240.4c-3.1-3.1-8.2-3.1-11.3,0l-55.9,55.9c-3.1,3.1-3.1,8.2,0,11.3L389.8,457c18.4,18.7,48.5,19,67.2,0.7\n\tc18.7-18.4,19-48.5,0.7-67.2C457.5,390.3,457.3,390,457,389.8L457,389.8z\"/>\n</svg>\n")

(defn extract-data [path]
  (let [parts (reverse (str/split path #"/"))
        [file-name & _] parts

        name (first (str/split file-name #"\."))
        theme (cond
                (str/index-of name "-outline") "outline"
                (str/index-of name "-sharp") "sharp"
                :else "base")

        name (-> name
                 (str/replace "-outline" "")
                 (str/replace "-sharp" ""))]
    {:theme theme
     :name name
     :path path
     :svg-str (slurp path)}))

(defn gen-icon [icon-data]
  (try
    (merge
      icon-data
      {:fn-str (create-fn (svg-str-to-clj (:svg-str icon-data)))})
    (catch Exception e
      (ks/pp icon-data)
      (throw e))))

(defn gen-themes [icons]
  (->> icons
       
       #_(take 10)
       
       (group-by :theme)
       (sort-by first)
       (map (fn [[theme icons]]
              (str
                "(def " (if (empty? theme)
                          "default"
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
  (let [themes ["base" "outline" "sharp"]]
    (->> themes
         (map (fn [theme]
                (str
                  "(defn " theme " [opts] ((get " theme "-theme (:name opts)) opts))")))
         (interpose "\n\n")
         (apply str))))

(defn generate-icon-cljc [{:keys [repo-path
                                  output-path
                                  namespace]}]
  (let [src-path (str repo-path "/src/svg")
        icons (->> src-path
                   io/file
                   file-seq
                   (map str)
                   (filter #(str/ends-with? % ".svg"))
                   (map extract-data)
                   (map gen-icon))

        clj-src (str
                  "(ns " namespace ")\n\n"
                  (gen-themes icons)
                  "\n\n"
                  (gen-theme-defns))]

    #_(println clj-src)
    (spit output-path clj-src)))

(comment

  (println
    (generate-icon-cljc
      {:repo-path "/Users/zk/code/ionicons"
       :output-path "src/cljc/rx/icons/ionicons.cljc"
       :namespace 'rx.icons.ionicons}))

  (ks/pp
    (gen-theme-defns))

  )
