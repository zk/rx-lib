(ns rx.media-model
  (:require [cljs.spec.alpha :as s :include-macros true]
            [cljs.spec.gen.alpha :as gen]
            [clojure.test.check.generators]
            [cljs.test :as test :refer-macros [deftest is]]
            [clojure.string :as str]
            [rx.specs]
            [rx.kitchen-sink :as ks]))

(s/def :media/id :rx.specs/id)
(s/def :media/orig-ios-local-identifier string?)
(s/def :media/orig-mime-type string?)
(s/def :media/orig-width int?)
(s/def :media/orig-height int?)
(s/def :media/orig-size-bytes int?)
(s/def :media/orig-created-ts :rx.specs/time-index)
(s/def :media/orig-updated-ts :rx.specs/time-index)
(s/def :media/orig-exif map?)

(defn rn-image-crop-picker-item->media
  [{:strs [modificationDate
           path
           exif
           width
           mime
           size
           creationDate
           localIdentifier
           height
           filename
           path]}]
  (let [mime-type (if (and filename
                           (str/ends-with? filename ".HEIC"))
                    "image/heic"
                    mime)]
    {:media/id localIdentifier
     :media/orig-ios-local-identifier localIdentifier
     :media/orig-mime-type mime-type
     :media/orig-width width
     :media/orig-height height
     :media/orig-size-bytes size
     :media/orig-created-ts (* (ks/parse-long creationDate) 1000)
     :media/orig-updated-ts (* (ks/parse-long modificationDate) 1000)}))

(defn photos-framework-asset->media [{:keys [local-identifier
                                             media-type
                                             width
                                             height
                                             created-ts
                                             modified-ts
                                             mime-type]}]
  (->> {:media/id local-identifier
        :media/orig-ios-local-identifier local-identifier
        :media/orig-ios-media-type media-type
        :media/orig-width width
        :media/orig-height height
        :media/orig-created-ts created-ts
        :media/orig-updated-ts modified-ts
        :media/orig-mime-type mime-type}
       (remove #(nil? (second %)))
       (into {})))


(comment
  ;; video

  (ks/pp
    (rn-image-crop-picker-item->media
      {"sourceURL"
       "file:///Users/zk/Library/Developer/CoreSimulator/Devices/8DDD4B61-F5BA-4499-A5E7-20657C5E63B1/data/Media/DCIM/100APPLE/IMG_0007.MOV",
       "creationDate" nil,
       "width" 1920,
       "cropRect" nil,
       "height" 1080,
       "path"
       "file:///Users/zk/Library/Developer/CoreSimulator/Devices/8DDD4B61-F5BA-4499-A5E7-20657C5E63B1/data/Containers/Data/Application/5C3F570C-1156-4FDD-8CB7-AE870EF4FA86/tmp/react-native-image-crop-picker/55929A72-CFC6-4EFC-8A4F-D0449DC2200E.mp4",
       "modificationDate" nil,
       "exif" nil,
       "mime" "video/mp4",
       "size" 4443237,
       "data" nil,
       "localIdentifier" "9CD98DB9-616D-44A0-B8C9-F69C28B8DEC6/L0/001",
       "filename" "IMG_0007.MOV"}))

  )

(defn remove-db-keys [media]
  (when media
    (dissoc media :db/id)))

(defn valid-for-upload? [{:keys [:media/orig-file-missing-ts]}]
  (and (not orig-file-missing-ts)
       true))

(defn uploading? [{:keys [:media/upload-progress]}]
  (and upload-progress
       (not= 100 (or upload-progress))))

(defn ios-asset-library-uri [{:keys [:media/orig-ios-local-identifier]}]
  (when orig-ios-local-identifier
    (str
      "assets-library://asset?id="
      (first (str/split orig-ios-local-identifier #"/")))))

(defn ios-asset-library-video-uri [{:keys [:media/orig-ios-local-identifier]}]
  (when orig-ios-local-identifier
    (str
      "assets-library://asset/asset.mov?id="
      (first (str/split orig-ios-local-identifier #"/"))
      "&ext=mov")))

(defn ios-photos-uri [{:keys [:media/orig-ios-local-identifier]}]
  (when orig-ios-local-identifier
    (str "photos://asset/" orig-ios-local-identifier)))

(defn s3-public-uri [{:keys [:media/s3-region
                             :media/s3-bucket
                             :media/s3-key]}]
  (when (and s3-region s3-bucket s3-key)
    (str "https://s3-" s3-region ".amazonaws.com/" s3-bucket "/" s3-key)))


(defn video? [{:keys [:media/orig-mime-type]}]
  (and orig-mime-type
       (str/includes? orig-mime-type "video/")))

(defn oriented-orig-width
  [{:keys [:media/orig-width
           :media/orig-height
           :media/orientation]}]
  (cond
    (get #{"up" "down"} orientation) orig-height
    :else orig-width))

(defn oriented-orig-height
  [{:keys [:media/orig-width
           :media/orig-height
           :media/orientation]}]
  (cond
    (get #{"up" "down"} orientation) orig-width
    :else orig-height))
