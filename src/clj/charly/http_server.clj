(ns charly.http-server
  (:require [rx.kitchen-sink :as ks]
            [aleph.http :as ah]
            [ring.util.response :refer [get-header]]
            [rum.server-render :as sr]
            [charly.static-resource-middleware :as srm]
            [charly.content-type-middleware :as ctm]
            [reitit.core :as rei]
            [clojure.string :as str]))

(defonce !servers (atom nil))

(defn stop-http-server! [{:keys [port]}]
  (when-let [server (get-in @!servers [port :aleph])]
    (swap! !servers assoc-in [port :aleph] nil)
    (when server
      (.close server))))

(defn match-route [routes req]
  (rei/match-by-path
    (rei/router routes)
    (:uri req)))

(defn ns-to-js [sym]
  (-> (str sym)
      (str/replace #"-" "_")
      (str/replace #"/" ".")))

(defn cljs-ns-response [main-ns req]
  {:body
   (str
     "<!DOCTYPE html>\n"
     (sr/render-static-markup
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
                   :href "/css/app.css"}]
           [:link {:rel "preconnect"
                   :href "https://fonts.gstatic.com"}]
           [:link {:rel "stylesheet"
                   :href "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&family=Sora:wght@400;500;600;700&display=swap"}]])
        [:body {:style {:width "100%"
                        :height "100%"}}
         [:div {:id "rx-root-mount-point"
                :style {:width "100%"
                        :height "100%"
                        :display 'flex}}]
         [:script {:dangerouslySetInnerHTML {:__html (str "var CHARLY_ENV=" (ks/to-json (ks/to-transit {:cljs-main main-ns})) ";")}}]
         [:script {:src "/cljs/app.js"}]]]))
   :headers {"Content-Type" "text/html"}
   :status 200})

(defn resolve-clj-handler [opts {:keys [clj cljs] :as route-data}]
  (if cljs
    (partial cljs-ns-response cljs)
    (fn [req]
      {:body (str "No handler " (pr-str route-data))})))

(defn create-http-handler [{:keys [dev-server routes] :as opts}]
  (let [{:keys [root-path]} dev-server]
    (-> (fn [req]
          (let [matched-route (match-route routes req)]
            (when matched-route
              (let [clj-handler (resolve-clj-handler opts (:data matched-route))]
                (clj-handler req)))))
        
        (srm/wrap-file root-path
          {:allow-symlinks? true})
        ctm/wrap-content-type)))

(defn start-http-server! [{:keys [dev-server] :as opts}]
  (let [{:keys [port] :or {port 5000}} dev-server
        id port]
    (stop-http-server! dev-server)
    (swap! !servers
      assoc-in
      [id :aleph]
      (ah/start-server
        (create-http-handler opts)
        {:port port}))))
