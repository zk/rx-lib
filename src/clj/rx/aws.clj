(ns rx.aws
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [byte-streams :as bs]
            [clojure.java.io :as io]
            [clojure.java.data :as jd])
  (:import [com.amazonaws.auth
            AWSStaticCredentialsProvider
            BasicAWSCredentials]

           [com.amazonaws.services.lambda
            AWSLambdaClient
            AWSLambdaClientBuilder]

           [com.amazonaws.services.lambda.model

            VpcConfig
            TracingConfig
            FunctionCode
            Environment

            CreateFunctionRequest
            UpdateFunctionCodeRequest
            UpdateFunctionConfigurationRequest
            GetFunctionRequest
            InvokeRequest
            ResourceConflictException
            ResourceNotFoundException
            InvalidParameterValueException
            AWSLambdaException]))

(defn credentials [access-key secret-key]
  (BasicAWSCredentials. access-key secret-key))

(defn lambda-client [c]
  (let [access-id (or (:access-id c)
                      (::access-id c))
        secret-key (or (:secret-key c)
                       (::secret-key c))
        region (or (:region c)
                   (::region c))]
    (-> (AWSLambdaClientBuilder/standard)
        (.withCredentials (AWSStaticCredentialsProvider.
                            (credentials
                              access-id
                              secret-key)))
        (.withRegion region)
        (.build))))

(defmethod jd/to-java
  [java.util.Map clojure.lang.PersistentArrayMap]
  [clazz m]
  (->> m
       (map (fn [[k v]]
              [(name k) v]))
       (into {})))

(defmethod jd/from-java java.nio.ByteBuffer [b]
  (bs/convert b String))

(defn validation-exception? [e]
  (str/includes?
    (.getMessage e)
    "ValidationException"))

(defn create-function [client-config payload]
  (try
    (when-let [zipFile (-> payload :code :zipFile)]
      (when (or (string? zipFile))
        (ks/throw-str "Zipfile must be type java.nio.Buffer")))
    [(jd/from-java
       (.createFunction
         (lambda-client client-config)
         (jd/to-java
           CreateFunctionRequest
           payload)))]
    (catch ResourceConflictException e
      [nil {:error-code :function-exists} e])
    (catch AWSLambdaException e
      (cond
        (validation-exception? e)
        [nil {:error-code :validation-failed} e]
        :else [nil {:error-code :unknown} e]))
    (catch Exception e
      (cond
        (validation-exception? e)
        [nil {:error-code :validation-failed} e]
        :else [nil {:error-code :unknown} e]))))

(defn path->zipfile [path]
  (bs/convert
    (io/file path)
    java.nio.ByteBuffer))



(defn get-function [client-config payload]
  (try
    [(jd/from-java
       (.getFunction
         (lambda-client client-config)
         (jd/to-java
           GetFunctionRequest
           payload)))]
    (catch ResourceNotFoundException e
      [nil {:error-code :resource-not-found}])))

(defn update-function-code [client-config payload]
  (try
    [{:success? true
      :update-result
      (jd/from-java
        (.updateFunctionCode
          (lambda-client client-config)
          (jd/to-java
            UpdateFunctionCodeRequest
            payload)))}]
    (catch ResourceNotFoundException e
      [nil {:error-code :resource-not-found} e])
    (catch InvalidParameterValueException e
      [nil {:error-code :validation-failed} e])
    (catch Exception e
      [nil {:error-code :unknown} e])))

(defn update-function-configuration [client-config payload]
  (try
    [{:success? true
      :update-result
      (jd/from-java
        (.updateFunctionConfiguration
          (lambda-client client-config)
          (jd/to-java
            UpdateFunctionConfigurationRequest
            payload)))}]
    (catch ResourceNotFoundException e
      [nil {:error-code :resource-not-found} e])
    (catch InvalidParameterValueException e
      [nil {:error-code :validation-failed} e])
    (catch Exception e
      [nil {:error-code :unknown} e])))

(defn invoke [client-config payload]
  (try
    [(-> (jd/from-java
           (.invoke
             (lambda-client client-config)
             (doto (jd/to-java
                     InvokeRequest
                     payload))))
         (update :logResult ks/from-base64-str))]
    (catch ResourceNotFoundException e
      [nil {:error-code :resource-not-found} e])
    (catch Exception e
      [nil {:error-code :unknown} e])))

(defn prep-invoke-payload [m]
  (bs/convert
    (ks/to-json m)
    java.nio.ByteBuffer))






(comment
  (ks/pp
    (create-function
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "0cDUzKX6WdChzPIPPaFrMzV7PUoz6k9wcun1/Dfc"}
      {:functionName "dummy-fn"
       :code {:zipFile (path->zipfile "test/resources/echo.zip")}
       :description "Dummy test functin, echo."
       :environment {:variables {:envkey "envval"}}
       :handler "run.echo"
       :memorySize 128
       :publish true
       :role "arn:aws:iam::753209818049:role/lambda-exec"
       :runtime "nodejs8.10"
       :tags {:tagkey "tagval"}
       :timeout 10
       :tracingConfig {:mode "Active"}}))

  ;; good
  (ks/pp
    (get-function
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "0cDUzKX6WdChzPIPPaFrMzV7PUoz6k9wcun1/Dfc"}
      {:functionName "dummy-fn"}))

  ;; not found
  (ks/pp
    (get-function
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "0cDUzKX6WdChzPIPPaFrMzV7PUoz6k9wcun1/Dfc"}
      {:functionName "not-found"}))

  ;; good
  (ks/pp
    (update-function-code
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "0cDUzKX6WdChzPIPPaFrMzV7PUoz6k9wcun1/Dfc"}
      {:functionName "dummy-fn"
       :zipFile (path->zipfile "test/resources/echo.zip")}))


  ;; failed validation
  (ks/pp
    (update-function-code
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "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"}
      {:functionName "not-found"}))

  ;; not found
  (ks/pp
    (update-function-code
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "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"}
      {:functionName "not-found"
       :zipFile (path->zipfile "test/resources/echo.zip")}))


  ;; INVOKE

  ;; good
  (ks/pp
    (invoke
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "0cDUzKX6WdChzPIPPaFrMzV7PUoz6k9wcun1/Dfc"}
      {:functionName "dummy-fn"
       :logType "Tail"
       :payload (prep-invoke-payload {:foo "bar"})}))

  ;; not found

  (ks/pp
    (invoke
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "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"}
      {:functionName "not-found"
       :logType "Tail"
       :payload (prep-invoke-payload {:foo "bar"})}))


  ;; unknown

  (ks/pp
    (invoke
      {:region "us-west-2"
       :access-key "AKIAJIMN7IBTFMNRLSZA"
       :secret-key "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"}
      {:functionName nil
       :logType "Tail"
       :payload (prep-invoke-payload {:foo "bar"})})))
