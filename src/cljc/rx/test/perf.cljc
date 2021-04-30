(ns rx.test.perf
  (:require [rx.kitchen-sink :as ks]
            [clojure.core.async :as async
             :refer [go <! timeout]]
            [rx.res :as res]))

(defn -<run-test [<f args]
  (go
    (let [start (ks/now)
          result (<! (apply <f args))]
      {::duration (- (ks/now) start)
       ::result result})))

(defn calc-run-metrics [results]
  (let [durations (->> results
                       (map ::duration))
        sum (reduce + durations)
        mean (/ sum (count durations))

        diffs (->> durations
                   (map #(- % mean))
                   (map #(* % %)))

        std-dev (ks/sqrt (/ (reduce + diffs) (count diffs)))]
    {::mean (ks/round mean)
     ::std-dev (ks/round std-dev)
     ::first-result (::result (first results))
     ::completed-ts (ks/now)}))

(defn <run-perf-test
  [{:keys [::test-fn
           ::<test-fn
           ::iterations
           ::args
           ::perf-test-desc]
    :as perf-test}]
  (go
    (try
      (let [<f (if test-fn
                 (fn [& args]
                   (go (apply test-fn args)))
                 <test-fn)

            _ (when-not <f
                (res/throw-anom
                  {:rx.res/anom
                   {:rx.anom/desc "Missing test-fn or <test-fn"
                    ::perf-test perf-test}}))

            run-results
            (loop [i (or iterations 1)
                   out []]
              (if (<= i 0)
                out
                (recur
                  (dec i)
                  (conj out (<! (-<run-test <f args))))))]

        (merge
          #_{::run-results run-results}
          (calc-run-metrics run-results)
          (select-keys perf-test [::perf-test-desc ::iterations])))
      
      (catch #?(:clj Exception :cljs js/Error) e
        (res/err->res e)))))

#_(defn <run-group [{:keys [::group-desc
                          ::perf-test
                          ::group-args]
                   :as group}]
  (go
    (let [test-results
          (loop [group-args group-args
                 out []]
            (if (empty? group-args)
              out
              (recur
                (rest group-args)
                (conj out
                  (merge
                    (select-keys (first group-args) [::args-desc])
                    (<! (<run-perf-test
                          (merge
                            perf-test
                            {::args (::args (first group-args))}))))))))]
      (merge
        (select-keys
          group
          [::group-desc])
        {::perf-test-results
         test-results}))))

(defn <run-group [{:keys [::group-desc
                          ::perf-tests]
                   :as group}]
  (go
    (let [test-results
          (loop [perf-tests perf-tests
                 out []]
            (if (empty? perf-tests)
              out
              (recur
                (rest perf-tests)
                (conj out
                  (merge
                    (<! (<run-perf-test
                          (first perf-tests))))))))]
      (merge
        (select-keys
          group
          [::group-desc])
        {::perf-test-results
         test-results
         ::completed-ts (ks/now)}))))

(comment

  (clojure-version)

  (go
    (ks/spy
      (<! (<run-perf-test
            {::desc "Example test"
             ::iterations 10
             ::args [1 2]
             ::<test-fn (fn [a b]
                          (go
                            (<! (timeout (rand 10)))
                            "hi"))}))))

  (go
    (ks/spy
      (<! (<run-group
            {::group-desc "Example Group"
             ::perf-tests
             [{::perf-test-desc "Example test"
               ::iterations 10
               ::args [1 2]
               ::<test-fn (fn [a b]
                            (go
                              (<! (timeout (rand 5)))
                              "foo"))}]}))))

  )

