(ns rx.static-site-gen
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

(defn htmls->files [manifest htmls]
  (->> htmls
       (map (fn [{:keys [render path]}]
              {:render (fn [opts]
                         (render-html (render opts)))
               :path path}))))

(defn csss->files [manifest csss]
  (->> csss
       (map (fn [{:keys [render path]}]
              {:render (fn [opts]
                         (render-css (render opts) opts))
               :path path}))))


(defn export-manifest [manifest]
  (let [{:keys [:files :output-path :opts :copy-dirs :html :css]} manifest
        output-path (if (= \/ (first (reverse output-path)))
                      output-path
                      (str output-path "/"))

        files (concat
                files
                (htmls->files manifest html))

        files (concat
                files
                (csss->files manifest css))]

    (doseq [{:keys [:render :path]} files]
      (let [full-path (str output-path path)]
        (io/make-parents full-path)
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


;; + Ring handler to gen ring result for HTML

(defn ring-response->html-str
  [{:keys [body]}]
  (rc/render-static-markup body))

(defn hiccup->html5-page-str [hiccup]
  (rc/render-static-markup hiccup))

(defn write-to-disk [hiccup path]
  (spit path (hiccup->html5-page-str hiccup)))

(comment

  (hiccup->html5-page-str
    [:html
     [:head]
     [:body
      [:h1
       "hello world"]]])

  (export-manifest
    {::html [["index.html" (fn []
                             [:div "OK"])]]
     ::output-path "target/oc"})

  (export-manifest
    {::files [{:render 
               (fn [_]
                 (render-html
                   [:html
                    [:head
                     [:title "Foooooo"]
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



;; Local testing

;; Output

;; CSS, HTML, JS

;; DECL SETUP
