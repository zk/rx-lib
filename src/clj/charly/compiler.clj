(ns charly.compiler
  (:require
   [rx.kitchen-sink :as ks]
   [reitit.core :as rei]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [figwheel.main.api :as fapi]
   [garden.core :as gc]
   [cljs.build.api :as bapi]
   [rum.server-render :as sr])
  (:refer-clojure :exclude [compile]))

(defn concat-paths [parts]
  (str/replace
    (->> parts
         (interpose "/")
         (apply str))
    #"/+" "/"))

;; CSS

(defn resolve-rules [sym]
  (let [ns-sym (symbol (namespace sym))]
    (use ns-sym :reload-all)
    ((resolve sym))))

(defn write-css-out [output-to
                     {:keys [path rules-fn]}
                     garden-opts
                     env]

  (let [output-path (concat-paths [output-to "css" path])]
    (io/make-parents (io/as-file output-path))
    (spit
      output-path
      (gc/css
        (ks/deep-merge
          {:pretty-print? true
           :vendors ["webkit" "moz" "ms"]
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
          garden-opts)
        (rules-fn env)))
    output-path))

(defn compile-css [{:keys [http-root-path]
                    css-spec :css}]
  (doseq [out (:outs css-spec)]
    (write-css-out
      http-root-path
      out
      (:garden css-spec))))

(defn compile-prod-css [{:keys [prod-target-dir]
                         css-spec :css}]
  (doseq [out (:outs css-spec)]
    (write-css-out
      prod-target-dir
      out
      (:garden css-spec))))

;; ClojureScript

(defn stop-figwheel-server! [{:keys [id]}]
  (let [id (or id "charly-cljs")]
    (try
      (fapi/stop id)
      (catch Exception e
        #_(ks/pn "Server already stopped")))))

(defn watch-dirs [{:keys [project-root]}]
  (->> [["src" "cljs"]
        ["src" "cljc"]]
       (map (fn [parts]
              (concat-paths
                (concat
                  [project-root]
                  parts))))))

(defn figwheel-opts [{:keys [cljs opts project-root target-dir] :as env}]
  (let [watch-dirs (watch-dirs env)]
    (ks/deep-merge
      {:mode :serve
       :open-url false
       :ring-server-options {:port 5001}
       :watch-dirs watch-dirs
       :validate-config true
       :rebel-readline false
       :launch-node false
       :hot-reload-cljs false
       :css-dirs [(concat-paths [target-dir "css"])]}
      (:figwheel cljs))))

(defn figwheel-compiler-opts [{:keys [cljs target-dir] :as opts}]
  (ks/deep-merge
    {:output-to (concat-paths
                  [target-dir "cljs" "app.js"])
     :output-dir (concat-paths
                  [target-dir "cljs"])
     :optimizations :none
     :source-map true
     :parallel-build true
     :asset-path "/cljs"}
    (:compiler cljs)))

(defn compile-prod-cljs [{:keys [prod-target-dir cljs-build-dir cljs]
                          :as env}]
  (println "Compiling cljs...")
  (bapi/build
    (apply bapi/inputs (watch-dirs env))
    (ks/deep-merge
      {:output-to (concat-paths
                    [cljs-build-dir "app.js"])
       :output-dir cljs-build-dir
       :optimizations :advanced
       :source-map (concat-paths
                     [cljs-build-dir "app.js.map"])
       :parallel-build true
       :asset-path "/cljs"}
      (:compiler cljs)))

  (io/make-parents
    (io/as-file
      (concat-paths
        [prod-target-dir "cljs" "app.js"])))

  (io/copy
    (io/as-file
      (concat-paths
        [cljs-build-dir "app.js"]))
    (io/as-file
      (concat-paths
        [prod-target-dir "cljs" "app.js"])))

  (io/copy
    (io/as-file
      (concat-paths
        [cljs-build-dir "app.js.map"]))
    (io/as-file
      (concat-paths
        [cljs-build-dir "app.js.map"]))))

(defn start-figwheel-server! [{:keys [cljs id] :as opts}]
  (let [id (or id "charly-cljs")]
    (stop-figwheel-server! cljs)
    (fapi/start
      (figwheel-opts opts)
      {:id id
       :options (figwheel-compiler-opts opts)})))



;; Static

(defn create-directory [path]
  (.mkdirs (io/file path)))

(defn copy-dir [{:keys [src dst]} target-dir]
  (let [dst-dir (concat-paths [target-dir dst])
        files (->> src
                   io/as-file
                   file-seq
                   (filter #(not (.isDirectory %)))
                   (filter #(.exists %)))]
    (println "Copying" src "to" dst-dir)
    (create-directory dst-dir)
    (doseq [file files]
      (let [to-file (io/as-file
                      (concat-paths
                        [dst-dir (.getName file)]))]
        (io/make-parents to-file)
        (io/copy file to-file)
        (println " " (.getPath to-file))))))

(defn copy-dirs [env to-dir]
  (let [{:keys [copy-dirs]} env]
    (doseq [dir copy-dirs]
      (try
        (copy-dir dir to-dir)
        (catch Exception e
          (println "Error copying directory" dir)
          (prn e))))))

(defn copy-directories [env]
  (let [{:keys [target-dir]} env]
    (copy-dirs env target-dir)))

(defn copy-prod-directories [env]
  (let [{:keys [prod-target-dir]} env]
    (copy-dirs env prod-target-dir)))

(defn copy-file [{:keys [src dst]} target-dir]
  (let [src-file (io/as-file src)
        dst-path (concat-paths [target-dir dst])
        dst-file (io/as-file dst-path)]
    (try
      (io/make-parents dst-file)
      (io/copy src-file dst-file)
      (println "Copying" src "to" (.getPath dst-file))
      (catch Exception e
        (println "! Error copying file. SOURCE"
          (.getPath src-file)
          "DEST"
          (.getPath dst-file))
        (throw e)))))

(defn copy-files [env]
  (let [{:keys [target-dir copy-files]} env]
    (doseq [file copy-files]
      (copy-file file target-dir))))

(defn copy-prod-files [env]
  (let [{:keys [prod-target-dir copy-files]} env]
    (doseq [file copy-files]
      (copy-file file prod-target-dir))))

(defn -gen-files [env target-dir]
  (let [{:keys [gen-files gen-context]} env]
    (doseq [{:keys [gen dst]} gen-files]
      (let [dst-file (io/as-file
                       (concat-paths
                         [target-dir dst]))
            out-str (gen gen-context)]
        (io/make-parents (io/as-file dst-file))
        (spit dst-file out-str)
        (println "Output generated file to" (.getPath dst-file))))))

(defn gen-files [env]
  (-gen-files env (:target-dir env)))

(defn gen-prod-files [env]
  (-gen-files env (:prod-target-dir env)))

(defn clean-dir [path]
  (when (= "/" path)
    (ks/throw-str "Can't clean root directory, path:" path))
  (println "Cleaning target dir" path)
  (sh/sh "rm" "-rf" path))

(defn clean-prod-dir [{:keys [clean-prod-dir? prod-target-dir]}]
  (when clean-prod-dir?
    (clean-dir prod-target-dir)))

(defn to-file [from-file static-path output-path]
  (-> from-file
      (#(.getPath %))
      (str/replace-first static-path "")
      (#(concat-paths
          [output-path %]))
      io/as-file))

(defn copy-static-file [{:keys [from-file to-file]}]
  (io/make-parents to-file)
  (io/copy from-file to-file))

(defn copy-static [{:keys [static-path] :as env} output-path]
  (let [files (->> static-path
                   io/as-file
                   file-seq
                   (filter #(.isFile %))
                   (map (fn [from-file]
                          {:from-file from-file
                           :to-file (to-file from-file static-path output-path)})))]
    (doseq [copy-spec files]
      (copy-static-file copy-spec))
    files))

(defn spa-template [env]
  (str
    "<!DOCTYPE html>\n"
    (sr/render-static-markup
      [:html
       {:style {:width "100%"
                :height "100%"}}
       (into
         [:head
          [:meta {:http-equiv "content-type"
                  :content "text/html"
                  :charset "UTF8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:link {:rel "stylesheet"
                  :href (str "/css/app.css?" (ks/now))}]
          [:link {:rel "preconnect"
                  :href "https://fonts.gstatic.com"}]
          [:link {:rel "stylesheet"
                  :href "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&family=Sora:wght@400;500;600;700&display=swap"}]])
       [:body {:style {:width "100%"
                       :height "100%"}}
        [:div {:id "rx-root-mount-point"
               :style {:width "100%"
                       :height "100%"
                       :display 'flex}}]
        [:script {:src (str "/cljs/app.js?" (ks/now))}]]])))

(defn generate-routes [{:keys [routes-fn] :as env} output-dir]
  (when routes-fn
    (let [routes (rei/routes
                   (rei/router
                     (routes-fn env)))
          specs (->> routes
                     (map (fn [[uri-path {:keys [name template]} :as route]]
                            (let [file-path (str (if (= "/" uri-path)
                                                   "index"
                                                   (str/replace uri-path #":" "__cln__"))
                                                 ".html")]
                              {:route route
                               :template (or template spa-template)
                               :output-path (concat-paths
                                              [output-dir file-path])}))))]
      (doseq [{:keys [template output-path]} specs]
        (io/make-parents (io/as-file output-path))
        (spit output-path (template env)))
      specs)))

(defn generate-css [{:keys [css-preamble-fq css-files] :as env} output-to minify?]
  (->> css-files
       (map (fn [out]
              (write-css-out
                output-to
                out
                {:preamble css-preamble-fq
                 :pretty-print? (not minify?)}
                env)))
       doall))

(defn compile
  "Takes an environment map and outputs a charly site to the target directory"
  [env output-path]
  (copy-static env output-path)
  #_(gen-files env)
  #_(compile-css env))

(defn compile-prod [env]
  (copy-prod-directories env)
  (copy-prod-files env)
  (gen-prod-files env)
  (compile-prod-css env)
  (compile-prod-cljs env)
  (println "Done compiling"))
