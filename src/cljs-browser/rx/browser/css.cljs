(ns rx.browser.css
  (:require [rx.kitchen-sink :as ks]
            [garden.core :as garden]
            [rx.anom :as anom]
            [clojure.string :as str]
            [dommy.core
             :as dommy
             :refer-macros [sel1]]))

(defn get-or-create-style-el [id]
  (or (.getElementById
        js/document
        id)
      (let [el (dommy/create-element :style)]
        (dommy/set-attr! el :id id)
        (dommy/append!
          (sel1 :head)
          el)
        el)))

(defn set-style-text [id text]
  (set! (.-innerText (get-or-create-style-el id)) text)
  nil)

(defn set-style [id rules]
  (set! (.-innerText (get-or-create-style-el id))
    (garden/css {:pretty-print? false} rules)))

(defn get-rule-by-index [style-id index]
  (let [style-el (get-or-create-style-el style-id)
        sheet (.-sheet style-el)]
    (.item (.-cssRules sheet) index)))

(defonce !batched-insert-rules (atom []))

(defonce !insert-timeout (atom nil))

(defn insert-rule [style-id index rule]
  (let [style-el (get-or-create-style-el style-id)
        sheet (.-sheet style-el)
        rules-length (.-length (.-cssRules sheet))
        rule-text (garden/css {:pretty-print? false} rule)]
    #_(ks/spy rule-text)
    (.insertRule sheet rule-text index)))

(defn delete-rule [style-id index]
  (let [style-el (get-or-create-style-el style-id)
        sheet (.-sheet style-el)]
    (.deleteRule sheet index)))

(defn update-rule [style-id index css-props]
  (let [style-el (get-or-create-style-el style-id)
        sheet (.-sheet style-el)
        item (.item (.-cssRules sheet) index)]
    (set!
      (.-cssText
        (.-style item))
      (garden/style css-props))))

#_(defn set-rule [style-id index rule]
  (let [style-el (get-or-create-style-el style-id)
        sheet (.-sheet style-el)
        rules-length (.-length (.-cssRules sheet))]
    (if (< (dec rules-length) index)
      (let [rule-text (garden/css {:pretty-print? false} rule)]
        (.insertRule
          sheet
          rule-text
          index))
      (let [css-text (garden/style )]
        (set!
          (.-content
            (.-style
              (.item (.-cssRules sheet) index)))
          css-text)))))

(def dev-style-el-id "rx-browser-css-dev-style-el")

(defn get-dev-style-el []
  (get-or-create-style-el dev-style-el-id))

(defn set-dev-style [rules]
  (set-style dev-style-el-id rules))

(defonce !current-index (atom -1))

(defn next-index! []
  (swap! !current-index inc))

(defn scoped-rule->rule [scoped-rule id]
  #_(when (or (not (map? scoped-rule))
              (not (sequential? scoped-rule)))
      (anom/throw-anom
        {:desc "Scoped rule must be either map or sequential"
         ::scoped-rule scoped-rule
         }))
  [(str "." id) scoped-rule])

(defn collapse-scoped-rules [scoped-rules]
  (let [maps (filter map? scoped-rules)
        sequentials (filter sequential? scoped-rules)]
    (concat
      (when-let [m (reduce merge maps)]
        [m])
      sequentials)))

(defn calc-rules-and-indexes [rules rule-indexes]
  (let [rules-range (range (max (count rules) (count rule-indexes)))
        rules (vec rules)
        rule-indexes (vec rule-indexes)]
    (->> rules-range
         (mapv (fn [i]
                 [(or (get rules i)
                      [".rx-empty-rule" {}])
                  (get rule-indexes i)])))))

(defn fill-empty-indexes [!current-index
                          rule+existing-rule-index]
  (loop [rule+existing-rule-index rule+existing-rule-index
         out []]
    (if (empty? rule+existing-rule-index)
      out
      (recur
        (rest rule+existing-rule-index)
        (conj
          out
          (if (second (first rule+existing-rule-index))
            (first rule+existing-rule-index)
            [(ffirst rule+existing-rule-index)
             (swap! !current-index inc)]))))))

