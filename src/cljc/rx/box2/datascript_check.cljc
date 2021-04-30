(ns rx.box2.datascript-check
  (:require [rx.kitchen-sink :as ks]
            [datascript.core :as d]))

(defn check-1 []
  (let [schema {:box/refs {:db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/many}}
        conn (d/create-conn schema)]

    (d/transact! conn [{:db/id 1
                        :box/created-ts 1000
                        :box/refs [{:db/id 3
                                    :box/created-ts 3000}
                                   {:db/id 4
                                    :box/created-ts 3000
                                    :box/foo "BAR"}]}
                       {:db/id 2
                        :box/created-ts 2000}])

    (ks/spy (d/q '[:find (pull ?e [*])
                   :where
                   [?e]
                   (not
                     [_ :box/refs ?e])]
              @conn))))

(defn check-2 []
  (let [schema {:box/refs {:db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/many}}
        conn (d/create-conn schema)]

    (d/transact! conn [{:db/id 1
                        :box/created-ts 1000
                        :box/refs [{:db/id 3
                                    :box/created-ts 3000}
                                   {:db/id 4
                                    :box/created-ts 3000
                                    :box/foo "BAR"}]}
                       {:db/id 2
                        :box/created-ts 2000}])

    (ks/spy (d/q '[:find ?e
                   :where
                   [_ :box/refs ?e]]
              @conn))))



(comment

  (check-1)

  (check-2)


  )
