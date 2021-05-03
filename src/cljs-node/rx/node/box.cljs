(ns rx.node.box
  (:require [rx.kitchen-sink :as ks]
            [rx.box.persist-local :as pl]
            [rx.box.query-local :as ql]
            [rx.box.sync-client :as sync-client]
            [rx.box :as box]
            [rx.node.sqlite :as sql]
            [rx.box.db-adapter-sqlite :as db-adapter-sqlite]
            [rx.node.box.db-adapter-sqlite :as das]
            [rx.node.aws :as aws]
            [rx.test.perf :as perf]
            [cljs.core.async
             :as async
             :refer [<!]
             :refer-macros [go]]))

(when-not (exists? js/window)
  (set! js/XMLHttpRequest (js/require "xhr2")))

(defn <conn [schema]
  (pl/<conn {:box/db-adapter
             (db-adapter-sqlite/create-db-adapter sql/SQL)
             :AWS aws/AWS}
    schema))

(defn <transact
  [conn txfs
   & [{:keys [debug-log?
              skip-entity-specs?]
       :as opts}]]
  (pl/<transact conn txfs opts))

(defn <pull [conn ident-or-ident-val & [deref-spec opts]]
  (pl/<pull conn ident-or-ident-val deref-spec opts))

(defn <pull-multi [conn ident-or-ident-val & [deref-spec opts]]
  (pl/<pull-multi conn ident-or-ident-val deref-spec opts))

(defn has-failures? [res] (sync-client/has-failures? res))

(defn <sync-entities [conn ents auth]
  [conn ents auth]
  (sync-client/<sync-entities conn ents auth))

(defn <query [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<query conn query opts))

#_(defn <count [conn query & [{:keys [debug-log?]
                             :as opts}]]
  (ql/<count conn query opts))

(defn <recreate-refs-table
  [{:keys [:box.pl/sqlite-db
           :box.pl/SQL
           :box/schema]
    :as conn}]
  #_(pl/<recreate-refs-table conn))

#_(defn <referenced-by [conn
                      ident
                      from-ref-key
                      & [{:keys [pull-keys
                                 honeysql]
                          :as opts}]]
  (ql/<referenced-by conn ident from-ref-key opts))

(defn col [k]
  (ql/col k))

(defn debug-conn
  [{:keys [schema
           destroy-db?]
    :as opts}
   <f]
  (pl/debug-conn
    sql/SQL
    opts
    <f))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn <perf-test [{:keys [group-desc
                          test-desc
                          iterations
                          <test-fn]}]
  (go
    (let [box-spec {:box/attrs
                    {:box.stress-test/id
                     {:box.attr/ident? true
                      :box.attr/value-type :box.type/string}}}

          dbs [{:local-db-name "stress_test_db_400kb_payload_1000_rows"
                :desc "400kb payload db"}
               {:local-db-name "stress_test_db_small"
                :desc "small db"}
               {:local-db-name "stress_test_db_100000_rows"
                :desc "100k rows"}]
            
          tests (->> dbs
                     (map
                       (fn [{:keys [local-db-name desc]}]
                         (go
                           (let [conn (<! (box/<conn
                                            {:box/db-adapter (das/create-db-adapter)}
                                            (merge
                                              box-spec
                                              {:box/local-db-name local-db-name})))]
                             {::perf/perf-test-desc
                              (str group-desc ", " desc)
                              ::perf/iterations iterations
                              ::perf/<test-fn <test-fn
                              ::perf/args [conn]}))))
                     ks/async-realize
                     <!)]
      
      (<! (perf/<run-group
            {::perf/group-desc group-desc
             ::perf/perf-tests tests})))))

