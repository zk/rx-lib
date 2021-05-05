(ns charly.main
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [charly.config :as config]
            [charly.compiler :as cmp]
            [charly.watch :as watch]
            [charly.cli :as cli]
            [figwheel.main.api :as fapi]
            [clojure.tools.cli :as tcli]
            [jansi-clj.core :refer [red bold]]))

(defn start-dev! [& [{config-path :config
                      :keys [disable-nrepl verbose]}]]
  (let [config (cli/read-config (or config-path "./charly.edn"))]
    (when-not (anom/? config)
      (let [env (config/config->env config)]
        (when verbose
          (ks/spy "ENV" env))
        (cli/compile-dev env)
        (watch/start-watchers! env)
        (cli/start-http-server! env)
        (cli/start-figwheel-server! env)
        (when-not disable-nrepl
          (cli/start-nrepl-server! env))))))

(defn build-prod! [& [{config-path :config :keys [verbose]}]]
  (println "\n")
  (println "Generating production build")
  (let [config (cli/read-config (or config-path "./charly.edn"))]
    (when-not (anom/? config)
      (let [env (config/config->env config)]
        (when verbose
          (ks/spy "ENV" env))
        (cli/compile-prod env))))
  (println "*** Done generating production build"))

(defn cljs-repl []
  (fapi/cljs-repl "charly-cljs"))

(def cli-options
  [["-c" "--config CONFIG_PATH" "Path to charly config"]
   ["-n" "--disable-nrepl" "Disable built int nrepl server"]
   ["-d" "--dev" "Start dev"]
   ["-b" "--build" "Build prod to build/prod"]
   ["-v" "--verbose" "Print debug info to stdout"]
   ["-h" "--help" "Show this usage description"]])

(defn -main [& args]
  (let [{:keys [options summary errors] :as opts}
        (tcli/parse-opts args cli-options)

        {:keys [config help dev prod build]} options]
    (cond
      help (do
             (println "Charly CLI")
             (println summary))
      dev (start-dev! options)
      build (build-prod! options)
      errors
      (doseq [s errors]
        (println (red (bold s))))
      :else (println "Please provide one of --dev or --build"))))


(comment

  (-main "-c" "./charly.edn")
  (-main "-h")
  (-main "--asdfasdf")

  (-main "--dev" "--disable-nrepl")

  (-main "--build")

  (cljs-repl)

  (start-dev! {:config-path "charly.edn"})

  (build-prod! {:config-path "charly.edn"})

  (start! {:config-path "./resources/charly/test_site/charly_error.edn"})

  (ks/spy (config/config->env (read-config "./resources/charly/test_site/charly.edn")))
  (read-config "./resources/charly/test_site/charly_error.edn"))


(comment

  )
