(ns rx.awsdeploy
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [rx.kitchen-sink :as ks]
            [clj-http.client :as hc]
            [rx.aws :as aws]
            [me.raynes.conch :as ch]
            [me.raynes.conch.low-level :as sh]
            [cljsbuild.compiler :as cc]))

(def lfn-required-keys
  #{:function-id
    :runtime-name
    :memory-mb
    :handler
    :role-arn
    :clean-script
    :package-script
    :package-dir})

(def lfn-defaults
  {:no-publish? false
   :timeout-seconds 10
   :tracing-config "PassThrough"})

(defn read-config []
  (edn/read
    (java.io.PushbackReader.
      (io/reader
        (.getBytes
          (slurp
            "awsdeploy.edn"))))))

(defn first-lambda []
  (-> (read-config)
      :lambda
      :functions
      first))

(defn lfnc->function-name [{:keys [function-id function-name] :as lfn-config}]
  (if function-id
    (str (namespace function-id)
         "__"
         (name function-id))
    function-name))

(defn proc-blocking [command]
  (try
    (let [p (apply sh/proc command)]
      (future (sh/stream-to-out p :out))
      (future (sh/stream-to-out p :err))
      (let [exit-code (sh/exit-code p)]
        (if (= 0 exit-code)
          [{:exit-code exit-code}]
          [nil {:exit-code exit-code}])))
    (catch Exception e
      [nil e])))

(defn proc-blocking-silent-stdout [command]
  (try
    (let [p (apply sh/proc command)]
      (future (sh/stream-to-string p :out))
      (future (sh/stream-to-string p :err))
      (let [exit-code (sh/exit-code p)]
        (if (= 0 exit-code)
          [{:exit-code exit-code}]
          [nil {:exit-code exit-code}])))
    (catch Exception e
      [nil e])))

(defn report-and-run [command]
  (println " *" command)
  (let [[response err] (proc-blocking command)]
    (if err
      (do
        (println " ! Error" command err)
        [nil err])
      [response err])))

(defn report-and-run-silent-stdout [command]
  (println " *" command)
  (let [[response err] (proc-blocking-silent-stdout command)]
    (if err
      (do
        (println " ! Error" command err)
        [nil err])
      [response err])))

(comment

  (prn
    (report-and-run
      ["lein" "with-profile" "prod" "cljsbuild" "once" "canter-apiserver"]))

  )

(defn zip-name [lfn-config]
  (str (lfnc->function-name lfn-config) ".zip"))

(defn lfnc->zip-path [{:keys [zip-path package-dir]
                       :as lfn-config}]
  (or zip-path
      (str package-dir "/" (zip-name lfn-config))))

(defn zip-byte-buffer [lfn-config]
  (-> lfn-config
      lfnc->zip-path
      aws/path->zipfile))

(defn report-update-function-code [lfn-config]
  (let [lfn-name (lfnc->function-name lfn-config)]
    (println " * Updating function" lfn-name)
    (let [[res err] (aws/update-function-code
                      lfn-config
                      {:functionName lfn-name
                       :zipFile (zip-byte-buffer lfn-config)})]
      (when err
        (println " ! Error updating function code" err))
      [res err])))

(defn lfnc->create-function [{:keys [description
                                     env
                                     handler
                                     memory-mb
                                     publish?
                                     role-arn
                                     runtime-name
                                     tags
                                     timeout-seconds
                                     tracing-config
                                     kms-key-arn
                                     vpc-config]
                              :as lfnc}]
  (merge
    {:code {:zipFile (-> lfnc
                         lfnc->zip-path
                         aws/path->zipfile)}
     :description description
     :environment {:variables env}
     :functionName (lfnc->function-name lfnc)
     :memorySize memory-mb
     :publish publish?
     :role role-arn
     :runtime runtime-name
     :tags tags
     :timeout timeout-seconds
     :handler handler}
    (when kms-key-arn
      {:kmsKeyArn kms-key-arn})
    (when tracing-config
      {:tracingConfig {:mode tracing-config}})
    (when vpc-config
      {:vpcConfig vpc-config})
    {:zip-path (lfnc->zip-path lfnc)}))

(defn report-create-function [lfn-config]
  (println " * Creating function" (lfnc->function-name lfn-config))
  (let [[res err exc :as pl] (aws/create-function
                               lfn-config
                               (lfnc->create-function lfn-config))]
    (when err
      (ks/pp exc)
      (println " ! Error creating function" err))
    pl))

