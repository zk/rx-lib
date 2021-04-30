(ns rx.browser.electron-build-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.styleguide :as sg]
            [rx.browser.forms :as forms]
            [rx.browser.buttons :as bts]
            [rx.theme :as th]))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/n-d-ca-1858/hindu-islamic-buildings-768.jpg"
             :height 300}}
    [:h1 {:id "electron-build"} "Electron Build"]
    [:p "Automated packaging of multiple electron apps from a single codebase."]]
   [sg/section
    [:h2 {:id "electron-build-usage"} "Usage"]
    [:p "Require " [:code "rx.electron.build"]]
    [sg/code-block
     (str
       (ks/pp-str
         '(r.e.b/build
            {:output-path "target/prod/totebook"
             :package-json-path "./electron/package.json"
             :app-id "com.heyzk.totebook"
             :app-description "Your second brain"
             :app-title "Totebook"
             :app-icon-path "./resources/totebook/icon.png"
             :main-namespace "totebook.entry"
             :node-modules-path "./electron/node_modules"
             :main-js-path "./electron/main.js"
             :css-rules [[:body {:background-color "red"}]]}))
       "\n"
       ";; Output\n"
       "* Compiling Clojurescript\n"
       "* Writing Static Assets (package.json, index.html, main.js, etc)\n"
       "* Packaging electron app\n"
       "✔ Done (209s)\n")]]
   [sg/section
    [:h2 {:id "electron-build-spec"} "Build Spec"]
    [:p "Configuration of the build is specified via a map passed to the " [:code "build"] " function."]
    [sg/options-list
     {:options
      [[:output-path "Build directory. CLJS and static assets will be placed in this directory to be compiled into the packaged app(s). May be deleted and recreated as needed by the build system so don't put anything important here."]
       [:package-json-path "package.json which is used as a base. Properties not generated via the build code will be passed through from this file."]
       [:app-id "Electron appId"]
       [:app-description "Electron app description"]
       [:app-title "Title used both in the electron config and as the window title"]
       [:app-icon-path "Path to app icon. 512x512 png."]
       [:main-namespace "Main namespace passed to cljs compiler. Entry point into your js."]
       [:node-modules-path "Path to app's node modules, symlinked to during build"]
       [:main-js-path "Path to main.js, copied to build directory"]
       [:css-rules "Garden rules to generate the resulting css file"]]}]]
   [sg/section
    [:h2 {:id "electron-build-future"} "Future Work"]
    [:p "Packaging and uploading somewhere for posterity. Github, perhaps. Git hash bundling, disallow packaging if not checked in."]]])


