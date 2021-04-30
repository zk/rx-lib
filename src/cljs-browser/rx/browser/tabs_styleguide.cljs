(ns rx.browser.tabs-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.tabs :as tabs]
            [rx.browser.styleguide :as sg]
            [rx.browser.buttons :as btn]
            [rx.browser.ui :as ui]
            [reagent.core :as r]))

(defn sections [opts]
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1875/christofle-factory-768.jpg"
             :height 300}}
    [:h1 {:id "tabs"} "Tabs"] 
    [:p "Rx provides a fully-featured tabs system that supports cases from a simple tabbed information component up to something you'd see in a web browser."]
    [:p "Tabs can laid out horizontally or vertically, can be closeable, and can have custom title renderers."]
    [:p "Tabs are built on the " [:code "rx.view"] " system. Each tab is represented by a view, and tabs are configured via the realized view map (in additon to opts passed to " [:code "rx.browser.tabs/view"] "."]
    [:p "Here's a basic example:"]
    (sg/example
      {:form [tabs/view
              {:style {:height 200}
               :initial-routes
               [[(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "Hello from tab 1."])
                    ::tabs/title "Tab 1"})]
                [(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "Hello from tab 2."])
                    ::tabs/title "Tab 2"})]
                [(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "You can close this tab"])
                    ::tabs/title "Closeable Tab"
                    ::tabs/closeable? true})]]}]})
    
    [:p "And with " [:code "{:vertical? true}"] "."]
    (sg/example
      {:form [tabs/view
              {:style {:height 200}
               :vertical? true
               :tab-width 120
               :initial-routes
               [[(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "Hello from tab 1."])
                    ::tabs/title "Tab 1"})]
                [(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "Hello from tab 2."])
                    ::tabs/title "Tab 2"})]
                [(fn []
                   {:render
                    (fn []
                      [ui/group {:pad 8} "You can close this tab"])
                    ::tabs/title "Closeable Tab"
                    ::tabs/closeable? true})]]}]})]

   [sg/section
    [:h2 {:id "tabs-opening-new"} "Opening New Tabs"]
    [:p "New tabs are opened imperitively via "
     [:code "rx.tabs/open"]]
    [sg/code-block
     "tabs-state (tabs/create-state)"
     "\n\n"
     (ks/pp-str
       '[tabs/view
         {:state tabs-state
          :style {:height 200}
          :vertical? true
          :tab-width 200}])
     "\n"
     (ks/pp-str
       '(tabs/open tabs-state route))]
    (let [state (tabs/create-state)]
      (sg/example
        {:form
         [ui/group
          {:gap 8}
          [tabs/view
           {:state state
            :style {:height 200}
            :vertical? true
            :tab-width 120}]
          [btn/bare
           {:label "Open New Tab"
            :icon {:set "feather"
                   :name "plus"
                   :stroke-width 3}
            :on-click (fn []
                        (tabs/open state
                          [(fn []
                             {:render
                              (fn []
                                [ui/group {:hpad 8} "You can close this tab"])
                              ::tabs/title (str "Tab " (ks/uuid))
                              ::tabs/closeable? true})]))}]]}))]

   [sg/section
    [:h2 {:id "tabs-component-options"} "Component Options"]
    [sg/options-list
     {:options
      [[:state "Tabs state map, used in API calls"]
       [:with-state "Function passed tabs state on initialization."]
       [:initial-routes [:span
                         [:code "rx.view"]
                         " to populate tabs component with."]]]}]]])


(comment

  (browser/<show-route!
    [sg/standalone
     {:component sections}])

  )
