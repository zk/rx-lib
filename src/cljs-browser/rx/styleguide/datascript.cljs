(ns rx.styleguide.datascript
  (:require [rx.kitchen-sink :as ks]
            [datascript.core :as d]
            #_[nsfw.test-harness :as th]
            [reagent.core :as r]))

(def conn (d/create-conn {:car/maker {:db/type :db.type/ref}
                          :car/model {:db/unique :db.unique/identity}
                          :car/colors {:db/cardinality :db.cardinality/many}
                          :maker/email {:db/unique :db.unique/identity}}))

(d/transact! conn
  [{:db/id -1
    :maker/name "BMW"
    :maker/country "Germany"}

   {:car/maker -1
    :car/name "i525"
    :car/colors ["red" "green" "blue"]}
   {:car/maker -1
    :car/name "i725"
    :car/colors ["pink"]}])

(def res (d/q '[:find ?name ?colors
                :where
                [?e :maker/name "BMW"]
                [?c :car/maker ?e]
                [?c :car/name ?name]
                [?c :car/colors ?colors]]
           @conn))

(defn $view []
  (r/create-class
    {:component-did-mount
     (fn [_]
       )
     :reagent-render
     (fn []
       [:div.container
        [:br] [:br] [:br]

        [:pre
         (ks/pp-str res)]])}))
