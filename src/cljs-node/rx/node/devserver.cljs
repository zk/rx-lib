(ns ^:figwheel-no-load rx.node.devserver
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom]
            #_[macchiato.http :as http]
            #_[macchiato.server :as server]
            [clojure.string :as str]
            [cljs.nodejs :as nodejs]
            [clojure.core.async :refer [<! go]]))


(nodejs/enable-util-print!)

(defn on-global-error [e]
  (js/console.error e))

(defonce add-global-listener
  (.on js/process "uncaughtException" on-global-error))

(def default-config
  (merge
    {:server "127.0.0.1"
     :port 5000
     :protocol :http}
    #_{:protocol :https
       :private-key "devcert/localhost.key"
       :certificate "devcert/localhost.crt"}))

(defonce !server (atom nil))

(defn stop-server []
  (when (:server-obj @!server)
    (.close (:server-obj @!server)))
  (reset! !server nil)
  (ks/pn "Stopped dev server"))

(defn start [{:keys [handler
                     <handler]
              :as config}]
  (stop-server)
  (let [config (merge default-config config)]
    (reset! !server
      {:config config
       :start-ts (ks/now)
       #_:server-obj
       #_(server/start
           (merge
             config
             {:handler (fn [req respond raise]
                         (cond
                           <handler
                           (go
                             (let [res (<! (<handler req))]
                               (cond
                                 (ks/error? res) (raise res)
                                 (anom/? res) (raise res)
                               
                                 :else
                                 (if-not (:status res)
                                   (raise "Missing :status in response map")
                                   (respond res)))))
                         
                           handler
                           (handler
                             req
                             (fn [response]
                               (when (and (map? response)
                                          (not (:status response)))
                                 (ks/throw-str "Missing status for response: " response))
                               (respond response))

                             (fn [response]
                               (when (and (map? response)
                                          (not (:status response)))
                                 (ks/throw-str "Missing status for raise: " response))
                               (raise response)))
                         
                           :else (respond
                                   {:body (str
                                            "rx.node.devserver: handler not provided"
                                            "\n\n"
                                            (ks/pp-str @!server))
                                    :status 500})))
              :on-success (fn [& args]
                            (ks/pn "Started dev server"))}))})))

(defn status [] @!server)

(comment

  (status)

  (start {:port 8000})

  )
