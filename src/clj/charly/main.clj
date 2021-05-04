(ns charly.main
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [charly.config :as config]
            [charly.compiler :as cmp]
            [charly.watch :as watch]
            [charly.cli :as cli]
            [figwheel.main.api :as fapi]))

(defn start-dev! [& [{:keys [config-path]}]]
  (let [config (cli/read-config (or config-path "./"))]
    (when-not (anom/? config)
      (let [env (config/config->env config)]
        (cli/compile-dev env)
        (watch/start-watchers! env)
        (cli/start-http-server! env)
        (cli/start-figwheel-server! env)))))

(defn build-prod! [& [{:keys [config-path]}]]
  (let [config (cli/read-config (or config-path "./"))]
    (when-not (anom/? config)
      (let [env (config/config->env config)]
        (cli/compile-prod env)))))

(defn cljs-repl []
  (fapi/cljs-repl "charly-cljs"))

(comment

  (cljs-repl)

  (start-dev! {:config-path "./resources/charly/test_site/charly.edn"})

  (build-prod! {:config-path "./resources/charly/test_site/charly.edn"})

  (start! {:config-path "./resources/charly/test_site/charly_error.edn"})

  (ks/spy (config/config->env (read-config "./resources/charly/test_site/charly.edn")))
  (read-config "./resources/charly/test_site/charly_error.edn")

  )

(comment

  )
