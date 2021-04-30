(ns rx.browser.cognito-test
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer-macros [<defn <? gol]]
            [rx.aws :as aws]
            [rx.browser.aws :as baws]
            [rx.cognito :as cog]
            [rx.test :as test
             :refer-macros [deftest <deftest]]))

(<deftest test-auth-needs-password-reset [opts]
  (let [res (<? (cog/<auth-username-password
                  (merge
                    {::aws/AWS baws/AWS
                     ::cog/client-id "3c01su9c0vbmd0oqh1l7g3k9jc"}
                    {:region "us-west-2"}
                    opts)
                  "zk+needs-password-reset@heyzk.com"
                  "foobarbaz"))]
    [[(cog/new-password-required? res) nil res]]))

(<deftest test-auth-username-password [opts]
  (let [res (<? (cog/<auth-username-password
                  (merge
                    {::aws/AWS baws/AWS
                     ::cog/client-id "3c01su9c0vbmd0oqh1l7g3k9jc"}
                    {:region "us-west-2"}
                    opts)
                  "zk+valid-user@heyzk.com"
                  "foobarbaz"))]
    [[(not (cog/new-password-required? res)) nil res]]))

(<deftest test-sign-up [opts]
  (let [res (<? (cog/<sign-up
                  (merge
                    {::aws/AWS baws/AWS
                     ::cog/client-id "3c01su9c0vbmd0oqh1l7g3k9jc"}
                    {:region "us-west-2"}
                    opts)
                  (str "zk+" (ks/uuid) "@heyzk.com")
                  "foobarbaz"))]
    [[false nil res]]))


(comment

  (test/<run-var-repl #'test-sign-up)

  (gol
    (ks/pp
      (<? (cog/<auth-username-password
            (merge
              {::aws/AWS baws/AWS
               ::cog/client-id "3c01su9c0vbmd0oqh1l7g3k9jc"}
              {:region "us-west-2"})
            "zk+3fc120b0495745f48a70c222f8756c4b@heyzk.com"
            "foobarbaz"))))

  )


