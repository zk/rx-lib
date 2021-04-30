(ns rx.box.query-local-test
  (:require [rx.kitchen-sink :as ks
             :refer-macros [go-try]]
            [rx.box.persist-local :as p]
            [rx.box.query-local :as q]
            [rx.box.test-helpers :as th]
            [rx.test :as test]
            [rx.anom :as anom]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go]]))

(test/<deftest query-simple [opts]
  (let [conn (<! (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}
        
                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}
                     :bar/order {:box.attr/index? true
                                 :box.attr/value-type :box.type/long}}}
                   [{:foo/id "foo-one"
                     :bar/order 3}
                    {:bar/id "bar-one"
                     :bar/order 1}
                    {:bar/id "bar-two"
                     :bar/order 2}]))

        ident-res (<! (q/<query
                        conn
                        {:where [:and
                                 [:ident-key :foo/id]]}
                        {:debug-log? false}))

        range-single-res
        (<! (q/<query
              conn
              {:where [:and
                       [:>= :bar/order 1]
                       [:< :bar/order 10]]}
              {:debug-log? false}))

        range-multi-res-1
        (<! (q/<query
              conn
              {:where [:and
                       [:ident-key :foo/id]
                       [:> :bar/order 0]
                       [:< :bar/order 10]]}
              {:debug-log? false}))

        range-multi-res-2
        (<! (q/<query
              conn
              {:where [:and
                       [:> :bar/order 0]
                       [:< :bar/order 10]
                       [:ident-key :foo/id]]}
              {:debug-log? false}))]
    [[(and (not (anom/anom? ident-res))
           ident-res
           (= "foo-one"
              (-> ident-res
                  first
                  :foo/id)))]
     [(and (not (anom/anom? range-single-res))
           range-single-res
           (= 3 (count range-single-res)))]
     [(and (not (anom/anom? range-multi-res-1))
           range-multi-res-1
           (= 1 (count range-multi-res-1)))]
     [(and (not (anom/anom? range-multi-res-2))
           range-multi-res-2
           (= 1 (count range-multi-res-2)))]]))


(test/<deftest query-non-existent-pull-keys [opts]
  (let [conn (<! (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}
                     
                     :bar/id {:box.attr/ident? true
                              :box.attr/value-type :box.type/string}
                     
                     :foo/bars
                     {:box.attr/ref? true
                      :box.attr/cardinality :box.cardinality/many}}}
                   [{:foo/id "foo-one"
                     :foo/bars
                     [{:bar/id "bar-one"}]}]))

        qr
        (<! (q/<query
              conn
              {:where [:and
                       [:ident-key :foo/id]]}
              {:debug-log? false
               :pull-keys
               [:foo/bars
                :doesnt-exist
                {:foo/bars2 [::baz]}]}))]
    [[(not (anom/? qr)) qr]]))

(test/<deftest query-without-pull-keys [opts]
  (let [conn (<! (th/<test-conn
                   opts
                   {:box/attrs
                    {:foo/id
                     {:box.attr/ident? true
                      :box.attr/value-type :box.type/string}
                     
                     :bar/id
                     {:box.attr/ident? true
                      :box.attr/value-type :box.type/string}
                     
                     :foo/bars
                     {:box.attr/ref? true
                      :box.attr/cardinality :box.cardinality/many}}}
                   [{:foo/id "foo"
                     :foo/bars [{:bar/id "bar"}]}]))

        res (<! (q/<query
                  conn
                  {:where [:and
                           [:ident-key :foo/id]]}))]

    [[(not= (:foo/bars (first res)) :box.cardinality/many) res]]))


