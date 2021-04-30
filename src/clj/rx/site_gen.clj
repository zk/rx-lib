(ns rx.site-gen
  (:require [rx.kitchen-sink :as ks]
            [rum.core :as rc]
            [rx.css-gen :as cg]
            [rx.theme :as th]
            [clojure.java.io :as io]))

(defn render-page [hiccup]
  (str
    "<!DOCTYPE html>"
    (rc/render-static-markup hiccup)))

(defn render-html [html-form & [opts]]
  (render-page html-form))

(defn render-css [css-form & [opts]]
  (cg/compile-css opts css-form))

(defn export-manifest [manifest]
  (let [{:keys [::files ::output-path ::opts ::copy-dirs]} manifest
        output-path (if (= \/ (first (reverse output-path)))
                      output-path
                      (str output-path "/"))]
    (doseq [{:keys [:render :path]} files]
      (let [full-path (str output-path path)]
        (spit full-path (render opts))))

    (doseq [[from-path to-path] copy-dirs]
      (let [files (->> from-path
                       io/file
                       file-seq
                       (remove #(.isDirectory %)))]
        (io/make-parents to-path)
        (doseq [from-file files]
          (io/copy
            from-file
            (io/file (str to-path "/" (.getName from-file)))))))))


(comment

  (export-manifest
    {::files [{:render 
               (fn [_]
                 (render-html
                   [:html
                    [:head
                     [:title "FOooooo"]
                     [:link {:rel "stylesheet" :href "css/app.css"}]]
                    [:body
                     [:h1 "hello world the quick brown fox jumps over the lazy dog..."]]]))
               :path "index.html"}
              {:render
               (fn [_]
                 (render-css
                   [[:body {:background-color "white"}]]))
               :path "css/app.css"}]
     ::output-path "target/html-gen"})

  )
