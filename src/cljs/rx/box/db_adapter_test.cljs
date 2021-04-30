(ns rx.box.db-adapter-test
  (:require [rx.kitchen-sink :as ks
             :refer-macros [go-try <?]]
            [rx.test :as test
             :refer-macros [deftest]]
            [rx.box.db-adapter :as da]
            [rx.res :as res]
            [rx.anom :as anom]
            [goog.object :as gobj]
            [rx.box.common :as com]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(def SCHEMA {:box/local-db-name "rx.box.db_adapter_test_testdb"
             :box/local-data-table-name "data_table"
             :box/local-refs-table-name "refs_table"
             :box/attrs
             {:foo/id {:box.attr/ident? true
                       :box.attr/value-type :box.type/string}
              :foo/order {:box.attr/index? true
                          :box.attr/value-type :box.type/long}
              :bar/id {:box.attr/ident? true
                       :box.attr/value-type :box.type/string}
              :foo/bar {:box.attr/ref? true}
              :foo/bars {:box.attr/ref? true
                         :box.attr/cardinality :box.cardinality/many}}})

(deftest <open-db [{:keys [::adapter]}]
  (go
    (let [nil-ds (<! (da/<open-db nil nil))
          invalid-schema (<! (da/<open-db adapter {}))
          ok (<! (da/<open-db adapter SCHEMA))]
      [[(ks/error? nil-ds) "Nil DBAdapter should return error"]
       [(ks/error? invalid-schema) (str "Invalid schema should return error: "
                                        invalid-schema)]
       [(not (ks/error? ok)) ok]])))

(deftest <ensure-db-structure [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          res (<! (da/<ensure-db-structure adapter db SCHEMA))]
      [[res res]])))

(deftest <store-entities [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          _ (<! (da/<ensure-db-structure adapter db SCHEMA))
          res (<! (da/<store-entities adapter db SCHEMA
                    [{:foo/id "foo-one"
                      :foo/other "key"}]))]
      [[(not (anom/code res)) res]])))

(deftest <fetch-entities [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          _ (<! (da/<ensure-db-structure adapter db SCHEMA))
          _ (<! (da/<store-entities adapter db SCHEMA
                  [{:foo/id "foo-one"
                    :foo/other "key"}]))
          res (<! (da/<fetch-entities adapter db SCHEMA
                    [[:foo/id "foo-one"]]))]
      [[(not (anom/code res)) res]
       [(= "key" (-> res
                     (get [:foo/id "foo-one"])
                     :foo/other))
        "Should contain :foo/other \"key\""]])))

(deftest <delete-idents [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          _ (<! (da/<ensure-db-structure adapter db SCHEMA))
          _ (<! (da/<store-entities adapter db SCHEMA
                  [{:foo/id "foo-one"
                    :foo/other "key"}]))
          _ (<! (da/<delete-idents adapter db SCHEMA
                  [[:foo/id "foo-one"]]))
          res (<! (da/<fetch-entities adapter db SCHEMA
                    [[:foo/id "foo-one"]]))]
      [[(not (anom/code res)) res]
       [(empty? res)
        "Should not pull back entity"]])))

(deftest <update-refs [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          _ (<! (da/<ensure-db-structure adapter db SCHEMA))
          _ (<! (da/<store-entities adapter db SCHEMA
                  [{:foo/id "foo-one"
                    :foo/other "key"}
                   {:bar/id "bar-one"
                    :bar/other "key"}]))

          
          res (<! (da/<update-refs adapter db SCHEMA
                    {[:foo/id "foo-one"]
                     [[:box/conj [:foo/id "foo-one"] :foo/bars [:bar/id "bar-one"]]]}))]

      #_(.toArray
          (gobj/get db (com/schema->refs-table-name SCHEMA))
          (fn [res]
            (ks/prn "res" res)))

      #_(ks/spy "here" (.toArray
                         (gobj/get db (com/schema->data-table-name SCHEMA))))
      
      [[(not (anom/code res)) res]])))

(deftest <resolve-ref-idents [{:keys [::adapter]}]
  (go
    (let [db (<! (da/<open-db adapter SCHEMA))
          _ (<! (da/<ensure-db-structure adapter db SCHEMA))
          _ (<! (da/<store-entities adapter db SCHEMA
                  [{:foo/id "foo-one"
                    :foo/other "key"}
                   {:bar/id "bar-one"
                    :bar/other "key"}]))
          _ (<! (da/<update-refs adapter db SCHEMA
                  {[:foo/id "foo-one"]
                   [[:box/conj [:foo/id "foo-one"] :foo/bars [:bar/id "bar-one"]]]}))

          res (<! (da/<resolve-ref-idents adapter db SCHEMA
                    [[[:foo/id "foo-one"]
                      :foo/bars {:box.attr/cardinality
                                 :box.cardinality/many}]]))]

      #_(ks/spy "res" res)

      [[(not (anom/code res)) res]
       [(= {:foo/bars [[:bar/id "bar-one"]]}
            (-> res
                (get [:foo/id "foo-one"])))]])))

(deftest <fetch-by-query [{:keys [::adapter]}]
  (go
    [[true]]
    #_(let [db (<! (da/<open-db adapter SCHEMA))
            _ (<! (da/<ensure-db-structure adapter db SCHEMA))
            _ (<! (da/<store-entities adapter db SCHEMA
                    [{:foo/id "foo-one"
                      :foo/order 1
                      :foo/other "key"}
                     {:foo/id "foo-two"
                      :foo/order 2
                      :foo/other "key"}]))
            res (<! (da/<fetch-by-query
                      adapter
                      db
                      SCHEMA
                      {:where [:and
                               [:ident-key :foo/id]
                               [:> :foo/order 1]]}))]

        #_(ks/spy "res" res)

        [[(not (res/anom res)) res]])))
