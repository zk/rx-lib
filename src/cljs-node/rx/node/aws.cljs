(ns rx.node.aws
  (:require [rx.kitchen-sink :as ks]
            [rx.aws :as aws]
            [rx.res :as res]))

(def AWS (js/require "aws-sdk"))

(defn <aws [config
            service-key
            method
            args]
  (aws/<aws AWS config service-key method args))

(def <dyndoc (partial aws/<dyndoc AWS))
(def <next (partial aws/<next AWS))
(def <next-n (partial aws/<next-n AWS))
(def <stream (partial aws/<stream AWS))

(defn <cogisp-user-password-auth [creds
                                  {:keys [cog-client-id]
                                   :as cog-config}
                                  {:keys [username
                                          password]
                                   :as opts}]
  (aws/<cogisp-user-password-auth
    AWS
    creds
    cog-config
    opts))

#_(def <cogisp-admin-set-user-password
  (partial
    aws/<cogisp-admin-set-user-password
    AWS))

(defn <cogisp-admin-set-user-password
  [creds
   {:keys [username
           password
           cog-pool-id]
    :as opts}]
  (aws/<cogisp-admin-set-user-password AWS creds opts))

(def <cogisp-refresh-auth
  (partial
    aws/<cogisp-refresh-auth
    AWS))

(def <cogisp-refresh-auth-if-needed
  (partial
    aws/<cogisp-refresh-auth-if-needed
    AWS))

(def auth-fresh? aws/auth-fresh?)


(def data res/data)
(def anom res/anom)
