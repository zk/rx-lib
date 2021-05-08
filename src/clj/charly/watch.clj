(ns charly.watch
  (:require [rx.kitchen-sink :as ks]
            [charly.compiler :as c]
            [charly.config :as config]
            [charly.cli :as cli]
            [clojure.java.io :as io]
            [hawk.core :as hawk]
            [clojure.tools.namespace.repl :as repl]))


(defn create-directory [path]
  (.mkdirs (io/file path)))

(defn css-watchers [env]
  (let [{:keys [project-root]} env]
    (->> env
         :css
         :outs
         (map (fn [{:keys [watch-paths] :as out}]
                {:paths (->> watch-paths
                             (map (fn [watch-path]
                                    (c/concat-paths
                                      [project-root watch-path]))))
                 :handler (fn [ctx {:keys [kind file] :as action}]
                            (try
                              (repl/refresh)
                              (c/write-css-out
                                (:http-root-path env)
                                out
                                (-> env :css :garden))
                              (catch Exception e
                                (println "Exception handling css compile" (pr-str action))
                                (prn e))))})))))

(defn start-watch-static [env]
  (let [{:keys [target-dir copy-dirs]} env]
    (doseq [{:keys [src]} copy-dirs]
      (create-directory src))
    (hawk/watch!
      {:watcher :polling}
      (concat
        (->> env
             :copy-dirs
             (map (fn [{:keys [src dst] :as dir}]
                    {:paths [src]
                     :handler (fn [ctx {:keys [kind file] :as action}]
                                (try
                                  (condp = kind
                                    :create (c/copy-dir dir target-dir)
                                    :modify (c/copy-dir dir target-dir)
                                    :delete (do
                                              (let [dst-file (io/as-file
                                                               (c/concat-paths
                                                                 [target-dir dst (.getName file)]))]
                                                (io/delete-file dst-file))))
                                  (catch Exception e
                                    (println "Exception handling filesystem change" (pr-str action))
                                    (prn e))))})))
        (->> env
             :copy-files
             (map (fn [{:keys [src dst] :as fl}]
                    {:paths [src]
                     :handler (fn [ctx {:keys [kind file] :as action}]
                                (try
                                  (condp = kind
                                    :create (c/copy-file fl target-dir)
                                    :modify (c/copy-file fl target-dir)
                                    :delete (do
                                              (let [dst-file (io/as-file
                                                               (c/concat-paths
                                                                 [target-dir dst (.getName file)]))]
                                                (io/delete-file dst-file))))
                                  (catch Exception e
                                    (println "Exception handling filesystem change" (pr-str action))
                                    (prn e))))})))
        (css-watchers env)))))

(defn static-dirs [{:keys [project-root dev-output-path]}]
  (let [static-path (c/concat-paths
                      [project-root "static"])]
    [{:paths [static-path]
      :handler (fn [ctx {:keys [kind file] :as action}]
                 (let [fq-static-path (c/concat-paths
                                        [(System/getProperty "user.dir") static-path])
                       to-file (c/to-file
                                 file
                                 fq-static-path
                                 dev-output-path)]
                   (when (or (.isFile file) (= :delete kind))
                     (let [copy-spec {:from-file file
                                      :to-file to-file}]
                       (try
                         (if (get #{:create :modify} kind)
                           (println "File changed, updating" (.getPath to-file))
                           (println "File deleted, removing" (.getPath to-file)))
                         (condp = kind
                           :create (c/copy-static-file copy-spec)
                           :modify (c/copy-static-file copy-spec)
                           :delete (io/delete-file (:to-file copy-spec)))
                         (catch Exception e
                           (println "Exception handling filesystem change" (pr-str action))
                           (prn e)))))))}]))

(defn config-file [{:keys [project-root] :as env}]
  (let [config-file-path (c/concat-paths
                           [project-root "charly.edn"])
        !last-env (atom env)]
    [{:paths [config-file-path]
      :handler (fn [ctx {:keys [kind file] :as action}]
                 (try
                   (let [last-env @!last-env
                         next-env (config/config->env
                                    (merge
                                      (ks/edn-read-string (slurp file))
                                      {:runtime-env :dev}))
                         _ (reset! !last-env next-env)]
                     (cli/compile-dev next-env))
                   (catch Exception e
                     (println "Exception handling filesystem change" (pr-str action))
                     (prn e))))}]))

(defn css [{:keys [project-root dev-output-path css-preamble-fq] :as env}]
  (let []
    (->> env
         :css-files
         (map
           (fn [{:keys [rules-path] :as css-spec}]
             (let [path (c/concat-paths
                          [project-root rules-path])]
               {:paths [path]
                :handler (fn [ctx {:keys [kind file] :as action}]
                           (try
                             (println "CSS changed, writing"
                               (c/write-css-out
                                 dev-output-path
                                 (merge
                                   css-spec
                                   {:rules-fn (config/resolve-sym (:rules css-spec))})
                                 {:preamble css-preamble-fq}
                                 env))
                             (catch Exception e
                               (println "Exception handling css compile" (pr-str action))
                               (prn e))))}))))))

(defn routes [{:keys [routes-file-paths project-root]}]
  (when routes-file-paths
    [{:paths routes-file-paths
      :handler (fn [ctx {:keys [kind file] :as action}]
                 (try
                   (let [config-file-path (c/concat-paths
                                            [project-root "charly.edn"])
                         env (config/config->env
                               (merge
                                 (ks/edn-read-string (slurp config-file-path))
                                 {:runtime-env :dev}))]
                     (cli/compile-dev env))
                   (catch Exception e
                     (println "Exception handling filesystem change" (pr-str action))
                     (prn e))))}]))

(defn start-watchers [env]
  (hawk/watch!
    {:watcher :polling}
    (concat
      (static-dirs env)
      (config-file env)
      (css env)
      (routes env))))

(defonce !watcher (atom nil))

(defn start-watch-static! [env]
  (when @!watcher
    (hawk/stop! @!watcher))
  
  (reset! !watcher
    (start-watch-static env)))

(defn start-watchers! [env]
  (when @!watcher
    (hawk/stop! @!watcher))

  (reset! !watcher
    (start-watchers env)))

(defn do-test []
  (start-watch-static!
    {:clean-target-dir? true
     :target-dir "target/charly/prod"
     :copy-dirs [{:src "resources/charly/demo-site/html"
                  :dst "html"}
                 {:src "resources/charly/demo-site/css"
                  :dst "css"}]
     :copy-files [{:src "resources/charly/demo-site/test-file.json"
                   :dst "test-file.json"}]
     :gen-context {:foo "bar"}
     :gen-files [{:gen (fn [ctx]
                         (str
                           "<!DOCTYPE html><html><head><title>gen title</title></head><body>"
                           (str "<pre>" (pr-str ctx) "</pre>")
                           "</body></html>"))
                  :dst "html/generated.html"}
                 {:gen (fn [ctx]
                         "body {background-color: green;}")
                  :dst "css/generated.css"}]}))

(comment

  (.getParent (io/as-file "resources/charly/demo-site/test-file.json"))

  (do-test)

  )

