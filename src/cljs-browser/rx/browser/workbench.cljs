(ns rx.browser.workbench
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.forms :as forms]
            [rx.browser.ui :as ui]
            [rx.browser.tabs :as tabs]
            [rx.browser.keybindings :as kb]
            [rx.browser.tonejs :as tone]
            [reagent.core :as r]))

(defn dots [& args]
  (let [[opts & children] (ks/ensure-opts args)
        size 4]
    (into
      [:div
       {:style (merge
                 {:padding 20
                  :background-image
                  "radial-gradient(#E4E0E4 1px, transparent 1px)"
                  :background-size (str size "px " size "px")
                  :background-position "0 0"}
                 (:style opts))}]
      children)))

(defn bench-state []
  {:!event-log (r/atom [])
   :!tracked-opts (r/atom nil)
   :!track-lc-count (r/atom 0)})

(defn bench-wrap-fn [id f]
  (fn [& args]
    (prn id args)
    (apply f args)))

(defn render-event [{:keys [payload]}]
  (let [{:keys [::key ::args ::filtered?]} payload]
    [ui/group
     {:gap 8}
     [ui/text {:scale "title"} (pr-str key)]
     [:pre (ks/pp-str args)]
     (when filtered?
       [:div "FILTERED"])]))

(defn workbench-comp [& [opts f]]
  (let [[opts f] (if f
                   [opts f]
                   [{} opts])

        {:keys [audio?]} opts]
    (when audio?
      (tone/trigger-ar! "C3" "32n"))
    (browser/<show-component!
      [(let [state (bench-state)
             !tabs-comp (atom nil)]
         (r/create-class
           {:component-did-mount
            (fn []
              (kb/add-bindings!
                ::bindings
                [{:key-name "o"
                  :meta? true
                  :handler
                  (fn [e]
                    (.preventDefault e)
                    (tabs/focus-tab-index
                      @!tabs-comp
                      1))}
                 {:key-name "e"
                  :meta? true
                  :handler
                  (fn [e]
                    (.preventDefault e)
                    (tabs/focus-tab-index
                      @!tabs-comp
                      0))}]))
            :component-will-unmount
            (fn []
              (kb/remove-bindings! ::bindings))
            :reagent-render
            (fn []
              [:div {:style {:position 'fixed
                             :top 0
                             :left 0
                             :right 0
                             :bottom 0
                             :display 'flex
                             :flex-direction 'row}}
               [dots
                {:style {:width "50%"
                         :display 'flex
                         :justify-content 'center
                         :align-items 'center}}
                [f state]]
               [:div {:style {:width "50%"
                              :padding 20
                              :display 'flex
                              :overflow 'hidden}}
                [tabs/view
                 {:on-comp #(reset! !tabs-comp %)
                  :style {:flex 1}
                  :initial-routes
                  [[(fn []
                      {:render
                       (fn []
                         [:div {:style {:height "100%"
                                        :width "100%"
                                        :overflow-y 'scroll}}
                          [ui/list-container
                           {:gap 20
                            :style {:flex 1}}
                           (->> @(:!event-log state)
                                (partition-by #(-> % ::payload))
                                (map (fn [group]
                                       (let [{:keys [::ts ::payload]} (last group)]
                                         [:div
                                          (condp = (::type payload)
                                            ::event
                                            [render-event {:payload payload}]
                                            [:pre payload])
                                          [ui/group
                                           {:horizontal? true
                                            :gap 8}
                                           [:div (count group)]
                                           [:div (ks/date-format
                                                   ts
                                                   "h:mm:ss aa")]]]))))]])
                       ::tabs/title "Events"})]
                   [(fn []
                      {:render
                       (fn []
                         [:div {:style {:overflow-y 'scroll
                                        :height "100%"
                                        :width "100%"}}
                          [ui/list-container
                           {:gap 20
                            :style {:flex 1}}
                           (->> @(:!tracked-opts state)
                                (map (fn [[k opts]]
                                       [ui/group
                                        [ui/text {:scale "title"} (pr-str k)]
                                        [:pre (ks/pp-str opts)]])))]])
                       ::tabs/title "Opts"})]]}]]])}))])))

(defn log-event [wb pl]
  (swap!
    (:!event-log wb)
    conj
    (merge
      {::ts (ks/now)
       ::payload pl})))

(defn track-event [wb k & [f]]
  {k (fn [& args]
       (log-event wb {::key k
                      ::args args})
       (when f
         (apply f args)))})

(defn track-events [wb m & [f]]
  (->> m
       (map (fn [[k filter-args]]
              [k (fn [& args]
                   (log-event wb
                     {::type ::event
                      ::key k
                      ::args ((or filter-args
                                  identity)
                              args)
                      ::filtered? (not (nil? filter-args))})
                   (when f
                     (apply f args)))]))
       (into {})))

(defn track-opts [{:keys [!tracked-opts]} k opts]
  (swap! !tracked-opts assoc k opts)
  opts)

(defn track-component-lifecycle [{:keys [!track-lc-count]
                                  :as wb}
                                 opts]
  (let [lc @!track-lc-count
        mount-notes (cycle ["C4" "E4" "G4" "B4"])
        update-notes (cycle ["D4" "F4" "A4" "C5"])
        unmount-notes (cycle ["E4" "G4" "B5" "D5"])]
    (swap! !track-lc-count inc)
    (merge
      opts
      {:on-did-mount
       (fn [this]
         (tone/trigger-ar! (nth mount-notes lc) "32n")
         (log-event wb {::key :on-did-mount
                        ::args [this]}))
       :on-did-update
       (fn [& args]
         (tone/trigger-ar! (nth update-notes lc) "32n")
         (log-event wb {::key :on-did-update
                        ::args args}))

       :on-will-unmount
       (fn [& args]
         (tone/trigger-ar! (nth unmount-notes lc) "32n")
         (log-event wb {::key :on-did-update
                        ::args args}))})))

(defn test-do []
  (workbench-comp
    (fn [wb]
      [forms/form
       (->> (track-events wb
              {:on-submit nil
               :on-submit-failed nil})
            (track-opts wb :form-opts)
            (track-component-lifecycle wb))
       [forms/group
        {:data-key :text}
        [forms/text
         (->> {:autofocus? true}
              (track-component-lifecycle wb))]]
       [forms/submit-button {:label "submit"}]])))

(comment

  (test-do)

  )
