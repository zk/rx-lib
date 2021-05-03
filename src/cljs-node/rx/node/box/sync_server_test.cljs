(ns rx.node.box.sync-server-test
  (:require [rx.kitchen-sink :as ks]
            [rx.node.box.sync-server :as r]
            [rx.res :as res]
            [rx.node.aws :as aws]
            [rx.test :as test
             :refer-macros [deftest <deftest]]
            [rx.anom :as anom :refer-macros [<defn <?]]
            [rx.test :as rt]
            [cljs.core.async :as async
             :refer [<! chan put!] :refer-macros [go]]))

(def schema
  {:box/ddb-data-table-name "rx-test-pk-string"
   :box/ddb-creds
   (:dev-creds
    (ks/edn-read-string
      (ks/slurp-cljs
        "~/.rx-secrets.edn")))

   :box/attrs
   {:foo/id {:box.attr/ident? true}}})

(<defn <id-token []
  (->> (aws/<cogisp-user-password-auth
         (:dev-creds
          (ks/edn-read-string
            (ks/slurp-cljs "~/.rx-secrets.edn")))
         {:cog-client-id "1em482svs0uojb8u7kmooulmmj"}
         {:username "zk+test-rx@heyzk.com"
          :password "Test1234"})
       <?
       :rx.aws/data
       :AuthenticationResult
       :IdToken))

(deftest all-ops-owned [_]
  [[(r/all-ops-owned?
      schema
      [{:box.sync/op-key :box.op/update
        :box.sync/entity {:foo/id "foo"
                          :box.sync/owner-id "zk"}}]
      {:sub "zk"})]])

(deftest verify-and-decode-cog-auth [_]
  [[(try
      (r/verify-and-decode-cog-auth nil nil)
      false
      (catch js/Error e true))]
   [(try
      (r/verify-and-decode-cog-auth
        {:aws.cog/client-id "client-id"
         :aws.cog/user-pool-id "user-pool-id"
         :aws.cog/allowed-use-claims ["foo" "bar"]
         :aws.cog/jwks-str "jwks-str"}
        nil)
      false
      (catch js/Error e true))]
   [(try
      (r/verify-and-decode-cog-auth
        {:aws.cog/client-id "client-id"
         :aws.cog/user-pool-id "user-pool-id"
         :aws.cog/allowed-use-claims ["foo" "bar"]
         :aws.cog/jwks-str "jwks-str"}
        "foo bar")
      false
      (catch js/Error e true))]])

(<deftest verify-and-decode-cog-auth-async [_]
  (let [id-token (<? (<id-token))
        verify-res
        (r/verify-and-decode-cog-auth
          {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"
           :aws.cog/user-pool-id "us-west-2_W1OuUCkYR"
           :aws.cog/allowed-use-claims ["id" "access"]
           :aws.cog/jwks-str
           "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"Uc55PuoXTb1+Yxr8cC++RWqrDr3K0NPimJ0p5VG26uE=\",\"kty\":\"RSA\",\"n\":\"9aW80tw1sCwxGStjzVOoofljyPFa_FkLUzSXaFu_Qjf2pRnuxn6QSxXkI_C_aj1z-clP0sOlss6nIcJIGeP4u9E2gz7_Df2uCkU4AfGxio9RzznY3MsQVp8kfKEdK_5Go3zXX8x526Ky5sGMjFWR8RO-JEOl6NvnmgY_rv8uMS6GMUQx0wDAU4rAYHGKTzphgqJAt1I-9xTB0v4MyrI8qg7xcs21A6AVpRj3svAZn1sVsF9IphSrZeSFJmw1WfJe5NKnekOJm7qG2CUjKcTTY7jdf_L6MGQky2wlTUstxHUeiFDBehbD1nRDaps9D6mzpDObb8MMmx1Cjv_SUBFJkQ\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"kA8o8ZKo39RBS26uCab1FjPWl0o8B03DxlFzgomdVTg=\",\"kty\":\"RSA\",\"n\":\"jpa17hbfmap73UQinjq7HuICMYo7YaJRD_c5X73TPzCaSJ2w760UrvNgfOgGeIvlZleg5lqEokH4kyI2IBUJJ_qNXZWym6_s_mwJTXtrlF8l11j30oeP7DmNfE_j6RGDT5_mHOLXcN1DFdQaEu9P3IzINMHHQh5tJk7s2Zb8V0cSLQjVgdoHE4S5HXPzhKe243j8kpMIPRUeuEm1IN17EklMsE0Mxvv69Aksnt219EYQL12DcjswbuIaVGAi0VnQ3YzYinyzDNJ7keD34Wn9KFvfMUQh5s1VO_opQ2h0V50aKmENqYnImrTss0lVuklEC0u0_sSI1eBwfI-Ww_iupw\",\"use\":\"sig\"}]}"}
          id-token)]
    [[(= "zk+test-rx@heyzk.com"
         (-> verify-res
             res/data
             :payload
             :email))]]))

