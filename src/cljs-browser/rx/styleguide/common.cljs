(ns rx.styleguide.common
  (:require [rx.kitchen-sink :as ks]
            [rx.css :as css]))

(defn $section [& args]
  (let [[{:keys [title var id]} & children] (ks/ensure-opts args)]
    (into
      [:div.sg-component
       {:id (or id (when var (pr-str var)))}
       [:div.sg-section-header
        {:style (merge
                  {:margin-top 30}
                  css/mg-bot-md)}
        [:h3
         {:style (merge
                   css/mg-none
                   {:font-size 20
                    :font-weight '500})}
         title]
        (when var
          [:h4.mg-none
           {:style {:font-weight '500}}
           (pr-str var)])]]
      children)))

(defn $subsection [& args]
  (let [[{:keys [title]} & children] (ks/ensure-opts args)]
    (into
      [:div.mg-bot-md
       [:h4.sg-subsection-header title]]
      children)))

(defn $opts [options]
  [:div
   (->> options
        (sort-by #(name (first %)))
        (map (fn [[k v]]
               (let [{:keys [desc values]}
                     (if (map? v)
                       v
                       {:desc v})]
                 [:div.sg-opts-row
                  {:key k}
                  [:div.row
                   [:div.col-md-6.col-lg-4
                    [:div.mg-bot-sm
                     [:span.sg-opts-prop (pr-str k)]]]
                   [:div.col-md-6.col-lg-8
                    [:div.mg-bot-sm
                     [:div.sg-opts-desc desc]]
                    [:div.mg-bot-sm
                     (when values
                       [:div.sg-opts-values
                        [:pre
                         #_[page/$interpose-children
                            {:class "flex-left"
                             :separator [:div " | "]}
                            (->> values
                                 (map (fn [v]
                                        [:div.sg-opts-value
                                         (pr-str v)])))]]])]]]]))))])

(defn $checkerboard [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    (into
     [:div.sg-checkerboard opts]
     children)))