(defn <perf-groups [_ group-specs]
  (go
    (->> group-specs
         (map #(<perf-test %))
         ks/async-realize
         <!
         vec)))



(comment

  (ks/<pp
    (<perf-groups
      nil
      [{:group-desc "Test Count"
        :iterations 10
        :<test-fn (fn [conn]
                    (box/<count
                      conn
                      {:where [:and
                               [:ident-key :box.stress-test/id]]}
                      {:debug-log? false}))}
       {:group-desc "Test Query"
        :iterations 10
        :<test-fn (fn [conn]
                    (go
                      (<! (box/<query
                            conn
                            {:where [:and
                                     [:ident-key :box.stress-test/id]]
                             :limit 10}
                            {:debug-log? false}))
                      "ok"))}]))

  (ks/<pp
    (<perf-test
      {:group-desc "Test Count"
       :iterations 10}
      (fn [conn]
        (box/<count
          conn
          {:where [:and
                   [:ident-key :box.stress-test/id]]}
          {:debug-log? false}))))

  (ks/<pp
    (<perf-group
      {:group-desc "Test Count"
       :iterations 10}
      (fn [conn]
        (box/<count
          conn
          {:where [:and
                   [:ident-key :box.stress-test/id]]}
          {:debug-log? false}))))

  (ks/<pp
    (<perf-test
      {:group-desc "Test Query"
       :iterations 10}
      (fn [conn]
        (go
          (<! (box/<query
                conn
                {:where [:and
                         [:ident-key :box.stress-test/id]]
                 :limit 10}
                {:debug-log? false}))
          "ok"))))

  (rand-str 10)

  (ks/<pp
    (pl/<conn
      (sql/gen-sql)
      {:box/local-db-name "test_db_name"
       :box/local-data-table-name "box_data"
       :box/local-refs-table-name "box_refs"}))

  (go
    (pl/<show-local-tables
      (<! (pl/<conn
            (sql/gen-sql)
            {:box/local-db-name "test_db_name"
             :box/local-data-table-name "box_data"
             :box/local-refs-table-name "box_refs"}))))

  (go
    (let [conn (<! (box/<conn
                     {:box/db-adapter (das/create-db-adapter)}
                     {:box/local-db-name "stress_test_db_10M_rows"
                      :box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}))
          start (ks/now)]
      (doseq [i (range 10)]
        (<! (box/<transact
              conn
              (->> (range 1000)
                   (map (fn [j]
                          {:box.stress-test/id (str (+ (* i 10000) j))})))))
        (prn (- (ks/now) start)))))

  (go
    (let [large-str (rand-str 400000)
          conn (<! (box/<conn
                     {:box/db-adapter (das/create-db-adapter)}
                     {:box/local-db-name "stress_test_db_400kb_payload_10k_rows"
                      :box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}))
          start (ks/now)
          page-count 10]
      (doseq [i (range page-count)]
        (<! (box/<transact
              conn
              (->> (range 1000)
                   (map (fn [j]
                          {:box.stress-test/id (str (+ (* i page-count) j))
                           :box.stress-test/test-str large-str})))))
        (prn (- (ks/now) start)))))

  (go
    (let [conn (<! (box/<conn
                     {:box/db-adapter (das/create-db-adapter)}
                     {:box/local-db-name "stress_test_db"
                      :box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}))
          start (ks/now)]
      (ks/prn
        "anom"
        (box/anom
          (<! (box/<transact
                conn
                (->> (range 1000)
                     (map (fn [j]
                            {:box.stress-test/id (str j)
                             :other-data "The quick brown fox jumps over the lazy dog, the quick brown fox jumps over the lazy dog"})))
                {:debug-log? false}))))
      (prn (- (ks/now) start))))
  

  (go
    (let [conn (<! (box/<conn
                     {:box/db-adapter (das/create-db-adapter)}
                     {:box/local-db-name "stress_test_db"
                      :box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}))
          start (ks/now)]


      (ks/prn
        (<! (box/<count
              conn
              {:where [:and
                       [:ident-key :box.stress-test/id]]}
              {:debug-log? true})))
      
      
      (prn (- (ks/now) start))))

  (go
    (let [conn (<! (box/<conn
                     {:box/db-adapter (das/create-db-adapter)}
                     {:box/local-db-name "stress_test_db_small"
                      :box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}))]
      (ks/spy
        (<! (perf/<run-chain
              {::perf/chain-desc "box <count"
               ::perf/perf-test
               {::perf/perf-test-desc "box <count on query by ident key"
                ::perf/iterations 10
                ::perf/<test-fn
                (fn []
                  (box/<count
                    conn
                    {:where [:and
                             [:ident-key :box.stress-test/id]]}
                    {:debug-log? false}))}
               ::perf/chain-args
               [{::perf/args-desc "small db"
                 ::perf/args [conn]}
                {::perf/args-sdesc ""}]})))))

  (defn <perf-test [conn-fn]
    (go
      (let [box-spec {:box/attrs
                      {:box.stress-test/id
                       {:box.attr/ident? true
                        :box.attr/value-type :box.type/string}}}

            dbs [{:local-db-name "stress_test_db_400kb_payload_1000_rows"
                  :desc "400kb payload db"}
                 {:local-db-name "stress_test_db_small"
                  :desc "small db"}
                 {:local-db-name "stress_test_db_100000_rows"
                  :desc "small db"}]
            
            tests (->> dbs
                       (map
                         (fn [{:keys [local-db-name desc]}]
                           {::perf/perf-test-desc
                            (str "box <count on query by ident key, " desc)
                            ::perf/iterations 10
                            ::perf/<test-fn conn-fn
                            ::perf/args [conn]})))

            (<! (perf/<run-group
                  {::perf/group-desc "Test count"
                   ::perf/perf-tests tests}))])))
  
  (go
    (let [count-fn (fn [conn]
                     (box/<count
                       conn
                       {:where [:and
                                [:ident-key :box.stress-test/id]]}
                       {:debug-log? false}))

          tests (->> 
                  (map (fn [{:keys [local-db-name desc]}]
                         (go
                           (let [conn (<! (box/<conn
                                            {:box/db-adapter (das/create-db-adapter)}
                                            {:box/local-db-name local-db-name
                                             :box/attrs
                                             {:box.stress-test/id
                                              {:box.attr/ident? true
                                               :box.attr/value-type :box.type/string}}}))]
                             {::perf/perf-test-desc
                              (str "box <count on query by ident key, " desc)
                              ::perf/iterations 10
                              ::perf/<test-fn count-fn
                              ::perf/args [conn]}))))
                  ks/async-realize
                  <!)]
      (ks/spy
        (<! (perf/<run-group
              {::perf/group-desc "Test count"
               ::perf/perf-tests tests})))))

  (set! *print-namespace-maps* false)

  
  )




