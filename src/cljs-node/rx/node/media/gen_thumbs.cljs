(ns rx.node.media.gen-thumbs
  (:require [rx.kitchen-sink :as ks]
            [rx.node.awsclient :as aws]
            [clojure.string :as str]
            [cljs.core.async
             :refer [<! chan put!]
             :refer-macros [go go-loop]]
            [goog.object :as gobj]))

(def JIMP (js/require "jimp"))
(def FS (js/require "fs"))

(defn <jimp-thumbnail [{:keys [content
                               scale-size
                               quality
                               output-path]
                        :or {quality 85}}]
  (if (and output-path)
    (let [ch (chan)]
      (.catch
        (.then
          (.read JIMP content)
          (fn [img]
            (.write
              (.quality
                (.scaleToFit
                  (.exifRotate img)
                  scale-size (.-AUTO JIMP))
                quality)
              output-path
              (fn [err img]
                (if err
                  (put! ch [nil err])
                  (.getBuffer
                    img (.-AUTO JIMP)
                    (fn [err buffer]
                      (if err
                        (put! ch [nil err])
                        (put! ch [buffer])))))))))
        (fn [err]
          (put! ch [nil err])))
      ch)
    (ks/throw-str "<thumbnail: missing output-path")))

(comment

  #_(go
      (<!
        (<jimp-thumbnail
          {:content (.readFileSync FS "/tmp/in.jpg")
           :scale-size 200
           :quality 85
           :output-path "/tmp/out.jpg"})))

  )


(defn s3-image-obj?
  [{:keys [ContentType]}]
  (get #{"image/jpeg"
         "image/jpg"
         "image/gif"
         "image/png"} ContentType))

(defn s3-event? [{:keys [eventSource]}]
  (= eventSource "aws:s3"))

(defn <fetch-s3-obj-for-s3-event [{:keys [s3]}]
  (go
    (let [bucket-name (-> s3 :bucket :name)
          obj-key (-> s3 :object :key)]
      (if (and s3 bucket-name obj-key)
        (let [[res err] (<! (aws/<s32
                              "getObject"
                              {:raw-result? true
                               :args [{:Bucket bucket-name
                                       :Key obj-key}]}))]
          (if res
            [(js->clj res :keywordize-keys true)]
            [nil err]))
        [nil {:message "Missing bucket-name or obj-key"}]))))

(defn lambda-request->events [lr]
  (:Records lr))

(defn s3-obj->jimp-thumb-optss [{:keys [Body]}
                                {:keys [thumb-output-file-path
                                        s3-acl
                                        s3-key-prefix
                                        size-specs]
                                 :as config}]
  (->> size-specs
       (map #(merge
               %
               {:prefix s3-key-prefix
                :content Body
                :output-path thumb-output-file-path
                :acl s3-acl}))))

(defn s3-thumb-payload [event
                        {:keys [ContentType]}
                        {:keys [prefix
                                suffix
                                acl]}
                        thumb-content]
  (let [s3-bucket (-> event :s3 :bucket :name)
        s3-key (-> event :s3 :object :key)
        thumb-key (str prefix s3-key suffix)]
    (merge
      {:Body thumb-content
       :Bucket s3-bucket
       :Key thumb-key
       :ContentType ContentType
       :CacheControl "public, max-age=31536000"}
      (when acl
        {:ACL acl}))))

;; output-path
;; acl
;; key-prefix
;;

