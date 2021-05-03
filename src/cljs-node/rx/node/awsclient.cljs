(ns rx.node.awsclient
  (:require [rx.kitchen-sink :as ks]
            [cljs.core.async
             :refer [<! chan put! close!]
             :refer-macros [go go-loop]]
            [goog.object :as gobj]))

(def AWS (js/require "aws-sdk"))
(def S3 (.-S3 AWS))
(def SNS (.-SNS AWS))
(def DynamoDB (.-DynamoDB AWS))
(def DocumentClient (.-DocumentClient (.-DynamoDB AWS)))
(def CognitoIdentityServiceProvider (.-CognitoIdentityServiceProvider AWS))

(defn set-credentials [{:keys [access-id
                               secret-key
                               region]}]
  (set!
    (.. AWS -config -credentials)
    (AWS.Credentials. access-id secret-key))
  (set!
    (.. AWS -config -region)
    region)
  (.. AWS -config))

(defn aws->clj [o]
  (ks/from-json (.stringify js/JSON o)))

(defn <aws [service-class method & args]
  (let [ch (chan)
        service-class-instance (service-class.)]
    (apply js-invoke
      (service-class.)
      method
      (concat
        (remove nil? (map clj->js args))
        [(fn [err data]
           (put! ch
             (if err
               [nil (aws->clj err)]
               [(aws->clj data)])))]))
    ch))

(defn <aws2 [service-class method
             {:keys [args
                     raw-result?]}]
  (let [ch (chan)
        process-res (if raw-result? identity aws->clj)
        service-class-instance (service-class.)]
    (if (gobj/get service-class-instance method)
      (apply js-invoke
        service-class-instance
        method
        (concat
          (remove nil? (map clj->js args))
          [(fn [err data]
             (put! ch
               (if err
                 [nil (process-res err)]
                 [(process-res data)]))
             (close! ch))]))
      (do
        (put! ch
          [nil {:message (str"Method " method " not found on service class " service-class)}])
        (close! ch)))
    ch))

(defn <s32 [method opts]
  (<aws2 S3 method opts))

(defn s3-get-signed-url
  [operation params]
  (.getSignedUrl
    (S3.)
    operation
    (clj->js params)))

(defn <dyndoc [fn-name payload]
  (go
    (let [[obj err] (<! (<aws
                          DynamoDB.DocumentClient
                          fn-name
                          payload))]
      (if err
        [nil err]
        [obj]))))

(defn <dynamodb [fn-name payload]
  (go
    (let [[obj err] (<! (<aws
                          DynamoDB
                          fn-name
                          payload))]
      (if err
        [nil err]
        [obj]))))

(defn <cogsp [fn-name payload]
  (go
    (let [[obj err] (<! (<aws
                          CognitoIdentityServiceProvider
                          fn-name
                          payload))]
      (if err
        [nil err]
        [obj]))))

(defn <sns [fn-name payload]
  (go
    (let [[obj err] (<! (<aws
                          SNS
                          fn-name
                          payload))]
      (if err
        [nil err]
        [obj]))))

(defn <s3 [fn-name payload & [config]]
  (go
    (let [[obj err] (<! (<aws
                          S3
                          fn-name
                          payload
                          config))]
      (if err
        [nil err]
        [obj]))))

(comment

  (go
    (prn (<! (<s3
               "putObject"
               (ks/from-json
                 "{\"Body\":\"...\",\"Bucket\":\"canter-prod-user-media\",\"Key\":\"thumbs/images/894b1d76-8d3c-4b4e-90d5-45a59bfc00a6-l0-001_300\",\"ContentType\":\"image/jpeg\",\"CacheControl\":\"public, max-age=31536000\",\"ACL\":\"public-read\"}")))))

  (go
    (prn (<! (<cogsp "signUp"
               {:ClientId "3u7loan85m1ong28r7p909o95m"
                :Username "zk+6@heyzk.com"
                :Password "Foobarbaz1"}))))

  (go
    (prn (<! (<cogsp "resendConfirmationCode"
               {:ClientId "3u7loan85m1ong28r7p909o95m"
                :Username "zk+6@heyzk.com"}))))


  (go
    (prn (<! (<cogsp "confirmSignUp"
               {:ConfirmationCode "021406"
                :ClientId "3u7loan85m1ong28r7p909o95m"
                :Username "zk+6@heyzk.com"}))))

  (go
    (prn (<! (<cogsp "adminGetUser"
               {:UserPoolId "us-west-2_5ORqg6dLg"
                :Username "zk+6@heyzk.com"}))))

  (go
    (prn (<! (<cogsp "initiateAuth"
               {:AuthFlow "USER_PASSWORD_AUTH"
                :ClientId "3u7loan85m1ong28r7p909o95m"
                :AuthParameters {:USERNAME "zk+6@heyzk.com"
                                 :PASSWORD "Foobarbaz1"}}))))


  )
