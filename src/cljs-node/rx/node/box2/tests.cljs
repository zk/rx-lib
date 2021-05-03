(ns rx.node.box2.tests
  (:require [rx.kitchen-sink :as ks]
            [rx.box2 :as bq]
            [rx.box2.sql :as bsql]
            #_[rx.box2.reagent :as br]
            [rx.node.sqlite2 :as sql]
            [rx.test :as test
             :refer-macros [deftest
                            <deftest]]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [datascript.query :as dsq]
            [datascript.query-v3 :as dsqv3]
            [clojure.core.async :as async
             :refer [go <!]]))

(def debug-explain
  {::bsql/debug-explain-query? true})

(def test-schema {:box/schema
                  {:box/attrs
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
                     :box/index? true
                     :box/value-type :box/long}
                    {:box/attr :box/created-ts
                     :box/index? true
                     :box/value-type :box/long}]}})

(<defn <test-conn [& [datoms]]
  (let [db (<! (sql/<open-db {:name ":memory:"}))]
    (anom/throw-if-anom
      (<! (sql/<exec db [(bq/create-datoms-sql)])))
    (anom/throw-if-anom
      (<! (sql/<batch-exec db (bq/create-indexes-sql-vecs))))
    (when datoms
      (anom/throw-if-anom
        (<! (sql/<batch-exec db (bq/insert-datoms-sqls datoms)))))
    (merge
      {::bsql/db db
       ::bsql/<exec sql/<exec
       ::bsql/<batch-exec sql/<batch-exec}
      test-schema)))

(defn <dump-conn [{:keys [::bsql/db
                          ::bsql/<exec]}]
  (go
    (ks/pp
      (<! (<exec db ["select * from datoms"])))))

(defn <run-sql [{:keys [::bsql/db
                        ::bsql/<exec]}
                sql-vec]
  (go
    (ks/spy
      "RUN SQL"
      (<! (<exec db sql-vec)))))

(def foaf-data
  [[[:box/id "zk"] "name" "Zack" false]
   [[:box/id "cindy"] "name" "Cindy" false]
   [[:box/id "zk"] "friend" [:box/id "cindy"] true true]])

(def refs-datoms
  [[[:box/id "one"] :box/id "one"]
   [[:box/id "one"] :box/created-ts 1000]
   [[:box/id "one"] :box/updated-ts 1000]
   [[:box/id "one"] :box/text "hithere"]
   [[:box/id "one"] :box/ref [:box/id "two"] true]
   [[:box/id "two"] :box/id "two"]
   [[:box/id "two"] :box/updated-ts 2000]
   [[:box/id "two"] :box/created-ts 2000]
   [[:box/id "three"] :box/id "three"]
   [[:box/id "three"] :box/created-ts 3000]
   [[:box/id "three"] :box/updated-ts 3000]
   [[:box/id "four"] :box/id "four"]
   [[:box/id "four"] :box/created-ts 4000]
   [[:box/id "four"] :box/updated-ts 4000]
   [[:box/id "one"] :box/refs [:box/id "three"] true true]
   [[:box/id "one"] :box/refs [:box/id "four"] true true]
   [[:box/id "five"] :box/id "five"]
   [[:box/id "five"] :box/created-ts 5000]
   [[:box/id "two"] :box/refs [:box/id "five"] true true]
   [[:box/id "six"] :box/id "six"]
   [[:box/id "six"] :box/bool? true]
   [[:box/id "seven"] :box/id "seven"]
   [[:box/id "seven"] :box/bool? false]])

