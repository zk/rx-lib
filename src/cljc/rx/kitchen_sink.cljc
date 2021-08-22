(ns rx.kitchen-sink
  #? (:clj
      (:require [clojure.string :as str]
                [cheshire.custom :as json]
                [clojure.pprint :as pprint]
                [clojure.java.io :as io]
                [clojure.spec.alpha :as s]
                [hashids.core :as hashids]
                [byte-transforms :as bt]
                [byte-streams :as bs]
                [camel-snake-kebab.core :as csk]
                [cognitect.transit :as transit]
                [camel-snake-kebab.core :as csk]
                [clojure.edn :as edn]
                [rx.anom :as anom :refer [gol]]
                [rx.err :as err]
                [clojure.core.async
                 :refer [<! >! chan put! close! pipe
                         go go-loop take! timeout alts!]])
      :cljs
      (:require [clojure.string :as str]
                [cognitect.transit :as transit]
                [goog.date :as gd]
                [goog.date.DateTime :as gdt]
                [goog.i18n.DateTimeFormat]
                [cljs.pprint :as pprint]
                [cljs.spec.alpha :as s]
                [cljs.reader :as reader]
                [goog.string :as gstring]
                [goog.string.format]
                [camel-snake-kebab.core :as csk]
                [rx.anom :as anom :refer-macros [gol]]
                [rx.err :as err]
                [goog.crypt :as crypt]
                [goog.crypt.base64 :as b64]
                [goog.crypt.Md5 :as Md5]
                [goog.crypt.Sha1 :as Sha1]
                [goog.crypt.Sha2 :as Sha2]
                [goog.crypt.Sha256 :as Sha256]
                [goog.crypt.Sha384 :as Sha384]
                [goog.crypt.Sha512 :as Sha512]
                [cljs.core.async
                 :refer [<! >! chan put! close! pipe take!
                         timeout alts!]
                 :refer-macros [go go-loop]]
                [cljs.core.async.impl.protocols]
                [cljs-time.core :as ct]
                [cljs-time.coerce :as cc]))

  #? (:clj
      (:import [java.util Date]
               [java.text SimpleDateFormat]
               [java.net URLEncoder]
               [org.pegdown PegDownProcessor Extensions]
               [org.joda.time.format ISODateTimeFormat]))
  #? (:cljs
      (:import [goog.string StringBuffer]))

  (:refer-clojure :exclude [uuid pr prn]))


(defn now []
  #? (:clj
      (System/currentTimeMillis)
      :cljs
      (.now js/Date)))


(defn floor [n]
  #? (:clj (Math/floor n)
      :cljs (.floor js/Math n)))

(defn ceil [n]
  #? (:clj (Math/ceil n)
      :cljs (.ceil js/Math n)))

#?
(:clj
 (do
   (def iso-formatter (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
   (def iso-parser (ISODateTimeFormat/dateTimeParser))))

(defn to-iso8601 [o]
  (cond
    (number? o) #? (:clj
                    (.format iso-formatter (Date. o))
                    :cljs
                    (.toUTCIsoString
                      (doto (gd/DateTime.)
                        (.setTime o))
                      false true))

    #? (:clj (= Date (class o))) #? (:clj (.format iso-formatter o))
    #? (:cljs (= js/Date (type o))) #? (:cljs
                                        (.toUTCIsoString
                                          (gd/DateTime. o)
                                          false true))

    (= nil o) nil
    (= "" o) nil
    :else nil))

(defn from-iso8601 [o]
  (cond
    (string? o)
    #? (:clj
        (.getMillis (.parseDateTime iso-parser o))
        :cljs
        (.getTime (gdt/fromIsoString o)))
    :else o))


