(ns rx.jvm.ops.build
  (:require [rx.kitchen-sink :as ks]
            [cljs.build.api :as bapi]
            [rx.anom :as anom]
            [clojure.java.shell :as sh]
            [me.raynes.conch.low-level :as ch])
  (:import [java.nio.file Path Paths Files]
           [java.nio.file.attribute FileAttribute]))

(defn proc-blocking [command]
  (let [p (apply ch/proc command)]
    (future (ch/stream-to-out p :out))
    (future (ch/stream-to-out p :err))
    (let [exit-code (ch/exit-code p)]
      (if (= 0 exit-code)
        [{:exit-code exit-code}]
        (anom/throw-anom
          {:desc "Command failed"
           ::command command
           ::exit-code exit-code})))))

(defn proc-blocking-silent-stdout [command]
  (let [p (apply ch/proc command)]
    (future (ch/stream-to-string p :out))
    (future (ch/stream-to-string p :err))
    (let [exit-code (ch/exit-code p)]
      (if (= 0 exit-code)
        [{:exit-code exit-code}]
        [nil {:exit-code exit-code}]))))

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

(defn nio-path [s]
  (Paths/get
      s
      (into-array [""])))

(defn create-symlink [from-path to-path]
  (try
    (Files/createSymbolicLink
      (.toAbsolutePath (nio-path to-path))
      (.toAbsolutePath
        (nio-path from-path))
      (into-array FileAttribute []))
    true
    (catch Exception e
      nil)))

(defn compile-cljs [{:keys [source-paths compiler]}]
  (bapi/build
    (apply bapi/inputs source-paths)
    compiler))

(def browser-foreign-libs
  [{:file "resources/dexie/dexie.min.js"
    :file-min "resources/dexie/dexie.min.js"
    :provides ["dexie"]}
   {:file "resources/tonejs/Tone.js"
    :file-min "resources/tonejs/Tone.js"
    :provides ["tonejs"]}
   {:file "resources/pinchzoomjs/pinch-zoom.min.js"
    :file-min "resources/pinchzoomjs/pinch-zoom.min.js"
    :provides ["pinch-zoom"]}])

(def common-foreign-libs [])

(defn report-build-error [e]
  (println "Build failed:")
  (let [a (anom/from-err e)]
    (println "-- desc")
    (println (::anom/desc a))
    (let [m (dissoc a ::anom/desc ::anom/stack ::anom/code)]
      (when-not (empty? m)
        (println "-- adtl")
        (ks/pp m)))
    (println "-- stack")
    (ks/pp
      (->> a
           ::anom/stack
           (remove
             #(re-find #"^(clj|clojure|nrepl|java)" %))
           vec))))

(defn clean-output-path [{:keys [::output-path]}]
  (cond
    (not output-path)
    (anom/throw-anom
      {:desc "Can't clean output path, output-path missing"
       ::output-path output-path})
    
    (.startsWith output-path "/")
    (anom/throw-anom
      {:desc "Can't clean output path, must be a relative path"
       ::output-path output-path})

    (not (.startsWith output-path "target"))
    (anom/throw-anom
      {:desc "Can't clean output path, must start with 'target' as safety check"
       ::output-path output-path})

    :else
    (report-and-run ["rm" "-rf" output-path])))

(defn cwd []
  (.getCanonicalPath (java.io.File. ".")))

(defn mkdir [path]
  (report-and-run ["mkdir" "-p" path]))

(defn zip [path file-name]
  (let [[res err] (proc-blocking-silent-stdout
                    ["zip"
                     "-vr"
                     file-name
                     "."
                     :dir
                     path])]
    (when err
      (println " ! Error zipping:" err))
    [res err]))

