(ns rx.jvm.ops.dev
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            [me.raynes.conch.low-level :as cl]
            [me.raynes.conch :as conch]
            [figwheel.main.api :as fapi]
            [aleph.http :as ah]
            [garden.core :as garden]
            [rum.server-render :as sr]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :refer [get-header]]
            [clojure.java.io :as io]))

(defn get-free-port []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn opts->output-dir [{:keys [target-path id]}]
  (let [target-path (if target-path
                      target-path
                      (str "target/dev/" id))]
    target-path))

(defn opts->output-to [opts]
  (str (opts->output-dir opts) "/app.js"))

(defn opts->app-css-path
  [{:keys [static-files-path]}]
  (str static-files-path "/css/app.css"))

(defn opts->css-dir
  [{:keys [static-files-path]}]
  (when static-files-path
    (str static-files-path "/css")))

(defn opts->node-opts [opts]
  (let [max-old-space-size
        (or (-> opts :node-proc :max-old-space-size)
            4096)
        js-path (opts->output-to opts)]
    (when-not js-path
      (anom/throw-anom
        {:desc "Missing js-path"
         :var #'opts->node-opts}))
    {:command ["node"
               (str "--max_old_space_size=" max-old-space-size)
               js-path]}))

(defn opts->figwheel-port [opts]
  (or (:figwheel-port opts)
      (get-free-port)))

(defn create-http-handler [{:keys [http-handler
                                   http-root-path] :as opts}]
  (-> (fn [req]
        {:body
         (str
           "<!DOCTYPE html>\n"
           (sr/render-static-markup
             (if http-handler
               (http-handler (merge opts req))
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
                           :href "/css/app.css"}]]
                  (->> opts
                       :font-urls
                       (map (fn [url]
                              [:link {:rel "stylesheet"
                                      :href url}]))))
                [:body {:style {:width "100%"
                                :height "100%"}}
                 [:div {:id "rx-root-mount-point"
                        :style {:width "100%"
                                :height "100%"
                                :display 'flex}}]
                 [:script {:src "/cljs/app.js"}]]])))
         :headers {"Content-Type" "text/html"}
         :status 200})
      (wrap-file http-root-path
        {:allow-symlinks? true})
      wrap-content-type))

(defonce !servers (atom nil))

(defn stop-http-server! [{:keys [id]}]
  (when-let [server (get-in @!servers [id :aleph])]
    (swap! !servers assoc-in [id :aleph] nil)
    (when server
      (.close server))))


(defn http-server? [opts] (:http-port opts))

(defn start-http-server! [{:keys [http-port id] :as opts}]

  (assert http-port)
  (assert id)
  
  (stop-http-server! opts)
  (swap! !servers
    assoc-in
    [id :aleph]
    (ah/start-server
      (create-http-handler opts)
      {:port http-port})))

(defn node-proc? [opts]
  (:node-proc opts))

(defn stop-node-proc! [{:keys [id]}]
    (when-let [proc (get-in @!servers [id :node-proc])]
      (ks/pr "Destorying node proc... ")
      (cl/destroy proc)
      (ks/pn "Done.")
      (swap! !servers assoc-in [id :node-proc] nil)))

(defn start-node-proc! [{:keys [id] :as opts}]
  (let [{:keys [command]} (opts->node-opts opts)]
    (when-not command
      (anom/throw-anom
        {:desc "Node command missing"
         :var #'start-node-proc!}))
    (stop-node-proc! opts)
    (ks/pr "Starting node proc... ")
    (swap! !servers
      assoc-in
      [id :node-proc]
      (let [proc (apply cl/proc command)]
        (future (cl/stream-to-out proc :out))
        (future (cl/stream-to-out proc :err))
        proc))
    (ks/pn "Done.")))

(defn remove-figwheel-port-files [opts]
  (let [output-dir (opts->output-dir opts)
        file-names
        [(str output-dir "/cljsc_opts.edn")
         (str output-dir "/figwheel/connect.js")]
        rm (conch/programs rm)]
    (doseq [file-name file-names]
      (try
        (rm file-name)
        (catch Exception e nil)))))

(defn stop-figwheel-server! [{:keys [id]}]
  (try
    (fapi/stop id)
    (catch Exception e
      #_(ks/pn "Server already stopped"))))

(defn start-figwheel-server! [opts]
  (stop-figwheel-server! opts)
  (let [root-path (opts->output-dir opts)
        {:keys [id watch-dirs main-namespace
                foreign-libs
                asset-path
                compiler
                hot-reload-cljs
                figwheel]} opts

        port (opts->figwheel-port opts)]

    (.mkdirs (.getParentFile (java.io.File. (opts->app-css-path opts))))

    (ks/pn "Output to:" (opts->output-to opts))
    (ks/pn "Port:" (or (-> opts :figwheel :ring-server-options :port)
                       port))
    
    (fapi/start
      (merge
        {:mode :serve
         :open-url false
         :ring-server-options {:port port}
         :watch-dirs watch-dirs
         :validate-config true
         :rebel-readline false
         :launch-node false}
        (when hot-reload-cljs
          {:hot-reload-cljs hot-reload-cljs})
        (when (opts->css-dir opts)
          {:css-dirs [(opts->css-dir opts)]})
        figwheel)
      {:id id
       :options
       (merge
         {:output-to (opts->output-to opts)
          :output-dir (opts->output-dir opts)
          :optimizations :none
          :source-map true
          :main main-namespace
          :parallel-build true
          :foreign-libs foreign-libs}
         (when asset-path
           {:asset-path asset-path})
         compiler)})))

#_(defn compile-app-css-to [opts]
  (garden/css
    (merge
      {:output-to nil
       :pretty-print? true
       :vendors ["webkit" "moz" "ms"]
       :preamble ["resources/public/css/bootstrap-reboot.min.css"
                  "resources/public/css/bootstrap-grid.min.css"]
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
      opts)
    []))

(defn stop-dev! [{:keys [id] :as opts}]
  (stop-figwheel-server! opts)
  (when (http-server? opts)
    (stop-http-server! opts))
  (when (node-proc? opts)
    (stop-node-proc! opts)))

(defn start-dev! [{:keys [id] :as opts}]
  (ks/pn "Starting" id "dev...")
  (start-figwheel-server! opts)
  (when (http-server? opts)
    (start-http-server! opts))
  (when (node-proc? opts)
    (start-node-proc! opts))
  (fapi/cljs-repl id))