(defn <run-thumbnail-job [{:keys [thumb-output-file-path
                                  s3-acl
                                  s3-key-prefix
                                  size-specs]
                           :as config}
                          lambda-request]
  (go
    (let [output-path thumb-output-file-path
          events (->> lambda-request
                      lambda-request->events)]
      (println
        (ks/to-json
          {:message "Processing events"
           :events events}))

      (doseq [event events]
        (let [[s3-obj err] (<! (<fetch-s3-obj-for-s3-event event))]
          (if s3-obj
            (if (s3-image-obj? s3-obj)
              (let [thumb-optss (s3-obj->jimp-thumb-optss s3-obj config)]
                (println
                  (ks/to-json {:message "Thumb generation options"
                               :thumb-optss
                               (->> thumb-optss
                                    (map #(assoc % :content "...")))}))

                (doseq [thumb-opts thumb-optss]
                  (let [[thumb-content err] (<! (<jimp-thumbnail thumb-opts))]
                    (if thumb-content
                      (let [thumb-payload (s3-thumb-payload
                                            event
                                            s3-obj
                                            thumb-opts
                                            thumb-content)
                            _ (println
                                (ks/to-json
                                  {:message "Thumb payload"
                                   :thumb-payload (assoc
                                                    thumb-payload
                                                    :Body
                                                    "...")}))
                            [res err] (<! (aws/<s3
                                            "putObject"
                                            thumb-payload))]
                        (println
                          (ks/to-json
                            {:message "Thumb upload result"
                             :result res
                             :error err})))
                      (println
                        (ks/to-json
                          {:message "Error creating thumbnail"
                           :error err}))))))
              (println
                (ks/to-json
                  (merge
                    {:message "S3 object not image, skipping"}
                    (dissoc
                      s3-obj
                      :Body)))))
            (println
              (ks/to-json err))))))))

(defn gen-thumbnail-lambda-handler [config]
  (partial <run-thumbnail-job config))



(comment

  [{:AcceptRanges "bytes",
    :LastModified "2019-02-02T06:37:49.000Z",
    :ContentLength 578633,
    :ETag "\"762adabf1be292d7915f92502dc488dc\"",
    :CacheControl "max-age=31536000",
    :ContentType "video/mp4",
    :Metadata {},
    :Body
    {:type "Buffer",
     :data
     [1 2 3]}}]


  (go
    (ks/pp
      (<!
        (<run-thumbnail-job
          {:thumb-output-file-path "/tmp/out.jpg"
           :s3-acl "public-read"
           :s3-key-prefix "thumbs/"
           :size-specs [{:suffix "_300"
                         :scale-size 300}
                        {:suffix "_800"
                         :scale-size 800}
                        {:suffix "_1400"
                         :scale-size 1400}]}
          {:Records  [ {:eventVersion "2.0", :eventSource "aws:s3", :awsRegion "us-west-2", :eventTime "2018-07-28T07:31:59.467Z", :eventName "ObjectCreated:Post", :userIdentity  {:principalId "AWS:AIDAIMUKEFXZI5FGHBKZI"}, :requestParameters  {:sourceIPAddress "76.173.177.110"}, :responseElements  {:x-amz-request-id "71D1DD48752F5CCB", :x-amz-id-2 "ROXrdiDIhvnSVhw9GxBrtQmqgIFKgl6rR3SuwzB2C2E1E5tEPMjO3c3XF2xdMpHChkN1BCVNYs0="}, :s3  {:s3SchemaVersion "1.0", :configurationId "e44fd775-9ed3-4c98-b50a-d0dc093cd64e", :bucket  {:name "canter-prod-user-media", :ownerIdentity  {:principalId "A103WEAFOPX8J"}, :arn "arn:aws:s3:::nalopastures"}, :object  {:key "0a8c2df1-4af6-48be-bd29-d0a691649ea2/00FDBB32-A4B0-4881-AA34-98DC9A1DAC63-L0-001", :size 7546441, :eTag "30a2ec4e6c8e55c9e9ffdd592f076efe", :sequencer "005B5C1BE7BC21D5CC"}}}]}))))


  (go
    (ks/pp
      (<!
        (<fetch-s3-obj-for-s3-event
          {:eventVersion "2.0", :eventSource "aws:s3", :awsRegion "us-west-2", :eventTime "2018-07-28T07:31:59.467Z", :eventName "ObjectCreated:Post", :userIdentity {:principalId "AWS:AIDAIMUKEFXZI5FGHBKZI"}, :requestParameters  {:sourceIPAddress "76.173.177.110"}, :responseElements  {:x-amz-request-id "71D1DD48752F5CCB", :x-amz-id-2 "ROXrdiDIhvnSVhw9GxBrtQmqgIFKgl6rR3SuwzB2C2E1E5tEPMjO3c3XF2xdMpHChkN1BCVNYs0="}, :s3  {:s3SchemaVersion "1.0", :configurationId "e44fd775-9ed3-4c98-b50a-d0dc093cd64e", :bucket  {:name "canter-prod-user-media", :ownerIdentity  {:principalId "A103WEAFOPX8J"}, :arn "arn:aws:s3:::nalopastures"}, :object  {:key "noid/1C31F242-46ED-40D3-A39B-A75D06151BA7-L0-001", :size 7546441, :eTag "30a2ec4e6c8e55c9e9ffdd592f076efe", :sequencer "005B5C1BE7BC21D5CC"}}}))))

  (go
    (ks/pp
      (<!
        (<aws
          S3
          "getObject"
          {:Bucket "canter-prod-user-media"
           :Key "noid/1C31F242-46ED-40D3-A39B-A75D06151BA7-L0-001"}))))

  (go
    (prn
      (<!
        (<s3-event->s3-obj
          {:eventVersion "2.0", :eventSource "aws:s3", :awsRegion "us-west-2", :eventTime "2018-07-28T07:31:59.467Z", :eventName "ObjectCreated:Post", :userIdentity {:principalId "AWS:AIDAIMUKEFXZI5FGHBKZI"}, :requestParameters  {:sourceIPAddress "76.173.177.110"}, :responseElements  {:x-amz-request-id "71D1DD48752F5CCB", :x-amz-id-2 "ROXrdiDIhvnSVhw9GxBrtQmqgIFKgl6rR3SuwzB2C2E1E5tEPMjO3c3XF2xdMpHChkN1BCVNYs0="}, :s3  {:s3SchemaVersion "1.0", :configurationId "e44fd775-9ed3-4c98-b50a-d0dc093cd64e", :bucket  {:name "nalopastures", :ownerIdentity  {:principalId "A103WEAFOPX8J"}, :arn "arn:aws:s3:::nalopastures"}, :object  {:key "images/894b1d76-8d3c-4b4e-90d5-45a59bfc00a6-l0-001", :size 7546441, :eTag "30a2ec4e6c8e55c9e9ffdd592f076efe", :sequencer "005B5C1BE7BC21D5CC"}}}))))

  (prn "HI")

  )
