(ns rx.browser.clipboard
  (:require [rx.kitchen-sink :as ks]
            [cljs.core.async :as async
             :refer [close! chan put!]
             :refer-macros [go]]))

(defn <clipboard-read-text []
  (let [ch (chan)]
    (.catchn
      (.then
        (.readText (.-clipboard js/navigator))
        (fn [t]
          (put! ch t)
          (close! ch)))
      (fn [err]
        (put! ch err)
        (close! ch)))
    ch))

(defn <clipboard-read []
  (let [ch (chan)]
    (.catch
      (.then
        (.read (.-clipboard js/navigator))
        (fn [t]
          (put! ch t)
          (close! ch)))
      (fn [err]
        (put! ch err)
        (close! ch)))
    ch))

(defn event->data-items [e]
  (->> (array-seq (.. e -clipboardData -items))
       (map (fn [dti]
              {::dti-obj dti}))
       doall))

;; Ok, so the paste data items have to be accessed in event handler's
;; 'context' -- the event data will be cleared by the browser as a
;; security thing. That's why this wonky function exists

(defn realize-data-items-chan [dtis]
  (let [ch (chan 100)
        !rem-count (atom (count dtis))]
    (loop [dtis dtis]
      (if (empty? dtis)
        nil
        (let [{:keys [::dti-obj]} (first dtis)
              dti-clj (merge
                        {::kind (.-kind dti-obj)
                         ::type (.-type dti-obj)}
                        (when-let [file (.getAsFile dti-obj)]
                          {::file file}))]
          (condp = (.-kind dti-obj)
            "string" (.getAsString dti-obj
                       (fn [s]
                         (put! ch
                           (merge
                             dti-clj
                             {::text s}))
                         (swap! !rem-count dec)
                         (when (= 0 @!rem-count)
                           (close! ch))))
            "file" (do
                     (put! ch dti-clj)
                     (swap! !rem-count dec)
                     (when (= 0 @!rem-count)
                       (close! ch))))
          (recur
            (rest dtis)))))
    ch))

(defn realize-data-items [dtis]
  (ks/spy "before" dtis)
  (loop [dtis dtis
         out []]
    (if (empty? dtis)
      out
      (let [{:keys [::dti-obj]} (first dtis)
            dti-clj (merge
                      {::kind (.-kind dti-obj)
                       ::type (.-type dti-obj)}
                      (when-let [file (.getAsFile dti-obj)]
                        {::file file}))]
        (recur
          (rest dtis)
          (conj out dti-clj))))))
