(ns rx.browser.pdfjs-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.css :as css]
            [rx.browser.styleguide :as sg
             :refer-macros [example]]
            [rx.browser.forms :as forms]
            [rx.browser.components :as cmp]
            [rx.browser.buttons :as btn]
            [rx.browser.pdfjs :as pdfjs]
            [rx.browser.ui :as ui]
            [rx.validators :as vld]
            [rx.theme :as th]
            [clojure.string :as str]
            [reagent.core :as r]
            [clojure.core.async :as async
             :refer [go <! timeout]]))

(def headings
  [[::pdfjs :h1 "PDF"]
   [::example :h2 "Example"]])

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1875/chapel-avioth-768.jpg"
             :width 300}}
    [sg/heading headings ::pdfjs]
    [:p "PDF viewer based on " [:a {:href "https://mozilla.github.io/pdf.js/"} "PDF.js"]]]
   [sg/section
    [sg/heading headings ::example]
    [:p "Example"]
    [sg/async-mount
     (fn []
       (go
         (let [pdf (<! (pdfjs/<pdf-obj "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf"))]
           [:div {:style {:width "100%"
                          :height 700
                          :border "solid #ccc 1px"}}
            [pdfjs/document
             {:pdf pdf}]])))]]
   [sg/section
    [sg/options-list
     {:options [[:zoom-enabled? "Enable zoom on mouse wheel"]]}]]])

(comment

  (browser/<set-root!
    [sg/standalone
     {:component sections
      :headings headings}])
 
  (browser/<set-root!
    [sg/standalone
     {:component range-section}])

  (browser/<set-root!
    [sg/standalone
     {:component search-bar-section}])

  (browser/<set-root!
    [sg/standalone
     {:component basic-form}])

  (browser/<set-root!
    [sg/standalone
     {:component login-form}])

  (browser/<set-root!
    [sg/standalone
     {:component signup-form}])

  )

;; Layout and nesting, you're required to use the 

;; Validations are just functions, but there are a few helpers to make composition easier.
