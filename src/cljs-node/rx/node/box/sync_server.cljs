(ns rx.node.box.sync-server
  (:require [rx.specs]
            [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.anom :as anom
             :refer-macros
             [<defn <? go-anom]]
            [rx.node.lambda :as lambda]
            [rx.node.jsonwebtoken :as jwt]
            [rx.http :as http]
            [clojure.string :as str]
            [rx.node.aws :as aws]
            [clojure.spec.alpha :as s]
            [rx.box.common :as com]
            [cljs.core.async :refer [<! chan] :refer-macros [go]]
            [cljs.test :as test :refer-macros [deftest is]]
            [cljs.spec.alpha :as s]
            [cljs.core.async :as async
             :refer [<! chan put! timeout close!] :refer-macros [go]]))

(defn cwlog [pl]
  (try
    (lambda/lambda-println
      (ks/to-json pl))
    (catch js/Error e
      (lambda/lambda-println
        (ks/to-json
          {:event-name "canterapi.error-logging"
           :error-text (str e)
           :error-stacktrace (.-stack e)})))))

(defn wrap-access-control [h]
  (fn [req respond raise]
    (h
      req
      (fn [resp]
        (respond (assoc-in resp [:headers "Access-Control-Allow-Origin"] "*")))
      raise)))

(defn wrap-log-request [h]
  (fn [req respond raise]
    (prn "Request" (:request-method req) (:uri req))
    (h req respond raise)))

(defn root-handler [req resp raise]
  (resp
    {:body "hello world!"
     :headers {"Content-Type" "text/html"}
     :status 200}))

(defn decode-twilio-param [s]
  (when s
    (js/decodeURIComponent (str/replace s #"\+" "%20"))))

(defn twilio-body->map [s]
  (let [parts (str/split s "&")]
    (->> parts
         (map (fn [part]
                (let [[k v] (str/split part "=")]
                  [k (decode-twilio-param v)])))
         (into {}))))

(defn throw-err [err-map]
  (throw
    (ex-info "" err-map)))

(defn throw-aws-err [err]
  (throw-err
    {:error :aws-error
     :error-text (str err)}))

(defn wrap-mod-request
  [handler f & args]
  (fn [req respond raise]
    (handler
      (apply f req args)
      respond
      raise)))

(defn log-json [pl]
  (println (ks/to-json pl)))

(defn token-valid?
  [token & [prop-matches]])

(defn <ddb-transit-by-pk [pk]
  (go
    (when pk
      (let [[res err] (<! (aws/<dyndoc
                            nil
                            "get"
                            {:TableName "canter-prod-pk-string"
                             :Key {:pk pk}}))]
        (if res
          [(-> res
               :Item
               :transit
               ks/from-transit)]
          err)))))

(defn ddb-owner-id [attr-schema obj]
  (let [{:keys [:box.attr/ddb-owner-key]} attr-schema
        owner-key (or ddb-owner-key :box.sync/owner-id)]
    (get obj owner-key)))

(defn data-type-owner-id [{:keys [:box.attr/ddb-owner-key]
                           :as schema}
                          m]
  (let [pk-key (com/resolve-ident-key schema m)]
    (str
      pk-key
      "__"
      (get m
        (or ddb-owner-key :box.sync/owner-id)))))

#_(defn default-to-ddb-item [schema obj]

  (ks/spec-check-throw
    :box/schema
    schema
    "Invalid schema")

  (ks/spec-check-throw
    #(not (nil? %))
    obj
    "Object to convert to ddb item can't be nil")
  
  (let [pk-key (com/resolve-ident-key schema obj)

        _ (when-not pk-key
            (throw (ex-info "Calculated pk key is nil"
                     {:object obj
                      :schema schema})))
         
        attr-schema (get
                      (-> schema :box/attrs)
                      pk-key)
        
        pk-val (get obj pk-key)
        {:keys [:box.attr/ddb-owner-key
                :box.attr/ddb-sort-numeric-key]}
        attr-schema
        
        owner-key (or ddb-owner-key :box.sync/owner-id)

        owner-id (get obj owner-key)

        _ (when-not owner-id
            (throw
              (ex-info "Owner id not found on object"
                {:object obj
                 :owner-key owner-key})))

        sort-numeric-key (or ddb-sort-numeric-key
                             :box.pers/created-ts)
        
        sort-numeric-value (get obj sort-numeric-key)]
    
    (merge
      {:pk pk-val
       :transit (ks/to-transit obj)}
      {:version (or (get obj :box.sync/match-version) 0)}
      (when sort-numeric-value
        {:sortNumeric sort-numeric-value})
      (when-let [s (data-type-owner-id schema obj)]
        {:dataTypeOwnerId s})
      (when-let [updated-ts (:box.pers/updated-ts obj)]
        {:updatedTs updated-ts})
      (->> obj
           (map (fn [[k v]]
                  [(str (namespace k)
                        "/"
                        (name k))
                   v]))
           (into {})))))

(defn matches-owner-id? [{:keys [:box.attr/ddb-owner-key]}
                         obj sub]
  (let [owner-key (or ddb-owner-key :box.sync/owner-id)
        owner-id (get obj owner-key)]
    (= sub owner-id)))

(defn attr-spec-for-obj [schema obj]
  (let [ident-key (com/resolve-ident-key schema obj)]
    (when ident-key
      (get
        (-> schema :box/attrs)
        ident-key))))

(defn ensure-all-objs-owned
  [schema objs auth]
  (let [{:keys [sub]} auth
        non-user-objs
        (remove
          #(matches-owner-id?
             (attr-spec-for-obj schema %)
             %
             sub)
          objs)]
    (when-not (empty? non-user-objs)
      (throw
        (ex-info
          "Some objs not owned by auth"
          {:event-name "sync.objs-not-matching-owner-anomaly"
           :non-owned-objs non-user-objs
           :sub (:sub auth)})))))

(defn update-op? [op]
  (= (:box.sync/op-key op) :box.op/update))

(defn op->ent [op]
  (:box.sync/entity op))

(defn all-ops-owned? [schema ops auth]
  [schema ops auth]
  (let [ents (->> ops
                  (filter update-op?)
                  (map op->ent))

        {:keys [sub]} auth
        non-user-objs
        (remove
          #(matches-owner-id?
             (attr-spec-for-obj schema %)
             %
             sub)
          ents)]
    (empty? non-user-objs)))

(defn ensure-all-ops-valid
  [schema ops auth]

  (ks/spec-check-throw
    (s/coll-of :box.sync/op)
    ops)
  
  (let [objs (->> ops
                  (filter update-op?)
                  (map :box.sync/entity))

        {:keys [sub]} auth
        non-user-objs
        (remove
          #(matches-owner-id?
             (attr-spec-for-obj schema %)
             %
             sub)
          objs)]
    (when-not (empty? non-user-objs)
      (throw
        (ex-info
          "Some objs not owned by auth"
          {:event-name "sync.objs-not-matching-owner-anomaly"
           :non-owned-objs non-user-objs
           :sub (:sub auth)})))))