#_(test/reg ::to-ddb-item
  (fn []
    [[(try
        (r/default-to-ddb-item nil nil)
        false
        (catch js/Error e true))]
     [(try
        (r/default-to-ddb-item {} nil)
        false
        (catch js/Error e true))]
     [(try
        (r/default-to-ddb-item {} {})
        false
        (catch js/Error e true))]
     [(try
        (r/default-to-ddb-item
          {:box/attrs
           {:foo/id {:box.attr/ident? true}}}
          {:foo/id "foo1"})
        false
        (catch js/Error e true))]
     [(r/default-to-ddb-item
        {:box/attrs
         {:foo/id {:box.attr/ident? true}}}
        {:foo/id "foo1"
         :baz/bap "asdf"
         :box.sync/owner-id "zk"})]]))

(comment

  (test/<run-ns-repl
    'rx.node.box.sync-server-test
    {})

  (test/<run-ns
    'rx.node.box.sync-server-test
    {})

  (test-to-ddb-item)

  (test-verify-and-decode-cog-auth)

  (test-verify-and-decode-cog-auth-async)

  (test/run-tests 'rx.node.box.sync-server-test)

  (r/default-to-ddb-item
    schema
    {:foo/id "foo"})
  
  (r/lambda-println "foo")

  ;; not owner
  (ks/<pp
    (r/<sync-objs
      schema
      [{:gpt/id "foo"
        :box.pers/owner-id "zk"}
       {:gpt/id "bar"}
       {:gpt/id "baz"
        :box.pers/delete-marked-ts 1}]
      {:sub "zk"}))


  ;; ok
  (ks/<pp
    (r/<sync-objs
      {:box/schema schema}

      [{:foo/id "foo"
        :hello "world"
        :box.pers/owner-id "zk"}
       {:foo/id "bar"
        :box.pers/owner-id "zk"}
       {:foo/id "baz"
        :box.pers/delete-marked-ts 1
        :box.pers/owner-id "zk"}]
      {:sub "zk"}))

  (ks/<pp
    (r/<sync-objs
      {:box/schema schema}

      [{:foo/id "foo"
        :hello "world2"
        :box.pers/owner-id "zk"
        :box.pers/force-update? true}
       {:foo/id "bar"
        :box.pers/owner-id "zk"
        :box.pers/force-update? false}
       {:foo/id "baz"
        :box.pers/delete-marked-ts 1
        :box.pers/owner-id "zk"
        :box.pers/force-update? true}]
      {:sub "zk"}))

  (ks/<pp
    (r/<sync-objs
      {:box/schema schema}
      [{:foo/id "bar"
        :box.pers/owner-id "zk"
        :box.pers/force-update? true}]
      {:sub "zk"}))

  (go
    (let [start (system-time)]
      (ks/pp
        (<! (r/<sync-objs
              {:box/schema schema}
              (->> (range 1)
                   (map 
                     (fn [i]
                       {:box.sync/op-key :box.op/update
                        :box.sync/ignore-match-version? true
                        :box.sync/entity
                        {:foo/id (str "foo" i)
                         :test/one "one"
                         :test/two "two"
                         :box.sync/owner-id "zk"}})))
              {:sub "zk"})))
      (ks/pn (- (system-time) start))))

  (require '[clojure.browser.repl :as cbr])

  (ks/<pp (<id-token))

  )
