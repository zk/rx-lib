(ns rx.browser.plotlyjs
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [reagent.core :as r]
            #_["react-plotly.js" :as plotly]))

(def plot-component nil #_(r/adapt-react-class (.-default plotly)))

(def plot plot-component)

(defn test-stuff []
  [plot-component
   {:data
    [{:z [(range 100)
          #_(reverse (range 100))]
      :type "surface"
      :mode "lines+markers"
      :marker {:color "red"}}]}])

(comment

  (prn (.-default plotly))

  (browser/<show-component!
    [test-stuff])

  )
