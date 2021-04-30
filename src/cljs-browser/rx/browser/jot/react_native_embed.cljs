(ns rx.browser.jot.react-native-embed
  (:require [rx.kitchen-sink :as ks]
            [rx.jot :as jot]
            [rx.theme :as th]
            [rx.browser :as browser]
            [rx.browser.jot :as bjot]
            [dommy.core :as dommy]
            [reagent.core :as r]
            [goog.object :as gobj]))

(defn post-message [clj-msg]
  (when-let [ReactNativeWebView
             (gobj/get js/window "ReactNativeWebView")]
    (let [postMessage (gobj/get ReactNativeWebView "postMessage")]
      (postMessage
        (try
          (ks/to-transit clj-msg)
          (catch js/Error e
            (println "Couldn't serialize message: " clj-msg)))))))

(defn listen-message! [f]
  (gobj/set js/window "receiveMessage"
    (fn [msg]
      (try
        (f (ks/from-transit msg))
        (catch js/Error e
          (ks/throw-str "Error decode message: " msg))))))

(defn bind-event-listeners [!opts ed-state]
  (->> @!opts
       (filter
         (fn [[k v]]
           (= :rx.jot/rn-embed-fn v)))
       (map (fn [[k v]]
              [k (fn [& args]
                   (post-message
                     (into [k] args)))]))
       (into {})))

(defn root [opts]
  (let [!opts (r/atom opts)
        ed-state (bjot/editor-state)
        
        !event-listeners (r/atom
                           (bind-event-listeners !opts ed-state))]

    (listen-message!
      (fn [[op & args]]
        (condp = op
          :update-opts (do
                         (reset! !opts (first args))
                         (reset! !event-listeners
                           (bind-event-listeners
                             !opts
                             ed-state)))
          :blur (bjot/blur ed-state)
          :clear (bjot/clear ed-state))))
    
    (r/create-class
      {:component-did-mount
       (fn []
         (let [{:keys [:rx.browser.jot/bg-color]}
               (th/des @!opts
                 [[:rx.browser.jot/bg-color :color/bg-0]])]
           (dommy/set-style!
             (gobj/get js/document "body")
             :background-color bg-color)
           (post-message [:mounted])))
       :reagent-render
       (fn []
         (let [opts @!opts]
           [:div
            {:style {:flex 1
                     :display 'flex
                     :overflow-y 'scroll}}
            [bjot/editor
             (merge
               {:state ed-state
                :style {:flex 1}}
               opts
               @!event-listeners)]]))})))

(defn init-embedded-editor [init-opts]
  (let [opts-transit (gobj/get js/window "JOT_INITIAL_OPTS_TRANSIT")
        opts (try
               (ks/spy "init opts"
                 (ks/from-transit opts-transit))
               (catch js/Error e
                 (ks/prn "Couldn't parse initial opts: " opts-transit)))]
    (browser/<show-component!
      [root (merge
              init-opts
              opts)])))

(comment

  (.postMessage (.-ReactNativeWebView js/window) {:foo "bar"})
  
  (init)

  )
