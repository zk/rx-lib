(ns rx.node.box.v2-tests
  (:require [rx.kitchen-sink :as ks]
            [rx.node.sqlite2 :as sql]
            [datascript.query :as dsq]
            [datascript.query-v3 :as dsqv3]
            [clojure.core.async :as async
             :refer [go <!]]))


(defn create-datoms-sql []
  "CREATE TABLE \"datoms\" (\"ENT\" text,\"ATR\" text,\"VAL\" text,\"REF\" boolean);")

(defn insert-datoms-sqls [datoms]
  (->> datoms
       (map (fn [[ent atr val ref?]]
              [(str "INSERT INTO datoms(ENT, ATR, VAL, REF) VALUES(?,?,?,?)")
               ent atr val ref?]))))

(defn run-test [query
                datoms]
  (sql/with-test-db
    (fn [db]
      (go
        (try
          (<! (sql/<exec db [(create-datoms-sql)]))
          (<! (sql/<batch-exec db (insert-datoms-sqls datoms)))
          (<! (sql/<exec db query))
          (catch js/Error e
            (.error js/console e)))))
    {:name ":memory:"}))

(defn test-select-all []
  (run-test
    ["select * from datoms"]
    [["zk" "name" "Zack" false]
     ["cindy" "name" "Cindy" false]
     ["zk" "friend" "cindy" true]]))

(def dq-basic-datoms
  [["sally" "age" 21]
   ["fred" "age" 42]
   ["ethel" "age" 42]
   ["fred" "likes" "pizza"]
   ["sally" "likes" "opera"]
   ["ethel" "likes" "sushi"]])

(defn test-foaf []
  (run-test
    ["SELECT r2.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
INNER JOIN datoms AS r3 ON r2.VAL = r3.ENT
WHERE 
r1.rowid != r2.rowid AND 
r2.rowid != r3.rowid AND
r1.ATR = \"name\" AND
r2.ATR = \"friend\" AND
r3.ATR = \"name\""]
    [["zk" "name" "Zack" false]
     ["cindy" "name" "Cindy" false]
     ["zk" "friend" "cindy" true]]))

