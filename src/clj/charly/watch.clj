(ns charly.watch
  (:require [rx.kitchen-sink :as ks]
            [charly.compiler :as c]
            [clojure.java.io :as io]
            [hawk.core :as hawk]))

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

(defonce !watcher (atom nil))

(defn start-watch-static! [env]
  (when @!watcher
    (hawk/stop! @!watcher))
  
  (reset! !watcher
    (start-watch-static env)))

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
