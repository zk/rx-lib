(ns rx.box.auth-test
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer-macros [<?]]
            [rx.test :as test
             :refer-macros [thrown]]
            [rx.res :as res]
            [rx.box.auth :as a]
            [rx.box.test-helpers :as th]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]))

(test/<deftest test-<auth-by-username-password [opts]
  (let [conn (<? (th/<test-conn opts))
        fail-res (<! (a/<auth-by-username-password conn nil nil))
        auth-res (<! (a/<auth-by-username-password
                       conn
                       "zk+test-rx@heyzk.com"
                       "Test1234"))]

    [{::test/passed? (anom/? fail-res)
      ::test/explain-data fail-res} 
     {::test/explain-data auth-res
      ::test/passed? (not (anom/? auth-res))}]))

(test/<deftest test-<refresh [opts]
  (let [conn (<? (th/<test-conn opts))
        auth-res (<? (a/<auth-by-username-password
                       conn
                       "zk+test-rx@heyzk.com"
                       "Test1234"))
        refresh-res (<! (a/<refresh-auth
                          conn
                          auth-res))]

    [{::test/explain-data refresh-res
      ::test/passed? (not (anom/? refresh-res))}]))


