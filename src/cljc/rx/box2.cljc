(ns rx.box2
  (:require [rx.kitchen-sink :as ks]
            [rx.box2.sql :as bsql]
            [rx.anom :as anom
             :refer [<defn <?]
             :refer-macros [<defn]]
            [datascript.query-v3 :as dsq]
            [datascript.pull-parser :as pp]
            [datascript.parser :as dp]
            [clojure.string :as str]
            [clojure.walk  :as walk]
            [clojure.core.async :as async
             :refer [go <!]]))

;; Idents

(defn resolve-ident [schema ent]
  (let [ident-key-set (->> schema
                           :box/attrs
                           (filter :box/ident?)
                           (map :box/attr)
                           set)
        k (->> ent
               keys
               (filter #(get ident-key-set %))
               first)

        v (get ent k)]
    (when (or (not k) (not v))
      (anom/throw-anom {:desc "Couldn't resolve ident"
                        ::ent ent
                        ::ident-keys-set ident-key-set}))
    [k v]))

(defn to-ident [schema o]
  (cond
    (vector? o) o
    (map? o) (resolve-ident schema o)
    :else (anom/throw-anom
            {:desc "Couldn't convert to ident"
             ::in o
             :box/schema schema})))

(defn ident-key [o]
  (first o))

(defn ident-val [o]
  (second o))

(defn box-ident? [o]
  (and (vector? o)
       (first o)
       (second o)))


;; TXFS Transaction fragments

(defn valid-txf? [txf]
  (and (vector? txf)
       (box-ident? (second txf))
       (get
         #{:box/assoc :box/dissoc :box/conj :box/disj :box/delete}
         (first txf))))

(defn throw-if-invalid-txfs [txfs]
  (let [invalid-txfs (->> txfs
                          (filter #(not (valid-txf? %))))]
    (if-not (empty? invalid-txfs)
      (anom/throw-anom
        {:desc "Invalid txfs"
         ::invalid-txfs invalid-txfs})
      txfs)))

;; SQL

(defn to-sql-vec [m]
  (into
    [(:sql m)]
    (:args m)))

(defn to-sql-bool [o]
  (if o true false))

(defn prepend-sql [s m]
  (update
    m
    :sql
    #(str s %)))

(defn append-sql [s m]
  (update
    m
    :sql
    #(str % s)))

(defn join-sql-maps [join-str ms]
  (->> ms
       (reduce
         (fn [accu m]
           (if (and (:sql m) (not (empty? (:sql m))))
             {:sql
              (str (:sql accu) join-str (:sql m))
              :args (into
                      (vec (:args accu))
                      (:args m))}
             accu)))))

;; Pull

(defn schema->ref-attrs-set [schema]
  (->> schema
       :box/attrs
       (filter :box/ref?)
       (map :box/attr)
       set))

(defn ref-attr? [schema attr]
  (let [ref-attrs (schema->ref-attrs-set schema)]
    (get ref-attrs attr)))

(defn schema->one-ref-attrs-set [schema]
  (->> schema
       :box/attrs
       (filter :box/ref?)
       (filter #(not (= :box/cardinality-many
                        (:box/cardinality %))))
       (map :box/attr)
       set))

(defn one-ref-attr? [schema attr]
  (let [one-ref-attrs (schema->one-ref-attrs-set schema)]
    (get one-ref-attrs attr)))

(defn schema->many-ref-attrs-set [schema]
  (->> schema
       :box/attrs
       (filter :box/ref?)
       (filter #(= :box/cardinality-many
                   (:box/cardinality %)))
       (map :box/attr)
       set))

(defn many-ref-attr? [schema attr]
  (let [ref-attrs (schema->many-ref-attrs-set schema)]
    (get ref-attrs attr)))

(defn serialize-value? [v]
  (or (sequential? v)
      (map? v)))

(defn val->sql-val [o]
  (cond
    (string? o) o
    (keyword? o) (->> [(namespace o)
                       (name o)]
                      (remove nil?)
                      (interpose "/")
                      (apply str))
    (serialize-value? o)
    (ks/to-transit o)
    (boolean? o) (if o 1 0)
    
    :else o))

(defn val->data-type [o]
  (cond
    (string? o) "STRING"
    (int? o) "LONG"
    (float? o) "DOUBLE"
    (boolean? o) "BOOL"
    (map? o) "MAP"
    (vector? o) "VEC"
    (set? o) "SET"
    (list? o) "LIST"
    (nil? o) "NIL"
    (keyword? o) "KEYWORD"
    :else (ks/throw-anom
            {:desc "Unknown type for conversion"
             ::value o
             ::type (type o)})))

(defn val->sql-val-quoted [o]
  (cond
    (string? o) (str "\"" o "\"")
    (keyword? o) (str
                   "\""
                   (->> [(namespace o)
                         (name o)]
                        (remove nil?)
                        (interpose "/")
                        (apply str))
                   "\"")
    :else o))

(defn eav-type->sql-column-name
  [t]
  (try
    (condp = t
      :ent "ENTITY"
      :atr "ATTR"
      :val "VALUE")
    (catch #?(:clj Exception :cljs js/Error) e
      (anom/throw-anom
        {:desc "Error converting eav-type to sql col"
         ::eav-type t}))))

(defn clause-sql-var [[clause-idx eav-type] & [override]]
  (str "c" clause-idx "." (or override (eav-type->sql-column-name eav-type))))

(defn fn-arg->sql [{:keys [symbol value]}
                   {:keys [sym->clause-idx+eav-type
                           in-syms-set
                           find-syms-set]}]
  (cond
    value
    (val->sql-val-quoted value)

    (and symbol
         (get in-syms-set symbol))
    "?"

    symbol
    (clause-sql-var
      (get sym->clause-idx+eav-type symbol))))

(defn clause-idx [{:keys [sym->clause-idx+eav-type]} sym]
  (first (get sym->clause-idx+eav-type sym)))

(defn ident->entity-sql-str [ident]
  (let [[ident-key ident-val]
        (if (sequential? ident)
          ident
          [:box/id ident])]
    (str
      (val->sql-val ident-key)
      " "
      ident-val)))

(defn pull-level-sql-vec [ppat+idents]
  (vec
    (concat
      [(str
         "SELECT * FROM datoms\n"
         "WHERE\n"
         (->> [(->> ppat+idents
                    (map (fn [[ppat [ident-key ident-val]]]
                           (let [{:keys [wildcard? attrs]} ppat
                                 attrs-vec (->> ppat
                                                :attrs
                                                (mapv first))]
                             (str
                               "("
                               (->> ["ENTITY=?"
                                     (when
                                         #_(and attrs (> (count attrs) 0))
                                         (not wildcard?)
                                         (str
                                           "("
                                           (->> attrs-vec
                                                (map (fn [attr-key]
                                                       (str "ATTR=?")))
                                                (interpose " OR ")
                                                (apply str))
                                           ")"))]
                                    (remove empty?)
                                    (interpose " AND ")
                                    
                                    (apply str))
                               ")"))))
                    (remove empty?)
                    (interpose " OR ")
                    (apply str))]
              (remove empty?)
              (interpose " AND ")
              (apply str)))]
      (->> ppat+idents
           (mapcat (fn [[ppat ident]]
                     (let [{:keys [wildcard? attrs]} ppat
                           attrs-vec (->> ppat
                                          :attrs
                                          (mapv first))]
                       (concat
                         [(ident->entity-sql-str ident)]
                         (when
                             (not wildcard?)
                           #_(and attrs (> (count attrs) 0))
                           (->> attrs-vec
                                (map val->sql-val)))))))
           (remove nil?)))))

(defn sql-val->val [o datatype]
  (condp = datatype
    "STRING" (str o)
    "LONG" (long o)
    "DOUBLE" (double o)
    "BOOL" (if (= 1 o) true false)
    "NIL" nil
    "KEYWORD" (keyword o) 
    o))

(defn sql-bool->bool [o]
  (= 1 o))

(defn sql-keyword->keyword [s]
  (keyword s))

(defn sql-entity->ident [s]
  (let [[idk idv] (remove empty? (str/split s #"\s" 2))
        [idk idv] (if (and idk idv)
                    [(keyword idk) idv]
                    [:box/id idv])]
    [idk idv]))

(defn serialized-data-type? [s]
  (get #{"MAP" "VEC" "LIST" "SET"} s))

(defn sql-row->datom
  [schema {:strs [ENTITY ATTR VALUE IS_REF REF_IDENT_KEY IS_REF_MANY DATATYPE]
           :as row}]
  (let [ref? (sql-bool->bool IS_REF)
        many? (sql-bool->bool IS_REF_MANY)]
    [(sql-entity->ident ENTITY)
     (sql-keyword->keyword ATTR)
     (if ref?
       (sql-entity->ident VALUE)
       (if (serialized-data-type? DATATYPE)
         (try
           ;; HERE
           (ks/from-transit VALUE)
           (catch #?(:clj Exception :cljs js/Error) e
             (prn row)
             (throw e)))
         (sql-val->val VALUE DATATYPE)))
     ref?
     many?
     DATATYPE]))

(defn datoms->ident->entity [datoms]
  (->> datoms
       (group-by first)
       (map (fn [[ident datoms]]
              (->> datoms
                   (reduce
                     (fn [[ident ent] datom]
                       (let [[_ k v ref? many?] datom]
                         [ident
                          (merge
                            {(first ident) (second ident)}
                            (update
                              ent
                              k
                              (fn [ov]
                                (cond
                                  (and ref? many?)
                                  (conj (vec ov) {(first v) (second v)})
                                  ref?
                                  {(first v) (second v)}
                                  :else v))))]))
                     [ident {}]))))
       (into {})))

(<defn <pull-level [{:keys [::bsql/<exec
                            ::bsql/db
                            :box/schema]
                     :as conn}
                    ppat+idents]
  (let [sql-vec (pull-level-sql-vec ppat+idents)
        #_#_
        _ (ks/spy sql-vec)

        rows (<! (<exec db sql-vec))
        _ (anom/throw-if-anom rows)
        #_#_
        _ (ks/spy rows)
        
        datoms (concat
                 (->> rows
                      #_reverse
                      (mapv #(sql-row->datom schema %))))

        ident->ppat (->> ppat+idents
                         (map
                           (fn [[k v]]
                             [v k]))
                         (into {}))
        
        ident->entity (->> datoms
                           datoms->ident->entity)

        child-ppat+idents
        (->> ident->ppat
             (mapcat (fn [[ident ppat]]
                       (->> ppat
                            :attrs
                            (mapcat (fn [[ref-key {:keys [subpattern]}]]
                                      (when subpattern
                                        (let [ref-val (get-in
                                                        ident->entity
                                                        [ident ref-key])
                                              idents (->> (if (map? ref-val)
                                                            [ref-val]
                                                            ref-val)
                                                          (map (fn [m]
                                                                 (let [[k v] (first m)]
                                                                   [k v]))))]
                                          (->> idents
                                               (map (fn [ident]
                                                      [subpattern ident])))))))
                            (remove empty?)))))
        
        child-ident->entity (when-not (empty? child-ppat+idents)
                              (<! (<pull-level conn child-ppat+idents)))

        ident->entity
        (merge
          ident->entity
          (->> ident->ppat
               (map (fn [[ident ppat]]
                      [ident
                       (->> ppat
                            :attrs
                            (filter
                              #(ref-attr? schema (first %)))
                            (reduce
                              (fn [m [ref-key]]
                                (let [ref-val (get-in ident->entity
                                                [ident ref-key])]
                                  (merge
                                    m
                                    (when ref-val
                                      {ref-key
                                       (or
                                         (if (map? ref-val)
                                           (get child-ident->entity
                                             (to-ident schema ref-val))
                                           (->> ref-val
                                                (mapv (fn [ref-map]
                                                        (or (get child-ident->entity
                                                              (to-ident schema ref-map))
                                                            ref-map)))))
                                         (get m ref-key))}))))
                              (get ident->entity ident)))]))
               (filter second)
               (into {})))]
    ident->entity))

(<defn <pull [conn pattern ident-or-ent & [not-found]]
  (when ident-or-ent
    (let [ident (to-ident (:box/schema conn) ident-or-ent)
          ppattern (pp/parse-pull pattern)]
      (let [ident->entity (<! (<pull-level conn [[ppattern ident]]))
            _ (anom/throw-if-anom ident->entity)]
        (or
          (get ident->entity ident)
          not-found)))))

(<defn <pull-multi [conn pattern ident-or-ents]
  (let [idents (map #(to-ident (:box/schema conn) %) ident-or-ents)
        ppattern (pp/parse-pull pattern)]
    (let [ident->entity (<! (<pull-level conn
                              (->> idents
                                   (mapv (fn [ident]
                                           [ppattern ident])))))
          _ (anom/throw-if-anom ident->entity)]
      (->> idents
           (map #(get ident->entity %))
           (remove nil?)
           vec))))


;; Query

(defn reduce-indexed
  "Same as reduce, but `f` takes [acc el idx]"
  [f init xs]
  (first
    (reduce
      (fn [[acc idx] x]
        (let [res (f acc x idx)]
          (if (reduced? res)
            (reduced [res idx])
            [res (inc idx)])))
      [init 0]
      xs)))

(defn join-clause? [v]
  (:pattern v))

(defn idx->eav-type
  "Derive eav type (Entity, Attribute, or Value) from position in datascript pattern"
  [i]
  (condp = i
    0 :ent
    1 :atr
    2 :val))

(defn pquery-syms [m]
  (let [!syms (atom [])]
    (walk/postwalk
      (fn [o]
        (when (and o (map? o) (:symbol o))
          (swap! !syms conj (:symbol o)))
        o)
      m)
    @!syms))

(defn qfind-return-value-syms [pq]
  (let [!syms (atom [])]
    (walk/postwalk
      (fn [o]
        (cond
          (and (sequential? o)
               (= :elements (first o)))
          (->> o
               second
               (map (fn [m]
                      (let [sym (or (-> m :variable :symbol)
                                    (-> m :symbol))]
                        (when sym
                          (swap! !syms conj sym)))))
               doall)
          (and (sequential? o)
               (= :element (first o)))
          (do
            (swap! !syms
              conj
              (or (-> o second :symbol)
                  (-> o second :variable :symbol)))))
        o)
      (:qfind pq))
    @!syms))

(defn qfind-syms [pq]
  (pquery-syms (:qfind pq)))

(defn qfind-syms-set [pq]
  (set (pquery-syms (:qfind pq))))

(defn qwhere-syms [pq]
  (pquery-syms (:qwhere pq)))

(defn qwhere-syms-set [pq]
  (set (qwhere-syms pq)))

(defn qwhere-sym? [pq sym]
  (get (qwhere-syms-set pq) sym))

(defn qin-syms [pq]
  (->> (pquery-syms (:qin pq))
       (remove #(= '$ %))
       vec))

(defn qin-syms-set [pq]
  (set (pquery-syms (:qin pq))))

(defn qfind-sym? [pq sym]
  (get (qfind-syms-set pq) sym))

(defn qin-sym? [pq sym]
  (get (qin-syms-set pq) sym))

(defn resolve-input [pq sym]
  (let [input (get
                (->> pq
                     qin-syms
                     (map (fn [input sym]
                            [sym input])
                       (:inputs pq))
                     (into {}))
                sym)]
    input
    #_(when input
        (cond
          (sym-collection-binding? pq sym)
          (map #(if (ident-sym? pq sym)
                  (ident->entity-sql-str %)
                  %)
            input)
          (ident-sym? pq sym)
          (ident->entity-sql-str input)

          :else input))))

(defn ident-sym? [pq sym]
  (get
    (->> pq
         :qwhere
         (map :pattern)
         (remove nil?)
         (map (fn [pat]
                (cond
                  ;; sym in entity position is an ident sym
                  (= (-> pat
                         first
                         :symbol)
                     sym)
                  (-> pat first :symbol)

                  ;; value in attr position is a ref attr, therefore
                  ;; symbol in 3rd position is an ident sym
                  (ref-attr?
                    (:box/schema pq)
                    (-> pat second :value))
                  (-> pat (nth 2) :symbol)
                  
                  ;; Symbol in attr position references an input that
                  ;; is a ref attr. If input is a collection then
                  ;; check the first value in collection
                  (ref-attr?
                    (:box/schema pq)
                    (resolve-input pq (-> pat second :symbol)))
                  (-> pat (nth 2) :symbol))))
         (remove nil?)
         set)
    sym))

(defn sym-collection-binding? [pq sym]
  (get
    (->> pq
         :qin
         (filter :binding)
         (map :binding)
         (map :variable)
         (map :symbol)
         set)
    sym))

(defn extract-row-and-pull [sql-row pq find-type]
  (let [row (->> pq
                 :qfind
                 ((fn [qfind]
                    (if (:elements qfind)
                      (:elements qfind)
                      [(:element qfind)])))
                 (mapv (fn [{:keys [symbol variable pattern] :as el}]
                         (let [symbol (or symbol (:symbol variable))
                               sym-ident? (ident-sym? pq symbol)
                               row-value (get sql-row (str symbol))]
                           {:row (if sym-ident?
                                   (sql-entity->ident row-value)
                                   row-value)
                            :pattern pattern
                            :symbol symbol}))))]
    (if (get #{:collection :single-scalar} find-type)
      (first row)
      row)))

(defn format-results-pull [sql-res pquery find-type]
  (if (= :single-scalar find-type)
    (extract-row-and-pull (first sql-res) pquery find-type)
    (->> sql-res
         (mapv (fn [row]
                 (extract-row-and-pull row pquery find-type))))))

(defn find-clauses [query]
  (->> query
       (drop 1)
       (take-while #(not (get #{:in :where} %)))))

(defn find-type [query]
  (try
    (let [query (if (map? query)
                  (:dq query)
                  query)
          clauses (find-clauses query)
          first-clause (and (sequential? clauses)
                            (first clauses))]
      (cond
        (and (vector? first-clause)
             (= '... (second first-clause)))
        :collection
        
        (and
          (sequential? first-clause)
          (vector? (first first-clause)))
        :single-tuple

        (and
          (sequential? first-clause)
          (= '. (second first-clause)))
        :single-scalar

        :else :relation))
    (catch #?(:clj Exception :cljs js/Error) e
      (anom/throw-anom
        (anom/from-err e {:var #'find-type})))))

(<defn <replace-pulls [{:keys [::bsql/db
                               ::bsql/<exec]
                        :as conn}
                       sym->input
                       formatted-results]


  (loop [results formatted-results
         out []]
    (if (empty? results)
      out
      (let [result (first results)

            pattern (or (-> result :pattern :value)
                        (get sym->input (-> result :pattern :symbol)))

            value (cond
                    (vector? result)
                    (<! (<replace-pulls conn sym->input result))

                    pattern
                    ;; Inefficient, should batch pulls
                    (<! (<pull conn pattern (:row result)))

                    :else (:row result))]
        (recur
          (if (anom/? value)
            nil
            (rest results))
          (if (anom/? value)
            value
            (cond
              (not value) out
              
              (and
                (sequential? value)
                :fix-go-and ; https://clojure.atlassian.net/browse/ASYNC-91#icft=ASYNC-91
                (empty? value))
              out

              :else (conj
                      out
                      value))))))))


;; ============

(defn sym->sql-var [pq sym & [override]]
  (try
    (clause-sql-var
      (get-in
        pq
        [:sym->clause-idx+eav-type sym])
      override)
    (catch #?(:clj Exception :cljs js/Error) e nil)))

(defn eav-clause? [{:keys [pattern]}]
  (boolean pattern))

(defn fn-clause? [{:keys [fn]}]
  (boolean fn))

(defn where-clause-idx [pq where-clause]
  (get
    (->> pq
         :join-clauses
         (map-indexed
           (fn [i wc]
             [wc i]))
         (into {}))
    where-clause))

(defn eav-clause-sql-maps [pq where-clause]
  (->> where-clause
       :pattern
       (map-indexed
         (fn [pat-part-idx pat-part]
           (cond
             ;; Query input symbol
             (and (:symbol pat-part)
                  (qin-sym? pq (:symbol pat-part)))
             (let [sym (:symbol pat-part)
                   input (resolve-input pq sym)
                   idsym? (ident-sym? pq sym)
                   sql-var (sym->sql-var pq sym)]
               (into
                 (if (sym-collection-binding? pq sym)
                   {:sql (str
                           "("
                           (->> input
                                (map (fn [_]
                                       (str sql-var "=?")))
                                (interpose " OR ")
                                (apply str))
                           ")")
                    :args (->> input
                               (map (fn [v]
                                      (if idsym?
                                        (ident->entity-sql-str v)
                                        (val->sql-val v)))))}
                   {:sql (str sql-var "=?")
                    :args [(if idsym?
                             (ident->entity-sql-str input)
                             (val->sql-val input))]})))
             
             ;; Hard coded value where clause value, usually an
             ;; attribute
             (not (nil? (:value pat-part)))
             {:sql (str "c" (where-clause-idx pq where-clause) "."
                        (condp = pat-part-idx
                          0 "ENTITY"
                          1 "ATTR"
                          2 "VALUE")
                        "=?")
              :args [(val->sql-val
                       (if (= 0 pat-part-idx)
                         (ident->entity-sql-str (:value pat-part))
                         (:value pat-part)))]})))
       (remove nil?)
       #_(map (fn [sql-map]
                (prepend-sql
                  (when (= 'not (:modifier where-clause))
                    "NOT ")
                  sql-map)))))

(defn infix-op-fn->sql-map [pq {:keys [fn args] :as where-clause}]
  (let [[first-sym
         sec-sym
         :as syms]
        (->> where-clause
             :args
             (map :symbol))

        [first-sql-var
         sec-sql-var]
        (map #(sym->sql-var pq %) syms)

        [first-input
         sec-input]
        (map #(resolve-input pq %) syms)

        [first-col-binding?
         sec-col-binding?]
        (map #(sym-collection-binding? pq %) syms)

        [first-ident?
         sec-ident?]
        (map #(ident-sym? pq %) syms)

        first-input-count (if first-col-binding?
                            (count first-input)
                            1)

        sec-input-count (if sec-col-binding?
                          (count sec-input)
                          1)

        el-count (* first-input-count
                   sec-input-count)

        op (-> fn :symbol str)

        perms
        (for [x (repeat first-input-count
                  (or first-sql-var "?"))
              y (repeat sec-input-count
                  (or sec-sql-var "?"))]
          [x y])

        args
        (for [x (if first-col-binding?
                  (if first-ident?
                    (ident->entity-sql-str first-input)
                    first-input)
                  [(if first-ident?
                     (ident->entity-sql-str first-input)
                     first-input)])
              y (if sec-col-binding?
                  (if sec-ident?
                    (ident->entity-sql-str sec-input)
                    sec-input)
                  [(if sec-ident?
                     (ident->entity-sql-str sec-input)
                     sec-input)])]
          [x y])]

    ;; weird undeclared var here for map (fn [...]) -- fn args always
    ;; trigger a warning


    (into
      {:sql (str
              "("
              (->> perms
                   (map #(let [l (first %)
                               r (second %)]
                           (str l " " op " " r)))
                   #_(map
                       (fn [x]
                         (let [l (first x)
                               r (second x)]
                           (str l " " op " " r))))
                   (interpose " OR ")
                   (apply str))
              ")")
       :args (->> args
                  (map #(remove nil? %))
                  flatten)})))

(defn sql-like-op-fn->sql-map [pq where-clause]
  (let [[var-sym test-sym]
        (->> where-clause
             :args
             (map :symbol))

        sql-var (sym->sql-var pq var-sym)
        test-input (resolve-input pq test-sym)

        col-binding? (sym-collection-binding? pq test-sym)]

    {:sql (str
            "("
            (->> (if col-binding?
                   test-input
                   [test-input])
                 (map (fn [_]
                        (str sql-var
                             " LIKE ?")))
                 (interpose " OR ")
                 (apply str))
            ")")
     :args (->> (if col-binding?
                  test-input
                  [test-input])
                (map val->sql-val))}))

(defn fn-clause-sql-map [pq qwhere-fn]
  (let [fn-sym (-> qwhere-fn :fn :symbol)]
    (cond
      (get '#{= < <= > >= !=} fn-sym)
      (infix-op-fn->sql-map pq qwhere-fn)
      (= 'sql-like fn-sym)
      (sql-like-op-fn->sql-map pq qwhere-fn)
      :else (ks/throw-anom
              {:desc "Unknown query function sym"
               :fn-sym fn-sym}))))

(defn dp-not? [o]
  (instance? #?(:cljs dp/Not
                :clj (ks/throw-anom
                       {:desc "datascript.parser/Not problem"}))
    o))

(defn normalize-qwhere-clause [clause]
  (if (dp-not? clause)
    (->> clause
         :clauses
         (map #(assoc % :modifier 'not)))
    [clause]))

(defn where-clause-sql-map [pq]
  (->> pq
       :where-group
       (map
         (fn [where-clause]
           (cond
             (eav-clause? where-clause)
             (eav-clause-sql-maps pq where-clause)
             (fn-clause? where-clause)
             [(fn-clause-sql-map pq where-clause)])))
       (reduce concat)
       (join-sql-maps " AND ")
       #_(append-sql " AND c0.ENTITY != c1.VALUE")))

(defn where-sql-map [pq]
  (let [sm (where-clause-sql-map pq)]
    (when sm
      (prepend-sql "\nWHERE " sm))))

(defn from-sql-map
  "Takes a parsed query and returns the `FROM` clause of an SQLite query"
  [pq]
  (try
    (->> pq
         :qwhere
         (mapcat normalize-qwhere-clause)
         (filter join-clause?)
         (drop 1)
         (map-indexed
           (fn [b-clause-idx {:keys [pattern modifier]}]
             (let [b-clause-idx (inc b-clause-idx)]
               (->> [{:sql (str "LEFT JOIN datoms AS c" b-clause-idx)}
                     {:sql (->> pattern
                                (map :symbol)
                                (map-indexed (fn [i o] [i o]))
                                (remove #(qin-sym? pq (second %)))
                                (filter second)
                                (remove
                                  (fn [[_ sym]]
                                    (= b-clause-idx
                                       (clause-idx pq sym))))
                                (map (fn [[i sym]]
                                       (str
                                         (sym->sql-var pq sym)
                                         (if (= 'not modifier)
                                           #_"!="
                                           "="
                                           "=")
                                         "c" b-clause-idx "." (eav-type->sql-column-name
                                                                (idx->eav-type i)))))
                                (interpose " AND ")
                                (apply str))}]
                    (join-sql-maps " ON ")))))
         (join-sql-maps "\n")
         (prepend-sql "FROM datoms AS c0\n"))
    (catch #?(:clj Exception :cljs js/Error) e
      (anom/throw-anom
        (anom/from-err
          e
          {:var #'from-sql-map})))))

(defn select-sql-map [pq]
  (try
    (let [find-sql-maps
          (->> pq
               qfind-return-value-syms
               (map (fn [sym]
                      {:sql (str (sym->sql-var pq sym)
                                 " AS "
                                 "\"" sym "\"")})))

          sort-sql-maps
          (->> pq
               :sort
               (map first)
               (map (fn [sym]
                      {:sql (str
                              #_"CAST("
                              (sym->sql-var pq sym)
                              #_" AS TEXT)"
                              " AS "
                              "\"" sym "\"")})))]

      (->> (concat
             find-sql-maps
             sort-sql-maps)
           (join-sql-maps ", ")
           (prepend-sql "SELECT DISTINCT ")
           #_(prepend-sql "SELECT DISTINCT c0.ENTITY as c0ent, c0.ATTR as c0attr, c0.VALUE as c0val,
c1.ENTITY as c1ent, c1.ATTR as c1attr, c1.VALUE as c1val,")))

    (catch #?(:clj Exception :cljs js/Error) e
      (anom/throw-anom
        (anom/from-err
          e
          {:var #'select-sql-map})))))

(defn order-by-sql-map [pq]
  (when (:sort pq)
    (let [sql-map (->> pq
                       :sort
                       (map (fn [[sym dir-key]]
                              (when-let [sql-var (sym->sql-var pq sym)]
                                {:sql (str
                                        #_sql-var
                                        #_" "
                                        "\"" sym "\" "
                                        (condp = dir-key
                                          :asc "ASC"
                                          :desc "DESC"))})))
                       (join-sql-maps ", "))]
      (when sql-map
        (prepend-sql "ORDER BY " sql-map)))))

(defn limit-offset-sql-map [{:keys [limit offset]}]
  (when (or limit offset)
    {:sql "LIMIT ? OFFSET ?"
     :args [(if limit
              limit
              -1)
            (if offset
              offset
              0)]}))

(defn not-pquery? [pq]
  (-> pq
      :where-group
      first
      :modifier
      (= 'not)))

(defn query-sql-vec [pqs]
  (let [sql-vec
        (->> pqs
             (reduce
               (fn [accu-sql-map pq]
                 (let [sm (select-sql-map pq)
                       fm (from-sql-map pq)
                       wm (where-sql-map pq)
                       om (order-by-sql-map pq)
                       lm (limit-offset-sql-map pq)
                       sql-map (join-sql-maps
                                 " "
                                 [sm fm wm #_om #_lm])]
                   (if (empty? accu-sql-map)
                     sql-map
                     (join-sql-maps
                       (if (not-pquery? pq)
                         "\nEXCEPT\n"
                         "\nINTERSECT\n")
                       [accu-sql-map sql-map]))))
               {})
             ((fn [sql-map]
                (let [om (->> pqs
                              (map order-by-sql-map)
                              (filter identity)
                              first)
                      lm (limit-offset-sql-map (first pqs))]
                  (join-sql-maps
                    " "
                    [sql-map om lm]))))
             to-sql-vec)]
    #_(println (first sql-vec))
    #_(ks/pp (rest sql-vec))
    sql-vec))

(defn calc-pquery-meta [pquery]
  (let [{:keys [qfind qwhere] :as ds-query} pquery
        join-clauses (->> qwhere
                          (mapcat normalize-qwhere-clause)
                          (filter join-clause?))
        sym->clause-idx+eav-type
        (->> join-clauses
             (reduce-indexed
               (fn [accu join-clause jcidx]
                 (let [{:keys [pattern]} join-clause]
                   (merge
                     accu
                     (->> pattern
                          (map-indexed
                            (fn [pidx {:keys [symbol] :as part}]
                              (if (or
                                    (not symbol)
                                    (get accu symbol))
                                nil
                                [symbol [jcidx
                                         (idx->eav-type pidx)]])))
                          (remove nil?)
                          (into {})))))
               {}))
        pq (merge
             ds-query
             {:join-clauses join-clauses
              :sym->clause-idx+eav-type
              sym->clause-idx+eav-type
              ;; calculate the table alias offset (c0, c1, ...) for
              ;; each symbol found in the datalog query.
              
              :in-syms-set (qin-syms-set ds-query)
              :find-syms-set (qfind-syms-set ds-query)})]
    pq))

(defn input-index-sym [pquery index]
  (-> pquery
      qin-syms
      (nth index)))

(defn convert-entity-idents
  "Convert any inputs marked as an entity input (based on query
  structure) to vector ident format"
  [{:keys [:box/schema] :as pq} inputs]
  (->> inputs
       (map-indexed
         (fn [i input]
           (let [sym (input-index-sym pq i)
                 idsym? (ident-sym? pq sym)
                 coll-binding? (sym-collection-binding? pq sym)]
             (if idsym?
               (if coll-binding?
                 (map #(to-ident schema %) input)
                 (to-ident schema input))
               input))))))

(defn convert-map-or-vec-query [q]
  (let [query-vec (cond
                    (vector? q) q
                    (map? q) (:dq q))
        pq (cond
             (vector? q) {}
             (map? q) (dissoc q :dq))]

    (merge
      pq
      (dsq/parse-query query-vec))))

(defn process-query [pq inputs]
  (let [inputs (convert-entity-idents pq inputs)
        where-groups
        (->> pq
             :qwhere
             (mapcat normalize-qwhere-clause)
             (partition-by :modifier))

        pqs (->> where-groups
                 (map (fn [where-group]
                        (merge
                          pq
                          {:where-group where-group})))
                 (map calc-pquery-meta)
                 (map #(assoc % :inputs inputs)))]
    pqs))

(<defn <query
  [query
   {:keys [::bsql/db
           ::bsql/<exec
           ::bsql/debug-explain-query?
           :box/schema]
    :as conn}
   & inputs]

  (when-not conn
    (anom/throw-anom
      {:desc "Invalid conn"
       :var #'<query}))
  
  (let [pquery (convert-map-or-vec-query query)
        
        pquery (merge
                 pquery
                 {:box/schema schema})
        
        pqs (process-query pquery inputs)

        #_#_
        _ (ks/spy pquery)
        
        sql-vec (query-sql-vec pqs)
        #_#_
        _ (ks/spy sql-vec)
        #_#_
        _ (do
            (println (first sql-vec))
            (ks/spy (rest sql-vec)))

        sql-res (<! (<exec db sql-vec))
        #_#_
        _ (ks/spy sql-res)

        _ (when debug-explain-query?
            (let [sql-vec
                  (concat
                    [(str "EXPLAIN QUERY PLAN " (first sql-vec))]
                    (rest sql-vec))]
              (ks/spy (<! (<exec db sql-vec)))))

        _ (anom/throw-if-anom sql-res)

        formatted-results
        (format-results-pull
          sql-res
          (first pqs)
          (find-type query))
        #_#_
        _ (ks/spy formatted-results)

        results
        (<! (<replace-pulls
              conn
              (->> pqs
                   first
                   qin-syms
                   (map (fn [input sym]
                          [sym input])
                     (:inputs (first pqs)))
                   (into {}))
              formatted-results))]
    (vec (distinct results))))

;; Transact

(declare normalize-txfs)

(defn ent->txfs [schema ent]
  (let [ident (resolve-ident schema ent)]
    (->> ent
         (mapcat (fn [[k v]]
                   (try
                     (cond
                       (one-ref-attr? schema k)
                       (concat
                         [[:box/assoc ident k (to-ident schema v)]]
                         (normalize-txfs schema [v]))
                       (many-ref-attr? schema k)
                       (concat
                         (->> v
                              (map (fn [v]
                                     [:box/assoc ident k (to-ident schema v)])))
                         (normalize-txfs schema v))
                       :else
                       [[:box/assoc ident k v]])
                     (catch #?(:clj Exception :cljs js/Error) e
                       (ks/throw-anom
                         {:desc "Couldn't convert entity key value into txf"
                          ::ent ent
                          ::key k
                          ::value v}))))))))

(defn normalize-txfs [schema txfs-or-ents]
  (->> txfs-or-ents
       (mapcat
         (fn [txf-or-ent]
           (if (vector? txf-or-ent)
             [txf-or-ent]
             (ent->txfs schema txf-or-ent))))))

(defn delete-txf-sql-map [schema
                          [op ident attr val :as txf]]
  (when-not (many-ref-attr? schema attr)
    (let [res
          {:sql (str
                  "("
                  (->> ["ENTITY=?"
                        "ATTR=?"
                        #_"VALUE=?"]
                       (interpose " AND ")
                       (apply str))
                  ")")
           :args (->> [(ident->entity-sql-str ident)
                       attr
                       #_(if (ref-attr? schema attr)
                           (ident->entity-sql-str val)
                           val)]
                      (map val->sql-val))}]
      res)))

(defn txfs-delete-sql-vec [schema txfs]
  (->> txfs
       (map #(delete-txf-sql-map schema %))
       (remove nil?)
       (join-sql-maps " OR ")
       (prepend-sql "DELETE FROM datoms WHERE ")
       to-sql-vec))

(defn insert-txf-sql-map [schema [op ident attr val :as txf]]
  (let [ref-one? (one-ref-attr? schema attr)
        ref-many? (many-ref-attr? schema attr)
        ref? (or ref-one? ref-many?)

        res {:sql (str
                    "("
                    (->> ["?"
                          "?"
                          "?"
                          "?"
                          "?"
                          "?"]
                         (interpose ", ")
                         (apply str))
                    ")")
             :args (->> [(ident->entity-sql-str ident)
                         attr
                         (if ref?
                           (ident->entity-sql-str val)
                           (val->sql-val val))
                         (to-sql-bool ref?)
                         (to-sql-bool ref-many?)
                         (when-not ref?
                           (val->data-type val))]
                        (map val->sql-val)
                        doall)}]
    res))

(defn txfs-insert-sql-vec [schema txfs]
  (->> txfs
       (map #(insert-txf-sql-map schema %))
       (join-sql-maps ", ")
       (prepend-sql "INSERT INTO datoms
(ENTITY,
 ATTR,
 VALUE,
 IS_REF,
 IS_REF_MANY,
 DATATYPE) VALUES \n")
       (to-sql-vec)))

(<defn <transact-update [{:keys [:box/schema
                                 ::bsql/<batch-exec
                                 ::bsql/<exec
                                 ::bsql/db]
                          :as conn}
                         txfs]
  (let [idents-txfs (->> txfs
                         (map second)
                         distinct
                         (mapv
                           (fn [ident]
                             [:box/assoc ident
                              (ident-key ident)
                              (ident-val ident)])))

        txfs (distinct (into txfs idents-txfs))

        ;; No multiple upsert in sql so need to delete all then
        ;; insert. Investigate perf on upsert via <batch-exec
        delete-vec (txfs-delete-sql-vec schema txfs)

        #_#_
        _ (ks/spy delete-vec)

        insert-vec (txfs-insert-sql-vec schema txfs)

        #_#_
        _ (ks/spy insert-vec)

        res (<! (<batch-exec
                  db
                  [delete-vec
                   insert-vec]))]
    res))

(defn delete-sql-map [_ [_ ident]]
  (let [sql-val (ident->entity-sql-str ident)]
    {:sql "ENTITY=? OR (VALUE=? AND IS_REF=true)"
     :args [sql-val sql-val]}))

(defn dissoc-sql-map [schema [_ ident attr ent-or-ident-or-nil]]
  (try
    (let [ent-val (ident->entity-sql-str ident)
          attr-val (val->sql-val attr)
          value-val (if (and (ref-attr? schema attr)
                             ent-or-ident-or-nil)
                      (ident->entity-sql-str
                        (to-ident schema ent-or-ident-or-nil))
                      ent-or-ident-or-nil)]

      {:sql (str
              "("
              "ENTITY=? AND ATTR=?"
              (when value-val
                " AND VALUE=?")
              ")")
       :args
       (into
         [ent-val attr-val]
         (when value-val
           [value-val]))})
    (catch #?(:clj Exception :cljs js/Error) e
      (anom/throw-anom
        {:desc "Error generating dissoc sql map"
         ::ident ident
         ::attr attr
         ::ent-or-ident-or-nil ent-or-ident-or-nil}))))

(<defn <transact-delete [{:keys [:box/schema
                                 ::bsql/<batch-exec
                                 ::bsql/<exec
                                 ::bsql/db]
                          :as conn}
                         txfs]
  (let [sql-map (->> txfs
                     (map (fn [[op & _ :as txf]]
                            (condp = op
                              :box/delete
                              (delete-sql-map schema txf)
                              :box/dissoc
                              (dissoc-sql-map schema txf))))
                     (join-sql-maps " OR ")
                     (prepend-sql "DELETE FROM datoms WHERE "))

        res (<? (<exec db (to-sql-vec sql-map)))]
    res))

(defn convert-txf-idents [schema txfs]
  (->> txfs
       (mapv (fn [[op ident attr val & rest :as txf]]
               (try
                 (into
                   [op
                    (if (map? ident)
                      (to-ident schema ident)
                      ident)
                    (when attr
                      attr)
                    (if (and (ref-attr? schema attr)
                             (= op :box/assoc))
                      (to-ident schema val)
                      val)]
                   rest)
                 (catch #?(:clj Exception :cljs js/Error) e
                   (ks/throw-anom
                     {:desc "Error converting txf idents"
                      ::txf txf
                      ::schema schema})))))))

(<defn <transact [{:keys [:box/schema
                          ::bsql/<batch-exec
                          ::bsql/<exec
                          ::bsql/db]
                   :as conn}
                  txfs-or-ents]
  (when-not (and conn txfs-or-ents)
    (anom/throw-anom
      {:desc "Incorrect <transact call"
       ::conn conn
       ::txfs-or-ents txfs-or-ents}))
  (let [txfs (normalize-txfs schema txfs-or-ents)

        #_#_
        _ (ks/spy txfs)
        
        txfs (convert-txf-idents schema txfs)

        #_#_
        _ (ks/spy txfs)
        _ (throw-if-invalid-txfs txfs)

        groups (partition-by
                 #(get #{:box/delete :box/dissoc} (first %))
                 txfs)]
    
    (doseq [txfs groups]
      (if (get #{:box/delete :box/dissoc} (ffirst txfs))
        (<? (<transact-delete
              conn
              txfs))
        (<? (<transact-update
              conn
              txfs))))
    txfs))


;; Init & conn

(defn create-datoms-sql []
  "CREATE TABLE IF NOT EXISTS datoms(
     ENTITY text,
     ATTR text,
     VALUE blob,
     IS_REF boolean,
     IS_REF_MANY boolean,
     DATATYPE boolean
   );")

(defn create-indexes-sql-vecs []
  [["CREATE INDEX IF NOT EXISTS entity_index ON datoms(ENTITY)"]
   ["CREATE INDEX IF NOT EXISTS attr_index ON datoms(ATTR)"]
   ["CREATE INDEX IF NOT EXISTS attr_value_index ON datoms(ATTR, VALUE)"]])

(defn db-open-options [schema]
  (let [db-name (or (:box/db-name schema)
                    ":memory:")]
    {:name db-name}))

(def default-schema
  {:box/db-name ":memory:"
   :box/attrs
   [{:box/attr :box/id
     :box/ident? true
     :box/value-type :box/string}
    {:box/attr :box/ref
     :box/ref? true
     :box/cardinality :box/cardinality-one}
    {:box/attr :box/refs
     :box/ref? true
     :box/cardinality :box/cardinality-many}
    {:box/attr :box/updated-ts
     :box/value-type :box/long}
    {:box/attr :box/created-ts
     :box/value-type :box/long}
    {:box/attr :box/deleted-ts
     :box/value-type :box/long}
    {:box/attr :box/archived-ts
     :box/value-type :box/long}]})

(<defn <conn [{:keys [::bsql/<open-db
                      ::bsql/<exec
                      ::bsql/<batch-exec
                      :box/schema]
               :as opts}]
  (let [schema
        (merge
            default-schema
            schema
            {:box/attrs
             (vec
               (concat
                 (:box/attrs default-schema)
                 (:box/attrs schema)))})
        db (<! (<open-db (db-open-options schema)))]
    (anom/throw-if-anom
      (<! (<exec db [(create-datoms-sql)])))
    (anom/throw-if-anom
      (<! (<batch-exec db (create-indexes-sql-vecs))))
    (merge
      opts
      {::bsql/db db
       :box/schema schema})))

(defn insert-datoms-sqls [datoms]
  (->> datoms
       (map (fn [[ident-or-val attr value ref? many?]]
              [(str "INSERT INTO datoms(ENTITY, ATTR, VALUE, IS_REF, IS_REF_MANY, DATATYPE)
                     VALUES(?,?,?,?,?,?)")
               (ident->entity-sql-str ident-or-val)
               (val->sql-val attr)
               (if ref?
                 (ident->entity-sql-str value)
                 (val->sql-val value))
               (to-sql-bool ref?)
               (to-sql-bool many?)
               (val->data-type value)]))))

(defn <inspect-conn [{:keys [::bsql/db
                             ::bsql/<exec]}]
  (go
    (ks/pp (<! (<exec db ["pragma table_info(\"datoms\")"])))
    (println "DATOMS")
    (ks/pp
      (<! (<exec db ["select * from datoms"])))))

(defn <destroy-db [{:keys [::bsql/db
                           ::bsql/<exec]}]
  (go
    (ks/pp (<! (<exec db ["drop table if exists datoms"])))))


(<defn <datoms [{:keys [::bsql/db
                        ::bsql/<exec]
                 :as conn}]
  (<! (<exec db ["select * from datoms"])))

(defn <sql [{:keys [::bsql/db
                    ::bsql/<exec]}
            sql-vec
            & [filter-fn]]
  (go
    (->> (<! (<exec db sql-vec))
         (filter (or filter-fn identity))
         (ks/spy "<sql"))))



(comment

  (ks/spy
    (dsq/parse-query
      '[:find ?e
        :in $ %
        :where
        (twitter? ?e)]))

  )