(defn report-create-or-update-function
  [lfn-config]
  (let [function-name (lfnc->function-name lfn-config)]
    (println " * Checking for existing" function-name)
    (let [[exists?] (aws/get-function
                      lfn-config
                      {:functionName function-name})]
      (if exists?
        (report-update-function-code lfn-config)
        (report-create-function lfn-config)))))

(defn report-test-invoke [lfn-config payload]
  (println " * Invoking"
    (lfnc->function-name lfn-config)
    "with payload"
    (pr-str payload))
  (let [[res err] (aws/invoke
                    lfn-config
                    {:functionName (lfnc->function-name lfn-config)
                     :logType "Tail"})]
    (if err
      (do
        (println " * Error running fn")
        [nil err])
      (let [data res
            err? (get #{"Unhandled" "Handled"} (:functionError data))]
        (if err?
          (do
            (ks/pp data)
            (println "\n")
            (println (:logResult data))
            (println "* Error running lfn" (lfnc->function-name lfn-config))
            [nil data])
          (do
            (println " * Run Log vvvvvvvvvvvvvvvvvvv\n\n")
            (println (:logResult data))

            (println "\n * Result vvvvvvvvvvvvvvvvvvvv\n\n")
            (println (:payload data))
            (println "\n")

            (println " * Test Invoke Succeeded")
            [data]))))))

(defn test-deployed-function [{:keys [verify-payloads]
                               :as lfn-config}]
  (println " * Verifying" (count verify-payloads) "payload(s)")
  (loop [pls verify-payloads]
    (if (empty? pls)
      [{:success? true}]
      (let [pl (first pls)
            [res err] (report-test-invoke lfn-config pl)]
        (if err
          [nil err]
          (recur (rest pls)))))))

(defn zip-package [{:keys [package-dir] :as lfn-config}]
  "zip -vr rx-node.zip ./* | grep --color=none totalbytes"
  (println " * Zipping" package-dir)
  (let [[res err] (proc-blocking-silent-stdout
                    ["zip"
                     "-vr"
                     (zip-name lfn-config)
                     "."
                     :dir
                     package-dir])]
    (when err
      (println " ! Error zipping:" err))
    [res err]))

(defn exit-on-error [fns]
  (loop [fns fns]
    (when-let [f (first fns)]
      (let [[res err] (f)]
        (if (if (:exit-code res)
              (= 0 (:exit-code res))
              (and res (not err)))
          (recur (rest fns))
          (println " ! Error encountered, exiting block"))))))

(defn deploy-lambda-fn [{:keys [clean-script
                                package-script
                                package-dir
                                include-node-modules?
                                cljsbuild]
                         :as lfn-config}]
  (exit-on-error
    (->> [(when clean-script
            #(report-and-run clean-script))
          #_(when package-dir
              #(report-and-run ["rm" "-rf" package-dir]))
          (when package-script
            #(report-and-run package-script))
          (when include-node-modules?
            #(report-and-run
               ["rsync" "-a" "--stats" "./node_modules" package-dir]))
          (when package-dir
            #(zip-package lfn-config))
          #(report-create-or-update-function lfn-config)
          #(test-deployed-function lfn-config)]
         (remove nil?)))
  #(println " * Complete"))

(defn deploy [id-or-lfc]
  (let [deploy-config (when (keyword? id-or-lfc)
                        (read-config))
        creds (if (keyword? id-or-lfc)
                (select-keys
                  deploy-config
                  [:region
                   :access-key
                   :secret-key]))]
    (if-let [lfc (if (keyword? id-or-lfc)
                   (->> deploy-config
                        :lambda
                        :functions
                        (filter #(= id-or-lfc (:function-id %)))
                        first)
                   id-or-lfc)]
      (deploy-lambda-fn
        (merge
          creds
          lfc))
      (println " ! No config found for id" id-or-lfc))))

(defn invoke [id]
  (let [deploy-config (read-config)
        creds (select-keys
                deploy-config
                [:region
                 :access-key
                 :secret-key])]
    (if-let [lfc (->> deploy-config
                      :lambda
                      :functions
                      (filter #(= id (:function-id %)))
                      first)]
      (test-deployed-function
        (merge
          creds
          lfc))
      (println " ! No config found for id" id))))

(comment

  (deploy
    (merge
      {:function-id :canter/apiserver_20191205
       :runtime-name "nodejs8.10"
       :memory-mb 512
       :timeout-seconds 30
       :handler "run.handler"
       :role-arn "arn:aws:iam::753209818049:role/lambda-exec"
       :include-node-modules? true
       :clean-script ["lein" "with-profile" "prod" "clean"]
       :package-script ["bin/apiserver-build-and-test"]
       :package-dir "target/prod/canter/apiserver"}
      {:region "us-west-2"
       :access-key ""
       :secret-key ""}))

  (deploy
    (merge
      {:function-id :evry/sync_lambda_20191201
       :runtime-name "nodejs8.10"
       :memory-mb 512
       :timeout-seconds 30
       :handler "run.handler"
       :role-arn "arn:aws:iam::753209818049:role/evry-prod-lambda"
       :include-node-modules? true
       :clean-script ["lein" "with-profile" "prod" "clean"]
       :package-script ["lein" "with-profile" "prod" "cljsbuild" "once" "evry-sync-lambda"]
       :package-dir "target/prod/evry-sync-lambda"}
      {:region "us-west-2"
       :access-key ""
       :secret-key ""}))

  (deploy
    (merge
      {:function-id :zkdev/sync_server_lambda_20191207
       :runtime-name "nodejs8.10"
       :memory-mb 512
       :timeout-seconds 30
       :handler "run.handler"
       :role-arn "arn:aws:iam::753209818049:role/zkdev-prod-sync-server-lambda"
       :include-node-modules? true
       :clean-script ["lein" "with-profile" "prod" "clean"]
       :package-script ["lein" "with-profile" "prod" "cljsbuild" "once" "zkdev-sync-server-lambda"]
       :package-dir "target/prod/zkdev-sync-server-lambda"}
      {:region "us-west-2"
       :access-key ""
       :secret-key ""}))

  (deploy
    (merge
      {:function-id :rx/sync_server_lambda_20191217
       :runtime-name "nodejs12.x"
       :memory-mb 512
       :timeout-seconds 30
       :handler "run.handler"
       :role-arn "arn:aws:iam::753209818049:role/rx-sync-server-lambda"
       :include-node-modules? true
       :clean-script ["lein" "with-profile" "prod" "clean"]
       :package-script ["lein" "with-profile" "prod" "cljsbuild" "once" "rx-sync-server-lambda"]
       :package-dir "target/prod/rx-sync-server-lambda"}
      {:region "us-west-2"
       :access-key ""
       :secret-key ""}))


  #_(deploy :canter/anomaly-reporting)

  #_(deploy :canter/apiserver_20190513)

  #_(deploy
      (merge
        {:function-id :canter/reminderpoll_20190731
         :runtime-name "nodejs8.10"
         :memory-mb 512
         :timeout-seconds 270
         :handler "run.handler"
         :role-arn "arn:aws:iam::753209818049:role/lambda-exec"
         :include-node-modules? true
         :clean-script ["lein" "with-profile" "prod" "clean"]
         :package-script ["lein" "with-profile" "prod"
                          "cljsbuild" "once" "canter-reminderpoll"]
         :package-dir "target/prod/canter/reminderpoll"
         :verify-payloads [{}]}
        {:region "us-west-2"
         :access-key ""
         :secret-key ""}))

  #_(deploy :canter/generatethumbs)

  #_(report-and-run
      ["rsync" "-a" "--stats" "./node_modules" "target/prod/canter/apiserver"])

  (zip-package
    {:function-id :canter/apiserver_20190526
     :runtime-name "nodejs8.10"
     :memory-mb 512
     :timeout-seconds 30
     :handler "run.handler"
     :role-arn "arn:aws:iam::753209818049:role/lambda-exec"
     :include-node-modules? true
     :clean-script ["lein" "with-profile" "prod" "clean"]
     :package-script ["bin/apiserver-build-and-test"]
     :package-dir "target/prod/canter/apiserver"})

  )


(comment

  (-> {:user/account-email "zk+60@heyzk.com",
       :user/id "2399a725-2559-4bdc-ad28-546077473e33",
       :user/cognito-user-sub "2399a725-2559-4bdc-ad28-546077473e33",
       :user/push-tokens-ios
       #{"5bbd4cddebec021483f2f6e276a62c0d233b1fd71f55ad08ba17e89d4dc9cb98"},
       :user/push-tokens-ios-with-errors
       #{"5bbd4cddebec021483f2f6e276a62c0d233b1fd71f55ad08ba17e89d4dc9cb98"},
       :user/business-name "Z",
       :db/id 3}
      (dissoc :user/push-tokens-ios-with-errors))


  )