(def movies-db
  [[[:box/id -100] "person/name" "James Cameron" nil nil]
   [[:box/id -100] "person/born" -485308800000 nil nil]
   [[:box/id -101] "person/name" "Arnold Schwarzenegger" nil nil]
   [[:box/id -101] "person/born" -707702400000 nil nil]
   [[:box/id -102] "person/name" "Linda Hamilton" nil nil]
   [[:box/id -102] "person/born" -418608000000 nil nil]
   [[:box/id -103] "person/name" "Michael Biehn" nil nil]
   [[:box/id -103] "person/born" -423532800000 nil nil]
   [[:box/id -104] "person/name" "Ted Kotcheff" nil nil]
   [[:box/id -104] "person/born" -1222473600000 nil nil]
   [[:box/id -105] "person/name" "Sylvester Stallone" nil nil]
   [[:box/id -105] "person/born" -741312000000 nil nil]
   [[:box/id -106] "person/name" "Richard Crenna" nil nil]
   [[:box/id -106] "person/born" -1359763200000 nil nil]
   [[:box/id -106] "person/death" 1042761600000 nil nil]
   [[:box/id -107] "person/name" "Brian Dennehy" nil nil]
   [[:box/id -107] "person/born" -993513600000 nil nil]
   [[:box/id -108] "person/name" "John McTiernan" nil nil]
   [[:box/id -108] "person/born" -599011200000 nil nil]
   [[:box/id -109] "person/name" "Elpidia Carrillo" nil nil]
   [[:box/id -109] "person/born" -264384000000 nil nil]
   [[:box/id -110] "person/name" "Carl Weathers" nil nil]
   [[:box/id -110] "person/born" -693187200000 nil nil]
   [[:box/id -111] "person/name" "Richard Donner" nil nil]
   [[:box/id -111] "person/born" -1252540800000 nil nil]
   [[:box/id -112] "person/name" "Mel Gibson" nil nil]
   [[:box/id -112] "person/born" -441676800000 nil nil]
   [[:box/id -113] "person/name" "Danny Glover" nil nil]
   [[:box/id -113] "person/born" -739929600000 nil nil]
   [[:box/id -114] "person/name" "Gary Busey" nil nil]
   [[:box/id -114] "person/born" -802396800000 nil nil]
   [[:box/id -115] "person/name" "Paul Verhoeven" nil nil]
   [[:box/id -115] "person/born" -992736000000 nil nil]
   [[:box/id -116] "person/name" "Peter Weller" nil nil]
   [[:box/id -116] "person/born" -710812800000 nil nil]
   [[:box/id -117] "person/name" "Nancy Allen" nil nil]
   [[:box/id -117] "person/born" -616118400000 nil nil]
   [[:box/id -118] "person/name" "Ronny Cox" nil nil]
   [[:box/id -118] "person/born" -992304000000 nil nil]
   [[:box/id -119] "person/name" "Mark L. Lester" nil nil]
   [[:box/id -119] "person/born" -728956800000 nil nil]
   [[:box/id -120] "person/name" "Rae Dawn Chong" nil nil]
   [[:box/id -120] "person/born" -278985600000 nil nil]
   [[:box/id -121] "person/name" "Alyssa Milano" nil nil]
   [[:box/id -121] "person/born" 93571200000 nil nil]
   [[:box/id -122] "person/name" "Bruce Willis" nil nil]
   [[:box/id -122] "person/born" -466732800000 nil nil]
   [[:box/id -123] "person/name" "Alan Rickman" nil nil]
   [[:box/id -123] "person/born" -752976000000 nil nil]
   [[:box/id -124] "person/name" "Alexander Godunov" nil nil]
   [[:box/id -124] "person/born" -634089600000 nil nil]
   [[:box/id -124] "person/death" 800755200000 nil nil]
   [[:box/id -125] "person/name" "Robert Patrick" nil nil]
   [[:box/id -125] "person/born" -352080000000 nil nil]
   [[:box/id -126] "person/name" "Edward Furlong" nil nil]
   [[:box/id -126] "person/born" 239328000000 nil nil]
   [[:box/id -127] "person/name" "Jonathan Mostow" nil nil]
   [[:box/id -127] "person/born" -255398400000 nil nil]
   [[:box/id -128] "person/name" "Nick Stahl" nil nil]
   [[:box/id -128] "person/born" 313200000000 nil nil]
   [[:box/id -129] "person/name" "Claire Danes" nil nil]
   [[:box/id -129] "person/born" 292723200000 nil nil]
   [[:box/id -130] "person/name" "George P. Cosmatos" nil nil]
   [[:box/id -130] "person/born" -914889600000 nil nil]
   [[:box/id -130] "person/death" 1113868800000 nil nil]
   [[:box/id -131] "person/name" "Charles Napier" nil nil]
   [[:box/id -131] "person/born" -1064188800000 nil nil]
   [[:box/id -131] "person/death" 1317772800000 nil nil]
   [[:box/id -132] "person/name" "Peter MacDonald" nil nil]
   [[:box/id -133] "person/name" "Marc de Jonge" nil nil]
   [[:box/id -133] "person/born" -658713600000 nil nil]
   [[:box/id -133] "person/death" 834019200000 nil nil]
   [[:box/id -134] "person/name" "Stephen Hopkins" nil nil]
   [[:box/id -135] "person/name" "Ruben Blades" nil nil]
   [[:box/id -135] "person/born" -677289600000 nil nil]
   [[:box/id -136] "person/name" "Joe Pesci" nil nil]
   [[:box/id -136] "person/born" -848707200000 nil nil]
   [[:box/id -137] "person/name" "Ridley Scott" nil nil]
   [[:box/id -137] "person/born" -1012608000000 nil nil]
   [[:box/id -138] "person/name" "Tom Skerritt" nil nil]
   [[:box/id -138] "person/born" -1147219200000 nil nil]
   [[:box/id -139] "person/name" "Sigourney Weaver" nil nil]
   [[:box/id -139] "person/born" -638496000000 nil nil]
   [[:box/id -140] "person/name" "Veronica Cartwright" nil nil]
   [[:box/id -140] "person/born" -653270400000 nil nil]
   [[:box/id -141] "person/name" "Carrie Henn" nil nil]
   [[:box/id -142] "person/name" "George Miller" nil nil]
   [[:box/id -142] "person/born" -783648000000 nil nil]
   [[:box/id -143] "person/name" "Steve Bisley" nil nil]
   [[:box/id -143] "person/born" -568598400000 nil nil]
   [[:box/id -144] "person/name" "Joanne Samuel" nil nil]
   [[:box/id -145] "person/name" "Michael Preston" nil nil]
   [[:box/id -145] "person/born" -998352000000 nil nil]
   [[:box/id -146] "person/name" "Bruce Spence" nil nil]
   [[:box/id -146] "person/born" -766540800000 nil nil]
   [[:box/id -147] "person/name" "George Ogilvie" nil nil]
   [[:box/id -147] "person/born" -1225324800000 nil nil]
   [[:box/id -148] "person/name" "Tina Turner" nil nil]
   [[:box/id -148] "person/born" -949881600000 nil nil]
   [[:box/id -149] "person/name" "Sophie Marceau" nil nil]
   [[:box/id -149] "person/born" -98582400000 nil nil]
   [[:box/id -200] "movie/title" "The Terminator" nil nil]
   [[:box/id -200] "movie/year" 1984 nil nil]
   [[:box/id -200] "movie/director" [:box/id -100] true nil]
   [[:box/id -200] "movie/cast" [:box/id -101] true true]
   [[:box/id -200] "movie/cast" [:box/id -102] true true]
   [[:box/id -200] "movie/cast" [:box/id -103] true true]
   [[:box/id -200] "movie/sequel" [:box/id -207] true nil]
   [[:box/id -201] "movie/title" "First Blood" nil nil]
   [[:box/id -201] "movie/year" 1982 nil nil]
   [[:box/id -201] "movie/director" [:box/id -104] true nil]
   [[:box/id -201] "movie/cast" [:box/id -105] true true]
   [[:box/id -201] "movie/cast" [:box/id -106] true true]
   [[:box/id -201] "movie/cast" [:box/id -107] true true]
   [[:box/id -201] "movie/sequel" [:box/id -209] true nil]
   [[:box/id -202] "movie/title" "Predator" nil nil]
   [[:box/id -202] "movie/year" 1987 nil nil]
   [[:box/id -202] "movie/director" [:box/id -108] true nil]
   [[:box/id -202] "movie/cast" [:box/id -101] true true]
   [[:box/id -202] "movie/cast" [:box/id -109] true true]
   [[:box/id -202] "movie/cast" [:box/id -110] true true]
   [[:box/id -202] "movie/sequel" [:box/id -211] true nil]
   [[:box/id -203] "movie/title" "Lethal Weapon" nil nil]
   [[:box/id -203] "movie/year" 1987 nil nil]
   [[:box/id -203] "movie/director" [:box/id -111] true nil]
   [[:box/id -203] "movie/cast" [:box/id -112] true true]
   [[:box/id -203] "movie/cast" [:box/id -113] true true]
   [[:box/id -203] "movie/cast" [:box/id -114] true true]
   [[:box/id -203] "movie/sequel" [:box/id -212] true nil]
   [[:box/id -204] "movie/title" "RoboCop" nil nil]
   [[:box/id -204] "movie/year" 1987 nil nil]
   [[:box/id -204] "movie/director" [:box/id -115] true nil]
   [[:box/id -204] "movie/cast" [:box/id -116] true true]
   [[:box/id -204] "movie/cast" [:box/id -117] true true]
   [[:box/id -204] "movie/cast" [:box/id -118] true true]
   [[:box/id -205] "movie/title" "Commando" nil nil]
   [[:box/id -205] "movie/year" 1985 nil nil]
   [[:box/id -205] "movie/director" [:box/id -119] true nil]
   [[:box/id -205] "movie/cast" [:box/id -101] true true]
   [[:box/id -205] "movie/cast" [:box/id -120] true true]
   [[:box/id -205] "movie/cast" [:box/id -121] true true]
   [[:box/id -205]
    "trivia"
    "In 1986, a sequel was written with an eye to having\n  John McTiernan direct. Schwarzenegger wasn't interested in reprising\n  the role. The script was then reworked with a new central character,\n  eventually played by Bruce Willis, and became Die Hard"
    nil
    nil]
   [[:box/id -206] "movie/title" "Die Hard" nil nil]
   [[:box/id -206] "movie/year" 1988 nil nil]
   [[:box/id -206] "movie/director" [:box/id -108] true nil]
   [[:box/id -206] "movie/cast" [:box/id -122] true true]
   [[:box/id -206] "movie/cast" [:box/id -123] true true]
   [[:box/id -206] "movie/cast" [:box/id -124] true true]
   [[:box/id -207] "movie/title" "Terminator 2: Judgment Day" nil nil]
   [[:box/id -207] "movie/year" 1991 nil nil]
   [[:box/id -207] "movie/director" [:box/id -100] true nil]
   [[:box/id -207] "movie/cast" [:box/id -101] true true]
   [[:box/id -207] "movie/cast" [:box/id -102] true true]
   [[:box/id -207] "movie/cast" [:box/id -125] true true]
   [[:box/id -207] "movie/cast" [:box/id -126] true true]
   [[:box/id -207] "movie/sequel" [:box/id -208] true nil]
   [[:box/id -208]
    "movie/title"
    "Terminator 3: Rise of the Machines"
    nil
    nil]
   [[:box/id -208] "movie/year" 2003 nil nil]
   [[:box/id -208] "movie/director" [:box/id -127] true nil]
   [[:box/id -208] "movie/cast" [:box/id -101] true true]
   [[:box/id -208] "movie/cast" [:box/id -128] true true]
   [[:box/id -208] "movie/cast" [:box/id -129] true true]
   [[:box/id -209] "movie/title" "Rambo: First Blood Part II" nil nil]
   [[:box/id -209] "movie/year" 1985 nil nil]
   [[:box/id -209] "movie/director" [:box/id -130] true nil]
   [[:box/id -209] "movie/cast" [:box/id -105] true true]
   [[:box/id -209] "movie/cast" [:box/id -106] true true]
   [[:box/id -209] "movie/cast" [:box/id -131] true true]
   [[:box/id -209] "movie/sequel" [:box/id -210] true nil]
   [[:box/id -210] "movie/title" "Rambo III" nil nil]
   [[:box/id -210] "movie/year" 1988 nil nil]
   [[:box/id -210] "movie/director" [:box/id -132] true nil]
   [[:box/id -210] "movie/cast" [:box/id -105] true true]
   [[:box/id -210] "movie/cast" [:box/id -106] true true]
   [[:box/id -210] "movie/cast" [:box/id -133] true true]
   [[:box/id -211] "movie/title" "Predator 2" nil nil]
   [[:box/id -211] "movie/year" 1990 nil nil]
   [[:box/id -211] "movie/director" [:box/id -134] true nil]
   [[:box/id -211] "movie/cast" [:box/id -113] true true]
   [[:box/id -211] "movie/cast" [:box/id -114] true true]
   [[:box/id -211] "movie/cast" [:box/id -135] true true]
   [[:box/id -212] "movie/title" "Lethal Weapon 2" nil nil]
   [[:box/id -212] "movie/year" 1989 nil nil]
   [[:box/id -212] "movie/director" [:box/id -111] true nil]
   [[:box/id -212] "movie/cast" [:box/id -112] true true]
   [[:box/id -212] "movie/cast" [:box/id -113] true true]
   [[:box/id -212] "movie/cast" [:box/id -136] true true]
   [[:box/id -212] "movie/sequel" [:box/id -213] true nil]
   [[:box/id -213] "movie/title" "Lethal Weapon 3" nil nil]
   [[:box/id -213] "movie/year" 1992 nil nil]
   [[:box/id -213] "movie/director" [:box/id -111] true nil]
   [[:box/id -213] "movie/cast" [:box/id -112] true true]
   [[:box/id -213] "movie/cast" [:box/id -113] true true]
   [[:box/id -213] "movie/cast" [:box/id -136] true true]
   [[:box/id -214] "movie/title" "Alien" nil nil]
   [[:box/id -214] "movie/year" 1979 nil nil]
   [[:box/id -214] "movie/director" [:box/id -137] true nil]
   [[:box/id -214] "movie/cast" [:box/id -138] true true]
   [[:box/id -214] "movie/cast" [:box/id -139] true true]
   [[:box/id -214] "movie/cast" [:box/id -140] true true]
   [[:box/id -214] "movie/sequel" [:box/id -215] true nil]
   [[:box/id -215] "movie/title" "Aliens" nil nil]
   [[:box/id -215] "movie/year" 1986 nil nil]
   [[:box/id -215] "movie/director" [:box/id -100] true nil]
   [[:box/id -215] "movie/cast" [:box/id -139] true true]
   [[:box/id -215] "movie/cast" [:box/id -141] true true]
   [[:box/id -215] "movie/cast" [:box/id -103] true true]
   [[:box/id -216] "movie/title" "Mad Max" nil nil]
   [[:box/id -216] "movie/year" 1979 nil nil]
   [[:box/id -216] "movie/director" [:box/id -142] true nil]
   [[:box/id -216] "movie/cast" [:box/id -112] true true]
   [[:box/id -216] "movie/cast" [:box/id -143] true true]
   [[:box/id -216] "movie/cast" [:box/id -144] true true]
   [[:box/id -216] "movie/sequel" [:box/id -217] true nil]
   [[:box/id -217] "movie/title" "Mad Max 2" nil nil]
   [[:box/id -217] "movie/year" 1981 nil nil]
   [[:box/id -217] "movie/director" [:box/id -142] true nil]
   [[:box/id -217] "movie/cast" [:box/id -112] true true]
   [[:box/id -217] "movie/cast" [:box/id -145] true true]
   [[:box/id -217] "movie/cast" [:box/id -146] true true]
   [[:box/id -217] "movie/sequel" [:box/id -218] true nil]
   [[:box/id -218] "movie/title" "Mad Max Beyond Thunderdome" nil nil]
   [[:box/id -218] "movie/year" 1985 nil nil]
   [[:box/id -218] "movie/director" [:box/id -142] true true]
   [[:box/id -218] "movie/director" [:box/id -147] true true]
   [[:box/id -218] "movie/cast" [:box/id -112] true true]
   [[:box/id -218] "movie/cast" [:box/id -148] true true]
   [[:box/id -219] "movie/title" "Braveheart" nil nil]
   [[:box/id -219] "movie/year" 1995 nil nil]
   [[:box/id -219] "movie/director" [:box/id -112] true true]
   [[:box/id -219] "movie/cast" [:box/id -112] true true]
   [[:box/id -219] "movie/cast" [:box/id -149] true true]])

