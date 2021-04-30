(ns rx.browser.theme-otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.theme :as th]
            [clojure.string :as str]))

(defn debug-theme [{:keys [theme]}]
  [ui/group
   {:gap 40}
   [ui/group
    {:horizontal? true
     :gap 20}
    (->> theme
         (filter #(= "color"
                     (namespace (first %))))
         (group-by (fn [[k _]]
                     (first (str/split (name k) "-"))))
         (sort-by first)
         (map (fn [[title k+vs]]
                [ui/group
                 {:gap 8}
                 (concat
                   [[:div title]]
                   (->> k+vs
                        (sort-by #(str (first %)))
                        (map (fn [[k v]]
                               [ui/group
                                {:horizontal? true
                                 :align-items 'center
                                 :gap 8}
                                [:div
                                 {:style {:background-color v
                                          :width 100
                                          :height 30}}]
                                [:div
                                 {:style {:width 150}}
                                 k " "
                                 [:span
                                  {:style {:user-select 'all}}
                                  v]]]))))])))]
   (let [{:keys [bg-color fg-color
                 warn-fg-color
                 warn-bg-color]}
         (th/resolve {:theme theme}
           [[:bg-color :color/bg-0]
            [:fg-color :color/fg-0]
            [:warn-bg-color :color/warn-0]
            [:warn-fg-color :color/bg-0]])]
     
     [ui/group
      {:style {:background-color bg-color
               :color fg-color}
       :pad 20
       :gap 8}
      "The quick brown fox jumps over the lazy dog"
      [:div
       {:style {:color warn-fg-color
                :background-color warn-bg-color
                :padding 8}}
       "Warning!"]])])

(defn debug-themes [{:keys [themes]}]
  [ui/group {:gap 50}
   (->> themes
        (map (fn [{:keys [title theme]}]
               [:div
                [:h2 title]
                [debug-theme {:theme theme}]])))])

(defn test-colors []
  (browser/<show-component!
    [:div
     {:style {:padding 100}}
     [debug-themes
      {:themes
       [{:title "Light"
         :theme (th/gen-colors
                  {:fg (->> th/tailwind-colors
                            :grey
                            (sort-by first)
                            reverse
                            (mapv second))
                   :bg (->> th/tailwind-colors
                            :grey
                            (sort-by first)
                            #_reverse
                            (mapv second))
                   :pri (->> th/tailwind-colors
                             :blue
                             (sort-by first)
                             reverse
                             (mapv second))
                   :warn (->> th/tailwind-colors
                              :red
                              (sort-by first)
                              reverse
                              (mapv second))})}
        {:title "Dark"
         :theme (th/gen-colors
                  {:fg (->> th/tailwind-colors
                            :grey
                            (sort-by first)
                            #_reverse
                            (mapv second))
                   :bg (->> th/tailwind-colors
                            :grey
                            (sort-by first)
                            reverse
                            (mapv second))
                   :pri (->> th/tailwind-colors
                             :blue
                             (sort-by first)
                             #_reverse
                             (mapv second))
                   :warn (->> th/tailwind-colors
                              :red
                              (sort-by first)
                              #_reverse
                              (mapv second))})}]}]]))


(comment

  (test-colors)

  )
