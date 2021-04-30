(ns rx.css-gen
  (:require [clojure.string :as str]
            [garden.units :as u]
            [garden.color :as co]
            [garden.stylesheet :refer [at-media]]
            [garden.core :as garden]
            [garden.stylesheet :as gs]))

(defn compile-css [opts forms]
  (garden/css
    (merge
      {:output-to nil
       :pretty-print? true
       :vendors ["webkit" "moz" "ms"]
       :preamble ["resources/rx-css-reset/reset.css"
                  "resources/public/css/bootstrap-grid.min.css"]
       :auto-prefix #{:justify-content
                      :align-items
                      :flex-direction
                      :flex-wrap
                      :align-self
                      :transition
                      :transform
                      :background-clip
                      :background-origin
                      :background-size
                      :filter
                      :font-feature-settings
                      :appearance}}
      opts)
    forms))

(defn spit-compile-css [path opts forms]
  (spit
    path
    (compile-css opts forms)))

(comment

  (prn "HI")

  )
