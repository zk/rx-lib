(ns rx.box.datomic-import
  (:require [rx.kitchen-sink :as ks]))



(comment

  (set! *default-data-reader-fn*
    (fn
      [tag form]
      [(str tag) form]))
  
  

  (->> []
       #_reverse
       #_(take 1)
       (mapcat (fn [ent]
                 (try
                   (let [id [:box/id (-> ent :db/id second second)]]
                     (->> ent
                          (remove #(= :db/id (first %)))
                          (mapcat (fn [[k v]]
                                    (cond
                                      (and (vector? v)
                                           (= "db/id" (first v)))
                                      [[id
                                        (->> [(namespace k)
                                              (name k)]
                                             (remove empty?)
                                             (interpose "/")
                                             (apply str))
                                        [:box/id
                                         (-> v second second)]
                                        true]]
                                      (vector? v)
                                      (->> v
                                           (map (fn [ref-id]
                                                  [id
                                                   (->> [(namespace k)
                                                         (name k)]
                                                        (remove empty?)
                                                        (interpose "/")
                                                        (apply str))
                                                   [:box/id (-> ref-id second second)]
                                                   true
                                                   true])))
                                      :else
                                      [[id
                                        (str
                                          (namespace k)
                                          "/"
                                          (name k))
                                        v]])))
                          doall))
                   (catch Exception e
                     (prn ent e)))))
       (mapv (fn [[e a v ref? many?]]
               (if (inst? v)
                 [e a (.getTime v) ref? many?]
                 [e a v ref? many?])))
       doall
       ks/pp)

  )
