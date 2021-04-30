(ns rx.browser.ui-styleguide
  (:require [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.styleguide :as sg]))

(defn sections []
  [sg/sections
   [sg/section
    [sg/section-image
     {:url "https://www.oldbookillustrations.com/wp-content/high-res/1863/ludgate-hill-bridge-768.jpg"
      :style {:float 'right
              :width 250}}]
    [:h1 {:id "ui-components"} "Foundational UI"]
    [:p "Foundational UI components that can be used as building blocks to create more complex components."]]
   [sg/section
    [:h2 {:id "ui-group"} "Group"]
    [:p "Use to layout elements or components horizontally or vertically."]
    [:h3 "Examples"]
    [sg/checkerboard
     [ui/group
      {:gap 20}
      [ui/group
       [:h3 "Vertical, gap 20"]
       [ui/group
        {:gap 20}
        (->> (range 3)
             (map (fn [i]
                    [ui/text
                     {:style {:background-color "black"
                              :color 'white
                              :padding 5
                              :text-align 'center}}
                     i])))]]
      [ui/group
       [:h3 "Horizontal, gap 20"]
       [ui/group
        {:gap 20
         :horizontal? true}
        (->> (range 5)
             (map (fn [i]
                    [ui/text
                     {:style {:background-color "black"
                              :color 'white
                              :padding 5
                              :text-align 'center}}
                     i])))]]]
     ]
    [:h3 "API"]
    [:code "[ui/group opts? & children]"]
    [:h4 "Options"]
    [sg/options-list
     {:options [[:foo "bar"]]}]]
   (let [scales ["display" "headline" "title"
                 "header" "base" "subtext" "label"]]
     [sg/section
      [:h2 {:id "ui-text"} "Text"]
      [:h3 "Examples"]
      [sg/checkerboard
       [ui/group {:gap 20}
        (->> scales
             (map (fn [scale]
                    [ui/group
                     {:gap 4}
                     [ui/text scale]
                     [ui/text {:scale scale}
                      "The quick brown fox jumps over the lazy dog..."]])))]]])])


(comment

  (browser/<set-root!
    [sg/standalone {:component sections}])


  )
