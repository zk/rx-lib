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

(defn resolve-sym [sym & [opts]]
  (let [sym (str sym)
        sym (symbol (str/replace sym "'" ""))

        ns (namespace sym)
        ns (symbol ns)]
    (prn "resolve-sym" "ns:" ns "sym:" sym)
    (use ns :reload-all)
    (let [var (resolve sym)]
      (if var
        (let [var-val (var-get var)]
          (if (fn? var-val)
            var-val
            (fn [_] var-val)))
        (anom/anom {:desc "Couldn't resolve sym"
                    :sym sym})))))

(defn expand-routes [{:keys [routes] :as config}]
  (prn "res" routes)
  (let [routes-fn (resolve-sym routes)]
    (prn "res-fn" routes-fn)
    (merge
      {:routes-fn routes-fn}
      config)))

(defn expand-css [{:keys [css-files css-preamble project-root] :as config}]
  (prn "expand css")
  (merge
    config
    {:css-files (->> css-files
                     (map (fn [{:keys [rules path] :as spec}]
                            (merge
                              spec
                              {:rules-fn (resolve-sym rules)}))))
     :css-preamble-fq (->> css-preamble
                           (mapv #(concat-paths
                                    [project-root %])))}))

(defn expand-dev-server [{:keys [project-root] :as config}]
  (merge
    config
    {:dev-server {:root-path (concat-paths
                               [project-root "build/dev"])
                  :route-path-to-filename (fn [path]
                                            (let [path (if (= "/" path)
                                                         "index"
                                                         path)]
                                              (str (str/replace path #":" "__cln__") ".html")))
                  :port 5000}}))

(defn config->env [config]
  (-> config
      expand-project-root
      expand-build-paths
      expand-static-path
      expand-routes
      expand-css
      expand-dev-server))

(defn test-routes [opts]
  [["/" :root]
   ["/foo/:bar" :foo-bar]])

(def test-routes-def
  [["/" :root]
   ["/foo/:bar" :foo-bar]])

(defn test-css-rules [env]
  [[:body {:background-color "red"}]])

(comment

  (ks/pp (config->env
           {:id "charly-test-site",
            :project-root "resources/charly/test_site",
            :routes 'charly.config/test-routes}))

  (ks/pp (read-config "./resources/charly/test_site/charly.edn"))
  (read-config "./resources/charly/test_site/charly_error.edn")

  )


