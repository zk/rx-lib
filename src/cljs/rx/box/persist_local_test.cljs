(ns rx.box.persist-local-test
  (:require [rx.kitchen-sink :as ks
             :refer-macros [go-try <?]]
            [rx.res :as res]
            [rx.anom :as anom]
            [rx.box.persist-local :as p]
            [rx.box.test-helpers :as th]
            [rx.test :as test
             :refer-macros [thrown]]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go]]))

(test/<deftest transact-basic [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:t0/id
                     {:box.attr/ident? true
                      :box.attr/value-type :box.type/string}}}))]
    (let [transact-res (<? (p/<transact
                             conn
                             [{:t0/id "one"
                               :foo "bar"}]
                             {:debug-log? false}))

          pull-res (<? (p/<pull conn [:t0/id "one"] nil {:debug-log? false}))]

      [{::test/desc "Foo should equal \"bar\""
        ::test/explain-data pull-res
        ::test/passed?
        (and (not (anom/? pull-res))
             (= "bar" (:foo pull-res)))}])))

(test/<deftest transact-empty [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:t0/id {:box.attr/ident? true
                             :box.attr/value-type :box.type/string}}}))]
    [[(test/thrown (p/<transact conn []))
      "Throws an exception when the txfs list is empty"]]))

(test/<deftest transact-missing-ident [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:t0/id {:box.attr/ident? true
                             :box.attr/value-type :box.type/string}}}))
        res (<? (p/<transact
                  conn
                  [{:foo "bar"}]
                  {:debug-log? false}))]
    
    [[(anom/anom? res)]]))

(test/<deftest transact-assoc [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "one"]
                :bar/baz "bap"]]

        res (<! (p/<transact conn tx-pl
                  {:debug-log? false}))

        pull-res (<! (p/<pull
                       conn
                       [:foo/id "one"]
                       []
                       {:debug-log? false}))]
    [[pull-res]
     [(and (not (anom/anom? res))
           (> (count res) 0))]]))

(test/<deftest transact-assoc-nil-ident-val [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id nil]
                :bar/baz "bap"]]

        res (<! (p/<transact conn tx-pl
                  {:debug-log? false}))]
    [[(anom/anom? res)]]))


(test/<deftest transact-dissoc [opts]
  (let [conn (<! (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "one"]
                :bar/baz "bap"]]

        tx-res (<? (p/<transact conn tx-pl
                     {:debug-log? false}))

        m-before (<? (p/<pull conn [:foo/id "one"]))

        tx-pl [[:box/dissoc
                [:foo/id "one"]
                :bar/baz]]

        _ (<? (p/<transact conn tx-pl
                {:debug-log? false}))

        m-after (<? (p/<pull conn [:foo/id "one"]))]
    [[(and m-after
           (not (:bar/baz m-after)))]]))

(test/<deftest transact-delete [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "one"]
                :bar/baz "bap"]]

        tx-res (<? (p/<transact conn tx-pl
                     {:debug-log? false}))

        delete-res (<? (p/<transact conn
                         [[:box/delete [:foo/id "one"]]]
                         {:debug-log? false}))

        m-after (<? (p/<pull conn [:foo/id "one"] {:debug-log? true}))]
    [[(not (anom/anom? tx-res))]
     [(not (anom/anom? delete-res))]
     [(nil? m-after)]]))

(test/<deftest transact-conj [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}
                     :foo/bars {:box.attr/cardinality
                                :box.cardinality/many}}}))
        
        incorrect-cardinality-res
        (<! (p/<transact conn
              [[:box/conj
                [:foo/id "one"]
                :bar/baz
                [:bar/id "bar"]]]
              {:debug-log? false}))

        missing-to-ident-key-res
        (<! (p/<transact conn
              [[:box/conj
                [:foo/id "one"]
                :foo/bars
                [:bar/id "bar"]]]
              {:debug-log? false}))]
    [[(anom/? incorrect-cardinality-res)
      "Prop key not marked cardinality many should return an anomaly"]
     [(anom/? missing-to-ident-key-res)
      "Schema attrs missing to ident key should return anomaly"]]))

(test/deftest calculate-changes [opts]
  [[(test/thrown
      (p/calculate-changes
        {:box/attrs {}}
        [[:box/conj [:foo/id "one"] :foo/bars [:bar/id "two"]]]))
    "Missing cardinality many property should throw exception"]])

;; refs

(test/<deftest transact-with-ref [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}

                     :foo/bar {:box.attr/ref? true}

                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "foo-one"]
                :bar "baz"]
               [:box/assoc
                [:foo/id "foo-one"]
                :foo/bar
                {:bar/id "bar-one"
                 :bar/baz "bap"
                 :some/other "key"}]]

        res (<? (p/<transact conn tx-pl
                  {:debug-log? false}))

        m (<? (p/<pull conn
                [:foo/id "foo-one"]
                [:foo/bar]
                {:debug-log? false}))]
    [[(= "key" (-> m :foo/bar :some/other))]]))

