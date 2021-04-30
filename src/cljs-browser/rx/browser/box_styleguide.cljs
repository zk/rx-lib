(ns rx.browser.box-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.theme :as th]
            [rx.view :as view]
            [rx.browser :as browser]
            [rx.browser.css :as bcss]
            [rx.browser.styleguide :as sg]
            [rx.browser.test-ui :as test-ui]
            [rx.box.persist-local-test]
            [rx.box.query-local-test]
            [rx.box.db-adapter-dexie :as dexie]
            
            [datascript.parser :as dp]))

;; Complete data layer for web & mobile apps
;; persistence, querying, backend synchronization, and auth

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/n-d-1896/dandelions-768.jpg"
             :width 200}}
    [:h1 {:id "box"} "Box"]
    [:p "A complete data layer for web & mobile apps, including persistence, querying, backend synchronization, and auth. Inspired by "
     [:a {:href "https://www.datomic.com/"} "Datomic"]
     ", "
     [:a {:href "https://github.com/tonsky/datascript"} "Datascript"]
     ", and more."]
    [:p "Modern application are trending toward moving more and more functionality into the client, typically mobile and web apps. Thus there need to be tools to support richer and more complex client side programs."]
    [:p "Some characteristics of these programs include"]
    [:ol
     {:style {:list-style-type 'decimal
              :padding-left 20}}
     [:li "Offline-first -- the experience can't break if there's no internet connection."]
     [:li "Full-text search across everything"]
     [:li "Seamless, understandable, and introspectable data storage and backup"]
     [:li "Simple support for consuming data from local and remote sources in a single user flow"]
     [:li "Gradualy schema-ed. Minimal schema definition up front (only ident keys) to use basic storage and retrieval, add schema as you go to support indexing and full-text search."]]
    [:p "Box aims to provide tooling that solves these problems and makes building robust, offline-first apps simple."]]
   [sg/section
    [:h2 {:id "basic-concepts"} "Basic Concepts"]
    [:h3 {:id "entities"} "Entities"]
    [:p "Entities are at the core of Box. They are sets of key-value pairs that are identified by one or more of those key-value pairs."]
    [:p "They can be thought of as maps or dictionaries, where (at least) one key-value pair provides the identity of that map or dictionary."]
    [:p "For example:"]
    [sg/code-block
     (ks/pp-str
       {:foo/id "foo-1"
        :foo/title "The Quick Brown Fox"
        :foo/body "The quick bown fox jumps over the lazy dog."})]
    [:p "In this example the identity of this entity is " [:code "[:foo/id \"foo-1\"]"] ". We call the first value the identity key, and the second the identity value."]
    [:p "Note, your keys do not have to be namespace-qualified."]
    [:p "It might be helpful to think of the ident key as the 'type' of the entity. Just keep in mind that, in reality, it's a bit more nuanced."]
    [sg/code-block
     ";; Storing an entity\n"
     (ks/pp-str
       '(box/<transact
          conn
          {:foo/id "foo-1"
           :foo/title "The Quick Brown Fox"
           :foo/body "The quick bown fox jumps over the lazy dog."}))
     "\n"
     ";; Retreiving an entity\n"
     (ks/pp-str
       '(box/<pull conn [:foo/id "foo-1"]))
     "=>\n"
     (ks/pp-str
       {:foo/id "foo-1"
        :foo/title "The Quick Brown Fox"
        :foo/body "The quick bown fox jumps over the lazy dog."})]
    [:h3 {:id "schema"} "Schema"]
    [:p "The box schema is a declarative data structure that tells box how to store, query, and sync entities. It is how you inform box about ident keys, relationships between entities, and which keys to index."]
    [sg/code-block
     ";; Here we define an ident key, :foo/id\n"
     (ks/pp-str
       {:box/attrs
        {:foo/id
         {:box.attr/ident? true
          :box.attr/value-type :box.type/string}}})

     "\n;; Let's make the title searchable\n"
     (ks/pp-str
       {:box/attrs
        {:foo/id
         {:box.attr/ident? true
          :box.attr/value-type :box.type/string}
         :foo/title
         {:box.attr/index? true
          :box.attr/value-type :box.type/string}}})]
    [:p "Typically you'll start by defining your ident keys, and add additional schema as your application and needs grow."]
    [:h4 {:id "box-schema-options"} "Schema Options"]
    [sg/options-list
     {:options
      [[:box/local-db-name
        "Name used to create and access the local database."]
       [:box/local-data-table-name
        "Name used for the local data table, which holds entities."]
       [:box/local-refs-table-name
        "Name used for the local refs table, used to support many-to-one and many-to-many relationships."]
       [:box/attrs
        "Map of attribute keys to attribute schemas."]]}]
    [:h4 {:id "box-attribute-schema-options"}
     "Attribute Schema Options"]
    [sg/options-list
     {:options
      [[:box.attr/ident?
        "Mark this attribute as an ident key."]
       [:box.attr/index?
        "Index this attribute. Allows querying on this attribute."]
       [:box.attr/value-type
        (into
          [:span
           "Data type of attribute. One of "]
          (->> [:box.type/string
                :box.type/long
                :box.type/double
                :box.type/boolean]
               (map pr-str)
               (map (fn [s]
                      [:code s]))
               (interpose ", ")))]
       [:box.attr/entity-spec-key
        "If provided, will check against this spec key before inserting into database."]
       [:box.attr/cardinality
        (into 
          [:span "Used to mark a ref attribute as one-to-one or one-to-many. One of "]
          (concat
            (->> [:box.cardinality/one
                  :box.cardinality/many]
                 (map pr-str)
                 (map (fn [s]
                        [:code s]))
                 (interpose ", "))
            ["."]))]]}]
    [:h3 {:id "relations"} "Relations"]
    [:p "Box supports one-to-one, one-to-many, and many-to-many relationships via references. Entity keys can be specified as references via the "
     [:code ":box.attr/ref?"]
     " attribute schema key."]
    [:h4 {:id "one-to-one"} "One-to-One"]
    [:h4 {:id "one-to-many"} "One-to-Many"]
    [:h3 {:id "box-pull"} "Retrieving an Entity"]
    [:p "Often you'll know the entity's ident, in which case you can retreive the full entity via " [:code "box/<pull"] "." "Pull is an asynchronous operation that will retreive the full entity found in the database. You can also realize referenced entities by passing a vector of keys denoting which references to resolve."]
    [:h3 {:id "box-querying"} "Querying"]
    [:p "Retrieval of information"]
    [:h3 {:id "box-remote-sync"} "Remote Sync"]
    [:p "Box provides an API for synchronization of local entities with a backend storage engine (DynamoDB). Box provides API calls that support both immediate, deferred, and batched acknowledgement, as well as automatic delayed retries."]
    [:h3 {:id "box-auth"} "Auth"]
    [:p "Entities have the concept of an owner, where only the owner is allowed read / write privileges for that entity."]]
   [sg/section
    [:h2 {:id "box-usage"} "Usage"]
    [:h3 {:id "box-inserting-entities"} "Inserting / Updating Entities"]
    [sg/code-block
     (ks/pp-str
       '(box/<transact
          conn
          [{:foo/id "id-one"
            :foo/order 1}
           {:foo/id "id-two"
            :foo/order 2}]))
     "\n - or - \n\n"
     (ks/pp-str
       '(box/<transact
          conn
          [[:box/assoc [:foo/id "id-one"] :foo/order 1]
           [:box/assoc [:foo/id "id-two"] :foo/order 2]]))]
    [:p "The previous examples are equivalent, and result in the creation (or updating) of two entities. When maps are provided existing entities are updated via " [:code "merge"] "."]

    [:h3 {:id "box-removing-attributes"} "Removing Attributes"]
    [:p "Attributes can be removed from a persisted entity via "
     [:code "box/dissoc"]]
    [sg/code-block
     (ks/pp-str
       '(box/<transact
          conn
          [[:box/dissoc
            [:foo/id "id-one"]
            :foo/order]]))
     "\n"
     ";; entity after transact\n"
     '{:foo/id "id-one"}]]
   [sg/section
    [:h2 {:id "box-testing"} "Testing"]
    [test-ui/run-and-report-detail
     {:test-context
      {:namespaces [:rx.box.persist-local-test
                    :rx.box.query-local-test]
       :box/db-adapter (dexie/create-db-adapter)}}]]])

(defn standalone []
  (browser/<set-root!
    [sg/standalone
     {:component sections}]))

(comment

  (browser/<set-root!
    [sg/standalone
     {:component sections}])

  (browser/<show-component!
    [sections])

  (bcss/set-dev-style nil)

  (do
    (view/set-opts! {:rx.theme/id :rx.theme/light})
    (standalone))

  rx.browser.box-styleguide

  (.log js/console "HI")
  
  )



