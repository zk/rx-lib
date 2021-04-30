(ns rx.anom
  (:require [clojure.string :as str]
            [clojure.core.async
             :refer [go <!]]))

;; https://github.com/cognitect-labs/anomalies

;; ::group :unavailble 'etc

(defn namespace-bare-keys [opts]
  (->> opts
       (map (fn [[k v]]
              [(if-not (namespace k)
                 (keyword
                   "rx.anom"
                   (name k))
                 k)
               v]))
       (into {})))

(defn namespace-code [opts]
  (if-let [code (::code opts)]
    (if-not (namespace code)
      (assoc
        opts
        ::code (keyword "rx.anom" (name code)))
      opts)
    opts))

(defn namespace-opts [opts]
  (-> opts
      namespace-bare-keys
      namespace-code))

(defn anom [opts & [extra]]
  (merge
    {::code ::unknown}
    extra
    (-> opts
        namespace-bare-keys
        namespace-code)))

(defn code [o] (::code o))

(defn anom? [o]
  (and (map? o)
       (::code o)))

(def ? anom?)

(defn err->stack [err]
  #?(:clj (->> (.getStackTrace err)
               (mapv str))
     :cljs (->> (str/split
                  (.-stack err)
                  #"\n")
                (map str/trim)
                (remove empty?)
                vec)))

(defn err->message [err]
  #?(:clj (.getMessage err)
     :cljs (.-message err)))

(defn throw-if-anom [o]
  (when (anom? o)
    (throw (ex-info "Anom found" o)))
  o)

#?(:clj
   (defn error? [o]
     (instance? Exception o))
   :cljs
   (defn error? [o]
     (instance? js/Error o)))

(defn add-frame [anom frame]
  (if frame
    (update
      anom
      ::stack
      #(conj
         (vec %)
         frame))
    anom))

(defn var->frame [v]
  (when v
    (let [m (meta v)]
      (-> m
          (select-keys [:ns :name :line :column :file])
          (dissoc :ns :name)
          (assoc :sym (symbol (:ns m) (:name m)))))))

(defn from-err [err & [{:keys [code var]}]]
  (let [ed (ex-data err)
        code (or code :rx.anom/unknown)
        message (or (err->message err)
                    (pr-str err))

        anom (if (anom? ed)
               (merge
                 {:rx.anom/desc message}
                 ed)
               (merge
                 (-> {:rx.anom/desc message
                      :rx.anom/stack (err->stack err)
                      :rx.anom/code code}
                     namespace-code)
                 ed))]
    (add-frame anom (var->frame var))))

(defn throw-if-anom-or-error [o]
  (when (anom? o)
    (throw (ex-info "Anom found" o)))
  (when (error? o)
    (throw (ex-info "Anom found" (from-err o))))
  o)



#?(:clj
   (defn cljs-env?
     "Take the &env from a macro, and tell whether we are expanding into cljs."
     [env]
     (boolean (:ns env))))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for Clojure code.
      https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
     [then else]
     (if (cljs-env? &env) then else)))

#?(:clj
   (defmacro go-anom
     "Like go but catches the first thrown error and returns it."
     [& body]
     `(if-cljs
          (cljs.core.async/go
            (try
              ~@(rest body)
              (catch js/Error e#
                (rx.anom/from-err e# {:var ~(first body)}))))
          (clojure.core.async/go
            (try
              ~@(rest body)
              (catch Throwable e#
                (rx.anom/from-err e# {:var ~(first body)})))))))

#?(:clj
   (defmacro gol
     "Like go but prints the error"
     [& body]
     `(if-cljs
          (cljs.core.async/go
            (try
              ~@body
              (catch js/Error e#
                (println "Error in go block")
                (when (ex-data e#)
                  (prn (ex-data e#)))
                (prn e#)
                (.error js/console e#)
                (rx.anom/from-err e#))))
          (clojure.core.async/go
            (try
              ~@body
              (catch Throwable e#
                (println "Error in go block" (pr-str e#))
                (rx.anom/from-err e#)))))))

#?(:clj
   (defmacro <?
     "Like <! but throws anoms."
     [ch]
     `(if-cljs
        (throw-if-anom-or-error (cljs.core.async/<! ~ch))
        (throw-if-anom-or-error (clojure.core.async/<! ~ch)))))

#?(:clj
   (defmacro <defn [sym args-vec & body]
     `(clojure.core/defn
        ~sym
        ~args-vec
        (rx.anom/go-anom #'~sym ~@body)))
   )

#?(:clj
   (defmacro test-form2 [body]
     [&env
      (prn &form)]))

(defn throw-anom [anom-bare]
  (let [anom (merge
               {::code ::unknown}
               (namespace-opts anom-bare))]
    (throw
      (ex-info
        (str
          "[" (name (::code anom)) "]"
          " "
          (::desc anom))
        anom))))

(comment

  (<defn hello-world [n]
    (<? (go-anom #'map
          (throw (Exception. "hi")))))

  (go
    (prn "2"
      (<! (hello-world 10))))

  (go
    (prn "2"
      (<!
        (go-anom #'reduce
          (prn "1" (<? (go-anom #'map
                         (throw (Exception. "hi")))))))))


  (macroexpand-1 '(test-form2 (prn "hi2")))

  (prn *file*)

  

  (let [anom (namespace-opts anom-bare)]
    (throw
      (ex-info
        (str
          "[" (name (::code anom)) "]"
          " "
          (::desc anom))
        anom)))

  (anom {:code :unknown
         :desc "Something went wrong!"})

  (set! *print-namespace-maps* nil)

  )
