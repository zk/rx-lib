(ns charly.compiler
  (:require
   [rx.kitchen-sink :as ks]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [figwheel.main.api :as fapi]
   [garden.core :as gc]
   [cljs.build.api :as bapi])
  (:refer-clojure :exclude [compile]))

(defn concat-paths [parts]
  (->> parts
       (remove nil?)
       (map-indexed
         (fn [i s]
           (cond
             (= i (max 0 (dec (count parts)))) s
             (str/ends-with? s "/") s
             :else (str s "/"))))
       (apply str)))

;; CSS

(defn resolve-rules [sym]
  (let [ns-sym (symbol (namespace sym))]
    (use ns-sym :reload-all)
    ((resolve sym))))

(defn write-css-out [http-root-path
                     {:keys [output-to]
                      rules-fn-sym :rules-fn}
                     garden-opts]
  
  (let [output-path (concat-paths [http-root-path output-to])]
    (println "Writing css out:" output-path)
    (io/make-parents (io/as-file output-path))
    (gc/css
      (ks/deep-merge
        {:output-to output-path
         :pretty-print? true
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
      (resolve-rules rules-fn-sym))))

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

(defn clean-target-dir [{:keys [clean-target-dir? target-dir]}]
  (when clean-target-dir?
    (clean-dir target-dir)))

(defn clean-prod-dir [{:keys [clean-prod-dir? prod-target-dir]}]
  (when clean-prod-dir?
    (clean-dir prod-target-dir)))

(defn compile
  "Takes an environment map and outputs a charly site to the target directory"
  [env]
  (clean-target-dir env)
  (copy-directories env)
  (copy-files env)
  (gen-files env)
  (compile-css env))

(defn compile-prod [env]
  (clean-prod-dir env)
  (copy-prod-directories env)
  (copy-prod-files env)
  (gen-prod-files env)
  (compile-prod-css env)
  (compile-prod-cljs env)
  (println "Done compiling"))