#? (:clj
    (defn uuid []
      (-> (java.util.UUID/randomUUID)
          str
          (str/replace #"-" ""))))

#? (:cljs
    #_(defn uuid []
        (let [d (now)
              uuid-str "xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx"]
          (str/replace uuid-str
            #"[xy]"
            (fn [c]
              (let [r (bit-or (mod (+ d (* (.random js/Math) 16)) 16) 0)
                    d (.floor js/Math (/ d 16.0))]
                (.toString
                  (if (= "x" c)
                    r
                    (bit-or
                      (bit-and 0x3 r)
                      0x8))
                  16)
                )))))
    (defn uuid []
      (letfn [(hex [] (.toString (rand-int 16) 16))]
        (let [rhex (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 16))) 16)]
          (str (hex) (hex) (hex) (hex)
               (hex) (hex) (hex) (hex)
               (hex) (hex) (hex) (hex)
               "4"   (hex) (hex) (hex)
               rhex  (hex) (hex) (hex)
               (hex) (hex) (hex) (hex)
               (hex) (hex) (hex) (hex)
               (hex) (hex) (hex) (hex))))))

(defn round [n]
  #? (:clj
      (if (float? n)
        (Math/round n)
        n)
      :cljs
      (.round js/Math n)))

(defn abs [n]
  #? (:clj
      (Math/abs n)
      :cljs
      (.abs js/Math n)))

(defn ellipsis [n s]
  (when s
    (let [len (count s)]
      (if (> len n)
        (str (->> s
                  (take n)
                  (apply str))
             "...")
        s))))

(defn ellipsis-center
  "Ellipses the middle of a long string."
  [n s]
  (let [n (max (- n 3) 0)]
    (cond
      (<= (count s) n) s
      :else (let [len (count s)
                  half-len (round (/ len 2.0))
                  to-take-out (- len n)
                  half-take-out (round (/ to-take-out 2.0))
                  first-half (take half-len s)
                  second-half (drop half-len s)]
              (str (->> first-half
                        (take (- half-len half-take-out))
                        (apply str))
                   "..."
                   (->> second-half
                        (drop half-take-out)
                        (apply str)))))))

(defn ellipsis-left [n s]
  (when s
    (let [len (count s)]
      (if (> len n)
        (str "..."
             (->> s
                  reverse
                  (take n)
                  reverse
                  (apply str)))
        s))))

(defn sformat [pattern s]
  #? (:clj
      (format pattern s)
      :cljs
      (gstring/format pattern s)))

(defn date-format [o pattern]
  (cond
    (number? o)
    #? (:clj
        (.format (SimpleDateFormat. pattern) (Date. o))
        :cljs
        (.format (goog.i18n.DateTimeFormat. pattern)
          (.fromTimestamp gd/DateTime o)))

    #? (:clj (= Date (class o))) #? (:clj (.format (SimpleDateFormat. pattern) o))
    #? (:cljs (= js/Date (type o))) #? (:cljs (.format (goog.i18n.DateTimeFormat. pattern) o))

    :else #?(:cljs (try
                     (.format (goog.i18n.DateTimeFormat. pattern)
                       o)
                     (catch js/Error e nil))
             :clj nil)))

(defn throw-str [& args]
  #? (:clj
      (throw (Exception. (apply str args)))
      :cljs
      (throw (js/Error. (apply str args)))))

(defn throw-ex-info [& args]
  (throw (apply ex-info args)))


#? (:clj
    (do
      (def secure-random-obj (java.security.SecureRandom.))

      (defn secure-random-str
        "Generates a random string of bytes in hex"
        [n]
        (let [buf (byte-array n)]
          (.nextBytes secure-random-obj buf)
          (->> buf
               (map #(Integer/toHexString (bit-and % 0xff)))
               (apply str))))))


#? (:clj
    (defn md5
      "Compute the hex MD5 sum of a string."
      [o & [opts]]
      (when o
        (let [md5-str (.toString
                        (new BigInteger 1
                          (bt/hash o :md5 opts))
                        16)
              pad (apply str (repeat (- 32 (count md5-str)) "0"))]
          (str pad md5-str)))))

(defn to-json [o & [opts-or-replacer space]]
  #? (:clj
      (json/generate-string o opts-or-replacer)
      :cljs
      (.stringify js/JSON (clj->js o) opts-or-replacer space)))

