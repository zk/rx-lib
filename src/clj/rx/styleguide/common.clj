(ns rx.styleguide.common
  (:require [rx.kitchen-sink :as ks]
            [cljfmt.core :as cf]))

(defmacro example [form]
  (cf/reformat-string
    (str form)))

#_(cf/reformat-string
    (nu/pp-str '(defn $section [& args]
                  (let [[{:keys [title var id]} & children] (page/ensure-opts args)]
                    (page/elvc
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
                      children)))))


#_(println
    (example
      (defn $section [& args]
        (let [[{:keys [title var id]} & children] (page/ensure-opts args)]
          (page/elvc
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
            children)))))