(<deftest test-query-entity []
  (let [conn (<? (<test-conn foaf-data))
        expect ["Cindy"]
        actual (<! (bq/<query
                     '[:find [?name ...]
                       :in $ ?e
                       :where
                       [?e :friend ?e2]
                       [?e2 :name ?name]]
                     conn
                     {:box/id "zk"}))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-foaf []
  (let [conn (<? (<test-conn foaf-data))
        expect ["Cindy"]
        actual (<! (bq/<query
                     '[:find [?name ...]
                       :where
                       [?e :name "Zack"]
                       [?e :friend ?e2]
                       [?e2 :name ?name]]
                     conn))
        ent (<! (bq/<pull
                  conn
                  '[*]
                  [:box/id "zk"]))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]
     [(not (nil? ent))
      "Pull entity succeeded"]]))

(def dq-basic-datoms
  [["sally" "age" 21]
   ["fred" "age" 42]
   ["ethel" "age" 42]
   ["fred" "likes" "pizza"]
   ["sally" "likes" "opera"]
   ["ethel" "likes" "sushi"]
   ["sally" "name" "Sally"]
   ["fred" "name" "Fred"]
   ["ethel" "name" "Ethel"]])

(<deftest test-query-basic []
  (let [expect [[[:box/id "fred"]]
                [[:box/id "ethel"]]]
        actual (<! (bq/<query
                     '[:find ?e
                       :where
                       [?e :age 42]]
                     (<! (<test-conn dq-basic-datoms))))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-unification []
  (let [expect [[[:box/id "fred"] "pizza"] [[:box/id "ethel"] "sushi"]]
        actual (<! (bq/<query
                     '[:find ?e ?x 
                       :where [?e :age 42]
                       [?e :likes ?x]]
                     (<! (<test-conn dq-basic-datoms))))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-blanks []
  (let [expect [["opera"] ["pizza"] ["sushi"]]
        actual (<! (bq/<query
                     '[:find ?x 
                       :where [_ :likes ?x]]
                     (merge
                       #_debug-explain
                       (<! (<test-conn dq-basic-datoms)))))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-sort-desc []
  (let [conn (<? (<test-conn dq-basic-datoms))
        expect [["Fred" 42] ["Ethel" 42] ["Sally" 21]]
        actual (<! (bq/<query
                     {:dq '[:find [?name ?age]
                            :where
                            [?e :age ?age]
                            [?e :name ?name]]
                      :sort '[[?age :desc]
                              [?name :desc]]}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-sort-asc []
  (let [conn (<? (<test-conn dq-basic-datoms))
        expect (vec (reverse [["Fred" 42] ["Ethel" 42] ["Sally" 21]]))
        actual (<! (bq/<query
                     {:dq '[:find [?name ?age]
                            :where
                            [?e :age ?age]
                            [?e :name ?name]]
                      :sort '[[?age :asc]
                              [?name :asc]]}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-sort-string []
  (let [conn (<? (<test-conn movies-db))
        expect [["Veronica Cartwright"] ["Tom Skerritt"] ["Tina Turner"]]
        actual (<! (bq/<query
                     {:dq '[:find [?name]
                            :where
                            [_ :person/name ?name]]
                      :sort '[[?name :desc]]
                      :limit 3}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))


(comment

  (test/<run-var-repl #'test-query-sort-string)

  )

(<deftest test-query-sort-pull-wrapped []
  (let [conn (<? (<test-conn))
        _ (<! (bq/<transact
                conn
                [{:box/id "one"
                  :box/created-ts 1}
                 {:box/id "two"
                  :box/created-ts 2}]))
        expect [[{:box/id "two", :box/created-ts 2}]
                [{:box/id "one", :box/created-ts 1}]]
        actual (<! (bq/<query
                     {:dq '[:find (pull ?e [*])
                            :where
                            [?e :box/id]
                            [?e :box/created-ts ?ts]]
                      :sort '[[?ts :desc]]}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-sort-pull-unwrapped []
  (let [conn (<? (<test-conn))
        _ (<! (bq/<transact
                conn
                [{:box/id "one"
                  :box/created-ts 1}
                 {:box/id "two"
                  :box/created-ts 2}]))
        expect [{:box/id "two"
                 :box/created-ts 2}
                {:box/id "one"
                 :box/created-ts 1}]
        actual (<! (bq/<query
                     {:dq '[:find [(pull ?e [*]) ...]
                            :where
                            [?e :box/id]
                            [?e :box/created-ts ?ts]]
                      :sort '[[?ts :desc]]}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-limit []
  (let [conn (<? (<test-conn dq-basic-datoms))
        expect [["Fred" 42] ["Ethel" 42]]
        actual (<! (bq/<query
                     {:dq '[:find [?name ?age]
                            :where
                            [?e :age ?age]
                            [?e :name ?name]]
                      :sort '[[?age :desc]
                              [?name :desc]]
                      :limit 2}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-offset []
  (let [conn (<? (<test-conn dq-basic-datoms))
        expect [["Ethel" 42] ["Sally" 21]]
        actual (<! (bq/<query
                     {:dq '[:find [?name ?age]
                            :where
                            [?e :age ?age]
                            [?e :name ?name]]
                      :sort '[[?age :desc]
                              [?name :desc]]
                      :offset 1}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-parent-ref []
  (let [conn (<? (<test-conn refs-datoms))
        expect [[[:box/id "four"]] [[:box/id "three"]]]
        actual (<! (bq/<query
                     {:dq '[:find ?e
                            :in $ ?p
                            :where
                            [?e :box/updated-ts]
                            [?p :box/refs ?e]]}
                     conn
                     {:box/id "one"}))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-query-missing-attr []
  (let [conn (<? (<test-conn refs-datoms))
        expect [[[:box/id "one"]]
                [[:box/id "seven"]] 
                [[:box/id "six"]] 
                [[:box/id "two"]]]
        actual (<! (bq/<query
                     {:dq '[:find ?e
                            :where
                            [?e]
                            (not
                              [_ :box/refs ?e])]}
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))


(<deftest test-query-order-by []
  (let [conn (<? (<test-conn refs-datoms))
        expect [[[:box/id "two"]]
                [[:box/id "one"]]]
        actual (<! (bq/<query
                     {:dq '[:find ?e
                            :where
                            [?e]
                            #_[?e :box/created-ts]
                            (not
                              [_ :box/refs ?e])
                            [?e :box/created-ts ?ts]]
                      :sort '[[?ts :desc]]
                      }
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(defn rq []
  (gol
    (bq/<sql
      (<? (<test-conn refs-datoms))
      ["SELECT DISTINCT c0.ENTITY AS \"?e\" FROM datoms AS c0
LEFT JOIN datoms AS c1 ON c0.ENTITY=c1.VALUE
--EXCEPT
--SELECT DISTINCT c0.ENTITY AS \"?e\" FROM datoms AS c0
--INNER JOIN datoms AS c1 ON c0.ENTITY=c1.VALUE 
--WHERE c1.ATTR=?"
       #_"box/refs"]
      )))

(comment

  (test/<run-var-repl #'test-query-order-by)

  (rq)

  )

(deftest test-edn-ch-0 []
  (go
    (let [expect [["Alien"]
                  ["Aliens"]
                  ["Braveheart"]
                  ["Commando"]
                  ["Die Hard"]
                  ["First Blood"]
                  ["Lethal Weapon"]
                  ["Lethal Weapon 2"]
                  ["Lethal Weapon 3"]
                  ["Mad Max"]
                  ["Mad Max 2"]
                  ["Mad Max Beyond Thunderdome"]
                  ["Predator"]
                  ["Predator 2"]
                  ["Rambo III"]
                  ["Rambo: First Blood Part II"]
                  ["RoboCop"]
                  ["Terminator 2: Judgment Day"]
                  ["Terminator 3: Rise of the Machines"] 
                  ["The Terminator"]]
          actual (<! (bq/<query
                       '[:find ?title
                         :where
                         [_ :movie/title ?title]]
                       (<! (<test-conn movies-db))))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-basic-queries-ch-1-0 []
  (go
    (let [expect [[[:box/id "-202"]]
                  [[:box/id "-203"]]
                  [[:box/id "-204"]]]
          actual (<! (bq/<query
                       '[:find ?e
                         :where
                         [?e :movie/year 1987]]
                       (<! (<test-conn movies-db))))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-basic-queries-ch-1-1 []
  (go
    (let [expect [[[:box/id "-200"] "The Terminator"]
                  [[:box/id "-201"] "First Blood"]
                  [[:box/id "-202"] "Predator"]
                  [[:box/id "-203"] "Lethal Weapon"]
                  [[:box/id "-204"] "RoboCop"]
                  [[:box/id "-205"] "Commando"]
                  [[:box/id "-206"] "Die Hard"]
                  [[:box/id "-207"] "Terminator 2: Judgment Day"]
                  [[:box/id "-208"] "Terminator 3: Rise of the Machines"]
                  [[:box/id "-209"] "Rambo: First Blood Part II"]
                  [[:box/id "-210"] "Rambo III"]
                  [[:box/id "-211"] "Predator 2"]
                  [[:box/id "-212"] "Lethal Weapon 2"]
                  [[:box/id "-213"] "Lethal Weapon 3"]
                  [[:box/id "-214"] "Alien"]
                  [[:box/id "-215"] "Aliens"]
                  [[:box/id "-216"] "Mad Max"]
                  [[:box/id "-217"] "Mad Max 2"]
                  [[:box/id "-218"] "Mad Max Beyond Thunderdome"]
                  [[:box/id "-219"] "Braveheart"]]
          actual (<! (bq/<query
                       '[:find ?e ?title
                         :where
                         [?e :movie/title ?title]]
                       (<! (<test-conn movies-db))))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-basic-queries-ch-1-2 []
  (go
    (let [expect [["Alan Rickman"]
                  ["Alexander Godunov"]
                  ["Alyssa Milano"]
                  ["Arnold Schwarzenegger"]
                  ["Brian Dennehy"]
                  ["Bruce Spence"]
                  ["Bruce Willis"]
                  ["Carl Weathers"]
                  ["Carrie Henn"]
                  ["Charles Napier"]
                  ["Claire Danes"]
                  ["Danny Glover"]
                  ["Edward Furlong"]
                  ["Elpidia Carrillo"]
                  ["Gary Busey"]
                  ["George Miller"]
                  ["George Ogilvie"]
                  ["George P. Cosmatos"]
                  ["James Cameron"]
                  ["Joanne Samuel"]
                  ["Joe Pesci"]
                  ["John McTiernan"]
                  ["Jonathan Mostow"]
                  ["Linda Hamilton"]
                  ["Marc de Jonge"]
                  ["Mark L. Lester"]
                  ["Mel Gibson"]
                  ["Michael Biehn"]
                  ["Michael Preston"]
                  ["Nancy Allen"]
                  ["Nick Stahl"]
                  ["Paul Verhoeven"]
                  ["Peter MacDonald"]
                  ["Peter Weller"]
                  ["Rae Dawn Chong"]
                  ["Richard Crenna"]
                  ["Richard Donner"]
                  ["Ridley Scott"]
                  ["Robert Patrick"]
                  ["Ronny Cox"]
                  ["Ruben Blades"]
                  ["Sigourney Weaver"]
                  ["Sophie Marceau"]
                  ["Stephen Hopkins"]
                  ["Steve Bisley"]
                  ["Sylvester Stallone"]
                  ["Ted Kotcheff"]
                  ["Tina Turner"] 
                  ["Tom Skerritt"] 
                  ["Veronica Cartwright"]]
          actual (<! (bq/<query
                       '[:find ?name
                         :where
                         [?p :person/name ?name]]
                       (merge
                         (<! (<test-conn movies-db))
                         #_debug-explain)))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-dpat-ch-2-0 []
  (go
    (let [expect [["Commando"]
                  ["Mad Max Beyond Thunderdome"] 
                  ["Rambo: First Blood Part II"]]
          actual (<! (bq/<query
                       '[:find ?title
                         :where
                         [?m :movie/title ?title]
                         [?m :movie/year 1985]]
                       (merge
                         (<! (<test-conn movies-db))
                         #_debug-explain)))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-dpat-ch-2-1 []
  (go
    [[(= (<! (bq/<query
               '[:find ?year
                 :where
                 [?m :movie/title "Alien"]
                 [?m :movie/year ?year]]
               (<! (<test-conn movies-db))))
         [[1979]])]]))

(deftest test-dpat-ch-2-2 []
  (go
    [[(= (<! (bq/<query
               '[:find ?name
                 :where
                 [?m :movie/title "RoboCop"]
                 [?m :movie/director ?d]
                 [?d :person/name ?name]]
               (<! (<test-conn movies-db))))
         [["Paul Verhoeven"]])]]))

(deftest test-dpat-ch-2-3 []
  (go
    (let [expect [["James Cameron"]
                  ["John McTiernan"] 
                  ["Jonathan Mostow"]
                  ["Mark L. Lester"]]
          actual (<! (bq/<query
                       '[:find ?name
                         :where
                         [?p :person/name "Arnold Schwarzenegger"]
                         [?m :movie/cast ?p]
                         [?m :movie/director ?d]
                         [?d :person/name ?name]]
                       (merge
                         #_debug-explain
                         (<! (<test-conn movies-db)))))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-param-queries-ch-3-0 []
  (go
    [[(= (<! (bq/<query
               '[:find ?title
                 :in $ ?year
                 :where
                 [?m :movie/year ?year]
                 [?m :movie/title ?title]]
               (<! (<test-conn movies-db))
               1988))
         [["Die Hard"] ["Rambo III"]])]]))

(deftest test-param-queries-ch-3-1 []
  (go
    (let [res (<! (bq/<query
                    '[:find ?title ?year
                      :in $ [?title ...]
                      :where
                      [?m :movie/title ?title]
                      [?m :movie/year ?year]]
                    (<! (<test-conn movies-db))
                    ["Lethal Weapon" "Lethal Weapon 2" "Lethal Weapon 3"]))
          target [["Lethal Weapon" 1987]
                  ["Lethal Weapon 2" 1989] 
                  ["Lethal Weapon 3" 1992]]]
      [[(= res target)
        {:res res
         :target target}]])))

(deftest test-param-queries-ch-3-2 []
  (go
    (let [conn (<! (<test-conn movies-db))
          expect [["Aliens"] ["The Terminator"]]
          actual (<! (bq/<query
                       '[:find ?title
                         :in $ ?actor ?director
                         :where
                         [?a :person/name ?actor]
                         [?d :person/name ?director]
                         [?m :movie/cast ?a]
                         [?m :movie/director ?d]
                         [?m :movie/title ?title]]
                       (merge
                         conn
                         #_debug-explain)
                       "Michael Biehn"
                       "James Cameron"))]
      #_(<! (<dump-conn conn))
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-more-queries-ch-4-0 []
  (go
    (let [expect [["movie/title"]
                  ["movie/year"] 
                  ["movie/director"] 
                  ["movie/cast"] 
                  ["trivia"]]
          actual (<! (bq/<query
                       '[:find ?attr
                         :in $ ?title
                         :where
                         [?m :movie/title ?title]
                         [?m ?attr]]
                       (<! (<test-conn movies-db))
                       "Commando"))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-more-queries-ch-4-1 []
  (go
    (let [expect (vec
                   (reverse
                     [["Alexander Godunov"]
                      ["Alan Rickman"] 
                      ["Bruce Willis"] 
                      ["John McTiernan"]]))
          actual (<! (bq/<query
                       '[:find ?name
                         :in $ ?title [?attr ...]
                         :where
                         [?m :movie/title ?title]
                         [?m ?attr ?p]
                         [?p :person/name ?name]]
                       (merge
                         (<! (<test-conn movies-db))
                         {::bsql/debug-explain-query? false})
                       "Die Hard"
                       [:movie/cast :movie/director]))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(<deftest test-pred-ch-5-0 []
  (let [expect [["Alien"] ["Mad Max"]]
        actual (<? (bq/<query
                     '[:find ?title
                       :in $ ?year
                       :where
                       [?m :movie/title ?title]
                       [?m :movie/year ?y]
                       [(<= ?y ?year)]]
                     (<! (<test-conn movies-db))
                     1979))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual nil #_ actual}]]))

(deftest test-pred-ch-5-1 []
  (go
    (let [actual (<! (bq/<query
                       '[:find ?actor
                         :where
                         [?d :person/name "Danny Glover"]
                         [?d :person/born ?b1]
                         [?e :person/born ?b2]
                         [_ :movie/cast ?e]
                         [(< ?b2 ?b1)]
                         [?e :person/name ?actor]]
                       (merge
                         (<! (<test-conn movies-db))
                         #_debug-explain)))
          expect [["Alan Rickman"]
                  ["Brian Dennehy"]
                  ["Bruce Spence"]
                  ["Charles Napier"]
                  ["Gary Busey"]
                  ["Joe Pesci"]
                  ["Michael Preston"]
                  ["Richard Crenna"]
                  ["Ronny Cox"]
                  ["Sylvester Stallone"] 
                  ["Tina Turner"] 
                  ["Tom Skerritt"]]]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(<deftest test-not-equals []
  (let [conn (<? (<test-conn movies-db))
        result (<! (bq/<query
                     '[:find ?title
                       :in $ ?year
                       :where
                       [?m :movie/title ?title]
                       [?m :movie/year ?y]
                       [(!= ?y ?year)]]
                     conn
                     1979))
        expect [["Aliens"]
                ["Braveheart"]
                ["Commando"]
                ["Die Hard"]
                ["First Blood"]
                ["Lethal Weapon"]
                ["Lethal Weapon 2"]
                ["Lethal Weapon 3"]
                ["Mad Max 2"]
                ["Mad Max Beyond Thunderdome"]
                ["Predator"]
                ["Predator 2"]
                ["Rambo III"]
                ["Rambo: First Blood Part II"]
                ["RoboCop"]
                ["Terminator 2: Judgment Day"]
                ["Terminator 3: Rise of the Machines"] 
                ["The Terminator"]]]
    [[(= expect result)
      nil
      {:expect expect
       :result result}]]))

(<deftest test-not []
  [[true]]
  #_(let [conn (<? (<test-conn refs-datoms))
          result (<? (bq/<query
                       '[:find ?e
                         :where
                         [?e :box/id]
                         [?p :box/refs ?e]
                         #_[(missing? ?p :box/refs)]]
                       conn))
          expect [[:box/id "one"]]]
      [[(= expect result)
        nil
        {:expect expect
         :result result}]]))

(comment

  (test/<run-var-repl #'test-not)

  )

(deftest test-inline-pull-query []
  (go
    (let [result (<! (bq/<query
                       '[:find (pull ?e [:box/created-ts])
                         :where
                         [?e :box/created-ts 1000]]
                       (<! (<test-conn refs-datoms))))
          expect [[{:box/id "one", :box/created-ts 1000}]]]
      [[(= expect result)
        nil
        {:expect expect
         :result result}]])))

(<deftest test-nested-inline-pull-query []
  (let [result (<! (bq/<query
                     '[:find [(pull ?e [:box/created-ts]) ...]
                       :where
                       [?e :box/created-ts 1000]]
                     (<? (<test-conn refs-datoms))))
        expect [{:box/id "one", :box/created-ts 1000}]]
    [[(= expect result)
      nil
      {:expect expect
       :result result}]]))



(<deftest test-inline-pull-query-error []
  (let [result (<! (bq/<query
                     '[:find (pull ?e '[*])
                       :where
                       [?e :box/created-ts]]
                     (<! (<test-conn refs-datoms))))]
    [[(anom/? result)
      nil
      {:result result}]]))



(deftest test-param-pull-query []
  (go
    (let [result (<! (bq/<query
                       '[:find (pull ?e pat)
                         :in $ pat
                         :where
                         [?e :box/created-ts 1000]]
                       (<! (<test-conn refs-datoms))
                       [:box/created-ts]))
          expect [[{:box/id "one", :box/created-ts 1000}]]]
      [[(= expect result)
        nil
        {:expect expect
         :result result}]])))

(comment

  (test/<run-var-repl #'test-param-pull-query)

  )

(<deftest test-like-query []
  (let [result (<! (bq/<query
                     '[:find ?text
                       :in $ ?ts ?like-query
                       :where
                       [?e :box/created-ts ?ts]
                       [?e :box/text ?text]
                       [(sql-like ?text ?like-query)]]
                     (<? (<test-conn refs-datoms))
                     1000
                     "%ith%"))
        expect [["hithere"]]]
    [[(= expect result)
      nil
      {:expect expect
       :result result}]]))

(<deftest test-bool-query-true []
  (let [result (<! (bq/<query
                     '[:find ?e
                       :where
                       [?e :box/bool? true]]
                     (<? (<test-conn refs-datoms))))
        expect [[[:box/id "six"]]]]
    [[(= expect result)
      nil
      {:expect expect
       :result result}]]))

(<deftest test-bool-query-false []
  (let [result (<! (bq/<query
                     '[:find ?e
                       :where
                       [?e :box/bool? false]]
                     (<? (<test-conn refs-datoms))))
        expect [[[:box/id "seven"]]]]
    [[(= expect result)
      nil
      {:expect expect
       :result result}]]))

(comment

  (test/<run-var-repl #'test-bool-query-false)

  )

(deftest test-ident-param []
  (go
    (let [conn (<! (<test-conn refs-datoms))
          result (<! (bq/<query
                       '[:find (pull ?e [*])
                         :in $ ?child
                         :where
                         [?e :box/refs ?child]]
                       conn
                       [:box/id "five"]))
          expect [[{:box/id "two",
                    :box/updated-ts 2000,
                    :box/created-ts 2000
                    :box/refs [{:box/id "five"}]}]]]
      [[(= expect result)
        nil
        {:expect expect
         :result result}]])))

(deftest test-ident-param-multi []
  (go
    (let [result (<! (bq/<query
                       '[:find (pull ?e [*])
                         :in $ [?child ...]
                         :where
                         [?e :box/refs ?child]]
                       (<! (<test-conn refs-datoms))
                       [[:box/id "five"]
                        [:box/id "two"]]))
          expect [[{:box/id "two",
                    :box/updated-ts 2000,
                    :box/created-ts 2000
                    :box/refs [{:box/id "five"}]}]]]
      [[(= expect result)
        nil
        {:expect expect
         :result result}]])))


;; Pull

(<deftest test-pull-entity []
  (let [expect
        {:box/id "one",
         :box/created-ts 1000,
         :box/updated-ts 1000,
         :box/text "hithere",
         :box/ref {:box/id "two"},
         :box/refs [{:box/id "three"} {:box/id "four"}]}

        actual
        (<! (bq/<pull
              (merge
                test-schema
                (<? (<test-conn refs-datoms)))
              '[*]
              {:box/id "one"}))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-pull-wildcard []
  (let [expect
        {:box/id "one",
         :box/created-ts 1000,
         :box/updated-ts 1000,
         :box/text "hithere",
         :box/ref {:box/id "two"},
         :box/refs [{:box/id "three"} {:box/id "four"}]}

        actual
        (<! (bq/<pull
              (merge
                test-schema
                (<? (<test-conn refs-datoms)))
              '[*]
              [:box/id "one"]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-pull-empty []
  (let [conn (<! (<test-conn))
        _ (<? (bq/<transact
                conn
                [[:box/assoc [:box/id "one"] :box/text "hi"]]))

        expect []
        actual
        (<! (bq/<query
              '[:find (pull ?e [:box/none])
                :where
                [?e :box/id "one"]]
              (merge
                test-schema
                conn)))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(deftest test-pull-attrs []
  (go
    (let [expect
          {:box/id "one",
           :box/created-ts 1000}
          actual
          (<! (bq/<pull
                (merge
                  test-schema
                  (<! (<test-conn refs-datoms)))
                '[:box/created-ts]
                [:box/id "one"]))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-pull-single-ref []
  (go
    (let [expect
          {:box/id "one",
           :box/created-ts 1000,
           :box/updated-ts 1000,
           :box/text "hithere",
           :box/ref
           {:box/id "two",
            :box/updated-ts 2000,
            :box/created-ts 2000,
            :box/refs [{:box/id "five"}]},
           :box/refs [{:box/id "three"} {:box/id "four"}]}
          actual
          (<! (bq/<pull
                (merge
                  test-schema
                  (<! (<test-conn refs-datoms)))
                '[*
                  {:box/ref [*]}]
                [:box/id "one"]))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(<deftest test-pull-multi-ref []
  (let [expect
        {:box/id "one",
         :box/created-ts 1000,
         :box/updated-ts 1000,
         :box/text "hithere",
         :box/ref {:box/id "two"},
         :box/refs
         [{:box/id "three", :box/created-ts 3000, :box/updated-ts 3000}
          {:box/id "four", :box/created-ts 4000, :box/updated-ts 4000}]}
        actual
        (<! (bq/<pull
              (merge
                test-schema
                (<? (<test-conn refs-datoms)))
              '[*
                {:box/refs [*]}]
              [:box/id "one"]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(deftest test-pull-multi-ref-limit []
  (go
    (let [expect
          {:box/id "one",
           :box/created-ts 1000,
           :box/ref {:box/id "two"},
           :box/refs
           [{:box/id "three", :box/created-ts 3000}]}
          actual
          (<! (bq/<pull
                (merge
                  test-schema
                  (<! (<test-conn refs-datoms)))
                '[*
                  {(limit :box/refs 1) [*]}]
                [:box/id "one"]))]
      [[true]]
      #_[[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(<deftest test-pull-multi []
  (let [expect
        [{:box/id "one",
          :box/created-ts 1000}
         {:box/id "two",
          :box/created-ts 2000}]
        actual
        (<! (bq/<pull-multi
              (merge
                test-schema
                (<? (<test-conn refs-datoms)))
              '[:box/created-ts]
              [{:box/id "one"}
               {:box/id "two"}]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-transact-basic []
  (let [conn (<? (<test-conn))
        _ (<! (bq/<transact
                (merge
                  test-schema
                  conn)
                [[:box/assoc
                  [:box/id "one"]
                  :box/created-ts
                  1000]
                 {:box/id "two"
                  :box/ref {:box/id "three"
                            :box/created-ts 3000}
                  :box/refs [{:box/id "four"
                              :box/created-ts 4000}
                             {:box/id "five"
                              :box/created-ts 5000}]
                  :box/created-ts 2000}]))
        expect [[:box/id "one"]]
        actual (<! (bq/<query
                     '[:find [?e ...]
                       :where
                       [?e :box/created-ts 1000]]
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-transact-bool []
  (let [conn (<? (<test-conn))
        _ (<! (bq/<transact
                (merge
                  test-schema
                  conn)
                [[:box/assoc
                  [:box/id "one"]
                  :box/bool?
                  false]
                 [:box/assoc
                  [:box/id "two"]
                  :box/bool?
                  true]]))
        expect [[:box/id "one"]]
        actual (<! (bq/<query
                     '[:find [?e ...]
                       :where
                       [?e :box/bool? false]]
                     conn))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(comment

  (test/<run-var-repl #'test-transact-bool)
  )

(<deftest test-transact-dissoc []
  (let [conn (<? (<test-conn refs-datoms))
        _ (<? (bq/<transact
                (merge
                  test-schema
                  conn)
                [[:box/dissoc
                  [:box/id "one"]
                  :box/refs
                  [:box/id "three"]]]))
        expect [{:box/id "one",
                 :box/refs [{:box/id "four"}]}]
        actual (<? (bq/<query
                     '[:find [(pull ?e [:box/refs]) ...]
                       :where
                       [?e :box/id "one"]]
                     conn))]

    #_(bq/<inspect-conn conn)

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-transact-ref-maps []
  ;; transact should _not_ expand entities in txfs like :box/assoc etc
  (let [conn (merge
               (<? (<test-conn))
               {::bsql/debug-explain-query? false})
        
        _ (<! (bq/<transact
                (merge
                  test-schema
                  conn)
                [[:box/assoc
                  {:box/id "one"}
                  :box/ref
                  {:box/id "two"
                   :box/created-ts 2000}]]))
        
        actual (<! (bq/<pull
                     conn
                     '[*
                       {:box/ref [*]}]
                     [:box/id "one"]))
        expect {:box/id "one", :box/ref {:box/id "two"}}]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(deftest test-transact-nested-single []
  (go
    (let [conn (merge
                 (<! (<test-conn))
                 {::bsql/debug-explain-query? false})
          _ (<! (bq/<transact
                  (merge
                    test-schema
                    conn)
                  [{:box/id "two"
                    :box/ref {:box/id "three"
                              :box/created-ts 3000}
                    :box/refs [{:box/id "four"
                                :box/created-ts 4000}
                               {:box/id "five"
                                :box/created-ts 5000}]
                    :box/created-ts 2000}]))
          expect [3000]
          actual (<! (bq/<query
                       '[:find [?created-ts ...]
                         :where
                         [[:box/id "three"] :box/created-ts ?created-ts]]
                       conn))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(deftest test-transact-nested-multi []
  (go
    (let [conn (<! (<test-conn))
          _ (<! (bq/<transact
                  (merge
                    test-schema
                    conn)
                  [{:box/id "two"
                    :box/ref {:box/id "three"
                              :box/created-ts 3000}
                    :box/refs [{:box/id "four"
                                :box/created-ts 4000}
                               {:box/id "five"
                                :box/created-ts 5000}]
                    :box/created-ts 2000}]))
          expect [5000]
          actual (<! (bq/<query
                       '[:find [?created-ts ...]
                         :where
                         ["five" :box/created-ts ?created-ts]]
                       conn))]
      [[(= expect actual)
        nil
        {:expect expect
         :actual actual}]])))

(<deftest test-transact-data-types []
  (let [conn (<? (<test-conn))
        transact-res
        (<! (bq/<transact
              (merge
                test-schema
                conn)
              [{:box/id "one"
                :box/string "hello"
                :box/long 123
                :box/double 1.23
                :box/bool true
                :box/map {:foo "bar"}
                :box/vec [1 2 3]
                :box/list '(1 2 3)
                :box/keyword :keyword}]))
        expect {:box/id "one",
                :box/vec [1 2 3],
                :box/list '(1 2 3),
                :box/string "hello",
                :box/bool true,
                :box/map {:foo "bar"}, 
                :box/double 1.23, 
                :box/long 123
                :box/keyword :keyword}
        actual (<! (bq/<pull
                     conn
                     '[*]
                     [:box/id "one"]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]
     [(not (anom/? transact-res))
      nil
      transact-res]]))

(<deftest test-transact-delete []
  (let [conn (<? (<test-conn))
        _ (<? (bq/<transact
                conn
                [{:box/id "one"
                  :box/string "hello"
                  :box/long 123
                  :box/double 1.23
                  :box/bool true
                  :box/map {:foo "bar"}
                  :box/vec [1 2 3]
                  :box/list '(1 2 3)}
                 [:box/delete [:box/id "one"]]
                 {:box/id "one"
                  :box/string "goodbye"}]))

        _ (<? (bq/<transact
                conn
                []))
        
        expect {:box/id "one"
                :box/string "goodbye"}
        actual (<! (bq/<pull
                     conn
                     '[*]
                     [:box/id "one"]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(<deftest test-transact-delete-ref []
  (let [conn (<? (<test-conn))
        _ (<? (bq/<transact
                conn
                [{:box/id "one"
                  :box/refs [{:box/id "two"}
                             {:box/id "three"}]}
                 [:box/delete [:box/id "two"]]]))

        expect {:box/id "one",
                :box/refs [{:box/id "three"}]}
        actual (<! (bq/<pull
                     conn
                     '[*]
                     [:box/id "one"]))]
    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))


(<deftest test-transact-zeros-string
  []
  (let [conn (<? (<test-conn))
        _ (<! (bq/<transact
                (merge
                  test-schema
                  conn)
                [[:box/assoc
                  [:box/id "one"]
                  :box/title
                  "000"]]))
        expect {:box/id "one"
                :box/title "000"}
        actual (<! (bq/<pull
                     conn
                     '[:box/title]
                     [:box/id "one"]))]

    [[(= expect actual)
      nil
      {:expect expect
       :actual actual}]]))

(comment

  (test/<run-var-repl #'test-transact-zeros-string)


  )

;; Reagent stuff

#_(<deftest test-reagent []
  (let [conn (<? (<test-conn refs-datoms))
        id (ks/uuid)
        ratom (<? (br/<tracked-query
                    id
                    '[:find ?e
                      :where
                      [?e :box/created-ts]]
                    conn))
        res1 @ratom
        _ (<? (br/<transact
                conn
                [[:box/delete [:box/id "one"]]]))
        res2 @ratom
        expect [[[:box/id "two"]]
                [[:box/id "three"]] 
                [[:box/id "four"]] 
                [[:box/id "five"]]]]

    [[(= expect res2)
      nil
      {:res2 res2
       :expect expect}]]))

(comment

  (go
    (let [conn (<! (<test-conn))]
      (<! (bq/<transact
            (merge
              test-schema
              conn)
            [[:box/assoc
              [:box/id "one"]
              :box/created-ts
              1000]
             {:box/id "two"
              :box/ref {:box/id "three"
                         :box/created-ts 3000}
              :box/refs [{:box/id "four"
                           :box/created-ts 4000}
                          {:box/id "five"
                           :box/created-ts 5000}]
              :box/created-ts 2000}]))
      (ks/pp (<! (bq/<query
                   '[:find [?e ...]
                     :where
                     [?e :box/created-ts 1000]]
                   conn)))))

  )


(comment
  
  (test/<run-var-repl #'test-pred-ch-5-0)
  (test/<run-var-repl #'test-pull-multi-ref)
  (test/<run-ns-repl 'rx.node.box2.tests)

  ;; https://docs.datomic.com/on-prem/query.html
  
  ;; http://www.learndatalogtoday.org/
  ;; punt test-param-queries-ch-3-3
  ;; punt test-more-queries-ch-4-2 & 3
  ;; punt test-pred-ch-5-2 & 3
  ;; punt tranformation functions ch 6
  ;; punt aggregates
  ;; punt rules

  
  (go
    (ks/pp
      (let [conn (<! (<test-conn refs-datoms))]
        (<! (sql/<exec
              (::bsql/db conn)
              ["SELECT DISTINCT c0.ENTITY AS \"?e\" FROM datoms AS c0
INNER JOIN datoms AS c1 ON c0.ENTITY=c1.VALUE WHERE NOT c1.ATTR=? AND c0.ENTITY!=c1.VALUE"
               "box/refs"])))))

  (go
    (ks/pp
      (let [conn (<! (<test-conn refs-datoms))]
        (<! (sql/<exec
              (::bsql/db conn)
              [(str "SELECT DISTINCT c0.ENTITY as c0ent, c0.ATTR as c0attr, c0.VALUE as c0val, c1.ENTITY as c1ent, c1.ATTR as c1attr, c1.VALUE as c1val from datoms" " AS c0 INNER JOIN datoms AS c1 ON c0.ENTITY=c1.VALUE")])))))

  (go
    (ks/pp
      (let [conn (<! (<test-conn refs-datoms))]
        (<! (sql/<exec
              (::bsql/db conn)
              ["
SELECT DISTINCT c0.ENTITY AS \"?e\" FROM datoms AS c0
EXCEPT
SELECT DISTINCT c0.VALUE AS \"?e\" FROM datoms AS c0 WHERE c0.ATTR=?
"
               "box/refs"])))))
  

  )
