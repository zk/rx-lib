(ns rx.node.awsdeploy
  (:require [rx.kitchen-sink :as ks]
            [rx.node.aws :as aws]
            [rx.specs :as specs]
            [cljs.spec.alpha :as s]
            [cljs.core.async
             :as async
             :refer [put! <! close! chan]
             :refer-macros [go]]))

(s/def :aws.lda/function-id :rx.specs/non-empty-string)
(s/def :aws.lda/runtime-name :rx.specs/non-empty-string)
(s/def :aws.lda/memory-mb #(and (nat-int? %)
                                (>= % 128)
                                (<= % 3008)
                                (= 0 (mod % 64))))

(s/def :aws.lda/timeout-seconds nat-int?)
(s/def :aws.lda/handler :rx.specs/non-empty-string)
(s/def :aws.lda/role-arn :rx.specs/non-empty-string)
(s/def :aws.lda/include-node-modules? boolean?)
(s/def :aws.lda/clean-script vector?)
(s/def :aws.lda/package-script vector?)
(s/def :aws.lda/package-dir :rx.specs/non-empty-string)

(s/def :aws.creds/region :rx.specs/non-empty-string)
(s/def :aws.creds/access-key :rx.specs/non-empty-string)
(s/def :aws.creds/secret-key :rx.specs/non-empty-string)

(s/def :aws.lda/deploy-spec
  (s/keys
    :req [:aws.lda/function-id
          :aws.lda/runtime-name
          :aws.lda/memory-mb
          :aws.lda/timeout-seconds
          :aws.lda/handler
          :aws.lda/role-arn]
    :opt [:aws.lda/include-node-modules?
          :aws.lda/clean-script
          :aws.lda/package-script
          :aws.lda/package-dir

          :aws.creds/regtion
          :aws.creds/access-key
          :aws.creds/secret-key]))

(def CP (js/require "child_process"))

(defn <run-and-report [ss & [opts]]
  (println " * Running" ss)
  (let [ch (chan)
        proc (.spawn CP
               (first ss)
               (clj->js (rest ss))
               (if opts
                 (clj->js opts)
                 #js {}))]

    (.on (.-stdout proc)
      "data"
      (fn [s]
        (print (.toString s))))

    (.on (.-stderr proc)
      "data"
      (fn [s]
        (print (.toString s))))

    (.on proc
      "close"
      (fn [exit-code]
        (put! ch exit-code)
        (close! ch)))
    ch))

(defn <exit-on-non-zero [in-fs]
  (go
    (loop [fs (remove nil? in-fs)]
      (when-not (empty? fs)
        (let [f (first fs)
              res (<! (f))]
          (if (= 0 res)
            (recur (rest fs))
            (println "Non-zero return hit, aborting")))))))

(defn <run-deploy [{:keys [:aws.lda/clean-script
                           :aws.lda/package-script
                           :aws.lda/package-dir
                           :aws.lda/include-node-modules?]}]
  (<exit-on-non-zero
    [(when clean-script
       #(<run-and-report clean-script))
     (when package-script
       #(<run-and-report package-script))
     #_(when include-node-modules?
         #(<run-and-report
            ["rsync" "-a" "--stats" "./node_modules" package-dir]))
     #_(when package-dir
         #(zip-package lfn-config))]))

(defn compile-command [{:keys [:aws.lda/deps]}]
  ["clj"
   "-Sdeps"
   (pr-str deps)
   "--main"
   "cljs.main"
   "--compile"
   "evry.sync-server-lambda"
   "--target"
   "node"
   "--output-to"
   "test-out.js"])

(comment

  (<run-deploy
    (merge
      {:aws.lda/function-id :evry/ddb-sync-test
       :aws.lda/runtime-name "nodejs8.10"
       :aws.lda/memory-mb 512
       :aws.lda/timeout-seconds 30
       :aws.lda/handler "run.handler"
       :aws.lda/role-arn "arn:aws:iam::753209818049:role/lambda-exec"
       :aws.lda/include-node-modules? true
       :aws.lda/clean-script ["lein" "with-profile" "prod" "clean"]
       :aws.lda/package-script ["lein" "with-profile" "prod" "cljsbuild" "once" "evry-sync-server-lambda"]
       :aws.lda/package-dir "target/prod/canter/apiserver"}
      {:aws.creds/region "us-west-2"
       :aws.creds/access-key "AKIAIU3CQIP2EY643CTA"
       :aws.creds/secret-key "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"}))


  (<run-and-report
    (compile-command
      (merge
        {:aws.lda/function-id :evry/ddb-sync-test
         :aws.lda/runtime-name "nodejs8.10"
         :aws.lda/memory-mb 512
         :aws.lda/timeout-seconds 30
         :aws.lda/handler "run.handler"
         :aws.lda/role-arn "arn:aws:iam::753209818049:role/lambda-exec"
         :aws.lda/include-node-modules? true
         :aws.lda/clean-script ["lein" "with-profile" "prod" "clean"]
         :aws.lda/package-script ["lein" "with-profile" "prod" "cljsbuild" "once" "evry-sync-server-lambda"]
         :aws.lda/package-dir "target/prod/canter/apiserver"}
        {:aws.creds/region "us-west-2"
         :aws.creds/access-key "AKIAIU3CQIP2EY643CTA"
         :aws.creds/secret-key "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"})))

  (<run-and-report ["ls" "-aul"])
  
  (prn "hello world")

  )
