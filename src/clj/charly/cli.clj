(ns charly.cli
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [charly.config :as config]
            [charly.compiler :as cmp]
            [charly.dev :as dev]
            [charly.http-server :as hs]
            [jansi-clj.core :refer :all])
  (:refer-clojure :exclude [compile]))

(def ! (red (bold "!")))

(defn read-config [path]
  (let [config (config/read-config path)]
    (when (anom/? config)
      (println (red (bold "[!]")) (str "Error parsing config file: " path))
      (println "   " (::anom/desc config)))
    config))

(defn gen-from-routes [env output-path]
  (println "* Generating html files from routes")
  (let [{:keys [routes-fn routes]} env]
    (if (anom/? routes-fn)
      (println ! "Couldn't resolve routes fn" routes)
      (let [routes-res (cmp/generate-routes env output-path)]
        (if (anom/? routes-res)
          (do
            (println ! "Error generating route html")
            (println "  " routes-res))
          (doseq [{:keys [output-path]} routes-res]
            (println "  Wrote" output-path)))))))

(defn gen-css [env output-path minify?]
  (println "* Generating css")
  (let [paths (cmp/generate-css env output-path minify?)]
    (doseq [path paths]
      (println "  Wrote" path))))

(defn compile-dev [env]
  (let [{:keys [dev-output-path]} env]
    (println "* Copying static files")
    (let [copy-res (cmp/copy-static env dev-output-path)]
      (doseq [{:keys [to-file]} copy-res]
        (println "  Wrote" (.getPath to-file)))
      (gen-from-routes env dev-output-path)
      (gen-css env dev-output-path false))))

(defn compile-prod [env]
  (let [{:keys [prod-output-path]} env]
    (println "* Copying static files")
    (let [copy-res (cmp/copy-static env prod-output-path)]
      (doseq [{:keys [to-file]} copy-res]
        (println "  Wrote" (.getPath to-file)))
      (gen-from-routes env prod-output-path)
      (gen-css env prod-output-path false)
      (cmp/compile-prod-cljs env)
      (cmp/generate-vercel-json env))))

(defn start-http-server! [env]
  (println "* Starting dev server")
  (hs/start-http-server! env))

(defn start-nrepl-server! [env]
  (println "* Starting nrepl server")
  (dev/start-clj-repl env))

(defn start-figwheel-server! [env]
  (cmp/start-figwheel-server! env))
