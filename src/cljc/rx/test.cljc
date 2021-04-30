(ns rx.test
  (:require #?(:clj  [rx.kitchen-sink :as ks :refer [go-try]]
               :cljs [rx.kitchen-sink :as ks :refer-macros [go-try]])
            #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.core.async
             :as async
             :refer [<!]]
            #?(:clj [rx.anom :as anom
                     :refer [go-anom <? <defn]]
               :cljs [rx.anom :as anom
                      :refer-macros [go-anom <? <defn]])
            [clojure.string :as str]
            [clojure.core.async
             :refer [go <!]]))

#?(:clj
   (defmacro deftest [sym & body]
     (let [ns# (or (namespace sym) (str (ns-name *ns*)))
           name# (name sym)
           kw# (keyword ns# name#)]
       `(do
          (defn ~sym ~@body)
          (rx.test/reg ~kw# ~sym)))))

#?(:clj
   (defmacro <deftest [sym args-vec & body]
     (let [ns# (or (namespace sym) (str (ns-name *ns*)))
           name# (name sym)
           kw# (keyword ns# name#)]
       `(do
          (<defn ~sym ~args-vec ~@body)
          (rx.test/reg ~kw# ~sym)))))

#?(:clj
   (defmacro thrown [& body]
     `(try
        ~@body
        false
        (catch js/Error e# e#))))

(s/def ::test-fn fn?)

(s/def ::assertion-spec
  (s/and
    map?
    (s/keys
      :opt [::test-fn
            ::<test-fn
            ::expected-value
            ::expected-value-pred
            ::f-or-<f])))

(s/def ::assertion-specs
  (s/coll-of ::assertion-spec))

(s/def ::assertion-result
  (s/keys
    :req [::result-value
          ::result-passed?]))

(s/def ::f-or-<f fn?)

(s/def ::test-spec
  (s/keys
    :req [::test-key
          ::f-or-<f]))

(s/def ::test-specs
  (s/coll-of ::test-spec))

(s/def ::test-key keyword?)

(defonce !default-test-opts (atom nil))

(defn set-default-test-opts! [opts]
  (reset! !default-test-opts opts))

(<defn <run-test-spec [test-spec & [context]]
  (ks/spec-check-throw ::test-spec test-spec)
  (let [{:keys [::f-or-<f]} test-spec

        start-ts (ks/now)

        [v error] (try
                    (let [res (f-or-<f (merge
                                         @!default-test-opts
                                         context))

                          res (loop [res res]
                                (if (ks/chan? res)
                                  (recur (<? res))
                                  res))

                          err (if (ks/error? res)
                                res
                                nil)]

                      (cond
                        (nil? res) [res
                                    "nil returned from test"]
                        
                        (and (sequential? res)
                             :deoptimization
                             (empty? res))
                        [res "Test assertions empty"]
                        
                        (and (sequential? res)
                             :deoptimization
                             (and (not (map? (first res)))
                                  (not (sequential? (first res)))))
                        [res "Invalid assertions"]
                        
                        (and err
                             (anom/? (.-data err)))
                        [res (.-data err)]
                        
                        err [res err]
                        
                        :else [res]))
                    (catch #?(:clj Exception :cljs js/Error) e
                      [nil (anom/from-err e)]))

        results (->> v
                     (mapv
                       (fn [assertion]
                         (if (vector? assertion)
                           (let [[result desc context] assertion]
                             (merge
                               {::passed? (if result true false)}
                               (when desc
                                 {::desc desc})
                               (when context
                                 {::explain-data context})))
                           (update
                             assertion
                             ::passed?
                             #(if % true false))))))]

    (merge
      (select-keys test-spec [::test-key ::doc])
      {::run-duration (- (ks/now) start-ts)}
      (if error
        {::test-error error
         ::test-error-result v
         ::passed? false}
        {::results results
         ::passed? (if (->> results
                            (reduce
                              #(and %1 (::passed? %2))
                              true))
                     true
                     false)}))))

(defonce !k->test-spec (atom {}))

(defn register-test-specs [test-specs]
  (ks/spec-check-throw ::test-specs test-specs)
  (swap! !k->test-spec
    merge
    (->> test-specs
         (map (fn [test-spec]
                [(::test-key test-spec) test-spec]))
         (into {}))))

(defn ns->str [ns]
  (if (keyword? ns)
    (name ns)
    (str ns)))

(defn test-keys-for-namespace [ns]
  (let [ns-str (ns->str ns)]
    (->> @!k->test-spec
         (filter
           (fn [[k v]]
             (= ns-str (namespace k))))
         (map first))))

(<defn <run-keys! [ks & [context]]
  (let [test-specs (vals (select-keys @!k->test-spec ks))
        start-time (ks/now)
        test-results (loop [test-specs test-specs
                            out []]
                       (if (empty? test-specs)
                         out
                         (recur
                           (rest test-specs)
                           (conj out
                             (<? (<run-test-spec
                                   (first test-specs)
                                   context))))))]
    {::test-results test-results
     ::run-duration (- (ks/now) start-time)}))

(defn meta->test-key [m]
  (keyword (str (:ns m)) (str (:name m))))

(<defn <run-all! [& [context]]
  (let [{:keys [:namespaces]} context
        start-time (ks/now)]
    (let [nss-set (->> namespaces
                       (map name)
                       set)
          test-specs (if namespaces
                       (->> @!k->test-spec
                            (map
                              (fn [[k test-spec]]
                                (when (get nss-set (namespace k))
                                  test-spec)))
                            (remove nil?))
                       (vals @!k->test-spec))
          test-results (loop [test-specs test-specs
                              out []]
                         (if (empty? test-specs)
                           out
                           (recur
                             (rest test-specs)
                             (conj out
                               (<? (<run-test-spec
                                     (first test-specs)
                                     context))))))]
      {::test-results test-results
       ::run-duration (- (ks/now) start-time)})))

(<defn <run-all-report-repl! [& [context]]
  (let [{:keys [namespaces]} context]
    (try
      (ks/pn "*** Running tests from")
      (let [nss-set (->> namespaces (map name) set)
            nss (if namespaces
                  (->> @!k->test-spec
                       (filter
                         (fn [[k test-spec]]
                           (when (get nss-set (namespace k))
                             k)))
                       (map first))
                  (keys @!k->test-spec))]
        (->> nss
             (map #(namespace %))
             distinct
             (map (fn [s]
                    (ks/pn s)))
             doall)
        (ks/pn))

      (let [run-result (<? (<run-all! context))
            {:keys [::test-results
                    ::run-duration]} run-result
            success-results (->> test-results
                                 (filter ::passed?))
            fail-results (->> test-results
                              (remove ::passed?))]

        
        (ks/pn)
        (doseq [test-result fail-results]
          (let [{:keys [::test-key
                        ::results
                        ::test-error]} test-result]
            (ks/pn "FAIL " test-key)
            (if test-error
              (do
                (ks/pn
                  "! Exception during test")
                (ks/pn "*" (.-message test-error))
                (if (ex-data test-error)
                  (ks/pn ">" (ex-data test-error))
                  (ks/pn ">" (pr-str (.-stack test-error)))))
              (->> results
                   (map-indexed
                     (fn [i result]
                       (let [{:keys [::passed?
                                     ::desc]}
                             result]
                         (if passed?
                           (do
                             (ks/pn
                               (inc i) 
                               ". OK"))
                           (do
                             (ks/pn
                               (inc i) 
                               ". [FAIL]"
                               (when desc
                                 (str " " desc))))))))
                   doall))
            (ks/pn)))

        (ks/pn
          "Completed"
          (->> test-results
               (mapcat ::results)
               count)
          "assertions across"
          (count test-results)
          "tests")
        (ks/pn
          (count success-results)
          "test succeeded,"
          (count fail-results)
          "failed.")
        (ks/pn "Took" (int run-duration) "ms"))
      (catch #?(:clj Exception :cljs js/Error) e
        (ks/pn " !!! Error running tests" e)
        (if-let [d (ex-data e)]
          (do
            (ks/pn e)
            (ks/pn (.-stack e))))))))

(defn reg [k & more]
  (let [test-key k
        {:keys [doc]}
        (cond
          (map? (first more)) (first more)
          (string? (first more)) {:doc (first more)}
          :else nil)
        f-or-<f (if (or (map? (first more))
                                (string? (first more)))
                          (second more)
                          (first more))]
    (assert (fn? f-or-<f))
    (register-test-specs
      [(merge
         {::test-key k
          ::f-or-<f f-or-<f}
         (when doc
           {::doc doc}))])
    k))

(defn remove-all-tests! []
  (reset! !k->test-spec {}))

(defn remove-test-key! [k]
  (swap! !k->test-spec dissoc k))

(defn <run-test-key [k & [context]]
  (<run-test-spec
    (get @!k->test-spec k)
    context))

(defn <run-var [sym & [context]]
  (<run-test-key
    (-> sym
        meta
        meta->test-key)
    context))

(<defn <run-ns [sym & [context]]
  (let [ns-str (ns->str sym)
        test-specs (->> @!k->test-spec
                        (filter (fn [[k _]]
                                  (= ns-str (namespace k))))
                        (map second))
        start-time (ks/now)
        test-results (loop [test-specs test-specs
                            out []]
                       (if (empty? test-specs)
                         out
                         (recur
                           (rest test-specs)
                           (conj out
                             (<? (<run-test-spec
                                   (first test-specs)
                                   context))))))]
    {::test-ns (keyword ns-str)
     ::test-results test-results
     ::run-duration (- (ks/now) start-time)
     ::passed? (if (->> test-results
                        (map ::passed?)
                        (reduce
                          #(and %1 %2)
                          true))
                 true
                 false)}))

(defn assertion-repl-str [{:keys [::passed? ::desc ::explain-data]}]
  (str
    (if passed?
      "✔"
      "⨱")
    " " desc
    "\n  "
    (when-not passed?
      (str
        (->> (str/split
               (ks/pp-str
                 (if (map? explain-data)
                   (dissoc
                     explain-data
                     #_::anom/stack)
                   explain-data))
               #"\n")
             (map-indexed #(if (= 0 %1)
                             %2
                             (str "  " %2)))
             (interpose "\n")
             (apply str))
        "\n "))))

(defn truncate-spec-probs [m]
  (if (-> m :cljs.spec.alpha/problems)
    (update
      m
      :cljs.spec.alpha/problems
      (fn [ps]
        (->> ps
             (map (fn [p]
                    (if (-> p :val :box.pl/AWS)
                      (assoc-in
                        p
                        [:val :box.pl/AWS]
                        "...")
                      p))))))
    m))

(defn truncate-spec-value [m]
  (if (-> m :cljs.spec.alpha/value :box.pl/AWS)
    (assoc-in
      m
      [:cljs.spec.alpha/value :box.pl/AWS]
      "...")
    m))

(defn truncate-keys [m]
  (-> m
      truncate-spec-probs
      truncate-spec-value))

(defn test-result-repl-str [{:keys [::test-key
                                    ::passed?
                                    ::test-error
                                    ::results]
                             :as tr}]
  (str
    (if passed?
      "PASS"
      "FAIL")
    " "
    test-key
    (when (not passed?)
      (if test-error
        (str
          "\n"
          (->> (str/split
                 (ks/pp-str
                   (if (map? test-error)
                     (truncate-keys test-error)
                     test-error))
                 #"\n")
               (map #(str "     " %))
               (interpose "\n")
               (apply str))
          "\n ")
        (str
          "\n"
          (->> results
               (map assertion-repl-str)
               (map (fn [s]
                      (->> (str/split s #"\n")
                           (map #(str "  " %))
                           (interpose "\n")
                           (apply str))))
               (interpose "\n")
               (apply str))
          "\n ")))))

(defn test-ns-result-repl-str [{:keys [::test-ns
                                       ::passed?
                                       ::test-results]
                                :as res}]
  (str
    (if passed?
      "PASS"
      "FAIL")
    " "
    test-ns
    (when-not passed?
      (str
        "\n"
        (->> test-results
             (map test-result-repl-str)
             (map (fn [s]
                    (->> (str/split s #"\n")
                         (map #(str "  " %))
                         (interpose "\n")
                         (apply str))))
             (interpose "\n")
             (apply str))))))

(defn test-nss-result-repl-str [{:keys [::ns-results
                                        ::passed?
                                        ::ns-count
                                        ::passed-count]
                                 :as res}]
  (str
    (str
      "\n"
      (->> ns-results
           (map test-ns-result-repl-str)
           (interpose "\n")
           (apply str)))
    "\n"
    passed-count "/" ns-count " passed -- " (::run-duration res) " ms"))

(<defn <run-test-key-repl [k & [context]]
  (let [res (<! (<run-test-key k context))]
    (ks/pn
      (test-result-repl-str res))))

(defn <run-var-repl [var & [context]]
  (ks/pn "Running" var)
  (<run-test-key-repl
    (-> var
        meta
        meta->test-key)
    context))

(<defn <run-ns-repl [sym & [context]]
  (let [res (<! (<run-ns
                  sym
                  context))]
    (println
      (test-ns-result-repl-str
        res))))

(<defn <run-nss [syms & [context]]
  (let [start-ts (ks/now)
        results (loop [syms syms
                       results []]
                  (if (empty? syms)
                    results
                    (let [res (<! (<run-ns
                                    (first syms)
                                    context))]
                      (recur
                        (rest syms)
                        (conj results res)))))

        ns-count (count syms)
        passed-count (->> results
                          (filter ::passed?)
                          count)]
    {::ns-count ns-count
     ::passed-count passed-count
     ::passed? (= passed-count ns-count)
     ::run-duration (- (ks/now) start-ts)
     ::ns-results results}))

(defn <run-nss-repl [syms & [context]]
  (go
    (try
      (let [res (<! (<run-nss syms context))]
        (ks/pn
          (str
            "\n"
            (test-nss-result-repl-str res))))
      (catch #?(:clj Exception :cljs js/Error) e
        (ks/prn e)))))

(comment

  !k->test-spec

  (remove-all-tests!)

  (reg :rx.test.example/test-plus

    {:doc "Tests the plus operator"}

    (fn [] [[(= 2
                (+ 1 1))
             '(= 2
                 (+ 1 1))]]))

  (ks/<pp (<run-test-key :rx.test.example/test-plus))

  (reg :rx.test.example/test-plus
    
    "Tests the plus operator"

    (fn [] [[(+ 1 1)]]))

  (reg :rx.test.example/test-plus
    [{:fn (fn [] (+ 1 1))
      :expect 2}])

  (register
    [::foo
     :fn
     :expect])

  (ks/<pp
    (<run-test-spec
      {::test-key :rx.test.example/one-plus-one
       ::assertions
       [{::test-fn (fn []
                     (+ 1 1))
         ::expected-value 2}
        {::test-fn (fn []
                     (+ 1 2))
         ::expected-value 2}]}))


  (do
    (reg :rx.test.example/test-plus
    
      "Tests the plus operator"
    
      [{:fn (fn [] (+ 1 1))
        :expect 2}
     
       {:fn (fn [] (+ 1 2))
        :expect 4}])
    
    (<run-all-and-report!))

  

  (ks/<pp (<run-all!))

  (<run-all-and-report!)
  
  (register-test-spec)

  (ks/pp @!k->test-spec)

  (reset! !k->test-spec nil)

  (macroexpand-1 '(deftest :foo
                    :test-fn (fn [])))

  (deftest :foo
    :test-fn (fn [])))












