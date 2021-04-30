(ns rx.aws-test
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.test :as test
             :refer-macros [thrown]]
            [rx.aws :as aws]
            [cljs.core.async :refer-macros [go]]))

(test/reg ::cog-auth-by-username-password
  (fn [{:keys [:rx.aws/AWS]}]
    (go
      (let [creds (:dev-creds
                   (ks/edn-read-string
                     (ks/slurp-cljs
                       "~/.rx-secrets.edn")))]
        [[(res/incorrect?
            (<!
              (aws/<cog-auth-by-username-password AWS
                {}
                nil
                nil)))
          "Should return incorrect for bad config"]
         
         [(res/incorrect?
            (<!
              (aws/<cog-auth-by-username-password AWS
                (merge
                  creds
                  {:aws.cog/client-id "test"})
                nil
                nil)))
          "Should return anom cat incorrect"]

         [(res/incorrect?
            (<!
              (aws/<cog-auth-by-username-password AWS
                (merge
                  creds
                  {:aws.cog/client-id "test-client-id"})
                "test-username"
                nil)))
          "Should throw an exception when missing password"]

         [(res/anom
            (<! (aws/<cog-auth-by-username-password AWS
                  (merge
                    creds
                    {:aws.cog/client-id "testclientid"})
                  "test-username"
                  "test-password")))]

         [(res/anom
            (<! (aws/<cog-auth-by-username-password AWS
                  (merge
                    creds
                    {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"})
                  "no-username"
                  "no-password")))]

         [(res/data
            (<! (aws/<cog-auth-by-username-password AWS
                  (merge
                    creds
                    {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"})
                  "zk+test-rx@heyzk.com"
                  "Test1234")))]]))))

(test/reg ::cog-refresh-auth
  (fn [{:keys [:rx.aws/AWS]}]
    (go
      (let [creds (:dev-creds
                   (ks/edn-read-string
                     (ks/slurp-cljs
                       "~/.rx-secrets.edn")))]
        [[(test/thrown
            (aws/<cog-refresh-auth AWS
              {}
              nil))
          "Should throw an exception when missing aws creds"]
         
         [(test/thrown
            (aws/<cog-refresh-auth AWS
              creds
              nil))
          "Should throw an exception when missing cog client id"]
         
         [(res/anom
            (<!
              (aws/<cog-refresh-auth AWS
                (merge
                  creds
                  {:aws.cog/client-id "test"})
                nil)))
          "Should return an anomaly when client id is incorrect"]

         [(res/data
            (<!
              (aws/<cog-refresh-auth AWS
                (merge
                  creds
                  {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"})
                (res/data
                  (<! (aws/<cog-auth-by-username-password AWS
                        (merge
                          creds
                          {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"})
                        "zk+test-rx@heyzk.com"
                        "Test1234"))))))]]))))

(test/reg ::cog-global-sign-out
  (fn [{:keys [:rx.aws/AWS]}]
    (go
      (let [creds (:dev-creds
                   (ks/edn-read-string
                     (ks/slurp-cljs
                       "~/.rx-secrets.edn")))]
        [[(test/thrown
            (aws/<cog-global-sign-out AWS
              {}
              nil))
          "Should throw an exception when missing aws creds"]

         [(res/data
            (<!
              (aws/<cog-global-sign-out AWS
                creds
                (res/data
                  (<! (aws/<cog-auth-by-username-password AWS
                        (merge
                          creds
                          {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"})
                        "zk+test-rx@heyzk.com"
                        "Test1234"))))))]]))))
