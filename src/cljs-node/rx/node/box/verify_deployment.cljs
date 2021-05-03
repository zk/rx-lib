(ns rx.node.box.verify-deployment
  "Utilities for verifying a new box sync deployment"
  (:require [rx.kitchen-sink :as ks
             :refer-macros [go-try <?]]
            [rx.res :as res]
            [rx.node.box :as box]
            [rx.node.aws :as aws]
            [rx.jwt :as jwt]
            [rx.git :refer-macros [head-hash]]
            [cljs.core.async
             :refer [<!]
             :refer-macros [go]]))

(defn <id-token [{:keys [:aws/creds
                         :aws.cog/client-id
                         :aws.cog/pool-id
                         :canary-username
                         :canary-password]}]
  (go-try
    (->> (aws/<cogisp-user-password-auth
           creds
           {:cog-client-id client-id}
           {:username canary-username
            :password canary-password})
         <!
         res/data
         :AuthenticationResult
         :IdToken)))

(defn <verify-deployment
  [{:keys [:aws/creds
           :box/schema
           :box/entity
           :aws.cog/client-id
           :aws.cog/pool-id
           :canary-username
           :canary-password]
    :as opts}]
  (go-try
    (let [conn (<? (box/<conn schema))

          _ (ks/pn "Connection: " (not (nil? conn)))

          res (<? (aws/<cogisp-admin-set-user-password
                    creds
                    {:username canary-username
                     :password canary-password
                     :pool-id pool-id}))

          _ (ks/pn "Set username / password on canary user: "
              (not (res/anom res)))

          token (<? (<id-token opts))

          _ (ks/pn "Created token: "
              (not (nil? token)))

          sub (-> token jwt/from-jwt :payload :sub)

          _ (ks/pn "User sub: " sub)

          res (<? (box/<sync-entities
                    conn
                    [(merge
                       entity
                       {:box.sync/owner-id sub})]
                    token))]

      (if (box/has-failures? res)
        (ks/<spy "Sync Failed" res)
        (ks/pn "Sync verified")))))

(comment

  (require 'zkdev.schema)

  (<verify-deployment
    {:box/schema
     zkdev.schema/SCHEMA
     :aws/creds
     (:dev-creds
      (ks/edn-read-string
        (ks/slurp-cljs "~/.zkdev-prod-secrets.edn")))
     :box/entity {:box.canary/id "foo"
                  :some/other "key"
                  :foo/bar 123}
     :aws.cog/pool-id "us-west-2_H4MFCDqBJ"
     :aws.cog/client-id "59dplcj3r4mc12jta38djmhi3g"
     

     :canary-username "zk+zkdev-prod-canary@heyzk.com"
     :canary-password "Test1234!"}))



