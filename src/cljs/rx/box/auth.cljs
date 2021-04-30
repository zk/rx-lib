(ns rx.box.auth
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :refer-macros [<defn <?]]
            [rx.jwt :as jwt]
            [rx.aws :as aws]
            [rx.box.common :as com]
            [rx.res :as res]
            [rx.anom :as anom]
            [rx.trace :as trace]
            [cljs.spec.alpha :as s]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]))

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

(defn ensure-aws-conn [{:keys [:box.pl/AWS] :as conn}]
  (when-not AWS
    (throw
      (ex-info "Conn missing AWS module" nil))))
(defn ensure-cog-client-config [conn]
  (when-not (->> conn
                 com/conn->schema
                 com/schema->cog-config)
    (throw (ex-info "Missing cognito config" nil))))

(<defn <auth-by-username-password [conn username password]
  (ensure-aws-conn conn)
  (ensure-cog-client-config conn)
  
  (let [AWS (:box.pl/AWS conn)
        schema (com/conn->schema conn)
        cog-config (com/schema->cog-config schema)
        cog-creds (com/schema->cog-client-creds schema)

        res (anom/throw-if-anom
              (<? (aws/<cog-auth-by-username-password
                    AWS
                    (merge
                      cog-config
                      cog-creds)
                    username
                    password)))

        cog-tokens res]

    {:box/cog-tokens cog-tokens
     :box/username username}))

(s/def :box/auth map?)

(defn ensure-auth [auth]
  (ks/spec-check-throw :box/auth auth "Invalid auth"))

(<defn <refresh-auth [conn auth]
  (ensure-aws-conn conn)
  (ensure-auth auth)
  (let [schema (com/conn->schema conn)
        res (->> (aws/<cog-refresh-auth
                   (:box.pl/AWS conn)
                   (merge
                     (com/schema->cog-client-creds schema)
                     (com/schema->cog-config schema))
                   (:box/cog-tokens auth))
                 <?)]
    (if (anom/? res)
      res
      {:box/cog-tokens
       (merge
         ;; keep refresh token
         (:box/cog-tokens auth)
         (->> res
              cog-auth-data->cog-tokens))})))

(defn sub [auth]
  (->> auth
       :box/cog-tokens
       :aws.cog/id-token
       jwt/jwt-str->payload
       :sub))

(defn id-token [auth]
  (-> auth
      :box/cog-tokens
      :aws.cog/id-token))
