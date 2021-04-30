(ns rx.jvm.ops.electron-dev
  (:require [rx.jvm.ops.dev :as dev]))

(def config
  {:id "rx-electron"
   :target-path "target/rx-electron/dev/cljs"
   :main-namespace "rx.dev.electron"
   :watch-dirs ["src/cljs-electron"
                "src/cljs-node"
                "src/cljs"
                "src/cljs-browser"
                "src/cljc"]
   :asset-path "cljs"
   :figwheel {:final-output-to "target/rx-electron/dev/cljs/app.js"
              :auto-bundle :webpack
              :bundle-freq :smart
              :pprint-config false
              :clean-outputs true
              :hot-reload-cljs false}
   :compiler
   {:output-to "target/rx-electron/dev/cljs/bundle_source.js"
    :recompile-dependents true
    :parallel-build true
    :target :bundle
    :hashbang false
    :infer-externs true
    :foreign-libs
    [{:file "resources/dexie/dexie.min.js"
      :file-min "resources/dexie/dexie.min.js"
      :provides ["dexie"]}
     {:file "resources/threejs/three.js"
      :file-min "resources/threejs/three.min.js"
      :provides ["threejs"]}
     {:file "resources/pixijs/pixi.js"
      :file-min "resources/pixijs/pixi.min.js"
      :provides ["pixijs"]}
     {:file "resources/tonejs/Tone.js"
      :file-min "resources/tonejs/Tone.js"
      :provides ["tonejs"]}
     {:file "resources/pinchzoomjs/pinch-zoom.min.js"
      :file-min "resources/pinchzoomjs/pinch-zoom.min.js"
      :provides ["pinch-zoom"]}
     {:file "resources/pdfjs/pdfjs-2.6.347.js"
      :file-min "resources/pdfjs/pdf-2.6.347.min.js"
      :provides ["pdfjs"]}]}})

(defn start! []
  (dev/start-dev! config))

(comment

  (start!)
  
  )