(defn from-json [o & [prevent-keywordize]]
  #? (:clj
      (let [keywordize? (not prevent-keywordize)]
        (if (string? o)
          (json/parse-string o keywordize?)
          (json/parse-stream (io/reader o) keywordize?)))
      :cljs
      (js->clj (.parse js/JSON o) :keywordize-keys
        (not prevent-keywordize))))


#? (:clj
    (defn to-transit [o & [opts]]
      (let [bs (java.io.ByteArrayOutputStream.)]
        (transit/write
          (transit/writer bs :json opts)
          o)
        (.toString bs)))
    :cljs
    (defn to-transit [o & [opts]]
      (transit/write
        (transit/writer :json opts)
        o)))

#? (:clj
    (defn from-transit [s & [opts]]
      (when s
        (transit/read
          (transit/reader
            (if (string? s)
              (java.io.ByteArrayInputStream. (.getBytes s "UTF-8"))
              s)
            :json
            opts))))
    :cljs
    (defn from-transit [s & [opts]]
      (when s
        (transit/read
          (transit/reader :json opts)
          s))))

(defn url-encode [s]
  (when s
    #? (:clj
        (java.net.URLEncoder/encode s)
        :cljs
        (js/encodeURIComponent s))))

(defn url-decode [s & [{:keys [encoding]}]]
  (when s
    #? (:clj
        (java.net.URLDecoder/decode s
          (or encoding
              "UTF-8"))
        :cljs
        (js/decodeURI s))))

(defn distinct-by
  [key coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                 ((fn [[f :as xs] seen]
                    (when-let [s (seq xs)]
                      (if (contains? seen (key f))
                        (recur (rest s) seen)
                        (cons f (step (rest s) (conj seen (key f)))))))
                  xs seen)))]
    (step coll #{})))


(defn parse-long [s & [default]]
  (if s
    #? (:clj
        (try
          (Long/parseLong s)
          (catch Exception e
            default))
        :cljs
        (let [res (js/parseInt s)]
          (if (js/isNaN res)
            default
            res)))
      default))

(defn parse-long-base [s base & [default]]
  (if s
    #? (:clj
        (try
          (Long/parseLong s base)
          (catch Exception e
            default))
        :cljs
        (let [res (js/parseInt s base)]
          (if (js/isNaN res)
            default
            res)))
      default))

(defn parse-double [s & [default]]
  (if s
    #? (:clj
        (try
          (Double/parseDouble s)
          (catch Exception e
            default))
        :cljs
        (let [res (js/parseFloat s)]
          (if (js/isNaN res)
            default
            res)))
      default))

(defn pp-str [o]
  #? (:clj
      (let [w (java.io.StringWriter.)]
        (pprint/pprint o w)
        (.toString w))
      :cljs
      (let [sb (StringBuffer.)
            sbw (StringBufferWriter. sb)]
        (pprint/pprint o sbw)
        (str sb))))

(defonce !print-newline? (atom true))
(defonce !print-fn (atom print))

(defn pr [& args]
  (@!print-fn (apply str args)))

(defn pn [& args]
  (@!print-fn 
   (str
     (->> args
          (interpose " ")
          (apply str))
     (when @!print-newline? "\n"))))

(defn prn [& args]
  (@!print-fn 
   (str
     (apply pr-str args)
     (when @!print-newline? "\n"))))

(defn pp [o]
  #? (:clj
      (pprint/pprint o)
      :cljs
      (pr (pp-str o))))

(defn <pp [ch]
  (go
    (try
      (pp (<! ch))
      (catch #? (:cljs js/Error
                 :clj Exception) e
        (pn e)
        (pn (.-stack e))))))

#? (:clj
    (defn ms [date]
      (cond
        (= java.util.Date (class date)) (.getTime date)
        (= org.joda.time.DateTime (class date))  (.getMillis date)))
    :cljs
    (defn ms [date]
      (.getTime date)))

