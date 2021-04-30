(ns rx.browser.test-ui
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.test :as t]
            [reagent.core :as r]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go go-loop]]))

(defn test-report-description [test-report]
  (let [{:keys [::t/run-duration]} test-report
        passed-count (->> test-report
                          ::t/test-results
                          (filter ::t/passed?)
                          count)
        total-count (->> test-report
                         ::t/test-results
                         count)]
    [:span
     passed-count
     " of "
     total-count
     " succeeded in "
     (->> test-report
          ::t/test-results
          (map ::t/test-key)
          (map namespace)
          distinct
          (interpose ", ")
          (apply str))
     #_(/ (ks/round run-duration) 1000) "s"]))

(defn test-report-detail [{:keys [test-report]}]
  (let []
    [:div
     (test-report-description test-report)
     #_[:ul
        {:style {:list-style-type 'none
                 :margin 0
                 :padding 0}}
        (->> test-report
             ::t/test-results
             (map ::t/test-key)
             (map namespace)
             distinct
             (map (fn [s]
                    [:li {:key s} s])))]]))

(defn run-and-report-detail [{:keys [test-context]}]
  (let [!test-report (r/atom nil)]
    (go
      (reset! !test-report
        (<! (t/<run-all! test-context))))
    (r/create-class
      {:reagent-render
       (fn []
         (if @!test-report
           [test-report-detail {:test-report @!test-report}]
           "Running..."))})))

(defn root-view []
  {:render
   (fn []
     [:div
      {:style {:padding 10
               :flex 1}}
      [test-report-detail
       {:test-report
        '{:rx.test/run-duration 2264.854999999865,
          :rx.test/test-results
          [{:rx.test/test-key :rx.box.db-adapter-test/<store-entities,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.box.db-adapter-dexie/result "[:foo/id \"foo-one\"]",
               :rx.res/data [{:foo/id "foo-one", :foo/other "key"}]}}{:rx.test/passed? true,
              :rx.test/desc
              {:rx.box.db-adapter-dexie/result "[:foo/id \"foo-one\"]",
               :rx.res/data [{:foo/id "foo-one", :foo/other "key"}]}}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-with-ref-many,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.box.db-adapter-dexie/result "[:bar/id \"many-bar-two\"]",
               :rx.res/data
               ({:foo/id "many-foo-one",
                 :bar "baz",
                 :foo/baz {:bar/id "many-bar-two"},
                 :foo/bars :box.cardinality/many}
                {:bar/id "many-bar-one", :some/other "key", :bar/baz "bap"}
                {:bar/id "many-bar-two", :some/other "key2"})}}
             {:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-dissoc,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/calculate-changes,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              "Missing cardinality many property should throw exception"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-empty,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc "Throws an exception when the txfs list is empty"}],
            :rx.test/passed? true}
           {:rx.test/test-key
            :rx.box.persist-local-test/transact-cardinality-one,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.box.db-adapter-dexie/result
               "[:bar/id \"cardi-one-bar-two\"]",
               :rx.res/data
               ({:foo/id "cardi-one-foo-one",
                 :foo/bar {:bar/id "cardi-one-bar-two"}}
                {:bar/id "cardi-one-bar-one"}
                {:bar/id "cardi-one-bar-two"})}}
             {:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<ensure-db-structure,
            :rx.test/results
            [{:rx.test/passed? true, :rx.test/desc {:rx.res/data "ok"}}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-with-ref,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<open-db,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc "Nil DBAdapter should return error"}
             {:rx.test/passed? true,
              :rx.test/desc
              "Invalid schema should return error: #error {:message \"Invalid persist local schema\", :data {:rx.res/category :rx.res/incorrect, :cljs.spec.alpha/problems ({:path [], :pred (cljs.core/fn [%] (cljs.core/contains? % :box/local-db-name)), :val {}, :via [:box/persist-local-schema], :in []} {:path [], :pred (cljs.core/fn [%] (cljs.core/contains? % :box/local-data-table-name)), :val {}, :via [:box/persist-local-schema], :in []} {:path [], :pred (cljs.core/fn [%] (cljs.core/contains? % :box/local-refs-table-name)), :val {}, :via [:box/persist-local-schema], :in []}), :cljs.spec.alpha/spec :box/persist-local-schema, :cljs.spec.alpha/value {}}}"}
             {:rx.test/passed? true,
              :rx.test/desc "[Tn [object Object]]"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<delete-idents,
            :rx.test/results
            [{:rx.test/passed? true, :rx.test/desc {:rx.res/data {}}}
             {:rx.test/passed? true,
              :rx.test/desc "Should not pull back entity"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-conj,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              "Prop key not marked cardinality many should return an anomaly"}
             {:rx.test/passed? true,
              :rx.test/desc
              "Schema attrs missing to ident key should return anomaly"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<fetch-by-query,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-missing-ident,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-basic,
            :rx.test/results
            [{:rx.test/passed? true, :rx.test/desc "Transact succeeded"}
             {:rx.test/passed? true, :rx.test/desc "Pull succeeded"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<resolve-ref-idents,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.res/data
               {[:foo/id "foo-one"] {:foo/bars [[:bar/id "bar-one"]]}}}}
             {:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<fetch-entities,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.res/data
               {[:foo/id "foo-one"] {:foo/id "foo-one", :foo/other "key"}}}}
             {:rx.test/passed? true,
              :rx.test/desc "Should contain :foo/other \"key\""}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.db-adapter-test/<update-refs,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.res/data "ok",
               :rx.box.db-adapter-dexie/del-result nil,
               :rx.box.db-adapter-dexie/upd-result nil}}],
            :rx.test/passed? true}
           {:rx.test/test-key
            :rx.box.persist-local-test/transact-missing-ref-data,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key
            :rx.box.persist-local-test/transact-cardinality-many,
            :rx.test/results
            [{:rx.test/passed? true,
              :rx.test/desc
              {:rx.box.db-adapter-dexie/result "[:root/id \"root\"]",
               :rx.res/data
               ({:child/id "child1", :child/sort-val 1}
                {:child/id "child0", :child/sort-val 0}
                {:child/id "child2", :child/sort-val 2}
                {:root/id "root", :root/children :box.cardinality/many})}}
             {:rx.test/passed? true,
              :rx.test/desc "Should return 1 child: 1 found"}
             {:rx.test/passed? true, :rx.test/desc "child2"}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-nested-refs,
            :rx.test/results [{:rx.test/passed? true}],
            :rx.test/passed? true}
           {:rx.test/test-key :rx.box.persist-local-test/transact-assoc,
            :rx.test/results [{:rx.test/passed? true}], 
            :rx.test/passed? true}]}}]])})
(comment

  (browser/<show-route! [root-view])

  )