(defn obj-to-ddb-update [schema obj & [ignore-match-version?]]

  (ks/spec-check-throw
    :box/schema
    schema
    "Invalid schema")

  (ks/spec-check-throw
    #(not (nil? %))
    obj
    "Object to convert to ddb item can't be nil")
  
  (let [{:keys [:box/ddb-data-table-name]} schema
        [pk-key pk-val] (com/resolve-ident schema obj)

        _ (when-not pk-key
            (throw (ex-info "Calculated pk key is nil"
                     {:object obj
                      :schema schema})))
         
        attr-schema (get
                      (-> schema :box/attrs)
                      pk-key)
        
        pk-val (get obj pk-key)
        {:keys [:box.attr/ddb-owner-key
                :box.attr/ddb-sort-numeric-key]}
        attr-schema
        
        owner-key (or ddb-owner-key :box.sync/owner-id)

        owner-id (get obj owner-key)

        _ (when-not owner-id
            (throw
              (ex-info "Owner id not found on object"
                {:object obj
                 :owner-key owner-key})))

        sort-numeric-key (or ddb-sort-numeric-key
                             :box.pers/created-ts)
        
        sort-numeric-value (get obj sort-numeric-key)

        version (or (get obj :box.sync/match-version) 0)

        data-type-owner-id (data-type-owner-id schema obj)

        updated-ts (:box.pers/updated-ts obj)

        obj (dissoc obj :box.sync/ignore-match-version?)]

    (merge
      {:TableName ddb-data-table-name
       :Key {:pk pk-val}
       :UpdateExpression
       (->> (concat
              ["set #transit = :transit"
               (when (not ignore-match-version?)
                 "#version = :nextVersion")
               (when sort-numeric-value
                 "#sortNumeric = :sortNumeric")
               (when data-type-owner-id
                 "#dataTypeOwnerId = :dataTypeOwnerId")
               (when updated-ts
                 "#updatedTs = :updatedTs")]
              (->> obj
                   (sort-by first)
                   (map-indexed
                     (fn [i [k v]]
                       (str
                         "#prop" i
                         " = "
                         ":prop" i)))))
            (remove nil?)
            (interpose ", ")
            (apply str))
       :ExpressionAttributeNames
       (merge
         {"#transit" "transit"}
         (when (not ignore-match-version?)
           {"#version" "version"})
         (when sort-numeric-value
           {"#sortNumeric" "sortNumeric"})
         (when data-type-owner-id
           {"#dataTypeOwnerId" "dataTypeOwnerId"})
         (when updated-ts
           {"#updatedTs" "updatedTs"})
         (->> obj
              (sort-by first)
              (map-indexed
                (fn [i [k v]]
                  [(str "#prop" i)
                   (str (namespace k)
                        "/"
                        (name k))]))))
       :ExpressionAttributeValues
       (merge
         {":transit" (ks/to-transit obj)}
         (when (not ignore-match-version?)
           {":currentVersion" version
            ":nextVersion" (inc version)})
         (when sort-numeric-value
           {":sortNumeric" sort-numeric-value})
         (when data-type-owner-id
           {":dataTypeOwnerId" data-type-owner-id})
         (when updated-ts
           {":updatedTs" updated-ts})
         (->> obj
              (sort-by first)
              (map-indexed
                (fn [i [k v]]
                  [(str ":prop" i)
                   (get obj k)]))))}
      (when-not ignore-match-version?
        {:ConditionExpression "#version = :currentVersion"}))))

