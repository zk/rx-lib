(ns rx.node.jsonwebtoken
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [cljs.core.async
             :refer [chan timeout put! <!]
             :refer-macros [go]]
            [clojure.test
             :as test
             :refer-macros [deftest is]]))

(def JWT (js/require "jsonwebtoken"))
(def JWKTOPEM (js/require "jwk-to-pem"))
(def JOSE (js/require "node-jose"))

(def JWK (.-JWK JOSE))

(defn <generate-keystore [key-type key-size & [props]]
  (let [ch (chan)]
    (let [ks (.createKeyStore JWK)]
      (.then
        (.generate ks
          (or key-type "RSA")
          (or key-size 256)
          (clj->js props))
        (fn [res]
          (put! ch
            {:private-keystore (js->clj
                                 (.toJSON ks true)
                                 :keywordize-keys
                                 true)
             :public-keystore (js->clj
                                (.toJSON ks false)
                                :keywordize-keys
                                true)}))))
    ch))

(comment

  (go
    (ks/pp (<! (<generate-keystore
                 "RSA"
                 2048))))

  )

(defn verify-with-jwk [token-str jwk alg]
  (js->clj
    (.verify
      JWT
      token-str
      (JWKTOPEM (clj->js jwk))
      (clj->js
        {:algorithms [alg]}))
    :keywordize-keys true))

(comment

  (go
    (prn
      (verify-with-jwk
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InVIZi1OOUt5NEFpN2piQXBVcWFqNWVjZmZUd21ZT2NTMGpHazhmcElXX2MifQ.eyJmb28iOiJiYXIiLCJpYXQiOjE1NDYzMDc2MTZ9.Damhzk_DFARm-nOUhJtv2np-IJY3EOMC9zsFyIf4xcKmTgxw7wY5UKCDhvwGQD4NUB5eUU-FkLeoFfClasWQlQ"
        (-> (<! (<generate-keystore "RSA" 256))
            :public-keystore
            :keys
            first
            ks/spy)
        "RS256")))




  (go
    (prn (-> (<! (<generate-keystore "RSA" 512))
             :public-keystore
             :keys
             first
             clj->js
             JWKTOPEM)))

  )