(test/<deftest transact-with-ref-many [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}

                     :foo/bars {:box.attr/ref? true
                                :box.attr/cardinality :box.cardinality/many}

                     :foo/baz {:box.attr/ref? true
                               :box.attr/cardinality :box.cardinality/one}

                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "many-foo-one"]
                :bar "baz"]
               [:box/assoc
                [:foo/id "many-foo-one"]
                :foo/bars
                [{:bar/id "many-bar-one"
                  :bar/baz "bap"
                  :some/other "key"}]]
               [:box/assoc
                [:foo/id "many-foo-one"]
                :foo/baz
                {:bar/id "many-bar-two"
                 :some/other "key2"}]]

        res (<? (p/<transact conn tx-pl
                  {:debug-log? false}))

        m (<? (p/<pull conn
                [:foo/id "many-foo-one"]
                [:foo/bars
                 :foo/baz]
                {:debug-log? false}))]
    [[(not (anom/anom? res)) res]
     [(= "key" (-> m :foo/bars first :some/other))]]))

(test/<deftest transact-nested-refs [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}

                     :foo/bars {:box.attr/ref? true
                                :box.attr/cardinality :box.cardinality/many}

                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}

                     :bar/bazs {:box.attr/ref? true
                                :box.attr/cardinality :box.cardinality/many}

                     :baz/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "foo-one"]
                :foo/bars
                [{:bar/id "bar-one"
                  :some/other "key"
                  :bar/bazs [{:baz/id "baz-one"}]}
                 {:bar/id "bar-two"
                  :some/other "key"
                  :bar/bazs [{:baz/id "baz-one"
                              :baz/prop "hi"}
                             {:baz/id "baz-two"
                              :baz/prop "lo"}]}]]]

        res (<? (p/<transact conn tx-pl
                  {:debug-log? false}))

        m (<? (p/<pull conn
                [:foo/id "foo-one"]
                [{:foo/bars [:bar/bazs]}]
                {:debug-log? false}))]
    [[(= "baz-one"
         (-> m
             :foo/bars
             first
             :bar/bazs
             first
             :baz/id))]]))

(test/<deftest transact-cardinality-one [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}

                     :foo/bar {:box.attr/ref? true}

                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}}}))
        tx-pl [[:box/assoc
                [:foo/id "cardi-one-foo-one"]
                :foo/bar
                {:bar/id "cardi-one-bar-one"}]
               [:box/assoc
                [:foo/id "cardi-one-foo-one"]
                :foo/bar
                {:bar/id "cardi-one-bar-two"}]]

        res (<? (p/<transact conn tx-pl
                  {:debug-log? false}))

        m (<? (p/<pull conn
                [:foo/id "cardi-one-foo-one"]
                [{:foo/bars [:bar/bazs]}]
                {:debug-log? false}))]
    [[(not (anom/anom? res)) res]
     [(= "cardi-one-bar-two"
         (-> m
             :foo/bar
             :bar/id))]]))

(test/<deftest transact-cardinality-many [opts]
  (let [conn (<? (th/<test-conn
                   opts
                   {:box/attrs
                    {:root/id {:box.attr/ident? true
                               :box.attr/value-type :box.type/string}

                     :root/children {:box.attr/ref? true
                                     :box.attr/cardinality :box.cardinality/many
                                     :box.attr/limit 1
                                     :box.attr/sort-key :child/sort-val
                                     :box.attr/sort-order :box.sort/descending}

                     :child/id {:box.attr/ident? true
                                :box.attr/value-type :box.type/string}
        
                     :child/sort-val {:box.attr/index? true
                                      :box.attr/value-type :box.type/long}}}))
        tx-pl [[:box/assoc
                [:root/id "root"]
                :root/children
                [{:child/id "child1"
                  :child/sort-val 1}
                 {:child/id "child0"
                  :child/sort-val 0}
                 {:child/id "child2"
                  :child/sort-val 2}]]]

        res (<? (p/<transact conn tx-pl
                  {:debug-log? false}))

        m (<? (p/<pull conn
                [:root/id "root"]
                [:root/children]
                {:debug-log? false}))]
    [[(not (anom/anom? res)) res]
     [(= 1 (count (:root/children m)))
      (str "Should return 1 child: "
           (count (:root/children m))
           " found")]
     [(= "child2"
         (-> m
             :root/children
             first
             :child/id))
      (-> m
          :root/children
          first
          :child/id)]]))