(set! *print-namespace-maps* false)

(defn <sync-objs
  [opts ops auth]
  (go
    (try
      (let [{:keys [:box/schema]} opts

            schema (com/apply-default-attrs schema)

            creds (:box/ddb-creds schema)

            sub (:sub auth)

            _ (when-not (empty? (->> ops
                                     (remove :box.sync/op-key)))
                (throw
                  (ex-info "Invalid ops found" nil)))

            _ (ensure-all-ops-valid
                schema
                ops
                auth)

            update-ops (->> ops
                            (filter update-op?))

            ident+ddb-updates
            (->> update-ops
                 (map (fn [op]
                        (let [obj (:box.sync/entity op)]
                          [(com/resolve-ident schema obj)
                           (obj-to-ddb-update schema obj
                             (:box.sync/ignore-match-version? op))])))

                 ;; important!!
                 doall)

            update-results
            (->> ident+ddb-updates
                 (map
                   (fn [[ident ddb-update]]
                     (go
                       (try
                         (let [res (<! (aws/<dyndoc creds "update" ddb-update))
                               anom (res/anom res)
                               version-mismatch?
                               (= (res/error-code res)
                                  "ConditionalCheckFailedException")]
                           
                           (merge
                             {:box/ident ident}

                             (when version-mismatch?
                               {:box.sync/failure-ts (ks/now)
                                :box.sync/anom-code
                                :box.sync/version-mismatch})

                             (when (and anom
                                        (not version-mismatch?))
                               {:box.sync/failure-ts (ks/now)
                                :rx.res/anom anom})

                             (when (and
                                     (not version-mismatch?)
                                     (not anom))
                               {:box.sync/success-ts (ks/now)})))
                         (catch js/Error e
                           (res/err->res e))))))
                 async/merge
                 (async/reduce conj [])
                 <!)

            has-failures? (->> update-results
                               (filter :box.sync/failure-ts)
                               empty?
                               not)]
        {:rx.res/data
         {:box.sync/results update-results
          :box.sync/has-failures? has-failures?}})
      (catch js/Error e
        (let [anom (res/err->anom e)]
          #_(cwlog
              {:event-name "sync.batch-step-anomaly"
               :anom anom
               :sub (:sub auth)})
          {:rx.res/anom
           (merge
             anom
             {:box.sync/ops ops})})))))

(defn chan? [x]
  (instance? cljs.core.async.impl.channels.ManyToManyChannel x))

