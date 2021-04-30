(ns rx.aws
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.anom :as anom
             :refer-macros [<defn <?]]
            [goog.object :as gobj]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :refer [<! chan put! close! buffer]
             :refer-macros [go go-loop]]

            [rx.specs]))

(defn service-key->class [AWS k-or-s]
  (or
    (get
      {:s3 (.-S3 AWS)
       :sns (.-SNS AWS)
       :ddb (.-DynamoDB AWS)
       :dyndoc (.. AWS -DynamoDB -DocumentClient)
       :cogisp (.-CognitoIdentityServiceProvider AWS)
       :cognito-isp (.-CognitoIdentityServiceProvider AWS)
       :cwl (.-CloudWatchLogs AWS)}
      k-or-s)
    (gobj/get AWS k-or-s)))

(defn set-global-credentials!
  [AWS {:keys [access-id
               secret-key
               session-token
               region
               dynamoDbCrc32]
        :as opts}]
  (let [access-id (or access-id
                      (:aws.creds/access-id opts))
        secret-key (or secret-key
                       (:aws.creds/secret-key opts))
        region (or secret-key
                   (:aws.creds/region opts))]
    (when (and access-id secret-key)
      (set!
        (.. AWS -config -credentials)
        (AWS.Credentials.
          #js {:accessKeyId access-id
               :secretAccessKey secret-key
               :sessionToken session-token})))
    (when region
      (set!
        (.. AWS -config -region)
        region))

    (when (not (nil? dynamoDbCrc32))
      (set!
        (.. AWS -config -dynamoDbCrc32)
        dynamoDbCrc32))
    (.. AWS -config)))

(defn aws->clj [o]
  (ks/from-json (.stringify js/JSON o)))

#_(s/fdef <aws
  :args
  (s/cat
    :AWS #(not (nil? %))
    :opts (s/nilable
            (s/keys
              :req-un
              [::access-id
               ::secret-key
               ::region]))
    :service-class keyword?
    :method (s/and
              string?
              #(not (empty? %)))
    :payload sequential?))

