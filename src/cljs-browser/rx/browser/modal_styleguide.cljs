(ns rx.browser.modal-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.modal :as modal]
            [rx.browser.styleguide :as sg]
            [rx.browser.buttons :as btn]
            [clojure.core.async
             :as async
             :refer [<! go]]))


(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/n-d-1848/monstre-balloon-768.jpg"
             :height 300}}
    [:h1 {:id "modal"} "Modal"]
    [:p "Provides modal capabilities, built on " [:code "rx.view"] "."]]
   [sg/section
    [:h2 {:id "modal-usage"} "Usage"]
    [:p "Modal state is used via the API to interact with the modal system, for example opening modals."]
    [:p "Clicks outside of the modal content div will close the modal, as will hitting the Escape key."]
    [:p "An example:"]
    [sg/checkerboard
     {:style {:text-align 'center}}
     [btn/solid
      {:label "Open Modal"
       :on-click (fn []
                   (modal/<show!
                     [(fn []
                        {::modal/center-content? true
                         :render
                         (fn []
                           [:div
                            {:style {:width 200
                                     :height 80
                                     :background-color 'white
                                     :display 'flex
                                     :align-items 'center
                                     :justify-content 'center
                                     :border "solid black 1px"}}
                            "Modal Content"])})]))}]]

    [sg/code-block
     (str
       (ks/pp-str
         '[modal/component
           {:state modal-state}])
       "\n"
       (ks/pp-str
         '(modal/<show modal-state route)))]

    [:h3 {:id "modal-returning-data"} "Returning Data"]
    [:p "The " [:code "modal/<show"] " and " [:code "modal/<hide"] " functions work in conjuntion to allow you to 'return' data to the modal's " [:code "<show"] " call site via the channel returned by " [:code "<show"] "."]
    [:p "Say you've got a modal that asks for your age. Within the modal view code you can close the modal with " [:code "<hide"] " passing the value obtained from the user as the 2nd paramter. This value will put on the " [:code "<show"] "'s channel and that channel will be closed."]
    [:p "This allows you to write linear code and take advantage of modal UI as part of that process."]]

   [sg/section
    [:h2 {:id "modal-component-theme-rules"} "Theming"]
    [sg/theme-rules-list
     {:theme-rules modal/theme-rules}]]])

(defn test-stuff []
  (go
    (<! (modal/<hide!))
    (<! (modal/<show!
          [(fn []
             {::modal/fill-window? false
              :render
              (fn []
                [:div
                 {:style {:width 200
                          :height 80
                          :background-color 'white
                          :display 'flex
                          :flex 1
                          :align-items 'center
                          :justify-content 'center}}
                 "Modal Content"])})]))))

(comment

  (browser/<show-route!
    [sg/standalone {:component sections}])

  (modal/<hide!)


  (test-stuff)

  )






