(ns charly.config
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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

(defn read-config [path]
  (try
    (ks/edn-read-string (slurp path))
    (catch Exception e
      (anom/from-err e))))

(defn expand-project-root [config]
  (let [{:keys [project-root]
         :or {project-root ""}} config]
    config))

(defn expand-build-paths [config]
  (let [{:keys [project-root
                build-path]
         :or {build-path "build"}} config]
    (merge
      {:dev-output-path (concat-paths
                          [project-root build-path "dev"])
       :prod-output-path (concat-paths
                           [project-root build-path "prod"])}
      config)))

(defn expand-static-path [{:keys [project-root] :as config}]
  (merge
    {:static-path (concat-paths
                    [project-root "static"])}
    config))

(defn resolve-routes-fn [sym & [opts]]
  (let [var (resolve sym)]
    (if var
      (let [var-val (var-get var)]
        (if (fn? var-val)
          var-val
          (fn [_] var-val)))
      (anom/anom {:desc "Couldn't resolve routes sym"
                  :sym sym}))))

(defn expand-routes [{:keys [routes] :as config}]
  (merge
    {:routes-fn (resolve-routes-fn routes)}
    config))

(defn config->env [config]
  (-> config
      expand-project-root
      expand-build-paths
      expand-static-path
      expand-routes))

(defn test-routes [opts]
  [["/" :root]
   ["/foo/:bar" :foo-bar]])

(def test-routes-def
  [["/" :root]
   ["/foo/:bar" :foo-bar]])

(comment

  (resolve-routes-fn 'charly.config/test-rout)

  (expand-config (read-config "./resources/charly/test_site/charly.edn"))

  (ks/pp (config->env
           {:id "charly-test-site",
            :project-root "resources/charly/test_site",
            :routes 'test-site.routes/routes}))

  (ks/pp (read-config "./resources/charly/test_site/charly.edn"))
  (read-config "./resources/charly/test_site/charly_error.edn")

  )


