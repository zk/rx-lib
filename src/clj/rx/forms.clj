(ns rx.forms
  (:require [garden.stylesheet :as gs]
            [garden.core :as g]
            [garden.color :as gc]))

(defn px [n]
  (str n "px"))

(defn spinner [base-key
               {:keys [size
                       ring-size
                       ring-color
                       bar-color
                       base-pad]
                :or {size 20
                     ring-size 2
                     bar-color "red"
                     ring-color "#f0f0f0"
                     base-pad 5}}]
  (let [base-str (name base-key)]
    [[base-key
      (keyword (str base-str ":after"))
      {:border-radius "50%"
       :width (px size)
       :height (px size)
       :margin (px base-pad)
       :transition "opacity 0.8s ease"
       :opacity 0}]
     [base-key
      {;;:margin (str size "px auto")
       :font-size "10px"
       :position 'relative
       :text-indent "-9999em"
       :border-top (str (px ring-size) " solid " ring-color)
       :border-right (str (px ring-size) " solid " ring-color)
       :border-bottom (str (px ring-size) " solid " ring-color)
       :border-left (str (px ring-size) " solid " bar-color)
       :transform "translateZ(0)"
       :animation "load8 0.7s infinite linear"}
      [:&.active {:opacity 1}]]
     (gs/at-keyframes
       :load8
       ["0%" {:transform "rotate(0deg)"}]
       ["100%" {:transform "rotate(360deg)"}])]))

(defn css [& {:keys [base-fg-color
                     base-pad]
              :or {base-fg-color "black"
                   base-pad 5}}]
  [[:.rx-forms-form-row]
   [:.rx-forms-input-wrap
    {:flex 1
     :border (str "solid "
                  base-fg-color
                  " 1px")}]
   [:.rx-forms-text-input
    {:border 'none
     :outline 0}]
   [:.rx-forms-textarea
    {:border 'none
     :outline 0}]
   [:.rx-forms-select
    {:width "100%"}]
   [:.rx-forms-radio-input
    {:margin-right (px (* 1 base-pad))}]
   (spinner :.input-spinner
     {:ring-color "#f0f0f0"
      :bar-color "black"})])
