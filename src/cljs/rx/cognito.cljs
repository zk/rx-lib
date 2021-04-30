(ns rx.cognito
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer-macros [<defn <? gol]]
            [rx.aws :as aws]))

(defn new-password-required? [res]
  (= "NEW_PASSWORD_REQUIRED"
     (:ChallengeName res)))

(<defn <auth-username-password [opts username password]
  (when-not (and username password)
    (anom/throw-anom
      {:desc "username or password missing"
       :username username
       :password password}))

  (::aws/data
   (<? (aws/<aws
         (::aws/AWS opts)
         opts
         "CognitoIdentityServiceProvider"
         "initiateAuth"
         [{:AuthFlow "USER_PASSWORD_AUTH"
           :ClientId (::client-id opts)
           :AuthParameters {"USERNAME" username
                            "PASSWORD" password}}]))))

(defn <respond-update-password [opts
                                last-res
                                username
                                new-password]
  (when-not last-res
    (anom/throw-anom
      {:desc "initiate-res required"}))

  (when-not username
    (anom/throw-anom
      {:desc "username required"}))

  (when-not new-password
    (anom/throw-anom
      {:desc "new password required"}))

  (::aws/data
   (<? (aws/<aws
         (::aws/AWS opts)
         opts
         "CognitoIdentityServiceProvider"
         "respondToAuthChallenge"
         [{:ChallengeName "NEW_PASSWORD_REQUIRED"
           :Session (-> last-res
                        :Session)
           :ChallengeResponses
           {"USERNAME" username
            "NEW_PASSWORD" new-password}}]))))

(<defn <sign-up [opts username password]
  (::aws/data
   (<? (aws/<aws
         (::aws/AWS opts)
         opts
         "CognitoIdentityServiceProvider"
         "signUp"
         [{:ClientId (::client-id opts)
           :Username username
           :Password password}]))))

(<defn <create-user [opts username password]
  (let [res (<? (aws/<aws
                  (::aws/AWS opts)
                  opts
                  "CognitoIdentityServiceProvider"
                  "adminCreateUser"
                  [{:UserPoolId (::user-pool-id opts)
                    :Username username
                    :MessageAction "SUPPRESS"
                    :TemporaryPassword (ks/uuid)}]))

        res (<? (aws/<aws
                  (::aws/AWS opts)
                  opts
                  "CognitoIdentityServiceProvider"
                  "adminSetUserPassword"
                  [{:UserPoolId (::user-pool-id opts)
                    :Password password
                    :Username username
                    :Permanent true}]))]
    res))

