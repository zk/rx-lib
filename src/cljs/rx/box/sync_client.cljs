(ns rx.box.sync-client
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <? go-anom]]
            [rx.trace :as trace]
            [rx.box.common :as com]
            [rx.box.persist-local :as pl]
            [rx.box.auth :as auth]
            [rx.log-client :as log]
            [cljs-http.client :as http]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [cljs.env :as env]
            [clojure.core.async
             :refer [go <! chan put! close!]]))

;; Syncing

(defn ident-val [m k]
  (when-let [v (get m k)]
    [k v]))

(defn mark-synced [o]
  (when o
    (-> o
        (assoc :box.sync/success-ts (ks/now)))))

(defn <mark-failed-sync [conn ents res]
  (let [error-code (-> res anom/code)]
    (pl/<transact
      conn
      (->> ents
           (mapcat (fn [o]
                     (let [ident (com/resolve-ident
                                   (com/conn->schema conn)
                                   o)]
                       (remove nil?
                         [[:box/dissoc
                           ident
                           :box.sync/in-progress-ts]
                          [:box/assoc
                           ident
                           :box.sync/failure-ts
                           (ks/now)]
                          (when error-code
                            [:box/assoc
                             ident
                             :box.sync/failure-code error-code])]))))))))

(defn ensure-valid-ddb-conn [conn]
  (let [schema (com/conn->schema conn)]
    (when-not (:box/sync-ddb-uri schema)
      (anom/throw-anom
        {:desc "Missing :box/sync-ddb-uri"
         :code :invalid}))))

(<defn <send-ops [uri ops auth]
  (let [{:keys [success
                body
                headers
                error-code
                error-text
                status]
         :as resp}
        (<! (http/post
              uri
              {:headers {"Content-Type" "application/transit+json"}
               :timeout 20000
               :transit-params
               {:box.sync/ops ops
                :box.sync/auth (auth/id-token auth)}}))]

    (cond
      (= :timeout error-code)
      (anom/anom
        {:desc "Timeout hit (20000ms)"
         :code ::timeout}
        {::uri uri})

      (not= 200 status)
      (anom/anom
        {:desc "Response status not 200"
         :code ::non-200}
        {::uri uri})
      
      (= -1
         (.indexOf
           (or (get headers "Content-Type")
               (get headers "content-type"))
           "application/transit+json"))
      (anom/anom
        {:desc (str "Response content-type not application/transit+json: " (pr-str resp))
         :code ::invalid-content-type}
        {::uri uri})
      
      (not success)
      (anom/anom
        {:desc "Couldn't contact server"
         :code :unavailable}
        {::uri uri})

      (anom/code body) body

      (:rx.res/data body) (:rx.res/data body)

      (:rx.res/anom body)
      (merge
        (dissoc
          (:rx.res/anom body)
          ::anom/js-stack)
        {::source "server"})

      :else body)))

(defn remove-local-sync-keys [o]
  (-> o
      (dissoc
        :box.sync/dirty-ts
        :box.sync/in-progress-ts
        :box.sync/success-ts
        :box.sync/failure-ts
        :box.sync/failure-code)))

(defn has-failures? [res]
  (or (anom/anom? res)
      (->> res
           (filter :box.sync/failure-ts)
           empty?
           not)))

(<defn <run-sync-and-format-results [schema ents auth]
  (let [ident->ent (->> ents
                        (map (fn [ent]
                               [(com/resolve-ident schema ent)
                                ent]))
                        (into {}))

        uri (com/schema->sync-ddb-uri schema)
        
        ops (->> ident->ent
                 (map (fn [[ident obj]]
                        {:box.sync/op-key :box.op/update
                         :box.sync/ignore-match-version? true
                         :box.sync/ident-key (first ident)
                         :box.sync/entity obj})))

        res (<? (<send-ops uri ops auth))]

    res))

;; sync ents, should return rx.anom for anomolous situations

