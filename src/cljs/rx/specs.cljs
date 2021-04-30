(ns rx.specs
  (:require [cljs.spec.alpha :as s :include-macros true]
            [cljs.spec.gen.alpha :as gen]))

(s/def ::non-empty-string
  (s/and string? #(not (empty? %))))

(s/def ::time-index (s/and
                      int?
                      #(>= % 0)))

(s/def ::id (s/and string?
                   #(not (empty? %))
                   #(> (count %) 0)))

(s/def ::time-index-or-nil
  (s/or :time-index ::time-index
        :nil? nil?))

(s/def ::phone-number string?)

(s/def :rx.time/earlier-ts nat-int?)
(s/def :rx.time/later-ts nat-int?)

(s/def :pers/archived-ts ::time-index-or-nil)
(s/def :pers/created-ts ::time-index)
(s/def :pers/delete-marked-ts ::time-index)
(s/def :pers/updated-ts ::time-index)
(s/def :pers/version nat-int?)
(s/def :pers/owner-id ::id)
(s/def :pers/dirty-ts ::time-index)
(s/def :pers/sync-in-progress-ts ::time-index)
(s/def :pers/sync-failure-ts ::time-index)
(s/def :pers/sync-failure-code ::non-empty-string)
(s/def :pers/sync-success-ts ::time-index)

