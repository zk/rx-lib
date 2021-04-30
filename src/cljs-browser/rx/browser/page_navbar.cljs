(ns rx.browser.page-navbar
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [reagent.core :as r]))

(defn navbar [{:keys [render]}]
  (let [!opts (atom nil)
        !render-opts (r/atom nil)
        on-scroll (fn []
                    (let [{:keys [top-bound]
                           :or {top-bound 200}} @!opts]
                      (let [pct (ks/clamp
                                  (/
                                    (.-scrollY js/window)
                                    top-bound)
                                  0 1)]
                        (swap! !render-opts assoc :pct pct))))]
    (on-scroll)
    (r/create-class
      {:component-did-mount
       (fn []
         (.addEventListener js/window "scroll" on-scroll))
       :component-will-unmount
       (fn []
         (.removeEventListener js/window "scroll" on-scroll))
       :reagent-render
       (fn [opts]
         (reset! !opts opts)
         [:div
          {:style {:position 'fixed
                   :top 0
                   :left 0
                   :right 0}}
          [render @!render-opts]])})))

(defn test-navbar []
  (let [colors ["red" "blue" "green"]]
    (browser/<show-component!
      (into
        [:div
         {:style {:width "100%"}}
         [navbar
          {:render (fn [{:keys [pct]}]
                     [:div
                      {:style {:background-color 'white
                               :height (- 100 (* 30 pct))}}
                      [ui/g
                       {:horizontal? true
                        :style {:align-items 'center
                                :height "100%"
                                :width "100%"}}
                       [:div
                        "HELLO WORLD "
                        pct]]])}]]
        (->> (range 20)
             (map (fn [i]
                    [:div {:style
                           {:height 200
                            :width "100%"
                            :background-color (nth (cycle colors) i)}}])))))))



(comment

  (test-navbar)

  )
