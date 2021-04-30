(ns rx.browser.aws
  (:require [rx.kitchen-sink :as ks]
            [aws-sdk]
            [rx.aws :as aws]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [clojure.core.async
             :as async
             :refer [go <!]]))

(def AWS js/AWS)

(def <aws (partial aws/<aws AWS))

(comment

  ;; Cognito challenges
  ;; + NEW_PASSWORD_REQUIRED

  (gol
    (ks/pp
      (<?
        (aws/<aws
          AWS
          {:region "us-west-2"}
          "CognitoIdentityServiceProvider"
          "initiateAuth"
          [{:AuthFlow "USER_PASSWORD_AUTH"
            :ClientId "6p0n57fhplmlb62g6f17o2a8bk"
            :AuthParameters {"USERNAME" "zk@heyzk.com"
                             "PASSWORD" "foobarbaz"}}]))))

  )
