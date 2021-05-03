(ns rx.node.box.sync-server-dev
  (:require [rx.kitchen-sink :as ks]
            [rx.node.lambda :as lambda]
            [rx.node.box.sync-server :as sync-server]
            [rx.node.devserver :as server]
            [clojure.core.async :refer [go]]))

(def creds
  nil
  #_{:aws.creds/access-id nil
     :aws.creds/secret-key nil
     :aws.creds/region nil})

(def cog-config
  {:aws.cog/client-id "1em482svs0uojb8u7kmooulmmj"
   :aws.cog/user-pool-id "us-west-2_W1OuUCkYR"
   :aws.cog/allowed-use-claims ["id" "access"]
   :aws.cog/jwks-str "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"Uc55PuoXTb1+Yxr8cC++RWqrDr3K0NPimJ0p5VG26uE=\",\"kty\":\"RSA\",\"n\":\"9aW80tw1sCwxGStjzVOoofljyPFa_FkLUzSXaFu_Qjf2pRnuxn6QSxXkI_C_aj1z-clP0sOlss6nIcJIGeP4u9E2gz7_Df2uCkU4AfGxio9RzznY3MsQVp8kfKEdK_5Go3zXX8x526Ky5sGMjFWR8RO-JEOl6NvnmgY_rv8uMS6GMUQx0wDAU4rAYHGKTzphgqJAt1I-9xTB0v4MyrI8qg7xcs21A6AVpRj3svAZn1sVsF9IphSrZeSFJmw1WfJe5NKnekOJm7qG2CUjKcTTY7jdf_L6MGQky2wlTUstxHUeiFDBehbD1nRDaps9D6mzpDObb8MMmx1Cjv_SUBFJkQ\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"kA8o8ZKo39RBS26uCab1FjPWl0o8B03DxlFzgomdVTg=\",\"kty\":\"RSA\",\"n\":\"jpa17hbfmap73UQinjq7HuICMYo7YaJRD_c5X73TPzCaSJ2w760UrvNgfOgGeIvlZleg5lqEokH4kyI2IBUJJ_qNXZWym6_s_mwJTXtrlF8l11j30oeP7DmNfE_j6RGDT5_mHOLXcN1DFdQaEu9P3IzINMHHQh5tJk7s2Zb8V0cSLQjVgdoHE4S5HXPzhKe243j8kpMIPRUeuEm1IN17EklMsE0Mxvv69Aksnt219EYQL12DcjswbuIaVGAi0VnQ3YzYinyzDNJ7keD34Wn9KFvfMUQh5s1VO_opQ2h0V50aKmENqYnImrTss0lVuklEC0u0_sSI1eBwfI-Ww_iupw\",\"use\":\"sig\"}]}"})

(lambda/export-ring-handler
  (sync-server/create-ring-handler
    (merge
      {:box/schema
       {:box/ddb-data-table-name "rx-test-pk-string"}}
      cog-config)))

(comment

  (handler)

  (lambda/test-exports
    :handler

    #js {:resource "/{proxy+}",
         :path "/",
         :httpMethod "GET",
         :headers #js {:accept "*/*", :accept-encoding "gzip, deflate, br", :accept-language "en-US,en;q=0.9", :cache-control "no-cache", :content-type "application/transit+json", :cookie "_ga=GA1.2.1862870655.1528277081; __cfduid=d7e1a3769dbecb910beceb32bce80c5171530503328", :Host "napa.heyzk.com", :origin "https://napa.heyzk.com", :pragma "no-cache", :referer "https://napa.heyzk.com/", :User-Agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36", :X-Amzn-Trace-Id "Root=1-5b5b7eba-8134f29ac967b833ad750153", :X-Forwarded-For "67.52.80.164", :X-Forwarded-Port "443", :X-Forwarded-Proto "https"}
         :queryStringParameters nil,
         :pathParameters #js {:proxy "/"},
         :stageVariables nil,
         :requestContext
         #js {:resourceId "2s1puv", :resourcePath "/{proxy+}", :httpMethod "POST", :extendedRequestId "KtC9HHfqvHcFWwQ=", :requestTime "27/Jul/2018:20:21:14 +0000",
              :path "/",
              :accountId "753209818049", :protocol "HTTP/1.1", :stage "prod", :requestTimeEpoch 1532722874387, :requestId "9c2ca6eb-91da-11e8-9c42-136d2d41e7d5", :identity #js {:cognitoIdentityPoolId nil, :accountId nil, :cognitoIdentityId nil, :caller nil, :sourceIp "67.52.80.164", :accessKey nil, :cognitoAuthenticationType nil, :cognitoAuthenticationProvider nil, :userArn nil, :userAgent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36", :user nil}, :apiId "sgfd640k2f"}
         :body (ks/to-transit
                 [[:sync-objs
                   [{:gpt/id "foo"}]
                   {:sub "zk"}]])
         :isBase64Encoded false}
    {}
    {})


  (server/start
    {:port 7999
     :<handler sync-server/<standalone-handler})

  (prn "hi")

  
  )