(defn ms-delta-parts [delta]
  (when delta
    (let [ms delta
          s (/ ms 1000)
          m (/ (int s) 60)
          h (/ (int m) 60)
          d (/ (int h) 24)
          y (/ (int d) 365.0)]
      {:ms (int (mod ms 1000))
       :y (int y)
       :d (int (mod d 365))
       :h (int (mod h 24))
       :m (int (mod m 60))
       :s (int (mod s 60))})))

(def time-delta-parts ms-delta-parts)

(defn ms-delta-desc [delta]
  (when delta
    (let [{:keys [ms s m h d y]} (ms-delta-parts delta)]
      (cond
        (and (< s 60)
             (= 0 m h d y)) "less than a minute"
        (< m 2) "1 minute"
        (< h 1) (str (int m) " minutes")
        (< h 2) "1 hour"
        (< d 1) (str (int h) " hours")
        (< d 2) "1 day"
        (< y 1) (str (int d) " days")
        :else (str (sformat "%.1f" y) " years")))))

(defn ms-delta-desc-short [delta]
  (when delta
    (let [{:keys [y d h m]} (ms-delta-parts delta)]
      (->> [(when (> y 0) (str y "y"))
            (when (> d 0) (str d "d"))
            (when (> h 0) (str h "h"))
            (when (> m 0) (str m "m"))
            (when (and (= 0 y) (= 0 d) (= 0 h) (= 0 m))
              "<1m")]
           (remove nil?)
           (interpose " ")
           (apply str)))))

(comment

  (ms-delta-desc-short
    (* 1000 60))

  (ms-delta-parts 1000)

  )

(defn transform-keys [o transform-fn]
  (cond
    (map? o)
    (->> o
         (map (fn [[k v]]
                [(transform-fn k)
                 (transform-keys v transform-fn)]))
         (into {}))

    (coll? o)
    (->> o
         (map #(transform-keys % transform-fn))
         ((fn [out]
            (if (vector? o)
              (vec out)
              out)))
         doall)
    :else o))

(defn kebab-val [o]
  (csk/->kebab-case o))

(defn kebab-coll [m]
  (transform-keys
    m
    kebab-val))

(defn kebab-case [o]
  (if (coll? o)
    (kebab-coll o)
    (kebab-val o)))

(defn env-val [o]
  (csk/->SCREAMING_SNAKE_CASE o))

(defn env-case [o]
  (env-val o))

(defn camel-val [o]
  (csk/->camelCase o))

(defn camel-coll [o]
  (transform-keys
    o
    camel-val))

(defn camel-case [o]
  (if (coll? o)
    (camel-coll o)
    (camel-val o)))

(defn snake-val [o]
  (if (keyword? o)
    (keyword
      (namespace o)
      (csk/->snake_case
        (name o)))
    (csk/->snake_case o)))

(defn snake-coll [o]
  (transform-keys
    o
    snake-val))

(defn snake-case [o]
  (if (coll? o)
    (snake-coll o)
    (snake-val o)))

(defn spy [& os]
  (let [msg (->> os
                 butlast
                 (interpose " ")
                 (apply str))]
    (when-not (empty? msg)
      (pn msg))
    (pp (last os))
    (last os)))

(defn <spy [& os]
  (go
    (let [msg (->> os
                   butlast
                   (interpose " ")
                   (apply str))
          res (<! (last os))]
      (when-not (empty? msg)
        (pn msg))
      (pp res)
      res)))

#? (:clj
    (do
      (defn to-short-id [id salt-str]
        (hashids/encrypt id salt-str))

      (defn from-short-id [short-id salt-str]
        (hashids/decrypt short-id salt-str))))


#? (:clj
    (do
      (defn to-base64-str [s & [o]]
        (when s
          (bs/to-string
            (bt/encode
              s
              :base64
              (or o
                  {:url-safe? false})))))

      (defn from-base64-str [s & [o]]
        (when s
          (bs/to-string (bt/decode s :base64 o))))

      (defn sha256-bytes [data & [o]]
        (when data
          (bt/hash data :sha256 o)))

      (defn sha256 [s & [o]]
        (bs/to-string (sha256-bytes s o)))

      (defn sha512-bytes [data & [o]]
        (when data
          (bt/hash data :sha512 o)))

      (defn sha512 [s & [o]]
        (bs/to-string (sha512-bytes s o)))

      (defn hex-str->byte-array [s]
        (.toByteArray (BigInteger. s 16)))))

