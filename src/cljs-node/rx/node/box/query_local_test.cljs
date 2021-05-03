(ns rx.node.box.query-local-test
  (:require [rx.box.query-local-test :as qlt]
            [rx.box.test-helpers :as th]
            [rx.node.sqlite :as sqlite]
            [rx.test :as test]))

(comment

  (test/<run-all-report-repl!
    {:rx.sqlite/SQL sqlite/SQL
     :namespaces [:rx.box.query-local-test]})

  #_(prn "hi")
  
  #_(do
    (th/set-sql-obj sqlite/SQL)
    (test/run-tests 'rx.box.query-local-test))

  )