(defn validate-request [h]
  (fn [req respond raise]
    (h
      req
      respond
      #_(fn [resp]
          (respond (assoc-in resp [:headers "Access-Control-Allow-Origin"] "*")))
      raise)))

(defn claims-match? [claims matches-map]
  (let [ks (keys matches-map)]
    (= matches-map
       (select-keys claims ks))))

(s/def ::cog-config
  (s/keys
    :req [:aws.cog/client-id
          :aws.cog/user-pool-id
          :aws.cog/allowed-use-claims
          :aws.cog/jwks-str]))

(defn verify-and-decode-cog-auth
  [{:keys [:aws.cog/client-id
           :aws.cog/user-pool-id
           :aws.cog/allowed-use-claims
           :aws.cog/jwks-str]
    :as cog-config}
   token]

  (ks/spec-check-throw
    ::cog-config
    cog-config
    "Incorrect cog config")

  (ks/spec-check-throw
    :rx.specs/non-empty-string
    token
    "Invalid token"
    {:token token})

  (let [[token-verified? err]
        (jwt/verify-token-with-jwks
          token
          jwks-str)

        {:keys [payload]
         :as decoded-token}
        (try
          (jwt/decode-token token)
          (catch js/Error e
            (throw (ex-info
                     "Invalid token"
                     {:token token
                      :error e}))))

        claims (-> decoded-token :payload)

        target-claims {:aud client-id
                       :iss (str "https://cognito-idp.us-west-2.amazonaws.com/"
                                 user-pool-id)}

        claims-verified? (claims-match?
                           claims
                           target-claims)

        allowed-use-claims (set
                             (or
                               allowed-use-claims
                               ["access" "id"]))

        use-verified? (get allowed-use-claims
                        (:token_use payload))]

    
    (if token-verified?
      (if claims-verified?
        (if use-verified?
          {:rx.res/data decoded-token}
          {:rx.res/anom {:rx.anom/desc (str "Couldn't verify token use: " (:token_use payload))}})
        {:rx.res/anom {:rx.anom/desc "Couldn't verify cognito claims"}
         ::claims claims
         ::target target-claims})
      {:rx.res/anom err})))

(s/def ::sync-opts
  (s/keys
    :req
    [:box/schema
     :aws.cog/client-id
     :aws.cog/user-pool-id
     :aws.cog/jwks-str
     :aws.cog/allowed-use-claims]))

(defn <body-str [req]
  (let [ch (chan)
        body (:body req)]
    (try
      (if (string? body)
        (do
          (put! ch body)
          (close! ch))
        (let [stream body
              !body (atom nil)]

          (.on stream "readable" (fn []
                                   (swap! !body str (.read stream))))

          (.on stream "end" (fn []
                              (try
                                (let [body @!body]
                                  (when-not (nil? @!body)
                                    (put! ch @!body))
                                  (close! ch))
                                (catch js/Error e
                                  (put! ch (anom/from-err e))))))))
      (catch js/Error e
        (put! ch (anom/from-err e))))
    ch))

(defn <handle-sync-request [opts req]
  (go
    (try
      (let [body (<! (<body-str req))
            
            decoded-body (ks/from-transit body)

            {:keys [:box.sync/ops :box.sync/auth]} decoded-body

            decoded-token (anom/throw-if-anom
                            (verify-and-decode-cog-auth opts auth))
            resp-body (ks/to-transit
                        (<? (<sync-objs
                              opts
                              ops
                              (:payload decoded-token))))]
        {:status 200
         :headers {"Content-Type" "application/json+transit"}
         :body resp-body})
      (catch js/Error e
        (let [anom (anom/from-err e)]
          {:status 500
           :body anom})))))

(defn create-ring-handler
  [{:keys [] :as opts}]

  (ks/spec-check-throw
    ::sync-opts
    opts
    "Missing standalone handler opts")

  (-> (fn [req resp raise]
        (go
          (let [res (<! (<handle-sync-request opts req))]
            (if (anom/? res)
              (raise res)
              (resp res)))))
      
      http/wrap-transit-response
      http/wrap-transit-request
      validate-request))


(comment
  
  ((standalone-handler
     {:ddb-data-table-name "foo"})
   {:uri "/foo"
    :request-method :get}
   (fn [resp]
     (prn "resp" resp))
   (fn [err]
     (prn "raise" err)))

  (gi-adapter {}))




