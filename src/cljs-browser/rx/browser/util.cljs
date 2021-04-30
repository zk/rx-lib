(ns rx.browser.util
  (:require [rx.kitchen-sink :as ks]
            [dommy.core :as dommy]
            [clojure.string :as str]))

(defn on-drag [{:keys [on-click
                       on-down
                       on-up
                       on-move]}]
  (let [!m (atom nil)]
    (letfn [(internal-move [e]
              (.preventDefault e)
              (.stopPropagation e)
              (let [start (:start @!m)
                    {:keys [x y]} start
                    up-x (.-pageX e)
                    up-y (.-pageY e)
                    dx (- up-x x)
                    dy (- up-y y)]
                (when (and on-move
                           (or (> (ks/abs dx) 2)
                               (> (ks/abs dy) 2)))
                  (on-move
                    {:sx x
                     :sy y
                     :ex up-x
                     :ey up-y
                     :dx dx
                     :dy dy}
                    e)))
              nil)
            (internal-mu [e]
              (.preventDefault e)
              (.stopPropagation e)
              (let [start (:start @!m)
                    {:keys [x y]} start
                    up-x (.-pageX e)
                    up-y (.-pageY e)
                    dx (- up-x x)
                    dy (- up-y y)]
                (if (and (<= (ks/abs dx) 1)
                         (<= (ks/abs dy) 1))
                  (when on-click
                    (try
                      (on-click e)
                      (catch js/Error e
                        (.error js/console e)
                        nil)))
                  (when on-up
                    (on-up
                      {:sx x
                       :sy y
                       :ex up-x
                       :ey up-y
                       :dx dx
                       :dy dy}
                      e))))
              (dommy/unlisten!
                js/window
                :mouseup
                internal-mu)
              (dommy/unlisten!
                js/window
                :mousemove
                internal-move)
              nil)
            (internal-md [e]
              (.preventDefault e)
              (.stopPropagation e)
              (swap! !m assoc
                     :start {:x (.-pageX e)
                             :y (.-pageY e)})
              (dommy/listen!
                js/window
                :mouseup
                internal-mu)
              (dommy/listen!
                js/window
                :mousemove
                internal-move)
              (when on-down
                (on-down e))
              nil)]
      {:on-mouse-down internal-md
       #_:on-click
       #_(fn [e]
           (prn "click")
           #_(.preventDefault e)
           #_(.stopPropagation e)
           nil)})))

(defn interpose-children [{:keys [separator] :as opts} children]
  (into
    [:div
     (dissoc opts :separator)]
    (->> children
         (remove nil?)
         (interpose separator))))


(defn kw->class [k & appends]
  (when k
    (-> (pr-str k)
        (str/replace ":" "")
        (str/replace "." "-")
        (str/replace "/" "--"))
    #_(str (str (namespace k))
         "--"
         (name k)
         (when appends
           "-")
         (apply str appends))))
