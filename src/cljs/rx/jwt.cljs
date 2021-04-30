(ns rx.jwt
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]))

(defn from-jwt [s]
  (when s
    (let [[header-str
           payload-str
           sig-str] (str/split s ".")]

      {:header (-> header-str
                   ks/from-base64-str
                   ks/from-json)
       :payload (-> payload-str
                    ks/from-base64-str
                    ks/from-json)
       :signature sig-str})))

(defn jwt-str->payload [s]
  (:payload (from-jwt s)))

(defn jwt-fresh? [s & [now]]
  (when-let [exp (-> s jwt-str->payload :exp)]
    (< (or now (ks/now)) (* exp 1000))))

(comment
  (from-jwt "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

  
  )
