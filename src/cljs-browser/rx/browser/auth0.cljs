(ns rx.browser.auth0
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.local-storage :as ls]
            [auth0-spa-js]
            [auth0]
            [rx.anom :as anom :refer-macros [gol <? <defn]]
            [clojure.core.async :as async
             :refer [go <! put! chan close!]]))

(defn <create-client [opts]
  (ks/<promise (js/createAuth0Client (clj->js opts))))

(defn <login-with-redirect [client]
  (ks/<promise (.loginWithRedirect client)))

(defn <login-with-popup [client]
  (ks/<promise (.loginWithPopup client)))

(defn webauth [opts]
  (js/auth0.WebAuth.
    (clj->js opts)))

(defn ensure-webauth-client [opts]
  (if (map? opts)
    (webauth opts)
    opts))

(defn <passwordless-start [opts-or-client opts]
  (let [ch (chan)
        client (ensure-webauth-client opts-or-client)]
    (.passwordlessStart
      client
      (clj->js opts)
      (fn [err res]
        (when (or err res)
          (put! ch (if err
                     (anom/anom
                       {:desc (.-errorDescription err)
                        :err (js->clj err)})
                     res)))
        (close! ch)))
    ch))

(defn <parse-hash [webauth-opts-or-client & [opts]]
  (let [ch (chan)
        client (ensure-webauth-client webauth-opts-or-client)]
    (.parseHash
      client
      (fn [err res]
        (when (or err res)
          (put! ch (if err
                     (anom/anom
                       {:desc (.-errorDescription err)
                        :err (js->clj err)})
                     {:access-token (.-accessToken res)
                      :expires-in (.-expiresIn res)
                      :id-token (.-idToken res)})))
        (close! ch)))
    ch))

(defn <user-info [webauth-opts-or-client access-token]
  (let [ch (chan)
        client (ensure-webauth-client webauth-opts-or-client)]
    (try
      (.userInfo
        (.-client client)
        access-token
        (fn [err res]
          (when (or err res)
            (put! ch (if err
                       (anom/anom
                         {:desc (.-errorDescription err)
                          :err (js->clj err)})
                       (js->clj res :keywordize-keys true))))
          (close! ch)))
      (catch js/Error e
        (put! ch (anom/from-err e))
        (close! ch)))
    ch))

(defn <check-session [webauth-opts-or-client opts]
  (let [ch (chan)]
    (.checkSession
      (ensure-webauth-client webauth-opts-or-client)
      (clj->js opts)
      (fn [err res]
        (when (or err res)
          (put! ch (if err
                     (anom/anom
                       {:desc (.-errorDescription err)
                        :err (js->clj err)})
                     (js->clj res :keywordize-keys true))))
        (close! ch)))
    ch))

(defn <logout [webauth-opts-or-client opts]
  (let [ch (chan)]
    (.logout
      (ensure-webauth-client webauth-opts-or-client)
      (clj->js opts)
      (fn [err res]
        (when (or err res)
          (put! ch (if err
                     (anom/anom
                       {:desc (.-errorDescription err)
                        :err (js->clj err)})
                     (js->clj res :keywordize-keys true))))
        (close! ch)))
    ch))

(defn <logout-and-clear-local [& args]
  (ls/set-transit "wc-access-token" nil)
  (ls/set-transit "wc-user-info" nil)
  (apply <logout args))


;; CLJS Api

(defn get-current-user [] (ls/get-transit "wc-user-info"))

(<defn <handle-login-hash [webauth-opts-or-client]
  (let [client (ensure-webauth-client webauth-opts-or-client)
        auth (<? (<parse-hash client {}))
        user-info (<? (<user-info client (:access-token auth)))]
    (ls/set-transit "wc-access-token" (:access-token auth))
    (ls/set-transit "wc-user-info" user-info)
    user-info))

