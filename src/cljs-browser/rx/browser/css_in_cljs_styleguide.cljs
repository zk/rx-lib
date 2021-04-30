(ns rx.browser.css-in-cljs-styleguide
  (:require [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.browser.ui :as ui]
            [reagent.core :as r]))

(def headings
  [[::css-in-cljs :h1 "CSS in cljs"]
   [::example :h2 "Example"]
   [::vs-style-attribute
    :h2
    [:span "vs the " [:code "style"] " attribute"]]
   [::how-it-works :h2 "How it Works"]])

(defn example []
  (let [!colors (r/atom (cycle ["red" "green" "blue"]))]
    (r/create-class
      {:reagent-render
       (fn []
         [sg/section
          [sg/heading headings ::example]
          (sg/example
            {:form
             [ui/group
              {:style {:padding 10}
               :css [{:color 'black
                      :cursor 'pointer
                      :user-select 'none}
                     [:&:hover {:color 'white
                                :background-color (first @!colors)}]]
               :on-click (fn [] (swap! !colors rest))}
              "Click Me -- My hover state's background color is set dynamically based on the number of times you've clicked."]})])})))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/n-d/paris-fashion-768.jpg"
             :height 300
             :width 249.5}}
    [sg/heading headings ::css-in-cljs]
    [:p "CSS in cljs is a way to style components via the browser's style tags."]
    [:p
     "Typically styling is attached directly to an element using that element's "
     [:code "style"]
     " property. Unfortunately you miss out on a lot of the power of css being limited to setting css properties to a single element."]
    [:p "CSS in cljs allows you to use the full power of CSS in styling your dynamic components, including:"]
    [:ul
     {:style {:list-style-type 'disc
              :margin-left 20}}
     [:li "Use of pseudo classes and elements"]
     [:li "Styling child components"]
     [:li "Media and other at rules"]]
    [:p "Without CSS in cljs you'd have to resort to a series of js hacks to (poorly) emulate this functionality."]]
   [example]
   [sg/section
    [sg/heading headings ::vs-style-attribute]
    [:p "There are some differences in using the stylesheet vs an element's style attribute:"]
    [:ul
     {:style {:list-style-type 'disc
              :padding-left 20}}
     [:li "Numeric scalars must be suffixed with their units (px, rem, etc)"]]]
   [sg/section
    [sg/heading headings ::how-it-works]
    [:p "A style element is attached as a child of the document's head element. Components are given a unique id, set as the class of the component's root element. Rules specified in the component are attached to this style element scoped using the unique id class created for the component."]]])

;; differences -- sizes need to be suffixed by px

(comment

  (browser/<set-root!
    [sg/standalone
     {:headings headings
      :component sections}])

  )



