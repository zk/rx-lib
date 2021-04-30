(ns rx.browser.theme-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.theme :as th]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.browser.forms :as forms]
            [rx.browser.components :as cmp]
            [rx.browser.buttons :as btn]
            [rx.theme :as th]
            [clojure.string :as str]))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/uploads/2015/11/capital-choir-220x220.jpg"
             :width 300}}
    [:h1 {:id "theming"} "Theming"]
     [:p "The rx theming system allows you to flexably manage the visual presentation of components in a centralized but extensible way."]
     [:p "Where CSS styles use a common set of names to specify visual presentation of elements, theme uses component-specific names to specify visual presentation of that component."]
     
     [:p "Theme properties can be defined at the top level and overridden at call time."]
     [:p "Similar to css, except you're defining component-specific properties, like "
      [:code "label-fg-color"]
      ", and "
      [:code "over-border-color"]
      "."]
     [:p "Why? CSS and Style maps target the top-level element, where you need to specify component-specific visuals without knowing the internal element heirarchy of a component."]
     [:p "A way to have a semantic visual theming centralized, but overridable when composing components."]
     [:h3 "Namespace"]
     [:code "rx.theme"]]
   [sg/section
    {:gap 16}
    [:h2 {:id "theming-global-vs-local-theme-data"}
     "Global vs Local Theme Data"]
    [:p "Rx Theme provides a way to set the theme map for your entire application, which is provided to all components that participate in the theming system. These global values are overrideable at the component level."]]

   #_[sg/section
      [:h2 {:id "theming-specifying-properties"}
       "Specifying Theme Properties"]]

   [sg/section
    [:h2 {:id "theming-examples"} "Examples"]
    (into
      [cmp/group]
      (->> [{:sym 'btn/solid
             :comp btn/solid
             :opts {:label "Hello"}}
            {:sym 'btn/solid
             :comp btn/solid
             :opts {:label "Hello"
                    :theme {:bg-color "red"}}}
            {:sym 'btn/hollow
             :comp btn/hollow
             :opts {:label "Hello"
                    :theme {:bg-color "white"
                            :fg-color "red"
                            :border-radius 99}}}]
           (map (fn [{:keys [opts sym comp]}]
                  [cmp/group
                   {:gap 4}
                   [cmp/code
                    {:form [sym opts]}]
                   [sg/checkerboard
                    [comp opts]]]))))]])



(comment

  (browser/<set-root!
    [sg/standalone {:component sections}])

  )