(<defn <init-auth [webauth-opts-or-client]
  (try
    (if-let [access-token (ls/get-transit "wc-access-token")]
      (let [user-info (<! (<user-info webauth-opts-or-client access-token))]
        (if (anom/? user-info)
          (let [user-info (<! (<handle-login-hash webauth-opts-or-client))]
            (if (anom/? user-info)
              (do
                (println "Auth failed, clearing local auth state" (pr-str user-info))
                (ls/set-transit "wc-access-token" nil)
                (ls/set-transit "wc-user-info" nil)
                nil)
              (do
                (println "Auth succeeded via hash info")
                (ls/set-transit "wc-user-info" user-info)
                #_(set! js/window.location.hash "")
                user-info)))
          (do
            (println "Auth succeeded via stored token")
            (ls/set-transit "wc-user-info" user-info)
            user-info)))
      (let [user-info (<! (<handle-login-hash webauth-opts-or-client))]
        (if (anom/? user-info)
          (do
            (println "Auth failed, clearing local auth state" (pr-str user-info))
            (ls/set-transit "wc-access-token" nil)
            (ls/set-transit "wc-user-info" nil)
            nil)
          (do
            (println "Auth succeeded via hash info")
            (ls/set-transit "wc-user-info" user-info)
            #_(set! js/window.location.hash "")
            user-info))))
    (catch js/Error e
      (println "Auth failed with exception" e)
      (ls/set-transit "wc-access-token" nil)
      (ls/set-transit "wc-user-info" nil)
      (throw e))))

(comment

  (<logout-and-clear-local
    {:domain "welcap.us.auth0.com"
     :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"}
    {:clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
     :returnTo "http://localhost:5000"})

  (<init-auth
    {:domain "welcap.us.auth0.com"
     :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
     :responseType "token id_token"
     :scope "openid profile email"})

  (ls/set-transit "wc-user-info" nil)
  (ls/set-transit "wc-access-token" nil)

  (ls/get-transit "wc-user-info")
  (ls/get-transit "wc-access-token")

  (gol
    (ks/pp (<! (<handle-login-hash
                 {:domain "welcap.us.auth0.com"
                  :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
                  :responseType "token id_token"
                  :scope "openid profile email"}))))

  (ls/get-transit "wc-access-token")

  (ks/<pp
    (<passwordless-start
      {:domain "welcap.us.auth0.com"
       :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
       :responseType "token id_token"
       :scope "openid profile email"}
      {:send "link"
       :email "zk@welcomecapital.co"
       :connection "email"}))

  (gol
    (def auth (ks/spy (<! (<parse-hash
                            {:domain "welcap.us.auth0.com"
                             :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"}
                            {})))))

  (prn (.-accessToken auth))

  (gol
    (ls/set-transit
      "wc-user-info"
      (<! (<user-info
            {:domain "welcap.us.auth0.com"
             :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"}
            (ls/get-transit "wc-access-token")))))

  (ls/get-transit "wc-user-info")
  
  (gol
    (ks/pp
      (<! (<check-session
            {:domain "welcap.us.auth0.com"
             :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
             :responseType "token id_token"
             :redirectUri "http://localhost:5000"}
            {}
            (.-accessToken auth)))))

  (gol
    (ks/pp
      (<! (<parse-hash
            {:domain "welcap.us.auth0.com"
             :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"}
            {}))))

  (ks/<pp
    (<passwordless-start
      {:domain "welcap.us.auth0.com"
       :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
       :responseType "token id_token"
       :scope "openid profile email"}
      {:send "link"
       :email "zk@welcomecapital.co"
       :connection "email"}))

  (.passwordlessStart
    (webauth
      {:domain "welcap.us.auth0.com"
       :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
       :responseType "token id_token"})
    (clj->js
      {:send "link"
       :email "zk@welcomecapital.co"
       :connection "email"})
    (fn [err res]
      ))

  (.userInfo
    (webauth
      {:domain "welcap.us.auth0.com"
       :clientID "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
       :responseType "token id_token"})
    (fn [& a]
      (prn a)))

  (gol
    (let [client
          (<! (<create-client
                {:domain "welcap.us.auth0.com"
                 :client_id "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
                 :redirect_uri "http://localhost:5000/auth0-cb"}))]
      (time (<! (<login-with-redirect client)))))

  (gol
    (let [client
          (<! (<create-client
                {:domain "welcap.us.auth0.com"
                 :client_id "I3yfRzzSB8GsjkAMhnScttxhSVhaziuI"
                 :redirect_uri "http://localhost:5000"}))]
      (time (<! (<login-with-popup client)))))

  

  

  )