(defn ensure-valid-sync-entities [schema entities]

  (when (= 0 (count entities))
    (throw (ex-info "No entities supplied to sync"
             {:var ::<sync-entities
              :ents entities})))
  
  (when-let [invalid-ents
             (->> entities
                  (remove #(com/resolve-ident schema %)))]
                    
    (when (not (empty? invalid-ents))
      (throw (ex-info "Invalid entities found"
               {:invalid-entities
                invalid-ents})))))

(defn mark-owner [auth entity]
  (when entity
    (assoc entity
      :box.sync/owner-id
      (auth/sub auth))))

(<defn <sync-entities
  [conn ents auth & [ctx]]

  
  
  (ensure-valid-ddb-conn conn)

  (ensure-valid-sync-entities
    (com/conn->schema conn)
    ents)

  (try

    (when-not auth
      (throw (ex-info "Auth invalid"
               {:auth auth
                :rx.anom/code ::auth-invalid})))
    
    (let [schema (com/conn->schema conn)
          clean-ents (->> ents
                          (map remove-local-sync-keys)
                          (map #(mark-owner auth %)))

          res (<? (pl/<transact
                    conn
                    (->> clean-ents
                         (map #(com/resolve-ident schema %))
                         (mapcat
                           (fn [ident]
                             [[:box/assoc
                               ident
                               :box.sync/in-progress-ts
                               (ks/now)]
                              [:box/dissoc
                               ident
                               :box.sync/success-ts]])))))

          res (<? (<run-sync-and-format-results
                    schema
                    clean-ents
                    auth))

          sync-results (-> res
                           :box.sync/results)

          _ (when (empty? sync-results)
              (throw (ex-info
                       "Sync results empty, sync server should have returned results"
                       res)))

          success-results (->> sync-results
                               (filter :box.sync/success-ts))

          failure-results (->> sync-results
                               (remove :box.sync/success-ts))
          
          ident->ent (->> ents
                          (map (fn [ent]
                                 [(com/resolve-ident schema ent)
                                  ent]))
                          (into {}))

          success-ents (->> success-results
                            (map :box/ident)
                            (map #(get ident->ent %)))

          updated-ents (->> success-ents
                            (remove :box.sync/delete-marked-ts))

          deleted-ents (->> success-ents
                            (filter :box.sync/delete-marked-ts))

          merged-ents (->> updated-ents
                           (map mark-synced))

          merged-ents-retractions
          (->> merged-ents
               (mapcat (fn [obj]
                         (let [ident (com/resolve-ident schema obj)]
                           [[:box/dissoc
                             ident
                             :box.sync/in-progress-ts]
                            [:box/dissoc
                             ident
                             :box.sync/dirty-ts]
                            [:box/dissoc
                             ident
                             :box.sync/failure-code]
                            [:box/dissoc
                             ident
                             :box.sync/failure-ts]]))))

          updated-idents (->> success-results
                              (remove :box.sync/delete-marked-ts)
                              (map :box/ident))

          deleted-idents (->> success-results
                              (filter :box.sync/delete-marked-ts)
                              (map :box/ident))

          failure-idents (->> failure-results
                              (map :box/ident))

          failure-ents-txfs
          (->> failure-idents
               (remove nil?)
               (mapcat (fn [ident]
                         [[:box/assoc
                           ident
                           :box.sync/failure-ts
                           (ks/now)]
                          [:box/assoc
                           ident
                           :box.sync/failure-code
                           :unknown]])))

          delete-ents-txfs
          (->> deleted-ents
               (map (fn [o]
                      [:box/delete
                       (com/resolve-ident schema o)])))

          tx-frags (concat
                     merged-ents
                     merged-ents-retractions
                     failure-ents-txfs
                     delete-ents-txfs)

          update-res (<! (pl/<transact conn
                           tx-frags
                           {:debug-log? false}))]

      update-res)

    (catch js/Error e
      (go
        (let [res (<! (<mark-failed-sync conn ents (ex-data e)))]
          (when (anom/code res)
            (prn
              "box.sync.mark-sync-failed-anomaly"
              res))))
      (anom/from-err e))))

(defn <sync-idents [conn idents auth & [ctx]]
  (go
    (let [entities (<! (pl/<pull-multi conn idents))]
      (if (empty? entities)
        (anom/anom
          {:desc "Entities not found for idents"
           :code :not-found})
        (<! (<sync-entities conn entities auth))))))

(<defn <sync [conn txfs auth & [opts]]
  (let [entities (<? (pl/<transact conn txfs opts))]

    (if auth
      (let [res (<! (<sync-entities conn entities auth))]
        (if (anom/? res)
          (merge
            {:sync-success? false
             :transact-success? true
             :entities entities}
            (select-keys
              res
              [:sync-failed-entities]))
          (merge
            res
            {:sync-success? true
             :transact-success? true})))
      {:entities entities
       :transact-success? true
       :sync-success? false})))

(<defn <sync-mark [conn txfs auth & [opts]]
  (let [schema (com/conn->schema conn)
        mark-dirty-txfs (->> txfs
                             (remove pl/delete-txf?)
                             (map #(pl/txf->ident schema %))
                             (remove nil?)
                             (map (fn [ident]
                                    [:box/assoc
                                     ident
                                     :box.sync/dirty-ts (ks/now)])))]
    (<? (pl/<transact
          conn
          (concat
            txfs
            mark-dirty-txfs)
          opts))))

(<defn <sync-imd [conn txfs auth & [opts]]
  (let [entities (<? (<sync-mark conn txfs auth opts))]
    (go
      (let [res (<! (<sync-entities conn entities auth))]
        (when (and (anom/? res)
                   (not= ::auth-invalid (anom/code res)))
          (ks/spy "Error syncing immediate" res))))
    entities))

(<defn <sync-ack [conn txfs auth & [opts]]
  (let [entities (<? (<sync-mark conn txfs auth opts))
        res (<? (<sync-entities conn entities auth))]
    res))

(defn <sync-mo [conn txfs auth & [opts]]
  (let [ch (chan)]
    (go
      (try
        (let [entities (<? (pl/<transact conn txfs opts))]
          (put! ch (or entities []))
          (put! ch (<! (<sync-entities conn entities auth)))
          (close! ch))
        (catch js/Error e
          (anom/from-err e {:var #'<sync-mo}))))
    ch))

                                                    