(defn update-sheet-rules [{:keys [style-el-id]}
                          rule+existing-rule-index]
  (doseq [[rule rule-index]
          rule+existing-rule-index]
    (try
      (delete-rule style-el-id rule-index)
      (catch js/Error e nil))
    (try
      (insert-rule style-el-id rule-index rule)
      (catch js/Error e
        (prn e)
        (throw e)))))

(defn update-rules-state [{:keys [!id->rule-indexes]}
                          id
                          rule+existing-rule-index]
  (swap! !id->rule-indexes
    assoc
    id
    (mapv second rule+existing-rule-index)))

(defn set-comp-rules [{:keys [!id->rule-indexes
                              !current-index
                              style-el-id]
                       :as state}
                      id
                      scoped-rules]
  (let [rule-indexes (get @!id->rule-indexes id)
        
        rules (->> scoped-rules
                   collapse-scoped-rules
                   (map #(scoped-rule->rule % id)))

        rule+existing-rule-index (calc-rules-and-indexes
                                   rules
                                   rule-indexes)

        rule+existing-rule-index (fill-empty-indexes
                                   !current-index
                                   rule+existing-rule-index)]

    (update-sheet-rules
      state
      rule+existing-rule-index)

    (update-rules-state
      state
      id
      rule+existing-rule-index)

    

    state))

(def default-state
  {:!id->rule-indexes (atom nil)
   :!current-index (atom -1)
   :style-el-id "rx-component-style-rules"})

(defn set-comp-rules! [id scoped-rules]
  (set-comp-rules
    default-state
    id
    scoped-rules))

(defn comp-state []
  {::instance-id (str "i" (ks/uuid))
   ::!last-rules (atom nil)})

(defn update-css! [comp-state scoped-rules]
  (let [last-rules @(::!last-rules comp-state)]
    (when (not= last-rules scoped-rules)
      (set-comp-rules!
        (::instance-id comp-state)
        scoped-rules)
      (reset! (::!last-rules comp-state) scoped-rules))))

(defn class-str [bcss opts]
  (str (::instance-id bcss)
       (when (:class opts)
         " ")
       (:class opts)))

(defn el-opts [bcss opts]
  {:class (class-str bcss opts)})

(defn cls [k]
  (str/replace
    (str
      (namespace k)
      "__"
      (name k))
    "."
    "_"))

(defn csel [k]
  (str
    "."
    (cls k)))

(defonce
  test-state
  {:!id->rule-indexes (atom nil)
   :!current-index (atom -1)
   :style-el-id dev-style-el-id})

(comment

  (prn default-state)

  (ks/spy
    (set-comp-rules
      test-state
      "body"
      [#_{:background-color "red"}
       #_[:h1 {:color "green"}]
       [:p {:color "white"
            :cursor 'pointer}]
       [:p:hover {:color "blue"}]]))

  (set-comp-rules!
    "body"
    [#_{:background-color "red"}
     #_[:h1 {:color "green"}]
     [:p {:color "white"
          :cursor 'pointer}]
     [:p:hover {:color "blue"}]])

  (ks/spy
    (set-comp-rules
      {:!id->rule-indexes (atom {"sr" [0 1]})
       :!current-index (atom 10)}
      "sr"
      [{:background-color "red"}
       [:&:hover {:background-color "blue"}]
       ["> foo" {:background-color "bar"}]]))

  (ks/spy
    (set-comp-rules
      {:!id->rule-indexes (atom {"sr" [0 1]})
       :!current-index (atom 10)}
      "sr"
      [{:background-color "red"}]))

  (set-rule
    dev-style-el-id
    1
    [:body {:background-color "red"}])

  (.-cssText (get-rule-by-index
               dev-style-el-id
               0))

  (insert-rule
    dev-style-el-id
    0
    [:body {:background-color "red"}])

  (update-rule-style
    dev-style-el-id
    0
    nil)

  (delete-rule dev-style-el-id 0)

  
  (let [sheet (.-sheet (.getElementById js/document dev-style-el-id))]
    (js/console.log (.-cssRules sheet)))

  (garden/css {:pretty-print? false}
    [:foo {:bar "baz"}])

  (garden/style
    {:background-color 'red})

  )



(comment
  ;; broken

  [[:.comment-more {:opacity 0}]
   [:&:hover
    {:background-color 'red}
    [:.comment-more {:opacity 1}]]]

  )
