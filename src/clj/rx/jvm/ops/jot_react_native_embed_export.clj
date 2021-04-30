(ns rx.jvm.ops.jot-react-native-embed-export
  (:require [rx.kitchen-sink :as ks]
            [rx.jot.css :as jot-css]
            [clojure.java.io :as io]
            [cljs.build.api :as bapi]
            [garden.core :as garden]
            #_[figwheel-sidecar.system :as fs]
            [figwheel.main.api :as fapi]
            [clojure.string :as str]
            #_[com.stuartsierra.component :as component]
            [http.server]))

(defn html [{:keys []}]
  "<!DOCTYPE html>
<html>
  <head>
    <meta charset=\"UTF-8\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">
    <link rel=\"stylesheet\" href=\"css/jot-react-native-embed.css\" />
  </head>
  <body>
    <div id=\"rx-root-mount-point\"></div>
    <script src=\"js/jot-react-native-embed.js\"></script>
  </body>
</html>")

(defn delete-files-recursively
  [f1 & [silently]]
  (when (.isDirectory (io/file f1))
    (doseq [f2 (.listFiles (io/file f1))]
      (delete-files-recursively f2 silently)))
  (io/delete-file f1 silently))

(defn dev-path [opts]
  (or (:dev-path opts)
      (str
        "target/dev/"
        (-> opts
            :main-namespace
            str
            (str/replace #"\." "-")))))

(defn compile-cljs [{:keys [source-paths compiler output-path
                            main-namespace]}]
  (bapi/build
    (apply bapi/inputs source-paths)
    (merge
        {:output-to
         (str output-path "/cljs/jot-react-native-embed.js")
         :output-dir (str output-path "/cljs")
         :source-map (str output-path "/cljs/jot-react-native-embed.js.map")
         :optimizations :advanced
         :main (str main-namespace)
         :asset-path "cljs"
         :parallel-build true
         :foreign-libs
         [{:file "resources/dexie/dexie.min.js"
           :file-min "resources/dexie/dexie.min.js"
           :provides ["dexie"]}]}
        compiler)))

(defn compile-css [opts]
  (garden/css
    (merge
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
      opts)
    (jot-css/rules)))

(defn build [{:keys [output-path
                     main-namespace]
              :as opts}]
  (let [output-file (io/as-file output-path)]
    (when-not output-path
      (ks/throw-str ":output-path not provided"))
    (when-not (.exists output-file)
      (.mkdirs output-file))
    (when-not (.exists output-file)
      (ks/throw-str ":output-path does not exist: " output-path))
    (when-not (.isDirectory output-file)
      (ks/throw-str ":output-path is not a directory: " output-path))
    (when-not main-namespace
      (ks/throw-str ":main-namespace not provided"))

    (compile-cljs opts)

    (.mkdirs
      (io/as-file (str output-path "/js")))

    (io/copy
      (io/as-file
        (str output-path "/cljs/jot-react-native-embed.js"))
      (io/as-file
        (str output-path "/js/jot-react-native-embed.js")))

    (delete-files-recursively
      (str output-path "/cljs"))

    (spit
      (str output-path "/jot-react-native-embed.html")
      (html {}))
    
    (.mkdirs
      (io/as-file (str output-path "/css")))

    (spit
      (str output-path "/css/jot-react-native-embed.css")
      (compile-css opts))))

(defonce !figwheel-server (atom nil))

(defn stop-figwheel [figwheel-server]
  (when figwheel-server
    #_(component/stop @!figwheel-server)))

(defn start-figwheel [{:keys [main-namespace
                              source-paths]
                       :as opts}]
  (when-not main-namespace
    (ks/throw-str ":main-namespace required"))

  (when-not source-paths
    (ks/throw-str ":source-paths required"))

  (let [root-path (dev-path opts)
        safe-main-namespace (-> main-namespace
                                str
                                (str/replace #"\." "-"))]

    (try
      (fapi/stop safe-main-namespace)
      (catch Exception e
        (ks/pn "Server already stopped")))

    (fapi/start
      {:mode :repl
       :open-url false
       :ring-server-options {:port 44204}
       :watch-dirs source-paths
       :css-dirs [(str root-path "/css")]
       ;;:client-log-level :finest
       :validate-config true
       :rebel-readline false}
      {:id safe-main-namespace
       :options
       {:output-to (str root-path "/js/jot-react-native-embed.js")
        :output-dir (str root-path "/js")
        :optimizations :none
        :source-map true
        :main (str main-namespace)
        :asset-path "js"
        :parallel-build true
        :foreign-libs
        [{:file "resources/dexie/dexie.min.js"
          :file-min "resources/dexie/dexie.min.js"
          :provides ["dexie"]}]}})
    
    (spit
      (str root-path "/jot-react-native-embed.html")
      (html {}))
    
    (.mkdirs
      (io/as-file (str root-path "/css")))

    (spit
      (str root-path "/css/jot-react-native-embed.css")
      (compile-css opts))

    #_(fs/cljs-repl
        (:figwheel-system @!figwheel-server)
        safe-main-namespace)))

(defn recompile-dev-css [opts]
  (spit
    (str (dev-path opts) "/css/jot-react-native-embed.css")
    (compile-css opts)))

(defonce !http-server (atom nil))

(defn stop-http-server [http-server]
  (when http-server
    (http.server/stop http-server)))

(defn start-http-server [{:keys [main-namespace
                                 server-path]
                          :as opts}]
  (stop-http-server @!http-server)

  (let [path (dev-path opts)]
    (reset! !http-server
      (http.server/start
        {:dir server-path
         :port 44205}))))

(defn stop-dev []
  (stop-figwheel @!figwheel-server)
  (stop-http-server @!http-server))

(defn start-dev [opts]
  (start-http-server opts)
  (start-figwheel opts))

(comment

  (compile-css {})

  (stop-dev)

  (start-figwheel
    {:dev-path "./vee6/jot-react-native-embed"
     :source-paths ["src/cljs"
                    "src/cljs-browser"
                    "src/cljc"
                    "checkouts/nsfw/src/cljs"
                    "checkouts/nsfw/src/cljc"]
     :main-namespace 'rx.browser.jot.example-embed})

  (ks/spy
    (build
      {:output-path "./vee6/jot-react-native-embed"
       :source-paths ["src/cljs"
                      "src/cljs-browser"
                      "src/cljc"
                      "checkouts/nsfw/src/cljs"
                      "checkouts/nsfw/src/cljc"]
       :main-namespace 'rx.browser.jot.example-embed}))
  
  #_(start-figwheel
      {:output-path
       "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/3D7808A3-8A25-4023-9655-2280FBC30ED1/Canter.app/jot-react-native-embed"
       #_"./vee3/jot-react-native-embed"
       :server-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/3D7808A3-8A25-4023-9655-2280FBC30ED1/Canter.app"
       :source-paths ["src/cljs"
                      "src/cljs-browser"
                      "src/cljc"
                      "checkouts/nsfw/src/cljs"
                      "checkouts/nsfw/src/cljc"]
       :main-namespace 'rx.browser.jot.example-embed})

  (start-dev
    {:dev-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/92BBB83B-ECBA-438F-8CA0-7A14CFA243CA/Canter.app/jot-react-native-embed"
     :output-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/92BBB83B-ECBA-438F-8CA0-7A14CFA243CA/Canter.app"
     :server-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/92BBB83B-ECBA-438F-8CA0-7A14CFA243CA/Canter.app"
     :source-paths ["src/cljs"
                    "src/cljs-browser"
                    "src/cljc"
                    "checkouts/nsfw/src/cljs"
                    "checkouts/nsfw/src/cljc"]
     :main-namespace 'rx.browser.jot.example-embed})

  (fapi/stop "rx-browser-jot-example-embed")

  (recompile-dev-css
    {:dev-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/E381F78C-3320-4FD8-900D-B517E0AF7CE7/Canter.app/jot-react-native-embed"
     :output-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/E381F78C-3320-4FD8-900D-B517E0AF7CE7/Canter.app"
     :server-path "/Users/zk/Library/Developer/CoreSimulator/Devices/6339534D-7A4C-4BA2-9EDD-7242D5252607/data/Containers/Bundle/Application/E381F78C-3320-4FD8-900D-B517E0AF7CE7/Canter.app"
     :source-paths ["src/cljs"
                    "src/cljs-browser"
                    "src/cljc"
                    "checkouts/nsfw/src/cljs"
                    "checkouts/nsfw/src/cljc"]
     :main-namespace 'rx.browser.jot.example-embed})


  (let [main-namespace 'rx.browser.jot.example-embed
        safe-main-namespace (-> main-namespace
                                str
                                (str/replace #"\." "-"))
        root-path (dev-path
                    {:main-namespace main-namespace})
        source-paths ["src/cljs"
                      "src/cljs-browser"
                      "src/cljc"
                      "checkouts/nsfw/src/cljs"
                      "checkouts/nsfw/src/cljc"]]
    (fapi/start
      {:mode :repl
       :open-url false
       :ring-server-options {:port 44204}
       :css-dirs [(str root-path "/css")]
       :client-log-level :finest
       :validate-config true
       :rebel-readline false}
      {:id "test"
       :options
       {:output-to (str root-path "/js/jot-react-native-embed.js")
        :output-dir (str root-path "/js")
        :optimizations :none
        :source-map true
        :main (str main-namespace)
        :asset-path "js"
        :parallel-build true
        :foreign-libs
        [{:file "resources/dexie/dexie.min.js"
          :file-min "resources/dexie/dexie.min.js"
          :provides ["dexie"]}]}}))

  (fapi/cljs-repl "test")

  (fapi/stop "test")

  (stop-http-server @!http-server)

  (stop-figwheel @!figwheel-server)

  )
