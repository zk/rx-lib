(ns rx.jvm.ops.electron-build
  (:require [rx.kitchen-sink :as ks]
            [rx.jvm.ops.build :as build]
            [cljs.build.api :as bapi]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [rx.static-site-gen :as site-gen]
            [garden.core :as garden]
            [clojure.java.shell :as sh]
            [rx.jot.css :as jot-css]
            #_[zel.version])
  (:import [java.nio.file Path Paths Files]
           [java.nio.file.attribute FileAttribute]))

;; https://twitterarchiveeraser.medium.com/notarize-electron-apps-7a5f988406db

(defn notarize-after-hook-js-template [{:keys [::app-id]}]
  (str
    "// See: https://medium.com/@TwitterArchiveEraser/notarize-electron-apps-7a5f988406db

const fs = require('fs');
const path = require('path');
var electron_notarize = require('electron-notarize');

module.exports = async function (params) {
    // Only notarize the app on Mac OS only.
    if (process.platform !== 'darwin') {
        return;
    }
    console.log('afterSign hook triggered', params);

    // Same appId in electron-builder.
    let appId = '"
    app-id
    "'

    let appPath = path.join(params.appOutDir, `${params.packager.appInfo.productFilename}.app`);
    if (!fs.existsSync(appPath)) {
        throw new Error(`Cannot find application at: ${appPath}`);
    }

    console.log(`Notarizing ${appId} found at ${appPath}`);

    try {
        await electron_notarize.notarize({
            appBundleId: appId,
            appPath: appPath,
            appleId: process.env.NOTARIZE_APPLE_ID,
            appleIdPassword: process.env.NOTARIZE_APPLE_PASSWORD
        });
    } catch (error) {
        console.error(error);
    }

    console.log(`Done notarizing ${appId}`);
};"))

(defn html-template [{:keys [::app-title
                             ::main-namespace]}]
  (let [gen-ts (ks/now)]
    [:html
     {:style {:width "100%" :height "100%"}}
     [:head
      [:meta {:charset "utf-8"}]
      [:title app-title]
      [:link {:rel "stylesheet" :href "https://api.tiles.mapbox.com/mapbox-gl-js/v1.3.1/mapbox-gl.css"}]
      [:link {:rel "stylesheet" :href "https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css"}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Roboto+Condensed:300,400,700|Roboto:100,300,400,500,700|Inconsolata:400,700|Cardo:400,400i,700"}]
      [:link {:rel "stylesheet" :href "/css/app.css"}]
      [:script {:src "https://api.tiles.mapbox.com/mapbox-gl-js/v1.3.1/mapbox-gl.js"}]]
     [:body
      {:style {:width "100%" :height "100%"}}
      [:div {:id "rx-root-mount-point"
             :style {:width "100%" :height "100%" :display "flex"}}]
      [:script {:src (str "/cljs/app.js?" gen-ts)}]
      [:script (str
                 (str/replace
                   (str main-namespace)
                   "-" "_")
                 ".init();")]]]))

(defn gen-index-html [schema]
  (site-gen/render-page
    (html-template schema)))

(defn read-package-json [{:keys [::package-json-path]}]
  (slurp package-json-path))

(defn schema-package-json-entries [{:keys [::output-path
                                           ::app-id
                                           ::app-description
                                           ::app-title
                                           ::version
                                           ::publish-github
                                           ::mac-entitlements-path]}]
  (merge
    {:description app-description
     :name app-title
     :version version
     :scripts
     {:postinstall "electron-builder install-app-deps"}
     :build (merge
              {:appId app-id
               :files ["main.js"
                       "index.html"
                       "node_modules/**/*"
                       "cljs/**/*"
                       "css/**/*"]
               :mac {:publish (remove nil?
                                [(when publish-github
                                   "github")])}}
              (when mac-entitlements-path
                #_{:afterSign "./build/afterSignHook.js"}))}
    (when publish-github
      {:repository (:repository publish-github)
       :publish {:provider "github"
                 :releaseType "release"}})
    (when mac-entitlements-path
      {:mac {:hardenedRuntime true
             :entitlements "./build/entitlements.plist"}})))

(defn gen-package-json [{:keys [::output-path] :as schema}]
  (let [disk-package-json-str (read-package-json schema)]
    (ks/to-json
      (merge-with ks/deep-merge
        (ks/from-json disk-package-json-str)
        (schema-package-json-entries schema)))))

(defn compile-cljs [{:keys [::output-path
                            ::main-namespace
                            ::source-paths
                            ::compiler]
                     :as schema}]
  (bapi/build
    (apply bapi/inputs source-paths)
    (merge
      {:output-to (str output-path "/cljs/bundle_source.js")
       :output-dir (str output-path "/cljs")
       :source-map true #_(str output-path "/cljs/app.js.map")
       :optimizations :none
       :main (str main-namespace)
       :target :bundle
       :bundle-cmd {:none ["npx" "webpack"
                           (str output-path "/cljs/bundle_source.js")
                           "-o"
                           (str output-path "/cljs/app.js")
                           "--mode=development"]
                    :default ["npx" "webpack"
                              (str output-path "/cljs/bundle_source.js")
                              "-o"
                              (str output-path "/cljs/app.js")
                              "--mode=development"]}
       :closure-defines {'cljs.core/*global* "window"}
       :asset-path "cljs"
       :parallel-build true
       ;;:pseudo-names true
       ;;:pretty-print true
       :foreign-libs
       [{:file "resources/dexie/dexie.min.js"
         :file-min "resources/dexie/dexie.min.js"
         :provides ["dexie"]}
        {:file "resources/tonejs/Tone.js"
         :file-min "resources/tonejs/Tone.js"
         :provides ["tonejs"]}
        {:file "resources/pinchzoomjs/pinch-zoom.min.js"
         :file-min "resources/pinchzoomjs/pinch-zoom.min.js"
         :provides ["pinch-zoom"]}
        {:file "resources/pdfjs/pdfjs-2.6.347.js"
      :file-min "resources/pdfjs/pdf-2.6.347.min.js"
      :provides ["pdfjs"]}]
       :externs ["resources/dexie/dexie.ext.js"
                 "resources/tonejs/Tone.ext.js"]}
      compiler)))

(defn package-json-path [{:keys [::output-path]}]
  (str output-path "/package.json"))

(defn index-html-path [{:keys [::output-path]}]
  (str output-path "/index.html"))

(defn compile-css [{:keys [::css-rules]}]
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
    css-rules))

(defn namespace-opts [ns opts]
  (->> opts
       (map (fn [[k v]]
              [(keyword
                  (name ns)
                  (name k))
               v]))
       (into {})))

(defn build [opts]
  (let [{:keys [::output-path
                ::node-modules-path
                ::app-icon-path
                ::main-js-path
                ::mac-entitlements-path
                ::publish?]
         :as schema}
        (namespace-opts 'rx.jvm.ops.electron-build opts)

        start (ks/now)]

    (when-not (.exists (io/file app-icon-path))
      (ks/throw-anom
        {:code :not-found
         :desc "app-icon-path doesn't exist"}))

    (when-not (.exists (io/file main-js-path))
      (ks/throw-anom
        {:code :not-found
         :desc "main-js-path doesn't exist"}))

    (when (or (not (.exists (io/file node-modules-path)))
              (not (.isDirectory (io/file node-modules-path))))
      (ks/throw-anom
        {:code :incorrect
         :desc "node-modules-path doesn't exist or is not a directory"}))


    (try
      (when (and output-path
                 (.startsWith output-path "target"))
        (sh/sh "rm" "-rf" output-path))
      (catch Exception e
        (ks/pn
          "Error removing prod")))
    
    (ks/pn "* Compiling Clojurescript")

    (compile-cljs schema)

    (ks/pn "* Writing Static Assets (package.json, index.html, main.js, etc)")
    
    (spit
      (package-json-path schema)
      (gen-package-json schema))
    
    (spit
      (index-html-path schema)
      (gen-index-html schema))

    (.mkdirs
      (io/as-file
        (str output-path "/css")))

    (spit
      (str output-path "/css/app.css")
      (compile-css schema))

    (build/create-symlink
      node-modules-path
      (str output-path "/node_modules"))

    (.mkdirs
      (io/as-file
        (str output-path "/build")))
    (io/copy
      (io/as-file
        app-icon-path)
      (io/as-file
        (str output-path "/build/icon.png")))

    (when mac-entitlements-path
      (io/copy
        (io/as-file
          mac-entitlements-path)
        (io/as-file
          (str output-path "/build/entitlements.plist")))

      (spit
        (str output-path "/build/afterSignHook.js")
        (notarize-after-hook-js-template schema)))

    (io/copy
      (io/as-file
        main-js-path)
      (io/as-file
        (str output-path "/main.js")))

    (ks/pn "* Packaging electron app")

    (try
      (prn "YD" output-path
        (sh/with-sh-dir output-path
          (if publish?
            (sh/sh "electron-builder"
              "--publish"
              "always"
              :env (merge
                     (into {} (System/getenv))
                     {"GH_TOKEN" (::github-deploy-token schema)}))
            (sh/sh "electron-builder"
              :env (merge
                     (into {} (System/getenv))
                     {"GH_TOKEN" (::github-deploy-token schema)})))))
      (catch Exception e
        (ks/pn
          "!!! Error publishing")
        (ks/pn e)))

    (ks/pn
      "✔ Done "
      (str
        "("
        (ks/round (/ (- (ks/now) start) 1000.0))
        "s):")
      output-path)))

(comment

  (let [spec
        {:output-path "target/prod/totebook"
         :package-json-path "./electron/package.json"
         :app-id "com.heyzk.totebook"
         :app-description "Your second brain"
         :app-title "Totebook"
         :app-icon-path "./resources/totebook/icon.png"
         :main-namespace "totebook.entry"
         :node-modules-path "./electron/node_modules"
         :main-js-path "./electron/main.js"
         :css-rules (jot-css/rules)}])

  (html-template
    (namespace-opts
      'rx.electron.build
      {:output-path "target/prod/totebook"
       :package-json-path "./electron/package.json"
       :app-id "com.heyzk.totebook"
       :app-description "Your second brain"
       :app-title "Totebook"
       :app-icon-path "./resources/totebook/icon.png"
       :main-namespace "totebook.entry"
       :node-modules-path "./electron/node_modules"
       :main-js-path "./electron/main.js"
       :css-rules (jot-css/rules)}))
  
  (build
    {:output-path "target/prod/papers"
     :version "0.3.0"
     :package-json-path "./electron/package.json"
     :app-id "com.heyzk.papers"
     :app-description "Zel"
     :app-title "Zel"
     :app-icon-path "./resources/papers/icon.png"
     :main-namespace "zel.entry"
     :node-modules-path "./electron/node_modules"
     :main-js-path "./electron/main.js"
     :css-rules (jot-css/rules)
     :source-paths
     ["src/cljs-electron"
      "src/cljs-node"
      "src/cljs"
      "src/cljs-browser"
      "src/cljc"]})

  (ks/pp
    (gen-package-json
      {::output-path "target/prod/jelly"
       :version "0.9.0"
       ::package-json-path "./electron/package.json"
       :app-id "com.heyzk.jelly"
       ::app-description "Jelly!"
       :app-title "Jelly"
       :app-icon-path "./resources/jelly/icon.png"
       :main-namespace "zel.entry"
       :node-modules-path "./electron/node_modules"
       :main-js-path "./electron/main.js"
       :css-rules (jot-css/rules)}))

  (ks/pp
    (read-package-json
      {::output-path "target/prod/jelly"
       :version "0.8.0"
       ::package-json-path "./electron/package.json"
       :app-id "com.heyzk.jelly"
       ::app-description "Jelly!"
       :app-title "Jelly"
       :app-icon-path "./resources/jelly/icon.png"
       :main-namespace "zel.entry"
       :node-modules-path "./electron/node_modules"
       :main-js-path "./electron/main.js"
       :css-rules (jot-css/rules)}))

  (sh/with-sh-dir "target/prod/jelly"
    (sh/sh "yarn" "dist"))

  #_(build
      {:output-path "target/prod/papers"
       :version zel.version/version
       :package-json-path "./electron/package.json"
       :app-id "com.heyzk.papers"
       :app-description "Superpaper"
       :app-title "Superpaper"
       :app-icon-path "./resources/papers/icon.png"
       :main-namespace "zel.entry"
       :node-modules-path "./electron/node_modules"
       :main-js-path "./electron/main.js"
       :github-deploy-token
       (-> (slurp "/Users/zk/.zel-deploy.json")
           ks/from-json
           :gh-token)
       :publish-github
       {:repository "https://github.com/zk/zel-releases"
        :draft false}
       :publish? false
       :mac-entitlements-path "./resources/papers/entitlements.plist"
       :css-rules (jot-css/rules)
       :source-paths
       ["src/cljs-electron"
        "src/cljs-node"
        "src/cljs"
        "src/cljs-browser"
        "src/cljc"]})

  )