#? (:cljs
    (do
      (defn string->bytes [s]
        (crypt/stringToUtf8ByteArray s))

      (defn bytes->hex
        "convert bytes to hex"
        [bytes-in]
        (crypt/byteArrayToHex bytes-in))

      (defn digest [hasher bytes]
        (.update hasher bytes)
        (.digest hasher))

      (defn hash-bytes [s hash-type]
        (digest
          (case hash-type
            :md5 (goog.crypt.Md5.)
            :sha1 (goog.crypt.Sha1.)
            :sha2 (goog.crypt.Sha2.)
            :sha256 (goog.crypt.Sha256.)
            :sha384 (goog.crypt.Sha384.)
            :sha512 (goog.crypt.Sha512.)
            (throw (js/Error. (str "'" hash-type "' is not a valid hash algorithm."))))
          (string->bytes s)))

      (defn to-base64-str [s & [o]]
        (when s
          (if (string? s)
            (b64/encodeString s)
            (b64/encodeByteArray s))))

      (defn from-base64-str [s & [o]]
        (when (and s (string? s))
          (b64/decodeString s)))

      (defn sha256-bytes [data & [o]]
        (when data
          (hash-bytes data :sha256)))

      (defn sha256 [s & [o]]
        (bytes->hex (sha256-bytes s o)))

      (defn sha512-bytes [data & [o]]
        (when data
          (hash-bytes data :sha512)))

      (defn sha512 [s & [o]]
        (bytes->hex (sha512-bytes s 0)))

      (defn md5-bytes [data & [o]]
        (when data
          (hash-bytes data :md5)))

      (defn md5 [s & [o]]
        (bytes->hex (md5-bytes s o)))))

(defn pluralize [n singular plural]
  (if (= 1 n)
    singular
    plural))

#? (:cljs
    (do
      (defn set-timeout [f delta]
        (js/setTimeout f delta))

      (defn interval [f delta]
        (js/setInterval f delta))

      (defn clear-timeout [timeout]
        (js/clearTimeout timeout))))

