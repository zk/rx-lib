(ns rx.browser.test-runner
  (:require [rx.kitchen-sink :as ks]
            [rx.box.persist-local-test]
            [rx.box.db-adapter-dexie :as dexie]
            [rx.box.query-local-test]
            #_[rx.box.sync-client-test]
            #_[rx.aws-test]
            [rx.test :as test]
            #_[rx.node.sqlite :as sqlite]
            #_[rx.node.aws :as aws]))

(comment

  (test/<run-all-report-repl!
    {:box/db-adapter (dexie/create-db-adapter)})

  (test/<run-all-report-repl!
    {:namespaces [#_:rx.box.persist-local-test
                  :rx.box.query-local-test]
     :box/db-adapter (dexie/create-db-adapter)})

  (test/<run-key-report-repl!
    :rx.box.persist-local-test/transact-with-ref-many
    {:box/datastore (dexie/create-db-adapter)})

  (test/<run-all-report-repl!
    {:namespaces [:rx.box.sync-client-test]})

  (test/remove-all-tests!)

  )
