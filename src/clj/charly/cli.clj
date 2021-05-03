(ns charly.cli
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [charly.config :as config]
            [charly.compiler :as cmp]
            [jansi-clj.core :refer :all]))

(defn read-config [path]
  (let [config (config/read-config path)]
    (when (anom/? config)
      (println (red (bold "[!]")) (str "Error parsing config file: " path))
      (println "   " (::anom/desc config)))
    config))

(defn gen-from-routes [env]
  (println "* Generating html files from routes")
  )

(defn start-dev! [& [{:keys [config-path]}]]
  (let [config (read-config (or config-path "./"))]
    (when-not (anom/? config)
      (let [env (config/config->env config)
            {:keys [dev-output-path]} env]
        (println "* Copying static files")
        (cmp/copy-static env dev-output-path)
        (gen-from-routes env)))))


(comment

  (start-dev! {:config-path "./resources/charly/test_site/charly.edn"})

  (start! {:config-path "./resources/charly/test_site/charly_error.edn"})

  (ks/spy (config/config->env (read-config "./resources/charly/test_site/charly.edn")))
  (read-config "./resources/charly/test_site/charly_error.edn")

  )


