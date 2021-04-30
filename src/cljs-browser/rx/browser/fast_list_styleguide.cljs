(ns rx.browser.fast-list-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as b]
            [rx.browser.fast-list2 :as fl]
            [rx.browser.styleguide :as sg]
            [rx.browser.ui :as ui]
            [rx.browser.workbench :as wb]))

(def data (->> (range 0 100000)
               (mapv (fn [i]
                       {:sort i}))))

(defn sections [_]
  [sg/section
   [:h1 {:id "fast-list"} "Fast List"]
   [:p "Fast list is a performant, generalized list component that scales to hundreds of thousands of elements."]
   [:p "Features"]
   [:ul
    [:li "Will performantly render items even when top level scroller is the document itself"]]
   [:p "Restrictions:"]
   [:ul
    [:li "Data must fit into memory"]
    [:li "Items must have a uniform height"]]
   [:p "Fast list partitions your items into chunks of " [:code "page-size"] ". Pages are mounted / unmounted as the user scrolls."]
   [sg/checkerboard
    [ui/group
     {:gap 8}
     [ui/text {:scale "title"} "100k items"]
     [fl/list
      {:style {:height 300}
       :data-key :sort
       :data data
       :render-item
       (fn [item opts]
         [:div
          {:style {:background-color 'white
                   :margin-top 5
                   :margin-bottom 5
                   :padding 5}}
          [:pre
           {:style {:padding 0
                    :margin 0}}
           (pr-str item)
           " "
           (pr-str opts)]])}]]]
   [:h2 {:id "fast-list-usage"} "Usage"]
   [:h3 "Options"]
   [sg/options-list
    {:options
     [[:page-size "Number of items to render as one 'chunk'"]
      [:scroll-event-throttle "Maximum scroll update period in ms"]]}]

   [ui/section
    {:title "Inline List Example"}
    [fl/list
     {:data-key :sort
      :data (->> (range 1000)
                 (map (fn [i]
                        {:sort i})))
      :separator {:size 1 :color "#eee"}
      :render-item
      (fn [item opts]
        [:div
         {:style {:background-color 'white
                  :margin-top 5
                  :margin-bottom 5
                  :padding 5}}
         [:pre
          {:style {:padding 0
                   :margin 0}}
          (pr-str item)
          " "
          (pr-str opts)]])}]]])


(defn test-110k []
  (wb/workbench-comp
    (fn [wb]
      [fl/list
       {:style {:height 300
                :width 200}
        :data-key :sort
        :data data
        :render-item
        (fn [item opts]
          [:div
           {:style {:background-color 'white
                    :margin-top 5
                    :margin-bottom 5
                    :padding 5}}
           [:pre
            {:style {:padding 0
                     :margin 0}}
            (pr-str item)
            " "
            (pr-str opts)]])}])))

(comment

  (b/<set-root!
    [sg/standalone {:component sections}])

  (test-110k)

  )
