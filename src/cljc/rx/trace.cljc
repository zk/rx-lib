(ns rx.trace
  (:require [rx.kitchen-sink :as ks]
            [rx.log-client :as log]
            [cljs.tagged-literals]
            #?(:clj
               [clojure.core.async
                :refer [chan put! <! timeout]
                :refer-macros [go]]
               :cljs
               [cljs.core.async :as async
                :refer [chan put! <! timeout]
                :refer-macros [go]])))

(def ^:dynamic *ctx* nil)

(defonce !enabled? (atom false))

(defn on! [& [opts]]
  (reset! !enabled? true))

(defn off! [& [opts]]
  (reset! !enabled? false))

(defn enabled? []
  @!enabled?)

(defn js-value? [v]
  #?(:cljs (instance? cljs.tagged-literals/JSValue v)
     :clj false))

(defn serialize [v]
  (cond
    (map? v) (->> v
                  (map (fn [[k v]]
                         [k (serialize v)]))
                  (into {}))

    (vector? v) (->> v
                     (mapv serialize))

    (set? v) (->> v
                  (map serialize)
                  set)

    (list? v) (->> v
                   (map serialize))

    (seq? v) (->> v
                  (map serialize))

    (nil? v) nil

    (or (number? v)
        (string? v)
        (keyword? v)
        (symbol? v)
        (js-value? v)) v

    :else (pr-str v)))

(defn log-entry->trace [le]
  (when (:rx.log/body-edn le)
    (let [frame (ks/edn-read-string
                  {:default
                   (fn [tag value]
                     (str "#" tag "" value))}
                  (:rx.log/body-edn le))]
      (-> frame
          (update
            :rx.trace/frame-id
            #(or % (ks/uuid)))
          (update
            :rx.trace/created-ts
            #(or % (ks/now)))
          (update
            :rx.trace/group-id
            #(or % "default-group-id"))))))

(defn format-var [{:keys [::var] :as m}]
  (merge
    m
    (when var
      {::var (-> (meta var)
                 (select-keys
                   [:ns :name :file :line :end-line :column :end-column])
                 (update
                   :ns
                   (fn [ns]
                     #?(:clj (.getName ns)
                        :cljs ns))))})))

(defn format-args [{:keys [::args] :as m}]
  (merge
    m
    (when args
      {::args (mapv serialize args)})))

(def trace-keys [::group ::group-id ::exec-id ::var ::args ::namespace ::title ::created-ts])

(defn call [{:keys [::group
                    ::exec-id
                    ::var
                    ::args]
             :as opts}]
  (when (enabled?)
    #_(log/edn
        (->> (merge
               {::type :rx.trace.types/var-call}
               *ctx*
               (select-keys opts trace-keys))
             format-var
             format-args
             (remove #(nil? (second %)))
             (into {})))))

(defn value [v opts]
  (when (enabled?)
    #_(log/edn
        (format-var
          (merge
            *ctx*
            (select-keys opts trace-keys)
            {::type :rx.trace.types/value
             ::value (serialize v)}))))
  v)

(defn transition [opts old new]
  (when (enabled?)
    #_(log/edn
        (format-var
          (merge
            *ctx*
            (select-keys opts trace-keys)
            {::type :rx.trace.types/transition
             ::old old
             ::new new}))))
  new)

(defn return [v opts]
  (when (enabled?)
    #_(log/edn
        (format-var
          (merge
            (select-keys opts trace-keys)
            {::type :rx.trace.types/return-value
             ::value v}))))
  v)

(comment
  
  (on!)
  (call {:rx.trace/var #'return})

  )
