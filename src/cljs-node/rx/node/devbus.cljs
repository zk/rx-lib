(ns rx.node.devbus
  (:require
   [rx.kitchen-sink :as ks]
   [rx.node.awsclient :as ac]
   [cljs.core.async
    :refer [<!]
    :refer-macros [go]]))

(defn gen-gi-handle-state-upload [{:keys [s3-bucket
                                          s3-prefix]}]
  (when (or (not s3-bucket)
            (not s3-prefix))
    (ks/throw-str "gen-gi-handle-state-upload requires :s3-bucket, :s3-prefix"))
  (fn [id state]
    (when (or (not id)
              (not (string? id))
              (not state))
      (ks/throw-str "Missing or invalid id or state"))

    (go
      (println
        "Uploading state to id"
        id
        "with keys"
        (and (map? state)
             (keys state)))

      (let [put-res (<! (ac/<s3 "putObject"
                          {:Body (ks/to-transit state)
                           :Key (str s3-prefix "/" id ".transit.json")
                           :Bucket s3-bucket
                           :ContentType "application/transit+json"}))]
        (println put-res)))))

(defn gi-handlers [upload-config]
  {:devbus/upload-state (gen-gi-handle-state-upload upload-config)})

#_(go
    (let [<f (gen-gi-handle-state-upload {:s3-bucket "nalopastures"
                                          :s3-prefix "debug-app-state"})]
    (prn (<! (<f "foo" {:bar "baz"})))))