(defn test-dq-basic []
  (run-test
    ["
SELECT r1.ENT AS \"?e\"
FROM datoms AS r1
WHERE
r1.ATR = \"age\" AND
r1.VAL = 42
"]
    dq-basic-datoms))

(defn test-dq-unification []
  (run-test
    ["
SELECT r1.ENT, r2.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"age\" AND
r1.VAL = 42 AND
r2.ATR = \"likes\"
"]
    dq-basic-datoms))

(defn test-dq-blanks []
  (run-test
    ["
SELECT r1.VAL
FROM datoms AS r1
WHERE
r1.ATR = \"likes\"
"]
    dq-basic-datoms))

(def movies-db
  [[-100 "person/name" "James Cameron"]
   [-100 "person/born" "Sun Aug 15 14:00:00 HST 1954"]
   [-101 "person/name" "Arnold Schwarzenegger"]
   [-101 "person/born" "Tue Jul 29 14:00:00 HST 1947"]
   [-102 "person/name" "Linda Hamilton"]
   [-102 "person/born" "Tue Sep 25 14:00:00 HST 1956"]
   [-103 "person/name" "Michael Biehn"]
   [-103 "person/born" "Mon Jul 30 14:00:00 HST 1956"]
   [-104 "person/name" "Ted Kotcheff"]
   [-104 "person/born" "Mon Apr 06 13:30:00 HST 1931"]
   [-105 "person/name" "Sylvester Stallone"]
   [-105 "person/born" "Fri Jul 05 13:30:00 HST 1946"]
   [-106 "person/name" "Richard Crenna"]
   [-106 "person/born" "Mon Nov 29 13:30:00 HST 1926"]
   [-106 "person/death" "Thu Jan 16 14:00:00 HST 2003"]
   [-107 "person/name" "Brian Dennehy"]
   [-107 "person/born" "Fri Jul 08 13:30:00 HST 1938"]
   [-108 "person/name" "John McTiernan"]
   [-108 "person/born" "Sun Jan 07 14:00:00 HST 1951"]
   [-109 "person/name" "Elpidia Carrillo"]
   [-109 "person/born" "Tue Aug 15 14:00:00 HST 1961"]
   [-110 "person/name" "Carl Weathers"]
   [-110 "person/born" "Tue Jan 13 14:00:00 HST 1948"]
   [-111 "person/name" "Richard Donner"]
   [-111 "person/born" "Wed Apr 23 13:30:00 HST 1930"]
   [-112 "person/name" "Mel Gibson"]
   [-112 "person/born" "Mon Jan 02 14:00:00 HST 1956"]
   [-113 "person/name" "Danny Glover"]
   [-113 "person/born" "Sun Jul 21 13:30:00 HST 1946"]
   [-114 "person/name" "Gary Busey"]
   [-114 "person/born" "Fri Jul 28 14:30:00 HDT 1944"]
   [-115 "person/name" "Paul Verhoeven"]
   [-115 "person/born" "Sun Jul 17 13:30:00 HST 1938"]
   [-116 "person/name" "Peter Weller"]
   [-116 "person/born" "Mon Jun 23 14:00:00 HST 1947"]
   [-117 "person/name" "Nancy Allen"]
   [-117 "person/born" "Fri Jun 23 14:00:00 HST 1950"]
   [-118 "person/name" "Ronny Cox"]
   [-118 "person/born" "Fri Jul 22 13:30:00 HST 1938"]
   [-119 "person/name" "Mark L. Lester"]
   [-119 "person/born" "Mon Nov 25 13:30:00 HST 1946"]
   [-120 "person/name" "Rae Dawn Chong"]
   [-120 "person/born" "Mon Feb 27 14:00:00 HST 1961"]
   [-121 "person/name" "Alyssa Milano"]
   [-121 "person/born" "Mon Dec 18 14:00:00 HST 1972"]
   [-122 "person/name" "Bruce Willis"]
   [-122 "person/born" "Fri Mar 18 14:00:00 HST 1955"]
   [-123 "person/name" "Alan Rickman"]
   [-123 "person/born" "Wed Feb 20 13:30:00 HST 1946"]
   [-124 "person/name" "Alexander Godunov"]
   [-124 "person/born" "Sun Nov 27 14:00:00 HST 1949"]
   [-124 "person/death" "Wed May 17 14:00:00 HST 1995"]
   [-125 "person/name" "Robert Patrick"]
   [-125 "person/born" "Tue Nov 04 14:00:00 HST 1958"]
   [-126 "person/name" "Edward Furlong"]
   [-126 "person/born" "Mon Aug 01 14:00:00 HST 1977"]
   [-127 "person/name" "Jonathan Mostow"]
   [-127 "person/born" "Mon Nov 27 14:00:00 HST 1961"]
   [-128 "person/name" "Nick Stahl"]
   [-128 "person/born" "Tue Dec 04 14:00:00 HST 1979"]
   [-129 "person/name" "Claire Danes"]
   [-129 "person/born" "Wed Apr 11 14:00:00 HST 1979"]
   [-130 "person/name" "George P. Cosmatos"]
   [-130 "person/born" "Fri Jan 03 13:30:00 HST 1941"]
   [-130 "person/death" "Mon Apr 18 14:00:00 HST 2005"]
   [-131 "person/name" "Charles Napier"]
   [-131 "person/born" "Sat Apr 11 13:30:00 HST 1936"]
   [-131 "person/death" "Tue Oct 04 14:00:00 HST 2011"]
   [-132 "person/name" "Peter MacDonald"]
   [-133 "person/name" "Marc de Jonge"]
   [-133 "person/born" "Tue Feb 15 14:00:00 HST 1949"]
   [-133 "person/death" "Wed Jun 05 14:00:00 HST 1996"]
   [-134 "person/name" "Stephen Hopkins"]
   [-135 "person/name" "Ruben Blades"]
   [-135 "person/born" "Thu Jul 15 14:00:00 HST 1948"]
   [-136 "person/name" "Joe Pesci"]
   [-136 "person/born" "Mon Feb 08 14:30:00 HDT 1943"]
   [-137 "person/name" "Ridley Scott"]
   [-137 "person/born" "Mon Nov 29 13:30:00 HST 1937"]
   [-138 "person/name" "Tom Skerritt"]
   [-138 "person/born" "Thu Aug 24 13:30:00 HST 1933"]
   [-139 "person/name" "Sigourney Weaver"]
   [-139 "person/born" "Fri Oct 07 14:00:00 HST 1949"]
   [-140 "person/name" "Veronica Cartwright"]
   [-140 "person/born" "Tue Apr 19 14:00:00 HST 1949"]
   [-141 "person/name" "Carrie Henn"]
   [-142 "person/name" "George Miller"]
   [-142 "person/born" "Fri Mar 02 14:30:00 HDT 1945"]
   [-143 "person/name" "Steve Bisley"]
   [-143 "person/born" "Tue Dec 25 14:00:00 HST 1951"]
   [-144 "person/name" "Joanne Samuel"]
   [-145 "person/name" "Michael Preston"]
   [-145 "person/born" "Fri May 13 13:30:00 HST 1938"]
   [-146 "person/name" "Bruce Spence"]
   [-146 "person/born" "Sun Sep 16 14:30:00 HDT 1945"]
   [-147 "person/name" "George Ogilvie"]
   [-147 "person/born" "Wed Mar 04 13:30:00 HST 1931"]
   [-148 "person/name" "Tina Turner"]
   [-148 "person/born" "Sat Nov 25 13:30:00 HST 1939"]
   [-149 "person/name" "Sophie Marceau"]
   [-149 "person/born" "Wed Nov 16 14:00:00 HST 1966"]
   [-200 "movie/title" "The Terminator"]
   [-200 "movie/year" 1984]
   [-200 "movie/director" -100]
   [-200 "movie/cast" -101]
   [-200 "movie/cast" -102]
   [-200 "movie/cast" -103]
   [-200 "movie/sequel" -207]
   [-201 "movie/title" "First Blood"]
   [-201 "movie/year" 1982]
   [-201 "movie/director" -104]
   [-201 "movie/cast" -105]
   [-201 "movie/cast" -106]
   [-201 "movie/cast" -107]
   [-201 "movie/sequel" -209]
   [-202 "movie/title" "Predator"]
   [-202 "movie/year" 1987]
   [-202 "movie/director" -108]
   [-202 "movie/cast" -101]
   [-202 "movie/cast" -109]
   [-202 "movie/cast" -110]
   [-202 "movie/sequel" -211]
   [-203 "movie/title" "Lethal Weapon"]
   [-203 "movie/year" 1987]
   [-203 "movie/director" -111]
   [-203 "movie/cast" -112]
   [-203 "movie/cast" -113]
   [-203 "movie/cast" -114]
   [-203 "movie/sequel" -212]
   [-204 "movie/title" "RoboCop"]
   [-204 "movie/year" 1987]
   [-204 "movie/director" -115]
   [-204 "movie/cast" -116]
   [-204 "movie/cast" -117]
   [-204 "movie/cast" -118]
   [-205 "movie/title" "Commando"]
   [-205 "movie/year" 1985]
   [-205 "movie/director" -119]
   [-205 "movie/cast" -101]
   [-205 "movie/cast" -120]
   [-205 "movie/cast" -121]
   [-205
    "/trivia"
    "In 1986, a sequel was written with an eye to having\n  John McTiernan direct. Schwarzenegger wasn't interested in reprising\n  the role. The script was then reworked with a new central character,\n  eventually played by Bruce Willis, and became Die Hard"]
   [-206 "movie/title" "Die Hard"]
   [-206 "movie/year" 1988]
   [-206 "movie/director" -108]
   [-206 "movie/cast" -122]
   [-206 "movie/cast" -123]
   [-206 "movie/cast" -124]
   [-207 "movie/title" "Terminator 2: Judgment Day"]
   [-207 "movie/year" 1991]
   [-207 "movie/director" -100]
   [-207 "movie/cast" -101]
   [-207 "movie/cast" -102]
   [-207 "movie/cast" -125]
   [-207 "movie/cast" -126]
   [-207 "movie/sequel" -208]
   [-208 "movie/title" "Terminator 3: Rise of the Machines"]
   [-208 "movie/year" 2003]
   [-208 "movie/director" -127]
   [-208 "movie/cast" -101]
   [-208 "movie/cast" -128]
   [-208 "movie/cast" -129]
   [-209 "movie/title" "Rambo: First Blood Part II"]
   [-209 "movie/year" 1985]
   [-209 "movie/director" -130]
   [-209 "movie/cast" -105]
   [-209 "movie/cast" -106]
   [-209 "movie/cast" -131]
   [-209 "movie/sequel" -210]
   [-210 "movie/title" "Rambo III"]
   [-210 "movie/year" 1988]
   [-210 "movie/director" -132]
   [-210 "movie/cast" -105]
   [-210 "movie/cast" -106]
   [-210 "movie/cast" -133]
   [-211 "movie/title" "Predator 2"]
   [-211 "movie/year" 1990]
   [-211 "movie/director" -134]
   [-211 "movie/cast" -113]
   [-211 "movie/cast" -114]
   [-211 "movie/cast" -135]
   [-212 "movie/title" "Lethal Weapon 2"]
   [-212 "movie/year" 1989]
   [-212 "movie/director" -111]
   [-212 "movie/cast" -112]
   [-212 "movie/cast" -113]
   [-212 "movie/cast" -136]
   [-212 "movie/sequel" -213]
   [-213 "movie/title" "Lethal Weapon 3"]
   [-213 "movie/year" 1992]
   [-213 "movie/director" -111]
   [-213 "movie/cast" -112]
   [-213 "movie/cast" -113]
   [-213 "movie/cast" -136]
   [-214 "movie/title" "Alien"]
   [-214 "movie/year" 1979]
   [-214 "movie/director" -137]
   [-214 "movie/cast" -138]
   [-214 "movie/cast" -139]
   [-214 "movie/cast" -140]
   [-214 "movie/sequel" -215]
   [-215 "movie/title" "Aliens"]
   [-215 "movie/year" 1986]
   [-215 "movie/director" -100]
   [-215 "movie/cast" -139]
   [-215 "movie/cast" -141]
   [-215 "movie/cast" -103]
   [-216 "movie/title" "Mad Max"]
   [-216 "movie/year" 1979]
   [-216 "movie/director" -142]
   [-216 "movie/cast" -112]
   [-216 "movie/cast" -143]
   [-216 "movie/cast" -144]
   [-216 "movie/sequel" -217]
   [-217 "movie/title" "Mad Max 2"]
   [-217 "movie/year" 1981]
   [-217 "movie/director" -142]
   [-217 "movie/cast" -112]
   [-217 "movie/cast" -145]
   [-217 "movie/cast" -146]
   [-217 "movie/sequel" -218]
   [-218 "movie/title" "Mad Max Beyond Thunderdome"]
   [-218 "movie/year" 1985]
   [-218 "movie/director" -142]
   [-218 "movie/director" -147]
   [-218 "movie/cast" -112]
   [-218 "movie/cast" -148]
   [-219 "movie/title" "Braveheart"]
   [-219 "movie/year" 1995]
   [-219 "movie/director" -112]
   [-219 "movie/cast" -112]
   [-219 "movie/cast" -149]])

(defn test-edn-ch-0 []
  (run-test
    ["
SELECT r1.VAL
FROM datoms AS r1
WHERE
r1.ATR = \"movie/title\"
"]
    movies-db))

(defn test-basic-queries-ch-1-0 []
  (run-test
    ["
SELECT r1.ENT
FROM datoms AS r1
WHERE
r1.ATR = \"movie/year\" AND
r1.VAL = 1987
"]
    movies-db))

(defn test-basic-queries-ch-1-1 []
  (run-test
    ["
SELECT r1.ENT, r1.VAL
FROM datoms AS r1
WHERE
r1.ATR = \"movie/title\"
"]
    movies-db))

(defn test-basic-queries-ch-1-2 []
  (run-test
    ["
SELECT r1.VAL
FROM datoms AS r1
WHERE
r1.ATR = \"person/name\"
"]
    movies-db))

(defn test-basic-queries-ch-2-0 []
  (run-test
    ["
SELECT r1.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/year\" AND
r2.VAL = 1985
"]
    movies-db))

(defn test-basic-queries-ch-2-1 []
  (run-test
    ["
SELECT r2.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/year\" AND
r1.VAL = \"Alien\"
"]
    movies-db))

(defn test-basic-queries-ch-2-2 []
  (run-test
    ["
SELECT r3.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
INNER JOIN datoms AS r3 ON r2.VAL = r3.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/director\" AND
r3.ATR = \"person/name\" AND
r1.VAL = \"RoboCop\"
"]
    movies-db))

(defn test-basic-queries-ch-2-3 []
  (run-test
    ["
SELECT r4.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.VAL
INNER JOIN datoms AS r3 ON r2.ENT = r3.ENT
INNER JOIN datoms AS r4 ON r3.VAL = r4.ENT
WHERE
r1.ATR = \"person/name\" AND
r2.ATR = \"movie/cast\" AND
r3.ATR = \"movie/director\" AND
r4.ATR = \"person/name\" AND
r1.VAL = \"Arnold Schwarzenegger\"
"]
    movies-db))

(defn test-param-queries-ch-3-0 []
  (run-test
    ["
SELECT r2.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/year\" AND
r2.ATR = \"movie/title\" AND
r1.VAL = ?
"
     1988]
    movies-db))

(defn test-param-queries-ch-3-1 []
  (run-test
    ["
SELECT r1.VAL AS title, r2.VAL as year
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/year\" AND
(r1.VAL = ? OR r1.VAL = ? OR r1.VAL = ?)
"
     "Lethal Weapon"
     "Lethal Weapon 2"
     "Lethal Weapon 3"]
    movies-db))

(defn test-param-queries-ch-3-2 []
  (run-test
    ["
SELECT DISTINCT r5.VAL AS title
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r3.VAL
INNER JOIN datoms AS r3 ON r2.ENT = r4.VAL
INNER JOIN datoms AS r4 ON r3.ENT = r4.ENT
INNER JOIN datoms AS r5 ON r3.ENT = r5.ENT
WHERE
r1.ATR = \"person/name\" AND
r2.ATR = \"person/name\" AND
r3.ATR = \"movie/cast\" AND
r4.ATR = \"movie/director\" AND
r5.ATR = \"movie/title\" AND
r1.VAL = ? AND
r2.VAL = ?
"
     "Michael Biehn"
     "James Cameron"]
    movies-db))

(defn test-more-queries-ch-4-0 []
  (run-test
    ["
SELECT DISTINCT r2.ATR AS attr
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\"
"]
    movies-db))

(defn test-more-queries-ch-4-1 []
  (run-test
    ["
SELECT DISTINCT r3.VAL AS name
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
INNER JOIN datoms AS r3 ON r2.VAL = r3.ENT
WHERE
r1.ATR = \"movie/title\" AND
r3.ATR = \"person/name\" AND
r1.VAL = ? AND
(r2.ATR = ? OR r2.ATR = ?)
"
     "Die Hard"
     "movie/cast"
     "movie/director"]
    movies-db))

(defn test-pred-ch-5-0 []
  (run-test
    ["
SELECT DISTINCT r1.VAL AS title
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/year\" AND
r2.VAL <= ?
"
     1979]
    movies-db))

(defn test-pred-ch-5-1 []
  (run-test
    ["
SELECT DISTINCT r5.VAL AS actor
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
INNER JOIN datoms AS r3 ON r3.VAL < r2.VAL
INNER JOIN datoms AS r4 ON r3.ENT = r4.VAL
INNER JOIN datoms AS r5 ON r3.ENT = r5.ENT
WHERE
r1.ATR = \"person/name\" AND
r2.ATR = \"person/born\" AND
r3.ATR = \"person/born\" AND
r4.ATR = \"movie/cast\" AND
r5.ATR = \"person/name\" AND
r1.VAL = \"Danny Glover\" AND

"]
    movies-db))


;; ad hoc

(defn test-not-query []
  (run-test
    ["
SELECT r1.VAL
FROM datoms AS r1
INNER JOIN datoms AS r2 ON r1.ENT = r2.ENT
WHERE
r1.ATR = \"movie/title\" AND
r2.ATR = \"movie/year\" AND
r2.VAL != 1985
"]
    movies-db))

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

(defn clause-groups [join-clauses]
  (->> join-clauses
       ))

(defn eav-type->sql-column-name
  [t]
  (condp = t
    :ent "ENT"
    :atr "ATR"
    :val "VAL"))

(defn select-str [pq]
  (str "SELECT DISTINCT "
       (->> pq
            :qfind
            :elements
            (map (fn [{:keys [symbol]}]
                   (let [[clause-idx eav-type]
                         (get-in
                           pq
                           [:sym->clause-index+pos symbol])]
                     (str "c" clause-idx "."
                          (eav-type->sql-column-name eav-type)
                          " AS "
                          "\"" symbol "\""))))
            (interpose ", ")
            (apply str))))

(defn from-str [pq]
  "FROM datoms AS c0")

(defn idx->eav-type [i]
  (condp = i
    0 :ent
    1 :atr
    2 :val))

(defn join-str [pq]
  (->> pq
       :join-clauses
       (drop 1)
       (map-indexed
         (fn [b-clause-idx {:keys [pattern]}]
           (let [b-clause-idx (inc b-clause-idx)]
             (str
               "INNER JOIN datoms AS c" b-clause-idx " ON "
               
               (->> pattern
                    (map-indexed
                      (fn [i {:keys [symbol]}]
                        (if symbol
                          [symbol (idx->eav-type i)]
                          nil)))
                    (remove nil?)
                    (map (fn [[symbol b-eav-type]]
                           (let [[a-clause-idx a-eav-type]
                                 (get-in pq [:sym->clause-index+pos symbol])]
                             (str
                               "c" a-clause-idx "." (eav-type->sql-column-name a-eav-type)
                               " = "
                               "c" b-clause-idx "." (eav-type->sql-column-name b-eav-type)))))
                    (interpose " AND ")
                    (apply str))))))
       (interpose "\n")
       (apply str)))

(defn val->sql-val [o]
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

(defn fn-arg->sql [{:keys [symbol value]}
                   {:keys [sym->clause-index+pos]}]
  (cond
    value
    (val->sql-val value)

    symbol
    (let [[clause-idx eav-type]
          (get sym->clause-index+pos symbol)]
      (str "c" clause-idx "." (eav-type->sql-column-name eav-type)))))

(defn where-str [{:keys [join-clauses
                         qwhere] :as pq}]
  (str
    "WHERE\n"
    (->> [(->> join-clauses
               (map-indexed
                 (fn [clause-idx {:keys [pattern]}]
                   (->> pattern
                        (map-indexed
                          (fn [eav-idx {:keys [value]}]
                            (if value
                              [clause-idx (idx->eav-type eav-idx) value]
                              nil)))
                        (remove nil?)
                        (map (fn [[clause-idx eav-type value]]
                               (str "c" clause-idx "." (eav-type->sql-column-name eav-type) " = " (val->sql-val value))))
                        (interpose " AND ")
                        (apply str))))
               (interpose " AND\n")
               (apply str))
          (->> qwhere
               (filter :fn)
               (map (fn [{:keys [fn args]}]
                      (let [first-arg (first args)
                            second-arg (second args)
                            op (-> fn :symbol str)]
                        (->> [(fn-arg->sql first-arg pq)
                              op
                              (fn-arg->sql second-arg pq)]
                             (interpose " ")
                             (apply str)))))
               (interpose " AND\n")
               (apply str))]
         (interpose " AND\n")
         (apply str))))

(defn parse-query [query]
  (let [{:keys [qfind qwhere] :as ds-query} (dsqv3/parse-query query)
        join-clauses (->> qwhere
                          (filter join-clause?))
        pq (merge
             ds-query
             {:join-clauses join-clauses
              :sym->clause-index+pos
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
                                               (condp = pidx
                                                 0 :ent
                                                 1 :atr
                                                 2 :val)]])))
                                (remove nil?)
                                (into {})))))
                     {}))})]
    #_(ks/spy (merge
                pq
                {:select-str (select-str pq)
                 :from-str (from-str pq)
                 :join-str (join-str pq)
                 :where-str (where-str pq)}))
    (->> [(select-str pq)
          (from-str pq)
          (join-str pq)
          (where-str pq)]
         (interpose "\n")
         (apply str))))


(comment
  (join-clause?
    '[?m :movie/title ?title])

  (join-clause?
    '[(< ?m ?n)])

  (println
    (parse-query
      '[:find ?title
        :where
        [?m :movie/title ?title]
        [?m :movie/year ?movie-year]
        [(< ?movie-year 1980)]]))

  (run-test
    [(parse-query
       '[:find ?title
         :where
         [?m :movie/title ?title]
         [?m :movie/year ?movie-year]
         [(> ?movie-year 1981)]])]
    movies-db)
  )


(defn test-query-parsing []
  '[:find ?title
    :where
    [?m :movie/title ?title]
    [?m :movie/year 1985]]
  
  (run-test
    ["
SELECT c1.VAL
FROM datoms AS c1
INNER JOIN datoms AS c2 ON c1.ENT = c2.ENT
WHERE
c1.ATR = \"movie/title\" AND
c2.ATR = \"movie/year\" AND
c2.VAL >= 1985
"]
    movies-db)
  )

(defn q [q & inputs]
  (let [parsed-q (dsqv3/parse-query q)
        context  { :rels    []
                  :consts  {}
                  :sources {}
                  :rules   {}
                  :default-source-symbol '$ }
        context  (dsqv3/resolve-ins context (:qin parsed-q) inputs)
        context  (dsqv3/resolve-clauses context (:qwhere parsed-q))
        #_#_syms     (concat (dp/find-vars (:qfind parsed-q))
                       (map :symbol (:qwith parsed-q)))]
    context
    #_(native-coll (collect-to context syms (fast-set) [(map vec)]))))

(comment

  (test-select-all)
  (test-foaf)

  ;; https://docs.datomic.com/on-prem/query.html
  (test-dq-basic)
  (test-dq-unification)
  (test-dq-blanks)

  ;; http://www.learndatalogtoday.org/
  (test-edn-ch-0)
  (test-basic-queries-ch-1-0)
  (test-basic-queries-ch-1-1)
  (test-basic-queries-ch-1-2)
  (test-basic-queries-ch-2-0)
  (test-basic-queries-ch-2-1)
  (test-basic-queries-ch-2-2)
  (test-basic-queries-ch-2-3)
  (test-param-queries-ch-3-0)
  (test-param-queries-ch-3-1)
  (test-param-queries-ch-3-2)
  ;; punt test-param-queries-ch-3-3
  (test-more-queries-ch-4-0)
  (test-more-queries-ch-4-1)
  ;; punt test-more-queries-ch-4-2 & 3
  (test-pred-ch-5-0)
  (test-pred-ch-5-1)
  ;; punt test-pred-ch-5-2 & 3
  ;; punt tranformation functions ch 6
  ;; punt aggregates
  ;; punt rules
  (test-not-query)

  (ks/pp (q
           '[:find ?e
             :where
             [?e :attr/a "who"]
             [?e :attr/b "bar"]]
           {}))

  (ks/pp
    (dsqv3/resolve-clauses
      nil
      (:qwhere
       (dsqv3/parse-query
         '[:find ?e
           :where
           [?e :attr/a "who"]
           [?e :attr/b "bar"]]))))

  
 
  

  ;;

  

  (sql/with-test-db
    (fn [db]
      (go
        (try
          (<! (sql/<exec db [(create-datoms-sql)]))
          (<! (sql/<batch-exec db (insert-datoms-sqls
                                    [["zk" "name" "Zack" false]
                                     ["cindy" "name" "Cindy" false]
                                     ["zk" "friend" "cindy" true]])))
          (<! (sql/<exec db ["select * from datoms"]))
          (catch js/Error e
            (.error js/console e)))))
    {:name ":memory:"})

  

  )