(defn <generate-test-data [payload & [{:keys [ks-opts]}]]
  (go
    (let [{:keys [private-keystore
                  public-keystore]}
          (<! (<generate-keystore "RSA" 512 ks-opts))
          {:keys [kty kid n] :as key} (-> private-keystore :keys first)
          pem (JWKTOPEM (clj->js key) #js {:private true})]

      {:jwks-str (ks/to-json public-keystore)
       :token (.sign
                JWT
                (clj->js payload)
                pem
                (clj->js
                  {:keyid kid
                   :algorithm "RS256"}))})))

(comment

  (go
    (let [{:keys [jwks-str token]}
          (<! (<generate-test-data
                {:foo "bar"}))]
      (prn token)
      (println jwks-str)))


  )

(defn decode-token [token-str]
  (when token-str
    (js->clj
      (.decode JWT token-str #js {:complete true})
      :keywordize-keys true)))

(defn verify-token-with-jwks [token-str cog-jwks-str]
  (let [json-str (try
                   (-> cog-jwks-str ks/from-json)
                   (catch js/Error e
                     (throw
                       (ex-info
                         "Error parsing JWKS string"
                         {:jwks-str cog-jwks-str}))))

        jwks (-> json-str :keys)
        {:keys [header payload signature]} (decode-token token-str)
        target-kid (:kid header)
        {:keys [kty kid n] :as matching-jwk}
        (->> jwks
             (filter #(= (:kid %) target-kid))
             first)
        {:keys [alg]} header]
    (if matching-jwk
      (try
        [(verify-with-jwk
           token-str
           matching-jwk
           alg)]
        (catch js/Error e
          [nil {:message (.toString e)}]))
      [nil {:message (str "No matching jwk key for kid " target-kid)}])))


(comment

  (go
    (let [{:keys [jwks-str token]}
          (<! (<generate-test-data
                {:foo "bar"}))]
      (prn token)
      (prn (verify-token-with-jwks
             token
             "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"S+TDMUzvyYXaMLsqW6EITyXwZMhqMsfOY7RwGLCip6I=\",\"kty\":\"RSA\",\"n\":\"oaL9ta9PvuJG7WaTsqcfcw3kekJoIAU3rtqWPqLd4ZLoIdclWHdxRFqn9Vflkj6A7JF13OG4wEOeDykmoVX_uYywDIDjJr2fsPkSjq4xhgyCvD0IQO-am0EBFwrHGKEFKqeh-HX0OqeMOhPLtrcy5aOZCdah9k7Hq__nV4hU-npOYrX1o1MIdzwvMckqCMFV0eZFvVJjTRgdDyEhETyGwlRXnjvaWkIxQzrmNosFX7WyjI-yGy2vFnTbsC1HvAmhxz_WAQ5ODdXqDn6PdwWsvFTjsdfTmSQXPq3qnKEnytKuKuHnFeKKMRKLiHwLwTo9Yte4arkGO1QfZmSvP921-Q\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"KTTRYBGnbKpbYYA0IaFtZg66SApCHOKoRrYK/Ml2ViM=\",\"kty\":\"RSA\",\"n\":\"0osBQNKkUqyMdERzEpsJOaRE4meFQS-94fMDihC4L6iBWuOWF8JoS1YO-eNKbefaP2NiugR6ncvwe6zetjEhoqjcJR27Fx1RPIblNHKPGI9-V6kHjXHIVvSNtltm8BbcEVdkrklsRSfWXWssK0TWdm7bHLRHqH03y3McLsDWjxerA9t9GgTm9Ne9xTwK1JMeYDMAEZfa4bPsO0ef11PI_ruc_4-6pRllyDMi-353_05NiSNzG-hzFCQKsIe3UOmS5Yv_FzZsYnflwTlmlqb2B5Bvo_js8CKVByQfBIxMnmlNgrKPkfljFLn5pPpH0pYoxll2LlMS-_CSKasSVAS81w\",\"use\":\"sig\"}]}"))))

  )

(deftest test-verify-token-with-jwks
  (let [[res err] (verify-token-with-jwks
                    "eyJraWQiOiJLVFRSWUJHbmJLcGJZWUEwSWFGdFpnNjZTQXBDSE9Lb1JyWUtcL01sMlZpTT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3MTczM2M0My1hMjk3LTQxYjYtYjg4YS00NTBmNTMyZWQ1YjIiLCJldmVudF9pZCI6IjAzNDdjNzVlLTBhMTQtMTFlOS05OTIyLWJmZWUxMzkwNTQ2MCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NDU5NDE2NjgsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbVwvdXMtd2VzdC0yXzVPUnFnNmRMZyIsImV4cCI6MTU0NTk0NTI2OCwiaWF0IjoxNTQ1OTQxNjY4LCJqdGkiOiJhODFhMTVhYS02NTI3LTRmNjgtOWMyZi1lMDhkYjUxODI4NWEiLCJjbGllbnRfaWQiOiIzdTdsb2FuODVtMW9uZzI4cjdwOTA5bzk1bSIsInVzZXJuYW1lIjoiNzE3MzNjNDMtYTI5Ny00MWI2LWI4OGEtNDUwZjUzMmVkNWIyIn0.iq3HuXT162sKfAjXJJh57KvM6gdXvmN5qTGXI0ocp1WgSX3T_cjWs1Z654l4BnCcMW1OymgHpRdl115huLDZ0rpmYOXgNcF-_5j4H0rfjVQ7fwUqfLfYOsXfJBCprWPklpbBf3LmBj45UbsxmqiwrpIH3Ptvi6VmG6qAaStyJ1QfcOJxphS_TC5SkGwFDBNtuKYTKcpIOsaDCHWhuCg6MK9cXGX6MrYkoKQYAyMc_zpYSojRqcavOOBO9AkPWcJq0GKUxuY4oTBYsQdFwWm7-EDCfyBXHN7RiUJi0xOXLHcBxrl0G7rNAWg4CYYWDSz1mSnEImGpK3oGep7YAr0YxA"
                    "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"S+TDMUzvyYXaMLsqW6EITyXwZMhqMsfOY7RwGLCip6I=\",\"kty\":\"RSA\",\"n\":\"oaL9ta9PvuJG7WaTsqcfcw3kekJoIAU3rtqWPqLd4ZLoIdclWHdxRFqn9Vflkj6A7JF13OG4wEOeDykmoVX_uYywDIDjJr2fsPkSjq4xhgyCvD0IQO-am0EBFwrHGKEFKqeh-HX0OqeMOhPLtrcy5aOZCdah9k7Hq__nV4hU-npOYrX1o1MIdzwvMckqCMFV0eZFvVJjTRgdDyEhETyGwlRXnjvaWkIxQzrmNosFX7WyjI-yGy2vFnTbsC1HvAmhxz_WAQ5ODdXqDn6PdwWsvFTjsdfTmSQXPq3qnKEnytKuKuHnFeKKMRKLiHwLwTo9Yte4arkGO1QfZmSvP921-Q\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"KTTRYBGnbKpbYYA0IaFtZg66SApCHOKoRrYK/Ml2ViM=\",\"kty\":\"RSA\",\"n\":\"0osBQNKkUqyMdERzEpsJOaRE4meFQS-94fMDihC4L6iBWuOWF8JoS1YO-eNKbefaP2NiugR6ncvwe6zetjEhoqjcJR27Fx1RPIblNHKPGI9-V6kHjXHIVvSNtltm8BbcEVdkrklsRSfWXWssK0TWdm7bHLRHqH03y3McLsDWjxerA9t9GgTm9Ne9xTwK1JMeYDMAEZfa4bPsO0ef11PI_ruc_4-6pRllyDMi-353_05NiSNzG-hzFCQKsIe3UOmS5Yv_FzZsYnflwTlmlqb2B5Bvo_js8CKVByQfBIxMnmlNgrKPkfljFLn5pPpH0pYoxll2LlMS-_CSKasSVAS81w\",\"use\":\"sig\"}]}")]
    (is err)))

(deftest test-decode-token
  (is (not (decode-token "bad token")))
  (is
    (decode-token "eyJraWQiOiJLVFRSWUJHbmJLcGJZWUEwSWFGdFpnNjZTQXBDSE9Lb1JyWUtcL01sMlZpTT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3MTczM2M0My1hMjk3LTQxYjYtYjg4YS00NTBmNTMyZWQ1YjIiLCJldmVudF9pZCI6IjAzNDdjNzVlLTBhMTQtMTFlOS05OTIyLWJmZWUxMzkwNTQ2MCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NDU5NDE2NjgsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbVwvdXMtd2VzdC0yXzVPUnFnNmRMZyIsImV4cCI6MTU0NTk0NTI2OCwiaWF0IjoxNTQ1OTQxNjY4LCJqdGkiOiJhODFhMTVhYS02NTI3LTRmNjgtOWMyZi1lMDhkYjUxODI4NWEiLCJjbGllbnRfaWQiOiIzdTdsb2FuODVtMW9uZzI4cjdwOTA5bzk1bSIsInVzZXJuYW1lIjoiNzE3MzNjNDMtYTI5Ny00MWI2LWI4OGEtNDUwZjUzMmVkNWIyIn0.iq3HuXT162sKfAjXJJh57KvM6gdXvmN5qTGXI0ocp1WgSX3T_cjWs1Z654l4BnCcMW1OymgHpRdl115huLDZ0rpmYOXgNcF-_5j4H0rfjVQ7fwUqfLfYOsXfJBCprWPklpbBf3LmBj45UbsxmqiwrpIH3Ptvi6VmG6qAaStyJ1QfcOJxphS_TC5SkGwFDBNtuKYTKcpIOsaDCHWhuCg6MK9cXGX6MrYkoKQYAyMc_zpYSojRqcavOOBO9AkPWcJq0GKUxuY4oTBYsQdFwWm7-EDCfyBXHN7RiUJi0xOXLHcBxrl0G7rNAWg4CYYWDSz1mSnEImGpK3oGep7YAr0YxA")))

#_(prn (decode-token "eyJraWQiOiJLVFRSWUJHbmJLcGJZWUEwSWFGdFpnNjZTQXBDSE9Lb1JyWUtcL01sMlZpTT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3MTczM2M0My1hMjk3LTQxYjYtYjg4YS00NTBmNTMyZWQ1YjIiLCJldmVudF9pZCI6IjAzNDdjNzVlLTBhMTQtMTFlOS05OTIyLWJmZWUxMzkwNTQ2MCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NDU5NDE2NjgsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbVwvdXMtd2VzdC0yXzVPUnFnNmRMZyIsImV4cCI6MTU0NTk0NTI2OCwiaWF0IjoxNTQ1OTQxNjY4LCJqdGkiOiJhODFhMTVhYS02NTI3LTRmNjgtOWMyZi1lMDhkYjUxODI4NWEiLCJjbGllbnRfaWQiOiIzdTdsb2FuODVtMW9uZzI4cjdwOTA5bzk1bSIsInVzZXJuYW1lIjoiNzE3MzNjNDMtYTI5Ny00MWI2LWI4OGEtNDUwZjUzMmVkNWIyIn0.iq3HuXT162sKfAjXJJh57KvM6gdXvmN5qTGXI0ocp1WgSX3T_cjWs1Z654l4BnCcMW1OymgHpRdl115huLDZ0rpmYOXgNcF-_5j4H0rfjVQ7fwUqfLfYOsXfJBCprWPklpbBf3LmBj45UbsxmqiwrpIH3Ptvi6VmG6qAaStyJ1QfcOJxphS_TC5SkGwFDBNtuKYTKcpIOsaDCHWhuCg6MK9cXGX6MrYkoKQYAyMc_zpYSojRqcavOOBO9AkPWcJq0GKUxuY4oTBYsQdFwWm7-EDCfyBXHN7RiUJi0xOXLHcBxrl0G7rNAWg4CYYWDSz1mSnEImGpK3oGep7YAr0YxA"))

#_(.verify JWT
    "eyJraWQiOiJLVFRSWUJHbmJLcGJZWUEwSWFGdFpnNjZTQXBDSE9Lb1JyWUtcL01sMlZpTT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3MTczM2M0My1hMjk3LTQxYjYtYjg4YS00NTBmNTMyZWQ1YjIiLCJldmVudF9pZCI6IjAzNDdjNzVlLTBhMTQtMTFlOS05OTIyLWJmZWUxMzkwNTQ2MCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NDU5NDE2NjgsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbVwvdXMtd2VzdC0yXzVPUnFnNmRMZyIsImV4cCI6MTU0NTk0NTI2OCwiaWF0IjoxNTQ1OTQxNjY4LCJqdGkiOiJhODFhMTVhYS02NTI3LTRmNjgtOWMyZi1lMDhkYjUxODI4NWEiLCJjbGllbnRfaWQiOiIzdTdsb2FuODVtMW9uZzI4cjdwOTA5bzk1bSIsInVzZXJuYW1lIjoiNzE3MzNjNDMtYTI5Ny00MWI2LWI4OGEtNDUwZjUzMmVkNWIyIn0.iq3HuXT162sKfAjXJJh57KvM6gdXvmN5qTGXI0ocp1WgSX3T_cjWs1Z654l4BnCcMW1OymgHpRdl115huLDZ0rpmYOXgNcF-_5j4H0rfjVQ7fwUqfLfYOsXfJBCprWPklpbBf3LmBj45UbsxmqiwrpIH3Ptvi6VmG6qAaStyJ1QfcOJxphS_TC5SkGwFDBNtuKYTKcpIOsaDCHWhuCg6MK9cXGX6MrYkoKQYAyMc_zpYSojRqcavOOBO9AkPWcJq0GKUxuY4oTBYsQdFwWm7-EDCfyBXHN7RiUJi0xOXLHcBxrl0G7rNAWg4CYYWDSz1mSnEImGpK3oGep7YAr0YxA"

  (JWKTOPEM (-> (ks/from-json "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"S+TDMUzvyYXaMLsqW6EITyXwZMhqMsfOY7RwGLCip6I=\",\"kty\":\"RSA\",\"n\":\"oaL9ta9PvuJG7WaTsqcfcw3kekJoIAU3rtqWPqLd4ZLoIdclWHdxRFqn9Vflkj6A7JF13OG4wEOeDykmoVX_uYywDIDjJr2fsPkSjq4xhgyCvD0IQO-am0EBFwrHGKEFKqeh-HX0OqeMOhPLtrcy5aOZCdah9k7Hq__nV4hU-npOYrX1o1MIdzwvMckqCMFV0eZFvVJjTRgdDyEhETyGwlRXnjvaWkIxQzrmNosFX7WyjI-yGy2vFnTbsC1HvAmhxz_WAQ5ODdXqDn6PdwWsvFTjsdfTmSQXPq3qnKEnytKuKuHnFeKKMRKLiHwLwTo9Yte4arkGO1QfZmSvP921-Q\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"KTTRYBGnbKpbYYA0IaFtZg66SApCHOKoRrYK/Ml2ViM=\",\"kty\":\"RSA\",\"n\":\"0osBQNKkUqyMdERzEpsJOaRE4meFQS-94fMDihC4L6iBWuOWF8JoS1YO-eNKbefaP2NiugR6ncvwe6zetjEhoqjcJR27Fx1RPIblNHKPGI9-V6kHjXHIVvSNtltm8BbcEVdkrklsRSfWXWssK0TWdm7bHLRHqH03y3McLsDWjxerA9t9GgTm9Ne9xTwK1JMeYDMAEZfa4bPsO0ef11PI_ruc_4-6pRllyDMi-353_05NiSNzG-hzFCQKsIe3UOmS5Yv_FzZsYnflwTlmlqb2B5Bvo_js8CKVByQfBIxMnmlNgrKPkfljFLn5pPpH0pYoxll2LlMS-_CSKasSVAS81w\",\"use\":\"sig\"}]}")
                :keys
                second
                clj->js))
  #js {:algorithms ["RS256"]})


#_(JWKTOPEM (-> (ks/from-json "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"S+TDMUzvyYXaMLsqW6EITyXwZMhqMsfOY7RwGLCip6I=\",\"kty\":\"RSA\",\"n\":\"oaL9ta9PvuJG7WaTsqcfcw3kekJoIAU3rtqWPqLd4ZLoIdclWHdxRFqn9Vflkj6A7JF13OG4wEOeDykmoVX_uYywDIDjJr2fsPkSjq4xhgyCvD0IQO-am0EBFwrHGKEFKqeh-HX0OqeMOhPLtrcy5aOZCdah9k7Hq__nV4hU-npOYrX1o1MIdzwvMckqCMFV0eZFvVJjTRgdDyEhETyGwlRXnjvaWkIxQzrmNosFX7WyjI-yGy2vFnTbsC1HvAmhxz_WAQ5ODdXqDn6PdwWsvFTjsdfTmSQXPq3qnKEnytKuKuHnFeKKMRKLiHwLwTo9Yte4arkGO1QfZmSvP921-Q\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"KTTRYBGnbKpbYYA0IaFtZg66SApCHOKoRrYK/Ml2ViM=\",\"kty\":\"RSA\",\"n\":\"0osBQNKkUqyMdERzEpsJOaRE4meFQS-94fMDihC4L6iBWuOWF8JoS1YO-eNKbefaP2NiugR6ncvwe6zetjEhoqjcJR27Fx1RPIblNHKPGI9-V6kHjXHIVvSNtltm8BbcEVdkrklsRSfWXWssK0TWdm7bHLRHqH03y3McLsDWjxerA9t9GgTm9Ne9xTwK1JMeYDMAEZfa4bPsO0ef11PI_ruc_4-6pRllyDMi-353_05NiSNzG-hzFCQKsIe3UOmS5Yv_FzZsYnflwTlmlqb2B5Bvo_js8CKVByQfBIxMnmlNgrKPkfljFLn5pPpH0pYoxll2LlMS-_CSKasSVAS81w\",\"use\":\"sig\"}]}")
                :keys
                first
                clj->js))


#_"{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"S+TDMUzvyYXaMLsqW6EITyXwZMhqMsfOY7RwGLCip6I=\",\"kty\":\"RSA\",\"n\":\"oaL9ta9PvuJG7WaTsqcfcw3kekJoIAU3rtqWPqLd4ZLoIdclWHdxRFqn9Vflkj6A7JF13OG4wEOeDykmoVX_uYywDIDjJr2fsPkSjq4xhgyCvD0IQO-am0EBFwrHGKEFKqeh-HX0OqeMOhPLtrcy5aOZCdah9k7Hq__nV4hU-npOYrX1o1MIdzwvMckqCMFV0eZFvVJjTRgdDyEhETyGwlRXnjvaWkIxQzrmNosFX7WyjI-yGy2vFnTbsC1HvAmhxz_WAQ5ODdXqDn6PdwWsvFTjsdfTmSQXPq3qnKEnytKuKuHnFeKKMRKLiHwLwTo9Yte4arkGO1QfZmSvP921-Q\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"KTTRYBGnbKpbYYA0IaFtZg66SApCHOKoRrYK/Ml2ViM=\",\"kty\":\"RSA\",\"n\":\"0osBQNKkUqyMdERzEpsJOaRE4meFQS-94fMDihC4L6iBWuOWF8JoS1YO-eNKbefaP2NiugR6ncvwe6zetjEhoqjcJR27Fx1RPIblNHKPGI9-V6kHjXHIVvSNtltm8BbcEVdkrklsRSfWXWssK0TWdm7bHLRHqH03y3McLsDWjxerA9t9GgTm9Ne9xTwK1JMeYDMAEZfa4bPsO0ef11PI_ruc_4-6pRllyDMi-353_05NiSNzG-hzFCQKsIe3UOmS5Yv_FzZsYnflwTlmlqb2B5Bvo_js8CKVByQfBIxMnmlNgrKPkfljFLn5pPpH0pYoxll2LlMS-_CSKasSVAS81w\",\"use\":\"sig\"}]}"


;;; Tests

(deftest test-round-trip-verify
  (test/async done
    (go
      (let [{:keys [jwks-str token]} (<! (<generate-test-data {:foo "bar"}))
            [payload err] (verify-token-with-jwks token jwks-str)]
        (is (= "bar" (:foo payload)))
        (done)))))

(deftest test-fail-on-incorrect-key
  (test/async done
    (go
      (let [{:keys [jwks-str token]} (<! (<generate-test-data {:foo "bar"}))
            alt-jwks-str (:jwks-str (<! (<generate-test-data {:foo "bar"})))
            [payload err] (verify-token-with-jwks token alt-jwks-str)]
        (is (nil? payload))
        (done)))))

(deftest test-fail-on-missing-jwks
  (test/async done
    (go
      (let [{:keys [token]} (<! (<generate-test-data {:foo "bar"}))
            [payload err] (verify-token-with-jwks token nil)]
        (is (nil? payload))
        (done)))))