(defn format-phone [phone]
  (when phone
    (let [phone (str/replace phone #"[^\d]" "")
          pc (count phone)]
      (str
        (when (> pc 3)
          "(")
        (->> phone (take 3) (apply str))
        (when (> (count phone) 3)
          (str
            ") "
            (->> phone (drop 3) (take 3) (apply str))))
        (when (> (count phone) 6)
          (str
            "-"
            (->> phone (drop 6) (apply str))))))))

(defn initials [s]
  (when s
    (let [ps (-> s
                 str/trim
                 (str/split #"\s+"))]
      (str/upper-case
        (str
          (first (first ps))
          (when (> (count ps) 1)
            (first (last ps))))))))

(defn lookup-map [key coll]
  (->> coll
       (map (fn [o]
              [(get o key)
               o]))
       (into {})))

(defn url-slug [s]
  (when s
    (-> s
        str/trim
        str/lower-case
        (str/replace #"\s+" "-")
        (str/replace #"[^a-zA-Z0-9_-]+" "-")
        (str/replace #"-+" "-"))))

(defn pad [s n]
  (sformat (str "%0" n "d") s))


(defn cos [theta]
  #?(:clj (Math/cos theta)
     :cljs (.cos js/Math theta)))

(defn sin [theta]
  #?(:clj (Math/sin theta)
     :cljs (.sin js/Math theta)))

(defn sqrt [n]
  #?(:clj (Math/sqrt n)
     :cljs (.sqrt js/Math n)))

(defn atan [n]
  #?(:clj (Math/atan n)
     :cljs (.atan js/Math n)))

(defn sq [n] (* n n))

(def PI
  #?(:clj Math/PI
     :cljs (.-PI js/Math)))

#?(:clj
   (defmacro slurp-cljs [path]
     (try
       (let [path# (if (str/starts-with? path "~")
                     (str/replace-first
                       path
                       "~"
                       (System/getProperty "user.home"))
                     path)
             fc# (slurp path#)]
         `~fc#)
       (catch Exception e
         (throw e)))))

(defn chan? [x]
  (instance?
    #?(:cljs cljs.core.async.impl.channels.ManyToManyChannel
       :clj clojure.core.async.impl.channels.ManyToManyChannel)
    x))

(defn clamp [x low high]
  (if (and x low high)
    (cond
      (> x high) high
      (< x low) low
      :else x)
    x))

(defn spec-check-throw [spec x & [message data]]
  (when (not (s/valid? spec x))
    (let [default-message "Spec check failed"]
      (throw
        (ex-info
          (or message
              default-message)
          (merge
            data
            {:rx.res/category :rx.res/incorrect}
            (-> (s/explain-data spec x)
                #_(update-in
                    [:cljs.spec.alpha/spec]
                    (fn [o]
                      (if (or (keyword? o)
                              (string? o)
                              (nil? o))
                        o
                        (str o)))))))))))

(defn edn-read-string [& args]
  #?(:clj (apply edn/read-string args)
     :cljs (apply reader/read-string args)))

(defn looks-like-ts? [o]
  (and (nat-int? o)
       (> o 1000000)))

(defn readable-tss [m]
  (let [ts-keys (->> m
                     (filter
                       #(-> % second looks-like-ts?))
                     (map first))]
    (merge
      m
      (->> ts-keys
           (mapcat (fn [k]
                     [[(keyword
                         (namespace k)
                         (str (name k) "-iso8601"))
                       (to-iso8601 (get m k))]
                      [(keyword
                         (namespace k)
                         (str (name k) "-local"))
                       (date-format (get m k) "MMM d, yyyy h:mm:ss a Z")]]))
           (into {})))))

(defn promise->ch
  [p & [format-result format-error]]
  (let [ch (chan)]
    (.catch
      (.then p
        (fn [& args]
          (let [res (apply
                      (or format-result identity)
                      args)]
            (when res
              (put! ch res))
            (close! ch))))
      (fn [& args]
        (let [res (apply
                    (or format-error identity)
                    args)]
          (when res
            (put! ch res))
          (close! ch))))
    ch))

(defn promise->ch2
  [p & [format-result format-error]]
  (let [ch (chan)]
    (.catch
      (.then p
        (fn [& args]
          (let [res (apply
                      (or format-result identity)
                      args)]
            (when res
              (put! ch {:result res}))
            (close! ch))))
      (fn [& args]
        (let [res (apply
                    (or format-error identity)
                    args)]
          (when res
            (put! ch {:error res}))
          (close! ch))))
    ch))

(defn <promise [p & [{:keys [format-err]}]]
  (let [ch (chan)]
    (.catch
      (.then p
        (fn [o]
          (when o
            (put! ch o))
          (close! ch)))
      (fn [err]
        (when err
          (put! ch (merge
                     (err/from err)
                     (when format-err
                       (try
                         (format-err err)
                         (catch #?(:clj Exception :cljs js/Error) e
                           {:format-err "Error calling format-err in <promise"})))))
          #_(if (anom/error? err)
              (put! ch (anom/from-err err))
              (put! ch (anom/anom {:desc "Caught in promise"
                                   :err err}))))
        (close! ch)))
    ch))

(def <prom <promise)

(defn <promise->clj [p & [to-clj]]
  (go
    ((or to-clj #?(:cljs js->clj :clj identity))
      (<!
        (<promise p)))))

(defn base64-str-binary-length [s]
  (let [N (count s)
        k (->> s
               reverse
               (take-while #(= % "="))
               count)
        M (* 3 (floor (/ (- N k) 4)))]
    [N k M]))


;; --- https://github.com/alexanderkiel/async-error/blob/master/src/async_error/core.cljc

#?(:clj
   (defn cljs-env?
     "Take the &env from a macro, and tell whether we are expanding into cljs."
     [env]
     (boolean (:ns env))))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for Clojure code.
      https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
     [then else]
     (if (cljs-env? &env) then else)))

(defn throw-err [e]
  (when (instance? #?(:clj Throwable :cljs js/Error) e) (throw e))
  e)

(defn throw-anom [anom-bare]
  (let [anom (anom/namespace-opts anom-bare)]
    (throw
      (ex-info
        (::anom/desc anom)
        anom))))

#?(:clj
   (defmacro <?
     "Like <! but throws errors."
     [ch]
     `(if-cljs
        (throw-err (cljs.core.async/<! ~ch))
        (throw-err (clojure.core.async/<! ~ch)))))

#?(:clj
   (defn <??
     "Like <!! but throws errors."
     [ch]
     (throw-err (clojure.core.async/<!! ch))))

#?(:clj
   (defmacro go-try
     "Like go but catches the first thrown error and returns it."
     [& body]
     `(if-cljs
        (cljs.core.async/go
          (try
            ~@body
            (catch js/Error e# e#)))
        (clojure.core.async/go
          (try
            ~@body
            (catch Throwable t# t#))))))

#?(:clj
   (defn error? [o]
     (instance? Exception o))
   :cljs
   (defn error? [o]
     (instance? js/Error o)))

(defn deep-merge
  [a b]
  (if (map? a)
    (into a (for [[k v] b] [k (deep-merge (a k) v)]))
    b))

(defn ensure-opts [[opts & body :as args]]
  (let [body (if (map? opts)
               body
               (concat [opts] body))
        opts (if (map? opts)
               opts
               nil)]
    (vec
      (concat
        [opts]
        body))))

#?(:cljs
   (defn chan-closed? [o]
     (cljs.core.async.impl.protocols/closed? o)))

(defn async-realize [chs-list]
  (go
    (loop [chs-list chs-list
           out []]
      (if (empty? chs-list)
        out
        (recur
          (rest chs-list)
          (conj out (<! (first chs-list))))))))

(defn throttle*
  ([in msecs]
   (throttle* in msecs (chan)))
  ([in msecs out]
   (throttle* in msecs out (chan)))
  ([in msecs out control]
   (gol
     (loop [state ::init last nil cs [in control]]
       (let [[_ _ sync] cs]
         (let [[v sc] (alts! cs)]
           (condp = sc
             in (condp = state
                  ::init (do (>! out v)
                             (>! out [::throttle v])
                             (recur ::throttling last
                               (conj cs (timeout msecs))))
                  ::throttling (do (>! out v)
                                   (recur state v cs)))
             sync (if last 
                    (do (>! out [::throttle last])
                        (recur state nil
                          (conj (pop cs) (timeout msecs))))
                    (recur ::init last (pop cs)))
             control (recur ::init nil
                       (if (= (count cs) 3)
                         (pop cs)
                         cs)))))))
   out))

(defn map-async [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
              (recur))
            (close! out))))
    out))

(defn filter-async [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (pred x) (>! out x))
              (recur))
            (close! out))))
    out))

(defn throttle
  ([in msecs] (throttle in msecs (chan)))
  ([in msecs out]
   (->> (throttle* in msecs out)
        (filter-async #(and (vector? %) (= (first %) ::throttle)))
        (map-async second))))

(defn debounce [in ms]
  (let [out (chan)]
    (go
      (loop [last-val nil]
        (let [val   (if (nil? last-val) (<! in) last-val)
              timer (timeout ms)
              [new-val ch] (alts! [in timer])]
          (condp = ch
            timer (do (when-not
                          (>! out val)
                          (close! in))
                      (recur nil))
            in (if new-val (recur new-val))))))
    out))

(defn remove-nil-values [m]
  (when m
    (->> m
         (map (fn [[k v]]
                (if-not (nil? v)
                  [k v]
                  nil)))
         (remove nil?)
         (into {}))))

(defn today? [ms]
  (= (date-format ms "YYYYmmdd")
     (date-format (now) "YYYYmmdd")))

#?(:clj
   (defmacro <&
     [maybe-ch]
     `(let [res# ~maybe-ch]
        (if (rx.kitchen-sink/chan? res#)
          (if-cljs
              (throw-err (cljs.core.async/<! res#))
              (throw-err (clojure.core.async/<! res#)))
          res#))))

(defn reagent-structure? [o]
  (and (vector? o)
       (or (fn? (first o))
           (keyword? (first o)))))

(defn unwrap-children [children]
  (remove nil?
    (cond
      (and (sequential? children)
           (sequential? (first children))
           (reagent-structure? (ffirst children)))
      (concat
        (first children)
        (rest children))
      :else children)))


#?(:cljs
   (defn clog [& args]
     (apply js/console.log args)))

(comment

  (base64-str-binary-length
    "NGJmMDkxYWQwMTE5YTkzMjgyMDY7VQAAA1klA00CNjJkNWFjYjI3MWZjNDc5YzlhYjFhZmU3ODgzODdlY2YIbQAAAeclA00CNDcxYjdhYjA3OTBjNDdlYWE4Yzk2ZWU5M2JmMDQ2MzY0wQAABi0lA00CYTNiYmZmNjc4NzU1NGE1Zjk1MTgzN2M0OTM4MDEzYWYW3QAABo0lA00CZTVhYjkxZWU0MGI2NGJjMWE3ZmNmMGExZmJlOGNlZmMBtQAABd0lA00COGEzZDQxZjc5ZmU1NGI3MWFjODZlNDUwOGZhOWU2ODIVdwAAAQglA00CM2IwYTBkN2VkNjRlNDE3NmJkMmFiM2E1ODc2YzBjYTVAPwAAAW4lA00CNjVhMzdkYTMzYmRjNDQ0NThjZDZmMGJmZmVjODJiZTUi5AAAATslA00CNTAwYjVjNDc0MTRmNDUxNzgyMDY0ZmE3ZjIxYmVkOTcTwQAAAGAlA00CMTEzMzM0MDZhMmZkNDZkZGFhMWY4YTA0MzEwMzQ1YWIWGwAABBUlA00CNDBiZDFkNGVhOWQ5NDkyMThmODVjMjJjOGMzNTUwZTQ6cwAABislA00CZDFjOTQzYzBhODIxNGNmOGEzZTY0NjJiZGEwZDAzZjYh3gAAA80lA00CNmJkYTBhM2M4YWY2NGFhYTllNTU1OGQ0ZTg2ZTBiMDdBEQAAAe8lA00CYjJkZDBiMTgwYTFhNGMyYzk4NTI5YzdlZDIyZWUzZGQhXQAAABolA00CMDYzM2M5NDY5NGFiNGI1OGI0ZWJhOGYwNWJjZDJiZjcJJQAABY4lA00COTYyNDQxMmQxOTZiNDMyOGI5ODY4MzJjYjVlOGNjYzQjQgAAACgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPgJQNNAmVhODg2YTEyYjM5MDQxMTc5MDFiNWZkY2UzZTIwMzgwDn0AAAKUJQNNAjRhOWViODUzN2MwZDRlNTU5NWQ4MjBjNDhmN2RjNzMxFVcAAAAwJQNNAjA4YmY3MmM0Y2U1YTRhMDU4NzRhZmYwMGI0Nzg5Mzk4BOkAAALeJQNNAjUyZDk3NjZlMjhlODRmNjdhMTI1NWI5YzQ0N2Q5OTczGi0AAALmJQNNAmQ1OWQxNzYxMzQxNzRlOWFhMzQ4MmYxMmI3MGFiNGVjKG4AAAY6JQNNAjc1MmI4YWJmM2YzYjRmMDU4NjQxNWVjMWZjNjc2ZTA0HEcAAAM0JQNNAmE2OWM5MGVlYjlmZjRkMGFhYTEyNzk5ZTM1ZWY2NmQ0GhUAAAIBJQNNAjlmYzI5Zg==")

  )
