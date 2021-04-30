(ns rx.test-runner
  (:require [rx.box.persist-local-test]
            [rx.box.query-local-test]
            [rx.aws-test]
            [rx.test :as test]))


(comment

  (do
    (require 'rx.rn.sqlite)
    (require 'rx.rn.aws)
    
    (test/<run-all-report-repl!
      {:rx.rn.sqlite/SQL rx.rn.sqlite/SQL
       :rx.aws-test/AWS rx.rn.aws/AWS})

    (test/<run-all-report-repl!
      {:namespaces [:rx.box.query-local-test]
       :rx.rn.sqlite/SQL rx.rn.sqlite/SQL
       :rx.aws-test/AWS rx.rn.aws/AWS})

    )


  )

