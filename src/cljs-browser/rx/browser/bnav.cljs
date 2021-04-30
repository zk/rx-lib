(ns rx.browser.bnav
  (:require [rx.kitchen-sink :as ks]
            [rx.viewreg :as vr]
            [reagent.core :as r]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :refer [timeout <!]
             :refer-macros [go go-loop]]))

(defn gen-control []
  (r/atom {:!view-stack (atom '())}))

(defn has-prev-view? [{:keys [!view-stack]}]
  (when !view-stack
    (> (count @!view-stack) 1)))

(defn atom? [x] (instance? Atom x))

(s/def ::!view-stack any?)

(s/def ::control
  (s/and
    map?
    (s/keys
      :req-un [::!view-stack])))

(defn ensure-control [ctl]
  (when-not (s/valid? ::control ctl)
    (throw
      (ex-info
        (str
          "bnav control not valid: "
          (s/explain-data ::control ctl))
        {}))))

(defn log-route [op route extra]
  (println "[BNAV]" op extra)
  (ks/pp route))

(defn push-routes! [{:keys [!view-stack
                            !route-stack]
                     :as ctl}
                    new-routes]
  (swap! !view-stack into
    (map
      #(vr/render-route @vr/!registry % ctl)
      new-routes))
  (swap! !route-stack into new-routes))

(defn handle-routes-change [{:keys [!route-stack
                                    on-change-routes]}]
  (when on-change-routes
    (on-change-routes @!route-stack)))

(defn <push [ctl route & [opts]]
  (ensure-control ctl)
  (log-route "push" route opts)
  (go
    (try
      (push-routes! ctl [route])
      (handle-routes-change ctl)
      (catch js/Error e
        (println (.-message e))
        e))))

(defn <pop [ctl]
  (ensure-control ctl)
  (go
    (try
      (let [{:keys [!view-stack
                    !route-stack]} ctl]
        (swap! !view-stack
          (fn [vs]
            (if (> (count vs) 1)
              (pop vs)
              vs)))
        (swap! !route-stack pop)
        (handle-routes-change ctl))
      (catch js/Error e
        (println (.-message e))
        e))))

(s/def ::route
  (s/cat
    :route-key keyword?
    :route-opts map?))

(s/def ::initial-routes
  (s/coll-of ::route))

(s/fdef $container
  :args (s/cat
          :opts
          (s/and
            map?
            (s/keys
              :opt-un
              [::initial-routes]))))

(defn $container [{:keys [initial-routes
                          control
                          on-change-routes
                          style]}]
  (let [!view-stack (r/atom '())
        !route-stack (r/atom '())
        control-map
        {:!view-stack !view-stack
         :!route-stack !route-stack
         :on-change-routes on-change-routes}]

    (when control
      (reset! control control-map))

    (when initial-routes
      (push-routes!
        control-map
        initial-routes))

    (fn []
      (let [component (first @!view-stack)]
        [:div
         {:style (merge
                   {:flex 1
                    :overflow-y 'scroll
                    :display 'flex
                    :flex-direction 'column}
                   style)}
         (when component
           [(first component)
            (merge
              (second component)
              {:nav control-map})])]))))
