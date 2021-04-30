(ns rx.test-test
  (:require [rx.kitchen-sink :as ks]
            [rx.test :as test :refer-macros [deftest]]
            [clojure.core.async
             :refer [go <! timeout]]))

(test/deftest pass [opts]
  [[true]])

(test/deftest always-fail [opts]
  [[false "fail on purpose with message"]
   [true "passed"]
   [false "fail 2"]
   [true opts]])

(test/deftest nil-return-value [opts]
  nil)

(test/deftest empty-return-value [opts] [])

(test/deftest invalid-return-value [opts]
  ["should be a vector"])

(test/deftest test-body-throws-error []
  (ks/throw-str "Oops!")
  [[true]])

(test/<deftest test-body-throws-error-async []
  (ks/throw-str (<! (go "Oops async!")))
  [[true]])

(test/deftest test-map-assertions []
  [{::test/desc "Assertion description"
    ::test/passed? true}])

(comment

  (test/<run-var-repl #'test-map-assertions)

  (test/<run-nss-repl ['rx.test-test])

  )
