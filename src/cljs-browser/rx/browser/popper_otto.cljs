(ns rx.browser-popper-otto
  (:require [rx.browser.popper :as pop]
            [rx.browser.workbench :as wb]
            [rx.browser.ui :as ui]))

(defn test-positions []
  (wb/workbench-comp
    (fn []
      [:div
       {:style {:overflow-y 'scroll
                :height 200
                :padding 20}}
       [ui/g
        (->> [:bottom-left
              :bottom-center
              :bottom-right
              :top-left
              :top-center
              :top-right
              :right-top
              :right-center
              :right-bottom
              :left-top
              :left-center
              :left-bottom]
             (map (fn [position]
                    [:div {:style {:padding 20
                                   :padding-top 50
                                   :padding-bottom 50}}
                     [pop/wrap
                      {:position position
                       :body [:div (str position)]
                       :popper [:div
                                {:style {:pointer-events 'none}}
                                (->> (range 3)
                                          (map (fn [i]
                                                 [:div {:key i} "POPPER"])))]}]])))]])))

(defn test-mouse-all []
  (wb/workbench-comp
    (fn []
      [pop/wrap
       {:mouse-enabled? true
        :body [:div "Mouse Over"]
        :popper [:div
                 {:style {:pointer-events 'none}}
                 (->> (range 5)
                      (map (fn [i]
                             [:div {:key i} "POPPER"])))]}])))

(defn test-click []
  (wb/workbench-comp
    (fn []
      [pop/wrap
       {:position :right-top
        :render-body
        (fn [pop]
          [:div
           {:on-click
            (fn []
              (pop/toggle pop))}
           "Click"])
        :popper [ui/g
                 {:hpad 20
                  :style {:pointer-events 'none}}
                 (->> (range 5)
                      (map (fn [i]
                             [:div {:key i} "POPPER"])))]}])))

(defn test-nested []
  (wb/workbench-comp
    (fn []
      [pop/wrap
       {
        ;;:initial-visible? true
        :show-on-mouseover? true
        :hide-on-mouseout? true
        :mouseover-delay 0
        :mouseout-delay 500
        :body [:div "BASE"]
        :popper [:div
                 {:style {:pointer-events 'none}}
                 ]}])))

(defn test-esc []
  (wb/workbench-comp
    (fn []
      [:div
       [pop/wrap
        {:hide-on-esc? true
         :hide-on-mouseout? true
         :mouseout-delay 500
         :render-body (fn [pop]
                        [:div
                         {:on-click (fn []
                                      (pop/show pop))}
                         "BASE"])
         :popper [:div
                  {:style {:pointer-events 'none}}
                  "POPPER"]}]
       #_[pop/wrap
          {:hide-on-esc? true
           :hide-on-mouseout? true
           :mouseout-delay 500
           :render-body (fn [pop]
                          [:div
                           {:on-click (fn []
                                        (pop/<show pop))}
                           "BASE"])
           :popper [:div
                    {:style {:pointer-events 'none}}
                    "POPPER"]}]])))

(defn test-track-open []
  (wb/workbench-comp
    (fn []
      [:div
       {:style {:overflow-y 'scroll
                :height 200
                :padding 20}}
       [ui/g
        (->> [:bottom-left
              :bottom-center
              :bottom-right
              :top-left
              :top-center
              :top-right
              :right-top
              :right-center
              :right-bottom
              :left-top
              :left-center
              :left-bottom]
             (map-indexed
               (fn [i position]
                 [:div {:style {:padding 20
                                :padding-top 50
                                :padding-bottom 50}}
                  [pop/wrap
                   {:click-enabled? true
                    :position position
                    :body [:div (str position)]
                    :popper [:div
                             {:style {:pointer-events 'none}}
                             (->> (range 3)
                                  (map (fn [i]
                                         [:div {:key i} "POPPER"])))]}]])))]])))

(comment

  (test-esc)
  
  (test-mouse-all)

  (test-appear)

  (test-click)

  (test-track-open)

  )
