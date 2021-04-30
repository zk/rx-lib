(ns rx.box2.reagent
  (:require [rx.kitchen-sink :as ks]
            [rx.box2 :as box]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [reagent.core :as r]
            [reagent.ratom :as ratom
             :refer-macros [reaction]]
            [clojure.core.async :as async
             :refer [go <!]]))

(def !id->reaction
  (atom {}))

(comment

  (prn (count @!id->reaction))

  (prn (keys @!id->reaction))

  )

#_(ks/pp @!id->reaction)

(<defn <tracked-query [id & args]
  (let [inital-return-value (<! (apply box/<query args))
        out-atom (r/atom inital-return-value)]
    (swap! !id->reaction
      assoc
      id
      {:args args
       :out-atom out-atom
       :f box/<query})
    out-atom))

(defn remove-reaction [id]
  #_(println "remove reaction" id)
  (swap! !id->reaction dissoc id)
  (println "reaction-count" (count @!id->reaction))
  #_(println "Keys")
  #_(ks/pp (keys @!id->reaction))
  #_(prn "count" (count @!id->reaction)))

(<defn <query [& args]
  (let [id (ks/uuid)
        initial-value (<? (apply box/<query args))
        out-atom (r/atom initial-value)]
    (swap! !id->reaction
      assoc
      id
      {:args args
       :on-change (fn [v]
                    (reset! out-atom v))
       :f box/<query})
    (ratom/make-reaction
      (fn []
        @out-atom)
      :on-dispose
      (fn []
        (remove-reaction id)))))

(<defn <bind-query [on-change & args]
  (let [initial-value (<? (apply box/<query args))]
    (on-change initial-value)
    (swap! !id->reaction
      assoc
      on-change
      {:args args
       :on-change on-change
       :f box/<query})
    on-change))

(defn <atom-query [!atom & args]
  (apply <bind-query
    (fn [v]
      (reset! !atom v))
    args))

(defn unbind [key]
  (swap! !id->reaction dissoc key))

(<defn <pull [& args]
  (let [id (ks/uuid)
        initial-value (<? (apply box/<pull args))
        out-atom (r/atom initial-value)]
    (swap! !id->reaction
      assoc
      id
      {:args args
       :on-change (fn [v] (reset! out-atom v))
       :f box/<pull})
    #_(prn id args initial-value)
    (ratom/make-reaction
      (fn []
        @out-atom)
      :on-dispose
      (fn []
        (remove-reaction id)))))

(<defn <transact [& args]
  (let [tx-res (<? (apply box/<transact args))]
    #_(println (count @!id->reaction)
        "reactions in !id->reaction")
    #_(println "Ids:")
    #_(ks/pp (keys @!id->reaction))
    #_(println "---")
    (doseq [[id {:keys [f args on-change]}] @!id->reaction]
      (let [reaction-res (<? (apply f args))]
        #_(prn "run reaction" id reaction-res)
        #_(ks/spy args)
        (on-change reaction-res)))
    tx-res))
