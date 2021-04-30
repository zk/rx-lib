(ns rx.jvm.ops.browser-dev
  (:require [rx.jvm.ops.dev :as dev]
            [garden.core :as garden]
            [rx.jot.css]
            [nrepl.cmdline :as ncmd]
            [cider.nrepl]
            [cider.piggieback]))

(def config
  {:id "rx-browser"
   :target-path "target/rx-browser/dev/cljs"
   :http-port 5000
   :http-root-path "target/rx-browser/dev"
   :watch-dirs ["src/cljc"
                "src/cljs"
                "src/cljs-browser"]
   :asset-path "/cljs"
   :figwheel {:pprint-config false
              :clean-outputs true
              :hot-reload-cljs false}
   :compiler
   {:output-to "target/rx-browser/dev/cljs/app.js"
    :recompile-dependents true
    :parallel-build true
    :hashbang false
    :foreign-libs
    [{:file "resources/dexie/dexie.min.js"
      :file-min "resources/dexie/dexie.min.js"
      :provides ["dexie"]}
     {:file "resources/tonejs/Tone.js"
      :file-min "resources/tonejs/Tone.js"
      :provides ["tonejs"]}
     {:file "resources/pdfjs/pdfjs-2.6.347.js"
      :file-min "resources/pdfjs/pdf-2.6.347.min.js"
      :provides ["pdfjs"]}
     {:file "resources/onlycrayons/aws-sdk/aws-sdk.min.js"
      :file-min "resources/onlycrayons/aws-sdk/aws-sdk.min.js"
      :provides ["aws-sdk"]}]}

   #_#_:html [{:render ss/index-html
               :path "index.html"}]
   #_#_:css [{:render ss/garden-rules
              :path "css/app.css"}]
   :output-path "target/rx-browser/dev"
   :main-namespace 'rx.browser.dev-entry})

(defn compile-css []
  (.mkdirs (java.io.File. "target/rx-browser/dev/css"))
  (garden/css
    (merge
      {:output-to "target/rx-browser/dev/css/app.css"
       :pretty-print? false
       :vendors ["webkit" "moz" "ms"]
       :preamble ["resources/public/css/bootstrap-reboot.min.css"
                  "resources/public/css/bootstrap-grid.min.css"
                  "resources/rx-css/rx-css-reset.css"]
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
                      :appearance}})
    (concat
      (rx.jot.css/rules))))

(defn start! []
  (compile-css)
  (dev/start-dev! config))

(defn stop! []
  (dev/stop-dev! config))

(comment

  (start!)

  )