(defn <aws
  ([opts service-key method args]
   (<aws (::AWS opts) opts service-key method args))
  ([AWS
    {:keys [access-id
            secret-key
            session-token
            region]
     :as opts}
    service-key
    method
    args]
   (let [ch (chan)
         start-time (ks/now)]
     (try
       (let [encode (or (:clj->aws opts) clj->js)
             decode (or (:aws->clj opts) aws->clj)
             service-class (service-key->class
                             AWS
                             service-key)

             access-id (or access-id
                           (:aws.creds/access-id opts)
                           (::access-id opts))
             secret-key (or secret-key
                            (:aws.creds/secret-key opts)
                            (::secret-key opts))
             region (or region
                        (:aws.creds/region opts)
                        (::region opts))

             creds {:aws.creds/access-id access-id
                    :aws.creds/secret-key secret-key
                    :aws.creds/region region}]

         (comment
           (when-not access-id
             (ks/throw-str "Creds missing access-id"))
           (when-not secret-key
             (ks/throw-str "Creds missing secret-key"))
           (when-not region
             (ks/throw-str "Creds missing region")))
         (when-not service-class
           (ks/throw-str "No service class found for " service-key))

         (let [service-class-instance
               (service-class.
                 (clj->js
                   (merge
                     (when access-id
                       {:accessKeyId access-id})
                     (when secret-key
                       {:secretAccessKey secret-key})
                     (when session-token
                       {:sessionToken session-token})
                     (when region
                       {:region region}))))]
           (when-not (aget service-class-instance method)
             (close! ch)
             (ks/throw-str
               "No method "
               method
               " found for service key "
               service-key))
           #_(set-global-credentials! AWS opts)
           (apply
             js-invoke
             service-class-instance
             method
             (concat
               (encode args)
               [(fn [err data]
                  (try
                    (let [decoded-data (decode data)]
                      (put! ch
                        (if err
                          (anom/anom
                            {:desc (.-message err)
                             ::code (.-code err)
                             ::method method
                             ::service-key service-key
                             ::args args
                             ::creds creds
                             ::access-id access-id
                             ::region region
                             :rx.res/duration-ms (- (ks/now) start-time)})
                          (merge
                            {::data decoded-data
                             ::method method
                             ::service-key service-key
                             ::args args
                             ::creds creds
                             ::access-id access-id
                             ::region region
                             :rx.res/duration-ms (- (ks/now) start-time)})))
                      (close! ch))
                    (catch js/Error e
                      (put! ch
                        (anom/add-frame
                          (anom/from-err e)
                          (anom/var->frame #'<aws)))
                      (close! ch))))]))))
       (catch js/Error e
         (ks/pn e)
         (close! ch)
         (throw e)))
     ch)))

(defn <dyndoc [AWS creds method pl]
  (<aws AWS creds :dyndoc method [pl]))

(defn res->last-evaluated-key [res]
  (let [data (res/data res)]
    (cond
      (map? data) (:LastEvaluatedKey data)
      (sequential? data)
      (-> data last :LastEvaluatedKey))))

(defn res->next-token [res]
  (let [data (res/data res)]
    (cond
      (map? data) (:nextToken data)
      (sequential? data)
      (-> data last :nextToken))))

(defn paging-map-for [{:keys [::service-key
                              ::method]
                       :as res}]
  (cond
    (= service-key :dyndoc)
    (when-let [k (-> res res->last-evaluated-key)]
      {:ExclusiveStartKey k})

    (= service-key :cwl)
    (when-let [k (res->next-token res)]
      {:nextToken k})

    :else nil))

(<defn <next [AWS {:keys [::data
                          ::method
                          ::service-key
                          ::args
                          ::creds]
                   :as res}]
  (when (paging-map-for res)
    (<? (<aws
          AWS
          creds
          service-key
          method
          (into
            [(merge
               (first args)
               (paging-map-for res))]
            (rest args))))))

(<defn <next-n [AWS max-iters res-or-ch]
  (let [res (ks/<& res-or-ch)
        {:keys [iters ress]}
        (loop [last-res res
               ress [res]
               iters 0]
          (if (or (>= iters max-iters)
                  (not (paging-map-for last-res)))
            {:iters iters
             :ress ress}
            (let [next-res (<? (<next AWS last-res))]
              (recur
                next-res
                (conj ress next-res)
                (inc iters)))))

        last-res (last ress)

        data (->> ress
                  (map ::data)
                  (remove nil?)
                  vec)
        anom (->> ress
                  (filter anom/?)
                  (remove nil?)
                  vec
                  first)]
    (if anom
      anom
      (merge
        {::data data
         ::iters iters}
        (dissoc last-res ::data)))))

(defn <stream [AWS res-or-ch]
  (let [ch (chan (buffer 1))
        max-iters 1000]
    (go
      (let [res (ks/<& res-or-ch)

            _ (>! ch res)

            ;;_ (assert (= :dyndoc (::service-key res)))

            _ (loop [last-res res
                     iters 0]
                (if (or (>= iters max-iters)
                        (not (paging-map-for last-res)))
                  nil
                  (let [next-res (<! (<next AWS last-res))]
                    (>! ch next-res)
                    (recur
                      next-res
                      (inc iters)))))]
        (close! ch)))
    ch))



;; Cognito

(defn from-jwt [s]
  (when s
    (let [[header-str
           payload-str
           sig-str] (str/split s ".")]

      {:header (-> header-str
                   ks/from-base64-str
                   ks/from-json)
       :payload (-> payload-str
                    ks/from-base64-str
                    ks/from-json)
       :signature sig-str})))

(defn jwt-str->payload [s]
  (:payload (from-jwt s)))

(defn jwt-fresh? [s & [now]]
  (when-let [exp (-> s jwt-str->payload :exp)]
    (< (or now (ks/now)) (* exp 1000))))

(s/fdef <cogisp-user-password-auth
  :args
  (s/cat
    :AWS #(not (nil? %))
    :opts (s/nilable
            (s/keys
              :req-un
              [::access-id
               ::secret-key
               ::region]))
    :cog-opts (s/nilable
                (s/keys
                  :req-un
                  [::cog-client-id
                   ::username
                   ::password]))))

(<defn <cogisp-user-password-auth [AWS
                                   creds
                                   {:keys [cog-client-id]}
                                   {:keys [username
                                           password]}]
  (let [res (<? (<aws
                  AWS
                  creds
                  :cogisp
                  "initiateAuth"
                  [{:ClientId cog-client-id
                    :AuthFlow "USER_PASSWORD_AUTH"
                    :AuthParameters
                    {:USERNAME username
                     :PASSWORD password}}]))]
    
    res))

(s/def :aws.creds/access-id :rx.specs/non-empty-string)
(s/def :aws.creds/secret-key :rx.specs/non-empty-string)
(s/def :aws.creds/region :rx.specs/non-empty-string)

(s/def ::creds
  (s/or :bare-creds
        (s/keys
          :req-un [:aws.creds/access-id
                   :aws.creds/secret-key
                   :aws.creds/region])
        :ns-creds
        (s/keys
          :req [:aws.creds/access-id
                :aws.creds/secret-key
                :aws.creds/region])))

(defn id-token [auth]
  (-> auth
      :AuthenticationResult
      :IdToken))

(defn access-token [auth]
  (-> auth
      :aws.cog/access-token))

(defn cog-auth-data->cog-tokens [cog-auth-data]
  (let [ar (:AuthenticationResult cog-auth-data)]
    (merge
      (when-let [v (:AccessToken ar)]
        {:aws.cog/access-token v})
      (when-let [v (:ExpiresIn ar)]
        {:aws.cog/expires-in v})
      (when-let [v (:TokenType ar)]
        {:aws.cog/token-type v})
      (when-let [v (:IdToken ar)]
        {:aws.cog/id-token v})
      (when-let [v (:RefreshToken ar)]
        {:aws.cog/refresh-token v}))))

(<defn <cog-auth-by-username-password
  [AWS
   config
   username
   password]
  
  (when-not (s/valid? (s/keys
                        :req [:aws.cog/client-id])
              config)
    (anom/throw-anom
      {:desc "Invalid cog client id"
       :code :incorrect
       ::client-id (-> config
                       :aws.cog/client-id)}))
  
  (when-not (s/valid? :rx.specs/non-empty-string username)
    (anom/throw-anom
      {:desc "Invalid username"
       :code :incorrect
       ::username username}))

  (when-not (s/valid? :rx.specs/non-empty-string password)
    (anom/throw-anom
      {:desc "Inavlid password"
       :code :incorrect
       ::password password}))

  (let [res (<? (<aws
                  AWS
                  config
                  :cogisp
                  "initiateAuth"
                  [{:ClientId (:aws.cog/client-id config)
                    :AuthFlow "USER_PASSWORD_AUTH"
                    :AuthParameters
                    {:USERNAME username
                     :PASSWORD password}}]))]
    
    (cog-auth-data->cog-tokens
        (::data res))))

(<defn <cog-refresh-auth [AWS config auth]
  (ks/spec-check-throw ::creds config)
  (ks/spec-check-throw
    (s/keys
      :req [:aws.cog/client-id])
    config)
  
  (let [{:keys [:aws.cog/client-id]} config
        id-token (-> auth :aws.cog/id-token)
        refresh-token (-> auth :aws.cog/refresh-token)

        id-payload (jwt-str->payload id-token)
        
        {:keys [aus sub iss exp]} id-payload

        login-key (when iss (str/replace iss #"https?://" ""))
        login-val id-token
        res (<? (<aws
                  AWS
                  config
                  :cogisp
                  "initiateAuth"
                  [{:AuthFlow "REFRESH_TOKEN_AUTH"
                    :ClientId client-id
                    :AuthParameters
                    {:REFRESH_TOKEN refresh-token}}]))]
    (if (anom/? res)
      res
      (::data res))))

(<defn <cog-global-sign-out [AWS config auth]

  (ks/spec-check-throw ::creds config)

  (let [{:keys [:aws.cog/client-id]} config
        res (<? (<aws
                  AWS
                  config
                  :cogisp
                  "globalSignOut"
                  [{:AccessToken (access-token auth)}]))]
    res))

(s/fdef <cogisp-admin-set-user-password
  :args
  (s/cat
    :AWS #(not (nil? %))
    :opts (s/nilable
            (s/keys
              :req-un
              [::access-id
               ::secret-key
               ::region]))
    :cog-opts (s/nilable
                (s/keys
                  :req-un
                  [::cog-pool-id
                   ::username
                   ::password]))))

(defn <cogisp-admin-set-user-password
  [AWS
   creds
   {:keys [username
           password
           pool-id]}]
  (<aws
    AWS
    creds
    :cogisp
    "adminSetUserPassword"
    [{:Username username
      :Password password
      :Permanent true
      :UserPoolId pool-id}]))

(<defn <cogisp-refresh-auth [AWS
                             creds
                             {:keys [cog-client-id
                                     cog-region]}
                             auth]
  (let [id-token (-> auth :AuthenticationResult :IdToken)
        refresh-token (-> auth :AuthenticationResult :RefreshToken)
        id-payload (jwt-str->payload id-token)

        {:keys [aus sub iss exp]} id-payload

        login-key (when iss (str/replace iss #"https?://" ""))
        login-val id-token
        res (<? (<aws
                  AWS
                  creds
                  :cogisp
                  "initiateAuth"
                  [{:AuthFlow "REFRESH_TOKEN_AUTH"
                    :ClientId cog-client-id
                    :AuthParameters
                    {:REFRESH_TOKEN refresh-token}}]))]
    res))

(defn auth-fresh? [auth]
  (->> auth
       :aws.cog/id-token
       jwt-fresh?))

(<defn <cogisp-refresh-auth-if-needed
  [AWS
   creds
   cog-opts
   auth]
  (if (auth-fresh? auth)
    (do
      (prn "FRESH")
      {:rx.res/data auth})
    (do
      (prn "NOT FRESH")
      (<? (<cogisp-refresh-auth AWS creds cog-opts auth)))))

(comment

  (def creds
    (:dev-creds
     (ks/edn-read-string
       (ks/slurp-cljs "~/.rx-secrets.edn"))))

  (ks/<pp
    (<cogisp-admin-set-user-password
      (js/require "aws-sdk/dist/aws-sdk-react-native")
      creds
      {:username "zk@heyzk.com"
       :password "foobarbaz"
       :cog-pool-id "us-west-2_W1OuUCkYR"}))


  (go
    (def auth
      (<!
        (res/<data
          (<cogisp-user-password-auth
            (js/require "aws-sdk/dist/aws-sdk-react-native")
            creds
            {:username "zk@heyzk.com"
             :password "Test1234"
             :cog-client-id "1em482svs0uojb8u7kmooulmmj"})))))

  (ks/<pp
    (<cogisp-refresh-auth
      (js/require "aws-sdk/dist/aws-sdk-react-native")
      creds
      {:cog-client-id "1em482svs0uojb8u7kmooulmmj"}
      auth))


  (ks/<pp
    (<cogisp-refresh-auth-if-needed
      (js/require "aws-sdk/dist/aws-sdk-react-native")
      creds
      {:cog-client-id "1em482svs0uojb8u7kmooulmmj"}
      auth))

  ;; client id 1em482svs0uojb8u7kmooulmmj
  ;; pool id us-west-2_W1OuUCkYR
  ;; pool arn arn:aws:cognito-idp:us-west-2:753209818049:userpool/us-west-2_W1OuUCkYR

  )
