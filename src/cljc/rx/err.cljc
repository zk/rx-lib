(ns rx.err
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async
             :refer [go <!]]))


;; Error kernel. Core ideas:
;; + Just data
;; + Represent a failure at a single point in the code
;; + Have a call chain that describes the execution path back
;;   to the program's entry point
;; +


(defn err-obj->stack [err-obj]
  #?(:clj (->> (.getStackTrace err-obj)
               (mapv str))
     :cljs (->> (str/split
                  (.-stack err-obj)
                  #"\n")
                (map str/trim)
                (remove empty?)
                vec)))

#?(:clj
   (defn error-obj? [o]
     (or
       (instance? Exception o)
       (instance? Throwable o)))
   :cljs
   (defn error-obj? [o]
     (instance? js/Error o)))

(defn err->message [err]
  #?(:clj (.getMessage err)
     :cljs (.-message err)))

(def err? ::code)
(def ? err?)

(defn from-error-obj [err]
  (let [ed (ex-data err)
        code ::default
        message (err->message err)]
    (if (err? ed)
      ed
      {::desc message
       ::code code})))

(defn from-map [m]
  (merge
    (when-let [v (:desc m)]
      {::desc v})
    (when-let [v (:code m)]
      (::code v))
    (dissoc m :desc :code)))

(declare from)

(defn add-context [err context-entry]
  (if context-entry
    (update
      err
      ::context
      (fn [context]
        (conj
          (vec context)
          (dissoc (from context-entry) ::code))))
    err))

(defn from [o & [adtl-context]]
  (when o
    (add-context
      (cond
        (err? o) o
        (string? o) {::desc o
                     ::code ::default}
        (error-obj? o) (from-error-obj o)
        (map? o) (from-map o))
      adtl-context)))

(defn compound-message [{:keys [::desc ::context]}]
  (->> (concat
         (->> context
              reverse
              (map ::desc))
         [desc])
       (remove nil?)
       (interpose ": ")
       (apply str)))

(defn throw-err [o & [adtl-context]]
  (let [err (from o adtl-context)]
    (throw
      (ex-info (compound-message err) err))))

(defn throw-if-error [o]
  (when (or (err? o) (error-obj? o))
    (throw-err o))
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
   (defmacro govar
     "Like go but catches the first thrown error and returns it."
     [& body]
     `(if-cljs
          (cljs.core.async/go
            (try
              ~@(rest body)
              (catch js/Error e#
                (rx.err/from e# {:var ~(first body)}))))
          (clojure.core.async/go
            (try
              ~@(rest body)
              (catch Throwable e#
                (rx.err/from e# {:var ~(first body)})))))))

#?(:clj
   (defmacro goerr
     "Like go but catches the first thrown error and returns it."
     [& body]
     `(if-cljs
          (cljs.core.async/go
            (try
              ~@(rest body)
              (catch js/Error e#
                (rx.err/from e#))))
          (clojure.core.async/go
            (try
              ~@(rest body)
              (catch Throwable e#
                (rx.err/from e#)))))))

#?(:clj
   (defmacro gopr
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
                (rx.err/from e#))))
          (clojure.core.async/go
            (try
              ~@body
              (catch Throwable e#
                (println "Error in go block" (pr-str e#))
                (rx.err/from e#)))))))

#?(:clj
   (defmacro <?
     "Like <! but throws rx errs."
     [ch]
     `(if-cljs
        (throw-if-error (cljs.core.async/<! ~ch))
        (throw-if-error (clojure.core.async/<! ~ch)))))

#?(:clj
   (defmacro <defn [sym args-vec & body]
     `(clojure.core/defn
        ~sym
        ~args-vec
        (rx.err/govar #'~sym ~@body)))

   )


#?(:cljs

   (do

     (defn t-foo []
       (try
         (throw (js/Error. "foo!"))
         (catch js/Error e
           (throw-err e {:desc "FOO"}))))

     (defn t-bar []
       (try
         (t-foo)
         (catch js/Error e
           (throw-err e {:desc "Something else"}))))

     (defn t-baz []
       (try
         (t-bar)
         (catch js/Error e
           (throw-err e "Top level"))))

     (defn t-bap []
       (try
         (prn "tbap")
         (t-baz)
         (catch js/Error e
           (prn "E")
           (pprint (from e)))))

     ))

(comment

  (t-bap)

  (from "foo")
  (from {:desc "foo"})
  (from {::desc "foo"})
  (from {::code :somethin})
  (from (js/Error. "hi!"))
  
  (pprint (err-obj->stack (js/Error. "foo")))

  (compound-message
    {:rx.err/desc "Error contacting http server"
     :rx.err/code :rx.err/default
     :url "https://google.com/foo"
     :resp {:status 300}
     :rx.err/stack ["str1" "str2"]
     :rx.err/context
     [{:rx.err/desc "Error fetching details for item"
       :item-id "item123"}
      {:rx.err/desc "Error listing recent items"}
      {:rx.err/desc "Error in app"}]})

  )


