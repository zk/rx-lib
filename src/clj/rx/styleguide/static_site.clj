(ns rx.styleguide.static-site
  (:require [rx.kitchen-sink :as ks]
            [rx.static-site-gen :as site-gen]
            [clojure.java.io :as io]
            [garden.core :as garden]
            [rx.browser.styleguide.css :as css]
            [cljs.build.api :as bapi]
            [clojure.string :as str]
            [rx.jot.css :as jot-css]
            [me.raynes.conch :as conch]))

(defn compile-css []
  (garden/css
    {:output-to nil
     :pretty-print? true
     :vendors ["webkit" "moz" "ms"]
     :preamble ["resources/public/css/bootstrap-reboot.min.css"
                "resources/public/css/bootstrap-grid.min.css"]
     :auto-prefix #{:justify-content
                    :align-items
                    :flex-direction
                    :flex-wrap
                    :align-self
                    :transition
                    :transform
                    :background-clip
                    :background-origin
                    :background-size
                    :filter
                    :font-feature-settings
                    :appearance}}
    (concat
      (css/rules)
      (jot-css/rules))))

(defn delete-files-recursively
  [f1 & [silently]]
  (when (.isDirectory (io/file f1))
    (doseq [f2 (.listFiles (io/file f1))]
      (delete-files-recursively f2 silently)))
  (io/delete-file f1 silently))

(defn compile-cljs [{:keys [source-paths compiler output-path
                            main-namespace]}]
  (let [js-file-name (str (-> main-namespace
                              str
                              (str/replace "." "-")
                              (str/replace "/" "-"))
                          ".js")
        output-to (str output-path "/cljs/" js-file-name)]
    (bapi/build
      (apply bapi/inputs source-paths)
      (merge
        {:output-to output-to
         :output-dir (str output-path "/cljs")
         :source-map (str output-path "/cljs/" js-file-name ".map")
         :target :bundle
         :npm-deps false
         :aot-cache false
         :bundle-cmd
         {:default ["npx"
                    "--verbose"
                    "-n"
                    "--max_old_space_size=8192"
                    "webpack"
                    "--mode=production"
                    output-to
                    "-o"
                    output-to]}
         :optimizations :advanced
         :pseudo-names false
         :main (str main-namespace)
         :asset-path "cljs"
         :parallel-build true
         ;;:pretty-print true
         :closure-defines '{cljs.core/*global* "window"}
         :compiler-stats true
         :foreign-libs
         [{:file "resources/dexie/dexie.min.js"
           :file-min "resources/dexie/dexie.min.js"
           :provides ["dexie"]}]
         :externs ["resources/dexie/dexie.ext.js"
                   "resources/tonejs/Tone.ext.js"
                   "resources/opentypejs/opentype.ext.js"]}
        compiler))

    (ks/pn "Done compiling")
    (ks/pn "Copying assets")

    (io/copy
      (io/as-file
        (str output-path "/cljs/" js-file-name))
      (io/as-file
        (str output-path "/js/" js-file-name)))

    (io/copy
      (io/as-file
        (str output-path "/cljs/" js-file-name ".map"))
      (io/as-file
        (str output-path "/js/" js-file-name ".map")))

    (ks/pn "Deleting cljs directory")

    (delete-files-recursively
      (str output-path "/cljs"))))

(defn page-paths []
  ["/index.html"
   "/theming.html"
   "/css-in-cljs.html"
   "/frame.html"
   "/buttons.html"
   "/icons.html"
   "/popover.html"
   "/forms.html"
   "/canvas.html"
   "/jot.html"
   "/box.html"
   "/tabs.html"
   "/threejs.html"
   "/youtube.html"
   "/fast-list.html"
   "/modal.html"
   "/notebook.html"
   "/electron-build.html"
   "/opentype.html"
   "/piano.html"])

(defn html-template [& [gen-ts]]
  (let [gen-ts (or gen-ts (ks/now))]
    [:html
     {:style {:width "100%" :height "100%"}}
     [:head
      [:title "℞"]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:link {:rel "stylesheet" :href "https://api.tiles.mapbox.com/mapbox-gl-js/v1.3.1/mapbox-gl.css"}]
      [:link {:rel "stylesheet" :href "https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css"}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Roboto+Condensed:300,400,700|Roboto:100,300,400,500,700|Inconsolata:400,700|Cardo:400,400i,700"}]
      [:link {:rel "stylesheet" :href "/css/rx-styleguide.css"}]
      [:script {:src "https://api.tiles.mapbox.com/mapbox-gl-js/v1.3.1/mapbox-gl.js"}]]
     [:body
      {:style {:width "100%" :height "100%"}}
      [:div {:id "rx-root-mount-point"
             :style {:width "100%" :height "100%" :display "flex"}}]
      [:script {:src (str "/js/rx-browser-styleguide-main.js?" gen-ts)}]
      [:script "rx.browser.styleguide_main.init();"]]]))

(defn copy-samples [{:keys [output-path samples-path]}]
  (let [target-samples-parent-dir (io/as-file output-path)
        cp (conch/programs cp)]
    (cp "-R" "./resources/tonejs-samples" target-samples-parent-dir)))

(defn build [{:keys [output-path] :as opts}]
  (let [start-ts (ks/now)
        output-file (io/as-file output-path)
        css-file (io/as-file (str output-path "/css"))
        cljs-file (io/as-file (str output-path "/cljs"))
        js-file (io/as-file (str output-path "/js"))

        gen-ts (ks/now)]

    (ks/pn "Building styleguide static site...")

    (.mkdirs output-file)
    
    (when-not (.isDirectory output-file)
      (ks/throw-str "Output path doesn't exists or not directory"))

    (.mkdirs css-file)
    (.mkdirs cljs-file)
    (.mkdirs js-file)

    (ks/pn "Copying tonejs samples...")
    (copy-samples opts)
    
    (doseq [page-path (page-paths)]
      (spit
        (str output-path page-path)
        (site-gen/hiccup->html5-page-str
          (html-template gen-ts))))

    (spit
      (str output-path "/css/rx-styleguide.css")
      (compile-css))

    (compile-cljs opts)

    (ks/pn "Took" (- (ks/now) start-ts) "ms")))


(comment
  
  (build
    {:output-path "docs"
     :main-namespace 'rx.browser.styleguide-main
     :source-paths ["src/cljs"
                    "src/cljs-browser"
                    "src/cljc"]})

  (copy-samples
    {:output-path "docs"})

  )


