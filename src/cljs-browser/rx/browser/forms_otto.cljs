(ns rx.browser.forms-otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.workbench :as wb]
            [rx.browser.forms :as forms]
            [rx.browser.ui :as ui]
            [reagent.core :as r]))


(defn test-do []
  (wb/workbench-comp
    (fn [wb]
      [forms/form
       (->> (wb/track-events wb
             {:on-submit first
              :on-submit-failed nil})
            (wb/track-opts wb :form-opts))
       [forms/group
        {:data-key :text}
        [forms/text
         {:autofocus? true}]]
       [forms/submit-button {:label "submit"}]])))

(defn test-select []
  (wb/workbench-comp
    (fn [wb]
      [forms/form
       {:initial-data {:select-val "string"}}
       [forms/group
        {:data-key :select-val}
        [ui/g
         [forms/select
          {:options [{:label "Disabled"
                      :disabled? true}
                     {:value "string"
                      :label "string"}
                     {:value :keyword
                      :label "keyword"}
                     {:value {:baz "yes!"}
                      :label "map"}
                     {:value [1 2 3]
                      :label "vec"}
                     {:value true
                      :label "bool"}]}]]
        [forms/error-text]]
       [forms/with-form-state
        (fn [_ fs]
          [:pre (ks/pp-str (forms/form-data fs))])]])))

(defn test-filter-select []
  (wb/workbench-comp
    (fn [wb]
      [forms/form
       [forms/filter-select
        {:autofocus? false
         :placeholder "Filter select..."
         :options [{:value "string"
                    :label "string"}
                   {:value :keyword
                    :label "keyword"}
                   {:value {:baz "yes!"}
                    :label "map"}
                   {:value [1 2 3]
                    :label "vec"}
                   {:value true
                    :label "bool"}]}]])))

(defn test-form-context []
  (wb/workbench-comp
    (fn [wb]
      [forms/form
       {:prop-validations
        {:text (fn [v]
                 (when (empty? v)
                   "Empty!"))}}
       [forms/group
        {:data-key :text}
        [forms/text
         {:autofocus? true
          :validate-on-blur? true}]
        [forms/error-text]]
       [forms/with-form-state
        (fn [_ fs]
          [:pre (ks/pp-str (forms/form-data fs))])]])))

(comment

  (test-do)

  (test-select)

  (test-filter-select)

  (test-form-context)

  )
