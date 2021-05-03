(ns rx.log-ingress
  (:require [rx.kitchen-sink :as ks]
            [rx.http :as http]
            [clojure.string :as str]
            [cljs-http.client :as hc]
            [cljs.core.async :as async
             :refer [<! put! timeout]
             :refer-macros [go go-loop]]))

(def HTTP (js/require "http"))
(def NET (js/require "net"))

(defonce !server (atom nil))

(defonce !listeners (atom #{}))

(defn listen! [f]
  (swap! !listeners conj f))

(defn unlisten! [f]
  (swap! !listeners disj f))

(defn fire [le]
  (doseq [f @!listeners]
    (f le))
  {:handler-count (count @!listeners)})

(defn body->log-event [body]
  (ks/from-transit body))

(defn restart [& [{:keys [port]
                   :or {port 7330}}]]
  (when @!server
    (.close @!server))

  (reset! !server
    (let [server
          (.createServer HTTP
            (fn [req resp]
              (let [stream req
                    !body (atom nil)]

                (.on stream "readable"
                  (fn []
                    (swap! !body str (.read stream))))

                (.on stream "end"
                  (fn []
                    (let [body @!body]
                      (pr-str (fire (body->log-event body)))

                      (.writeHead resp 200
                        (clj->js
                          {"Content-Type" "text/plain"}))

                      (.end resp "ok")
                      ))))))]
      
      (.listen server port
        (fn [err]
          (if err
            (ks/prn "Error starting server: " err)
            (ks/prn "Server started"))))
      server)))

(defonce !!run-test-logging? (atom (atom nil)))

(defn stop-test-logging []
  (reset! (deref !!run-test-logging?) false))


(defn <paras []
  (->> (:body
        (<! (hc/get "https://www.gutenberg.org/files/768/768-h/768-h.htm")))
       (re-seq #"<p>([^<]*)</p>")
       (map second)
       (map (fn [s]
              (let [el (.createElement js/document "textarea")]
                (set! (.-innerHTML el) s)
                (.-value el))))
       (map #(str/replace % #"\n" " "))
       vec))

(defn start-test-logging []
  (let [!old-run? (deref !!run-test-logging?)
        !run? (atom true)]
    
    (reset! !old-run? false)
    (reset! !!run-test-logging? !run?)
    
    (go
      (let [paras (<! (<paras))]
        (loop [iters 0]
          (hc/post
            "http://localhost:7330"
            {:body (ks/to-transit
                     {:rx.log/event-name (str "foo.bar." iters)
                      :rx.log/body-edn
                      (pr-str
                        {:paras [(nth
                                   paras
                                   (rand (count paras)))]
                         :title "Whurthering Heights"
                         :author "Emily Bronte"})})
             :headers {"Content-Type"
                       "application/json+transit"}})
          (<! (timeout 1000))
          (when @!run?
            (recur (inc iters))))))))


(comment

  (stop-test-logging)

  (start-test-logging)

  (reset! !listeners #{})
  (restart)
  
  (hc/post
    "http://localhost:7330"
    {:body (ks/to-transit
             {:foo/bar "baz"})
     :headers {"Content-Type" "application/json+transit"}})


  (def !r? (atom true))

  


  )


