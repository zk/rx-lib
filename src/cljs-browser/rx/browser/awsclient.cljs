(ns rx.browser.awsclient
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [cljs.core.async
             :refer [<! chan put! close!]
             :refer-macros [go go-loop]]
            [goog.object :as gobj]
            #_[cljsjs.aws-sdk-js]
            [cljs.spec.alpha :as s]))

(def AWS #_ js/AWS)
(def S3 #_(.-S3 AWS))
(def SNS #_(.-SNS AWS))
(def DynamoDB #_(.-DynamoDB AWS))
(def DocumentClient #_(.. AWS -DynamoDB -DocumentClient))
(def CognitoIdentityServiceProvider #_(.-CognitoIdentityServiceProvider AWS))

(def service-key->class
  {:s3 S3
   :sns SNS
   :ddb DynamoDB
   :dyndoc DocumentClient
   :cognito-isp CognitoIdentityServiceProvider})

(defn set-global-credentials!
  [{:keys [access-id
           secret-key
           session-token
           region
           dynamoDbCrc32]}]
  (set!
    (.. AWS -config -credentials)
    (AWS.Credentials.
      #js {:accessKeyId access-id
           :secretAccessKey secret-key
           :sessionToken session-token}))
  (set!
    (.. AWS -config -region)
    region)

  (when (not (nil? dynamoDbCrc32))
    (set!
      (.. AWS -config -dynamoDbCrc32)
      dynamoDbCrc32))
  (.. AWS -config))

(defn aws->clj [o]
  (ks/from-json (.stringify js/JSON o)))

(defn <aws [service-class method payload
            & [{:keys [access-id
                       secret-key
                       session-token
                       region
                       raw?]
                :as creds}]]
  (let [ch (chan)]
    (set-global-credentials! creds)
    (apply
      js-invoke
      (service-class.
        (clj->js
          {:accessKeyId access-id
           :secretAccessKey secret-key
           :sessionToken session-token
           :region region}))
      method
      [(clj->js payload)
       (fn [err data]
         (put! ch
           (if err
             [nil (aws->clj err)]
             [(aws->clj data)])))])
    ch))

(s/fdef <awsres
  :args
  (s/cat
    :service-class keyword?
    :method (s/and
              string?
              #(not (empty? %)))
    :payload sequential?
    :opts (s/nilable
            (s/keys
              :req-un
              [::access-id
               ::secret-key
               ::region]))))

(defn data->last-evaluated-key [data]
  (cond
    (map? data) (:LastEvaluatedKey data)
    (sequential? data)
    (-> data :LastEvaluatedKey)))

(defn <awsres [service-key
               method
               args
               & [{:keys [access-id
                          secret-key
                          session-token
                          region]
                   :as opts}]]
  (let [ch (chan)
        encode (or (:clj->aws opts) clj->js)
        decode (or (:aws->clj opts) aws->clj)
        service-class (get service-key->class service-key)]
    (when-not service-class
      (ks/throw-str "No service class found for " service-key))
    (set-global-credentials! opts)
    (apply
      js-invoke
      (service-class.
        (clj->js
          {:accessKeyId access-id
           :secretAccessKey secret-key
           :sessionToken session-token
           :region region}))
      method
      (concat
        (encode args)
        [(fn [err data]
           (let [decoded-data (decode data)]
             (put! ch
               (merge
                 (when err
                   {:rx.res/anom
                    {:rx.anom/desc
                     (.-message err)
                     :rx.anom/js-stack
                     (.-stack err)}})
                 (when decoded-data
                   {:rx.res/data decoded-data})
                 {::method method
                  ::service-key service-key
                  ::args args
                  ::creds {:access-id access-id
                           :secret-key secret-key
                           :region region}}))))]))
    ch))

(defn <dyndoc [method pl opts]
  (go
    (let [res (<! (<awsres :dyndoc method [pl] opts))
          last-evaluated-key (data->last-evaluated-key
                               (res/data res))]
      (merge
        res
        (when last-evaluated-key
          {::last-evaluated-key last-evaluated-key})))))

(defn last-evaluated-key [res]
  (-> res ::last-evaluated-key))

(defn <next [{:keys [:rx.res/data
                     ::method
                     ::service-key
                     ::args
                     ::creds]
              :as res}]
  (assert (= service-key :dyndoc))
  (go
    (when (last-evaluated-key res)
      (<! (<dyndoc
            method
            (merge
              (first args)
              {:ExclusiveStartKey (last-evaluated-key res)})
            creds)))))

(defn <next-n [max-iters res-or-ch]
  (go
    (let [res (if (ks/chan? res-or-ch)
                (<! res-or-ch)
                res-or-ch)
          _ (assert (= :dyndoc (::service-key res)))
          {:keys [iters ress]}
          (loop [last-res res
                 ress [res]
                 iters 0]
            (if (or (>= iters max-iters)
                    (not (last-evaluated-key last-res)))
              {:iters iters
               :ress ress}
              (let [next-res (<! (<next last-res))]
                (recur
                  next-res
                  (conj ress next-res)
                  (inc iters)))))

          last-res (last ress)
          data (->> ress
                    (map :rx.res/data)
                    (remove nil?)
                    vec)
          anom (->> ress
                    (map :rx.res/anom)
                    (remove nil?)
                    vec
                    first)]
      (merge
        (when data
          {:rx.res/data data})
        (when anom
          {:rx.res/anom anom})
        {::iters iters}
        (dissoc last-res :rx.res/data)))))

(comment

  ;; client id 3u7loan85m1ong28r7p909o95m
  ;; pool id us-west-2_5ORqg6dLg
  ;; pool arn arn:aws:cognito-idp:us-west-2:753209818049:userpool/us-west-2_5ORqg6dLg

  (go
    (ks/pp
      (<!
        (<awsres
          :cognito-isp
          "adminCreateUser"
          [{:UserPoolId "us-west-2_5ORqg6dLg"
            :Username "zk+2@heyzk.com"
            :MessageAction "SUPPRESS"
            :TemporaryPassword (str "TempPassword"
                                    (int
                                      (* 10000 (rand))))
            :UserAttributes
            [{:Name "email"
              :Value "zk+2@heyzk.com"}]}]
          {:access-id ""
           :secret-key ""
           :region "us-west-2"
           :dynamoDbCrc32 false}))))

  (go
    (->> (<dyndoc
           "query"
           {:TableName "canter-prod-pk-string"
            :IndexName "dataTypeOwnerId-rmdrTriggerTs-index"
            :KeyConditionExpression "dataTypeOwnerId = :dtoid AND rmdrTriggerTs BETWEEN :low AND :high"
            :ExpressionAttributeValues
            {":dtoid" "rmdr:0a8c2df1-4af6-48be-bd29-d0a691649ea2"
             ":low" (ks/now)
             ":high" (+ (ks/now) (* 1000 60 60 24 30))}
            :Limit 100000}
           {:access-id ""
            :secret-key ""
            :region "us-west-2"
            :dynamoDbCrc32 false})
         (<next-n 10)
         <!
         res/data
         ks/pp))


  )
