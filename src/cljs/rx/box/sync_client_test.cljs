(ns rx.box.sync-client-test
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [rx.aws :as aws]
            [rx.box.common :as com]
            [rx.box.auth :as auth]
            [rx.box.test-helpers :as th]
            [rx.box.sync-client :as r]
            [rx.box.persist-local :as pl]
            [rx.test :as test]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]
             :refer-macros [go]]))

(def creds
  (:dev-creds
   (ks/edn-read-string
     (ks/slurp-cljs "~/.rx-prod-secrets.edn"))))

(<defn <auth [conn]
  (->> (auth/<auth-by-username-password
         conn
         "zk+test-rx@heyzk.com"
         "Test1234")
       <?))

(def cog-schema {:box/sync-ddb-uri
                 "https://8pg6p1yb85.execute-api.us-west-2.amazonaws.com/20191217/"
                 :box/cog-config
                 {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"
                  :aws.cog/pool-id "us-west-2_W1OuUCkYR"}
                 :box/cog-client-creds
                 {:aws.creds/access-id ""
                  :aws.creds/secret-key ""
                  :aws.creds/region "us-west-2"}})

(test/<deftest test-<sync-entities [opts]
  (let [conn (<? (th/<test-conn opts cog-schema))

        entities [{:box.canary/id "foo1"
                   :box.example/vector [{:foo/bar "baz"}]
                   :box.example/set #{1 2 3}
                   :box.sync/owner-id "357b0101-eae5-481d-9217-35403ec5fb6c"}
                  {:box.canary/id "foo2"
                   :box.sync/owner-id "357b0101-eae5-481d-9217-35403ec5fb6c"}]

        store-res (<? (pl/<transact
                        conn
                        entities))

        ok-res (<? (r/<sync-idents
                     conn
                     [[:box.canary/id "foo1"]
                      [:box.canary/id "foo2"]]
                     (<? (<auth conn))))
          
        empty-res
        (<! (r/<sync-entities
              conn
              [{}]
              (<? (<auth conn))))]

    [{::test/desc "Local storage of entities should succeed"
      ::test/passed? (not (anom/? store-res))
      ::test/explain-data store-res}

     {::test/desc "Sync should succeed"
      ::test/passed? (not (anom/? ok-res))
      ::test/explain-data ok-res}

     {::test/desc "Empty entities param should anom"
      ::test/passed? (anom/? empty-res)
      ::test/explain-data empty-res}]))

#_(<defn <verify-deployment
    [{:keys [:rx.aws/AWS]}]
    (let [conn (<! (pl/<conn
                     AWS
                     {:box/local-db-name "verify_deployment"
                      :box/local-data-table-name "verify_deployment_data"
                      :box/local-refs-table-name "verify_deployment_refs"
                      :box/ddb-data-table-name "canter-prod-pk-string"
                      :box/attrs
                      {:foo/id {:box.attr/ident? true}}}))]
      (r/<sync-entities
        conn
        {:foo/id "hello"}
        nil)))

(test/<deftest test-<sync-imd [opts]
  (let [conn (<! (th/<test-conn opts cog-schema))

        entities [{:box.canary/id "foo1"
                   :box.example/vector [{:foo/bar "baz"}]
                   :box.example/set #{1 2 3}}]

        sync-res (<! (r/<sync-imd
                       conn
                       entities
                       nil))]

    [{::test/desc "<sync ok"
      ::test/passed? (not (anom/? sync-res))
      ::test/explain-data sync-res}]))

(test/<deftest test-<sync-ack-fail [opts]
  (let [conn (<! (th/<test-conn opts cog-schema))

        entities [{:box.canary/id "foo1"
                   :box.example/vector [{:foo/bar "baz"}]
                   :box.example/set #{1 2 3}}]

        res (<! (r/<sync-ack
                  conn
                  entities
                  {:box/cog-tokens nil}))

        pull-res (<! (pl/<pull conn [:box.canary/id "foo1"]))]

    [{::test/desc "<sync-ack should fail without auth"
      ::test/passed? (anom/? res)
      ::test/explain-data res}

     {::test/desc "Data should be in local db"
      ::test/explain-data pull-res
      ::test/passed? (= (-> pull-res
                            :box.canary/id)
                        "foo1")}]))

(test/<deftest test-<sync-ack-success [opts]
  (let [conn (<! (th/<test-conn opts cog-schema))

        entities [{:box.canary/id "foo1"
                   :box.example/vector [{:foo/bar "baz"}]
                   :box.example/set #{1 2 3}}]

        res (<! (r/<sync-ack
                  conn
                  entities
                  (<? (<auth conn))))

        pull-res (<! (pl/<pull conn [:box.canary/id "foo1"]))]

    [{::test/desc "<sync-ack should succeed"
      ::test/passed? (not (anom/? res))
      ::test/explain-data res}

     {::test/desc "Data should be in local db"
      ::test/explain-data pull-res
      ::test/passed? (= (-> pull-res
                            :box.canary/id)
                        "foo1")}]))

#_(test/<deftest test-<sync-without-auth [opts]
  (let [conn (<! (th/<test-conn opts cog-schema))

        entities [{:box.canary/id "foo1"
                   :box.example/vector [{:foo/bar "baz"}]
                   :box.example/set #{1 2 3}
                   :box.sync/owner-id "357b0101-eae5-481d-9217-35403ec5fb6c"}]

        sync-res (<! (r/<sync
                       conn
                       entities
                       {:box/cog-tokens nil}))

        pull-res (<! (pl/<pull conn [:box.canary/id "foo1"]))]

    [{::test/desc "<sync ok"
      ::test/passed? (not (anom/? sync-res))
      ::test/explain-data sync-res}

     {::test/desc "Data returned by sync should be correct"
      ::test/explain-data sync-res
      ::test/passed?
      (and (map? sync-res)
           (= (-> sync-res
                  :entities
                  first
                  :box.canary/id)
              "foo1"))}

     {::test/desc "Data should be in local db"
      ::test/explain-data pull-res
      ::test/passed? (= (-> pull-res
                            :box.canary/id)
                        "foo1")}]))
