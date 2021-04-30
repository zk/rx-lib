(ns rx.jvm.ops.awslambda
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer [<defn gol <?]]
            [rx.jvm.ops.build :as build]
            [clojure.string :as str]
            [clj-http.client :as hc]
            [rx.aws :as aws]))

(defn expand-home [s]
  (if (.startsWith s "~")
    (str/replace-first s "~" (System/getProperty "user.home"))
    s))

(defn cljs-build-params
  "Build spec to cljs build api parameters"
  [{:keys [::build/output-path
           ::build/main-namespace
           ::build/cljs-compiler]}]
  {:source-paths ["src/cljc"
                  "src/cljs"
                  "src/cljs-node"]
   :compiler (merge
               {:output-to (str output-path "/cljs/awslambda.js")
                :output-dir (str output-path "/cljs")
                :optimizations :simple
                :source-map false
                :main (str main-namespace)
                :parallel-build true
                :verbose false
                :target :nodejs
                :foreign-libs build/common-foreign-libs
                :externs ["resources/dexie/dexie.ext.js"
                          "resources/tonejs/Tone.ext.js"]
                :warnings {:undeclared-ns true
                           :unprovided true
                           :undeclared-var true
                           :munged-namespace true
                           :ns-var-clash true}
                :warning-handlers
                #_nil
                [cljs.analyzer.api/default-warning-handler
                 (fn [warning-type env extra]
                   (when (and (get {:undeclared-ns true
                                    :unprovided true
                                    :undeclared-var true
                                    :munged-namespace true
                                    :ns-var-clash true}
                                warning-type)
                              (not (:macro-present? extra))
                              (not (= 'thisfn (:suffix extra))))
                     (let [si (cljs.analyzer/source-info env)]
                       (anom/throw-anom
                         {:desc (str "Error compiling " (:file si))
                          ::build/source-info si}))
                     #_(throw
                         (cljs.analyzer/error
                           env
                           (cljs.analyzer/error-message warning-type extra)))))]}
               cljs-compiler)})

(defn copy-cljs-assets [{:keys [::build/output-path]}]
  (build/report-and-run
    ["cp"
     (str output-path "/cljs/awslambda.js")
     (str output-path "/build/")]))

(defn create-build-directory [{:keys [::build/output-path]}]
  (build/mkdir (str output-path "/build")))

(defn copy-node-modules [{:keys [::build/output-path]}]
  (let [package-json (ks/from-json (slurp "./package.json") true)
        deps (get package-json "dependencies")
        lib-paths (->> deps
                       (map (fn [[s _]]
                              (str "node_modules/" s))))]
    (build/mkdir (str output-path "/build/node_modules"))
    (doseq [path lib-paths]
      (build/report-and-run
        ["cp" "-R" path (str output-path "/build/node_modules/")]))))

(defn create-zip [{:keys [::build/output-path]}]
  (println " * Creating zip file...")
  (build/mkdir (str output-path "/dist"))
  (build/zip
    (str output-path "/build")
    "../dist/awslambda.zip"))

(defn build-lambda! [spec]
  (try
    (build/clean-output-path spec)
    (create-build-directory spec)
    (println " * Compiling cljs...")
    (build/compile-cljs
      (cljs-build-params spec))
    (copy-cljs-assets spec)
    (copy-node-modules spec)
    (create-zip spec)
    (println "Build completed:" (str (build/cwd) "/" (::build/output-path spec)))
    (catch Exception e
      (build/report-build-error e))
    (catch AssertionError e
      (build/report-build-error e))))


;; Deploy

(defn lambda-fn-name [{:keys [::aws/lambda-fn-id ::aws/lambda-fn-name] :as spec}]
  (if lambda-fn-id
    (->> [(namespace lambda-fn-id)
          (name lambda-fn-id)]
         (remove nil?)
         (interpose "--")
         (apply str))
    lambda-fn-name))

(defn lambda-create-fn-payload
  [{:keys [::aws/lambda-description
           ::aws/lambda-env
           ::aws/lambda-handler
           ::aws/lambda-memory-mb
           ::aws/lambda-publish?
           ::aws/lambda-role-arn
           ::aws/lambda-runtime-name
           ::aws/lambda-tags
           ::aws/lambda-timeout-seconds
           ::aws/lambda-tracing-config
           ::aws/lambda-kms-key-arn
           ::aws/lambda-vpc-config
           ::build/output-path]
    :as spec}]
  (merge
    {:code {:zipFile (-> (str (build/cwd) "/" output-path "/dist/awslambda.zip")
                         aws/path->zipfile)}
     :description lambda-description
     :environment {:variables lambda-env}
     :functionName (lambda-fn-name spec)
     :memorySize lambda-memory-mb
     :publish lambda-publish?
     :role lambda-role-arn
     :runtime lambda-runtime-name
     :tags lambda-tags
     :timeout lambda-timeout-seconds
     :handler lambda-handler}
    (when lambda-kms-key-arn
      {:kmsKeyArn lambda-kms-key-arn})
    (when lambda-tracing-config
      {:tracingConfig {:mode lambda-tracing-config}})
    (when lambda-vpc-config
      {:vpcConfig lambda-vpc-config})
    {:zip-path (str (build/cwd) "/" output-path "/dist/awslambda.zip")}))

(defn report-create-function [spec]
  (println " * Creating function" (lambda-fn-name spec))
  (let [[res err exc :as pl] (aws/create-function
                               spec
                               (lambda-create-fn-payload spec))]
    (when err
      (ks/pp exc)
      (println " ! Error creating function" err))
    pl))

(defn zip-byte-buffer [zip-path]
  (aws/path->zipfile zip-path))

(defn report-update-function-code [{:keys [::build/output-path]
                                    :as spec}]
  (let [lfn-name (lambda-fn-name spec)]
    (println " * Updating function" lfn-name)
    (let [[res err] (aws/update-function-code
                      spec
                      {:functionName lfn-name
                       :zipFile (zip-byte-buffer (str output-path "/dist/awslambda.zip"))})]
      (when err
        (println " ! Error updating function code" err))
      [res err])))

(defn report-create-or-update-function
  [spec]
  (let [function-name (lambda-fn-name spec)]
    (println " * Checking for existing" function-name)
    (let [[exists?] (aws/get-function
                      spec
                      {:functionName function-name})]
      (if exists?
        (report-update-function-code spec)
        (report-create-function spec)))))

(defn deploy-lambda! [spec]
  (try
    (ks/pp
      (report-create-or-update-function spec))
    (catch Exception e
      (build/report-build-error e))
    (catch AssertionError e
      (build/report-build-error e))))

(defn verify-endpoint [{:keys [::aws/api-gateway-invoke-url]}]
  (let [res (hc/get api-gateway-invoke-url)]
    (println "Body")
    (println (:body res))
    (println "Status" (:status res))))

(defn test-good-build []
  (build-lambda!
    {::build/output-path "target/test-build"
     ::build/main-namespace "rx.node.lambda"}))

(defn test-cljs-error []
  (build-lambda!
    {::build/output-path "target/test-build"
     ::build/main-namespace "rx.node.ops.test.cljs-build-error"}))

(defn test-clean-error []
  (build-lambda!
    {::build/output-path "/nopath"
     ::build/main-namespace "rx.node.ops.test.cljs-build-error"}))

(defn test-oc-build []
  (build-lambda!
    {::build/output-path "target/oc-lambda"
     ::build/main-namespace "oc.node.awslambda"}))

(defn test-oc-deploy []
  (deploy-lambda!
    (ks/spy
      (merge
        {::aws/lambda-fn-id :onlycrayons_apiserver
         ::aws/lambda-runtime-name "nodejs12.x"
         ::aws/lambda-memory-mb 512
         ::aws/lambda-timeout-seconds 30
         ::aws/lambda-handler "awslambda.handler"
         ::aws/lambda-role-arn "arn:aws:iam::630648271175:role/service-role/onlycrayons-lambda-role-0jrh42uu"
         ::build/output-path "target/oc-lambda"}
        (ks/edn-read-string
          (slurp (expand-home "~/.aws/onlycrayons-lambda-deploy.edn")))))))

(defn test-verify-endpoint []
  (try
    (verify-endpoint
      {::aws/api-gateway-invoke-url "https://jr3ujmwd23.execute-api.us-west-2.amazonaws.com/prod/canary"})
    (catch Exception e
      (println " ! Verify failed:" e))))

(defn test-ship []
  (let [spec (merge
               {::build/output-path "target/oc-lambda"
                ::build/main-namespace "oc.node.awslambda"}
               {::aws/lambda-fn-id :onlycrayons_apiserver
                ::aws/lambda-runtime-name "nodejs12.x"
                ::aws/lambda-memory-mb 512
                ::aws/lambda-timeout-seconds 30
                ::aws/lambda-handler "awslambda.handler"
                ::aws/lambda-role-arn "arn:aws:iam::630648271175:role/service-role/onlycrayons-lambda-role-0jrh42uu"
                ::build/output-path "target/oc-lambda"}
               {::aws/api-gateway-invoke-url "https://jr3ujmwd23.execute-api.us-west-2.amazonaws.com/prod/canary"}
               (ks/edn-read-string
                 (slurp (expand-home "~/.aws/onlycrayons-lambda-deploy.edn"))))]

    (build-lambda! spec)
    (deploy-lambda! spec)
    (verify-endpoint spec)))

(comment

  (set! *print-namespace-maps* false)

  (test-good-build)

  (test-cljs-error)

  (test-clean-error)

  (test-oc-build)

  (test-oc-deploy)

  (test-verify-endpoint)
  
  (test-ship)
  
  )
