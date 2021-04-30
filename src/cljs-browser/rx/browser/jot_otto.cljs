(ns rx.browser.jot-otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.jot :as bjot]
            [rx.browser :as browser]
            [rx.browser.youtube :as yt]
            [rx.view :as view]
            [rx.jot :as jot]
            [reagent.core :as r]
            [editscript.core :as es]
            [editscript.edit :as ee]))

(defn debug-doc [{:keys [doc]}]
  [:pre (ks/pp-str
          (dissoc doc
            #_::jot/undo-stack
            #_::jot/redo-stack))])

(defn root [{:keys [initial-doc
                    doc-filter
                    hide-debugger?] :as opts}]
  (let [!doc (or (:!doc opts)
                 (r/atom initial-doc))]
    {:render
     (fn []
       [:div
        {:style {:display 'flex
                 :flex-direction 'row
                 :flex 1
                 :overflow 'hidden}}
        [:div
         {:style {:display 'flex
                  :flex 1
                  :width "50%"
                  :overflow-y 'scroll}}
         [bjot/editor
          (merge
            opts
            {:initial-doc @!doc
             :on-change
             (fn [doc]
               (reset! !doc doc))})]]
        (when-not hide-debugger?
          [:div
           {:style {:flex 1
                    :width "50%"
                    :overflow-y 'scroll}}
           (when doc-filter
             [:div "*filtered"])
           [debug-doc
            {:doc (if doc-filter
                    (doc-filter @!doc)
                    @!doc)}]])])}))

(defn solo []
  (browser/<set-root!
    [root {:initial-doc (jot/empty-doc)
           :hide-debugger? true}]))

(defn empty-doc []
  (browser/<set-root!
    [root {:initial-doc (jot/empty-doc)}]))

(defn one-line []
  (browser/<set-root!
    [root {:initial-doc (-> (jot/base-doc)
                            (jot/append-block
                              (jot/para
                                {:text "Hello world"})))}]))

(defn populated []
  (browser/<set-root!
    [root
     {:initial-doc
      {:rx.jot/block-id->block
       {"5f52cb9b3cbb48d3ab44a2517185565f"
        {:rx.jot/block-id "5f52cb9b3cbb48d3ab44a2517185565f",
         :rx.jot/type :rx.jot/heading-one,
         :rx.jot/content-text "Hello World Heading",
         :rx.jot/index 1000000,
         :rx.jot/reverse-expansion-text "# ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph},
        "1cef3aa851544482b8e29cf20a4ed74d"
        {:rx.jot/block-id "1cef3aa851544482b8e29cf20a4ed74d",
         :rx.jot/content-text "ul 2",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 3500000},
        "8092623307ed4d8ab037a1cc4d4f1416"
        {:rx.jot/block-id "8092623307ed4d8ab037a1cc4d4f1416",
         :rx.jot/content-text "ol 2",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 5500000},
        "52c170b5e99942d4884823ab5234acf7"
        {:rx.jot/block-id "52c170b5e99942d4884823ab5234acf7",
         :rx.jot/content-text
         "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/index 2500000,
         :rx.jot/offset->deco
         {28 {:rx.jot/styles #{:rx.jot/bold}},
          29 {:rx.jot/styles #{:rx.jot/bold}},
          30 {:rx.jot/styles #{:rx.jot/bold}},
          31 {:rx.jot/styles #{:rx.jot/bold}},
          32 {:rx.jot/styles #{:rx.jot/bold}},
          33 {:rx.jot/styles #{:rx.jot/bold}},
          34 {:rx.jot/styles #{:rx.jot/bold}},
          35 {:rx.jot/styles #{:rx.jot/bold}},
          36 {:rx.jot/styles #{:rx.jot/bold}},
          37 {:rx.jot/styles #{:rx.jot/bold}},
          38 {:rx.jot/styles #{:rx.jot/bold}},
          39 {:rx.jot/styles #{:rx.jot/bold}},
          40 {:rx.jot/styles #{:rx.jot/bold}},
          41 {:rx.jot/styles #{:rx.jot/bold}},
          42 {:rx.jot/styles #{:rx.jot/bold}},
          43 {:rx.jot/styles #{:rx.jot/bold}},
          44 {:rx.jot/styles #{:rx.jot/bold}},
          45 {:rx.jot/styles #{:rx.jot/bold}},
          46 {:rx.jot/styles #{:rx.jot/bold}},
          47 {:rx.jot/styles #{:rx.jot/bold}},
          48 {:rx.jot/styles #{:rx.jot/bold}},
          49 {:rx.jot/styles #{:rx.jot/bold}},
          50 {:rx.jot/styles #{:rx.jot/bold}},
          51 {:rx.jot/styles #{:rx.jot/bold}},
          52 {:rx.jot/styles #{:rx.jot/bold}},
          53 {:rx.jot/styles #{:rx.jot/bold}},
          54 {:rx.jot/styles #{:rx.jot/bold}},
          61 {:rx.jot/styles #{:rx.jot/italic}},
          62 {:rx.jot/styles #{:rx.jot/italic}},
          63 {:rx.jot/styles #{:rx.jot/italic}},
          64 {:rx.jot/styles #{:rx.jot/italic}},
          65 {:rx.jot/styles #{:rx.jot/italic}},
          66 {:rx.jot/styles #{:rx.jot/italic}},
          67 {:rx.jot/styles #{:rx.jot/italic}},
          68 {:rx.jot/styles #{:rx.jot/italic}},
          69 {:rx.jot/styles #{:rx.jot/italic}},
          70 {:rx.jot/styles #{:rx.jot/italic}},
          71 {:rx.jot/styles #{:rx.jot/italic}},
          72 {:rx.jot/styles #{:rx.jot/italic}},
          73 {:rx.jot/styles #{:rx.jot/italic}},
          74 {:rx.jot/styles #{:rx.jot/italic}},
          75 {:rx.jot/styles #{:rx.jot/italic}},
          76 {:rx.jot/styles #{:rx.jot/italic}},
          77 {:rx.jot/styles #{:rx.jot/italic}}}},
        "53efe8fdad6145628816847707cbbabf"
        {:rx.jot/block-id "53efe8fdad6145628816847707cbbabf",
         :rx.jot/content-text
         "The quick brown fox jumps over the lazy dog...",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/index 1500000},
        "e020c38167c74f9a83729eafed4f6516"
        {:rx.jot/block-id "e020c38167c74f9a83729eafed4f6516",
         :rx.jot/content-text "ul 3",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 4000000},
        "2f49766c78a14ad7a75085fdebbbd997"
        {:rx.jot/block-id "2f49766c78a14ad7a75085fdebbbd997",
         :rx.jot/content-text "ol 1",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 5000000,
         :rx.jot/reverse-expansion-text "1. ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph},
        "2b8a3e5b2853464e8d2408d5a5bdbbf0"
        {:rx.jot/block-id "2b8a3e5b2853464e8d2408d5a5bdbbf0",
         :rx.jot/content-text "ol 3",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 6000000},
        "fc12e0d887cd46a3b9b33bc44950c0a6"
        {:rx.jot/block-id "fc12e0d887cd46a3b9b33bc44950c0a6",
         :rx.jot/content-text "ul 1",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 3000000,
         :rx.jot/reverse-expansion-text "* ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph}},
       :rx.jot/block-order
       (sorted-map
         1000000 "5f52cb9b3cbb48d3ab44a2517185565f",
         1500000 "53efe8fdad6145628816847707cbbabf",
         2500000 "52c170b5e99942d4884823ab5234acf7",
         3000000 "fc12e0d887cd46a3b9b33bc44950c0a6",
         3500000 "1cef3aa851544482b8e29cf20a4ed74d",
         4000000 "e020c38167c74f9a83729eafed4f6516",
         5000000 "2f49766c78a14ad7a75085fdebbbd997",
         5500000 "8092623307ed4d8ab037a1cc4d4f1416",
         6000000 "2b8a3e5b2853464e8d2408d5a5bdbbf0"),
       :rx.jot/selection
       {:rx.jot/start ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/end ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/anchor ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/focus ["fc12e0d887cd46a3b9b33bc44950c0a6" 0]}}}]))

(defn web-image []
  (browser/<set-root!
    [root
     {:initial-doc
      (-> (jot/base-doc)
          (jot/append-block
            (jot/create-block
              {::jot/type ::jot/media
               ::jot/atomic? true
               :media/url "https://images.unsplash.com/photo-1509631179647-0177331693ae?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=934&q=80"}))
          (jot/append-block
            (jot/heading-two
              {:text "foo bar"}))
          (jot/add-block-behaviors
            {::jot/media {::jot/render bjot/render-media}}))}]))

(defn placeholder []
  (browser/<set-root!
    [root
     {:!doc (r/atom (jot/empty-doc))
      :render-placeholder
      (fn [& _]
        [:div
         {:style {:margin-left 5
                  :display 'flex
                  :flex-direction 'row
                  :align-items 'center
                  :justify-content 'flex-start
                  :color "#ccc"}}
         [:div
          [:a {:href "#"
               :style {:color "#ccc"}}
           "insert"]]])}]))

(defn embed []
  (browser/<set-root!
    [root
     {:!doc (r/atom
              (-> (jot/empty-doc)
                  (jot/append-block
                    (jot/create-block
                      {::jot/embed? true
                       ::jot/type :test-embed}))
                  (jot/append-block
                    (jot/create-block
                      {::jot/embed? true
                       ::jot/type :test-embed}))
                  (jot/append-block
                    (jot/para))))
      :embeds [{::jot/type :test-embed
                ::jot/render
                (fn []
                  (r/create-class
                    {:reagent-render
                     (fn []
                       [:div
                        {:style {:padding 30
                                 :text-align 'center
                                 :background-color "#eee"}}
                        (ks/uuid)])}))}]
      :render-placeholder
      (fn [& _]
        [:div
         {:style {:margin-left 5
                  :display 'flex
                  :flex-direction 'row
                  :align-items 'center
                  :justify-content 'flex-start
                  :color "#ccc"}}
         [:div
          [:a {:href "#"
               :style {:color "#ccc"}}
           "insert"]]])}]))

(defn embed-view []
  (browser/<set-root!
    [root
     {:!doc (r/atom
              (-> (jot/empty-doc)
                  (jot/append-block
                    (jot/create-block
                      {::jot/embed? true
                       ::jot/type :test-embed}))
                  (jot/append-block
                    (jot/create-block
                      {::jot/embed? true
                       ::jot/type :test-embed}))
                  (jot/append-block
                    (jot/para))))
      :embeds [{::jot/type :test-embed
                ::view/route
                [(fn []
                   {:render
                    (fn []
                      (r/create-class
                        {:reagent-render
                         (fn []
                           [:div
                            {:style {:padding 30
                                     :text-align 'center
                                     :background-color "#eee"}}
                            (ks/uuid)])}))})]}]
      :render-placeholder
      (fn [& _]
        [:div
         {:style {:margin-left 5
                  :display 'flex
                  :flex-direction 'row
                  :align-items 'center
                  :justify-content 'flex-start
                  :color "#ccc"}}
         [:div
          [:a {:href "#"
               :style {:color "#ccc"}}
           "insert"]]])}]))

(defn deco-basic []
  (browser/<set-root!
    [root
     {:!doc (r/atom
              {:rx.jot/block-id->block
               {"cde8743030ee4fca8486b3a5b8343bc7"
                {:rx.jot/block-id "cde8743030ee4fca8486b3a5b8343bc7",
                 :rx.jot/type :rx.jot/paragraph,
                 :rx.jot/content-text "foo",
                 :rx.jot/index 1000000,
                 :rx.jot/offset->deco
                 {1 {:rx.jot/styles #{:rx.jot/bold}},
                  2 {:rx.jot/styles #{:rx.jot/bold}},
                  3 {:rx.jot/styles #{:rx.jot/bold}}}}},
               :rx.jot/block-order
               (sorted-map
                 1000000 "cde8743030ee4fca8486b3a5b8343bc7"),
               :rx.jot/selection
               {:rx.jot/start ["cde8743030ee4fca8486b3a5b8343bc7" 4],
                :rx.jot/end ["cde8743030ee4fca8486b3a5b8343bc7" 4],
                :rx.jot/anchor ["cde8743030ee4fca8486b3a5b8343bc7" 4],
                :rx.jot/focus ["cde8743030ee4fca8486b3a5b8343bc7" 4]}}
              
              #_(-> (jot/base-doc)
                    (jot/append-block
                      (jot/para {:id "foo"
                                 :text "hello world"}))
                    (jot/set-selection
                      {:start ["foo" 0]})
                    (jot/update-current-block
                      jot/update-styles
                      0 5
                      (fn [styles]
                        #{::jot/bold
                          ::jot/italic
                          ::jot/underline}))))
      :embeds [{::jot/type :test-embed
                ::view/route
                [(fn []
                   {:render
                    (fn []
                      (r/create-class
                        {:reagent-render
                         (fn []
                           [:div
                            {:style {:padding 30
                                     :text-align 'center
                                     :background-color "#eee"}}
                            (ks/uuid)])}))})]}]
      :render-placeholder
      (fn [& _]
        [:div
         {:style {:margin-left 5
                  :display 'flex
                  :flex-direction 'row
                  :align-items 'center
                  :justify-content 'flex-start
                  :color "#ccc"}}
         [:div
          [:a {:href "#"
               :style {:color "#ccc"}}
           "insert"]]])}]))

(defn custom-theme []
  (browser/<set-root!
    [root
     {:theme {::bjot/bg-color "black"
              ::bjot/fg-color "white"
              ::bjot/header-fg-color "#aaa"
              ::bjot/font-family "monospace"
              ::bjot/font-size 20}
      :!doc (r/atom
              (-> (jot/base-doc)
                  (jot/append-block
                    (jot/create-block
                      {::jot/type ::jot/heading-three
                       ::jot/content-text "Custom Themed Document"
                       ::jot/reverse-expansion-text "## "
                       ::jot/reverse-expansion-type ::jot/paragraph}))
                  (jot/append-block
                    (jot/para
                      {:text "Black background, white text, grey headers, monospaced font."}))
                  (jot/append-block
                    (jot/create-block
                      {::jot/type ::jot/ordered-list-item
                       ::jot/content-text "ol 1"}))
                  (jot/append-block
                    (jot/create-block
                      {::jot/type ::jot/unordered-list-item
                       ::jot/content-text "ul 1"}))))}]))

(defn merge-inline-embeds []
  (browser/<set-root!
    [root
     {:initial-doc
      (let [embed-id-1 (ks/uuid)
            embed-id-2 (ks/uuid)]
        (-> (jot/base-doc)
            (jot/append-block
              (-> (jot/create-block
                    {::jot/type ::jot/paragraph
                     ::jot/content-text "Here's a link embed."})
                  (jot/set-decos
                    9
                    (->> (range 4)
                         (map (fn [i]
                                {::jot/embed-id embed-id-1}))))))
            (jot/append-block
              (-> (jot/create-block
                    {::jot/type ::jot/paragraph
                     ::jot/content-text "Here's a link embed."})
                  (jot/set-decos
                    9
                    (->> (range 4)
                         (map (fn [i]
                                {::jot/embed-id embed-id-2}))))
                  ))
            (jot/set-embed-data
              {::jot/embed-id embed-id-1
               ::jot/embed-type ::jot/link
               :url "https://google.com"})
            (jot/set-embed-data
              {::jot/embed-id embed-id-2
               ::jot/embed-type ::jot/link
               :url "https://google.com"})))}]))

(defn unordered-list []
  (browser/<set-root!
    [root {:initial-doc (-> (jot/base-doc)
                            (jot/append-block
                              (jot/create-block
                                {::jot/type ::jot/ordered-list-item
                                 ::jot/content-text "ol 1"})))}]))

(defn nested-list []
  (browser/<set-root!
    [root {:initial-doc (-> (jot/base-doc)
                            (jot/append-block
                              (jot/create-block
                                {::jot/type ::jot/unordered-list-item
                                 ::jot/content-text "ol 1"}))
                            (jot/append-block
                              (jot/create-block
                                {::jot/type ::jot/unordered-list-item
                                 ::jot/content-text "indented 1"
                                 ::jot/indent-level 1}))
                            (jot/append-block
                              (jot/create-block
                                {::jot/type ::jot/unordered-list-item
                                 ::jot/content-text "indented 2"
                                 ::jot/indent-level 2}))
                            (jot/append-block
                              (jot/create-block
                                {::jot/type ::jot/unordered-list-item
                                 ::jot/content-text "indented 4"
                                 ::jot/indent-level 4})))}]))

(defn inline-embed-period []
  (browser/<set-root!
    [root
     {:initial-doc
      (let [embed-id (ks/uuid)]
        (-> (jot/base-doc)
            (jot/append-block
              (-> (jot/create-block
                    {::jot/type ::jot/paragraph
                     ::jot/content-text "Here's a link."})
                  (jot/set-decos
                    9
                    (->> (range 4)
                         (map (fn [i]
                                {::jot/embed-id embed-id}))))))
            (jot/set-embed-data
              {::jot/embed-id embed-id
               ::jot/embed-type ::jot/link
               :url "https://google.com"})))}]))

(defn single-block-embed []
  (browser/<set-root!
    [root
     {:initial-doc
      (-> {:rx.jot/block-id->block
           {"25ab38d727824a77b167b7d20c431c91"
            {:rx.jot/block-id "25ab38d727824a77b167b7d20c431c91",
             :rx.jot/type :rx.jot/heading-two,
             :rx.jot/content-text "The Last Question (2 Paragraphs)"},
            "6e1f85f1fe474560bc67691b6417eb5a"
            {:rx.jot/block-id "6e1f85f1fe474560bc67691b6417eb5a",
             :rx.jot/type :rx.jot/paragraph,
             :rx.jot/content-text "The Last Question by Isaac Asimov © 1956"},},
           :rx.jot/block-order {0 "25ab38d727824a77b167b7d20c431c91",
                                1000 "6e1f85f1fe474560bc67691b6417eb5a",},
           :rx.jot/selection
           {:rx.jot/start ["6e1f85f1fe474560bc67691b6417eb5a" 0],
            :rx.jot/end ["6e1f85f1fe474560bc67691b6417eb5a" 10],
            :rx.jot/anchor ["6e1f85f1fe474560bc67691b6417eb5a" 0]
            :rx.jot/focus ["6e1f85f1fe474560bc67691b6417eb5a" 10]},
           :rx.jot/undo-stack [],
           :rx.jot/redo-stack [], 
           :rx.jot/block-transforms nil}
          jot/edn->doc
          (jot/set-sel-inline-embed
            {::jot/embed-type :highlight
             ::jot/embed-id "my-embed"})
          #_(jot/remove-inline-embed
              "my-embed"))
      :doc-filter (fn [doc]
                    (select-keys doc
                      [::jot/block-id->block
                       ::jot/selection
                       ::jot/embed-id->embed-data]))
      :embeds
      [{::jot/type :highlight
        ::jot/render
        (fn [opts embed-data text]
          [:span (merge
                   {:style {:background-color 'yellow}}
                   (select-keys opts [:data-start-offset :data-end-offset :data-block-id]))
           text])}]}]))

(defn multiblock-inline-embed []
  (browser/<set-root!
    [root
     {:initial-doc
      (-> {:rx.jot/block-id->block
           {"25ab38d727824a77b167b7d20c431c91"
            {:rx.jot/block-id "25ab38d727824a77b167b7d20c431c91",
             :rx.jot/type :rx.jot/heading-two,
             :rx.jot/content-text "The Last Question (2 Paragraphs)"},
            "6e1f85f1fe474560bc67691b6417eb5a"
            {:rx.jot/block-id "6e1f85f1fe474560bc67691b6417eb5a",
             :rx.jot/type :rx.jot/paragraph,
             :rx.jot/content-text "The Last Question by Isaac Asimov © 1956"},
            "090e3277787244178f73bc3c1ef1ad8b"
            {:rx.jot/block-id "090e3277787244178f73bc3c1ef1ad8b",
             :rx.jot/type :rx.jot/paragraph,
             :rx.jot/content-text
             "The last question was asked for the first time, half in jest, on May 21, 2061, at a time when humanity first stepped into the light. The question came about as a result of a five dollar bet over highballs, and it happened this way:"}
            "090e3277787244178f73bc3c1ef1ad8c"
            {:rx.jot/block-id "090e3277787244178f73bc3c1ef1ad8c",
             :rx.jot/type :rx.jot/paragraph,
             :rx.jot/content-text
             "The quick brown fox jumps over the lazy dog"}},
           :rx.jot/block-order {0 "25ab38d727824a77b167b7d20c431c91",
                                1000 "6e1f85f1fe474560bc67691b6417eb5a",
                                2000 "090e3277787244178f73bc3c1ef1ad8b"
                                3000 "090e3277787244178f73bc3c1ef1ad8c"},
           :rx.jot/selection
           {:rx.jot/start ["6e1f85f1fe474560bc67691b6417eb5a" 10],
            :rx.jot/end ["090e3277787244178f73bc3c1ef1ad8c" 10],
            :rx.jot/anchor ["6e1f85f1fe474560bc67691b6417eb5a" 10]
            :rx.jot/focus ["090e3277787244178f73bc3c1ef1ad8c" 10],},
           :rx.jot/undo-stack [],
           :rx.jot/redo-stack [], 
           :rx.jot/block-transforms nil}
          jot/edn->doc
          (jot/set-sel-inline-embed
            {::jot/embed-type :highlight
             ::jot/embed-id "my-embed"})
          #_(jot/remove-inline-embed
              "my-embed"))
      :embeds
      [{::jot/type :highlight
        ::jot/render
        (fn [opts embed-data text]
          [:span (merge
                   {:style {:background-color 'yellow}}
                   (select-keys opts [:data-start-offset :data-end-offset :data-block-id]))
           text])}]}]))

(defn docception []
  (browser/<set-root!
    [root
     {:initial-doc
      (-> (jot/base-doc)
          (jot/append-block
            (jot/para {:text "Level 0"}))
          (jot/append-block
            (let [embed-id (ks/uuid)]
              (-> (jot/create-block
                    {::jot/type ::jot/docception
                     ::jot/atomic? true
                     ::jot/doc (-> (jot/base-doc)
                                   (jot/append-block
                                     (jot/para {:text "level 1"})))})))))}]))

(defn gutter []
  (browser/<set-root!
    [root
     {:initial-doc
      {:rx.jot/block-id->block
       {"5f52cb9b3cbb48d3ab44a2517185565f"
        {:rx.jot/block-id "5f52cb9b3cbb48d3ab44a2517185565f",
         :rx.jot/type :rx.jot/heading-one,
         :rx.jot/content-text "Hello World Heading",
         :rx.jot/index 1000000,
         :rx.jot/reverse-expansion-text "# ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph},
        "1cef3aa851544482b8e29cf20a4ed74d"
        {:rx.jot/block-id "1cef3aa851544482b8e29cf20a4ed74d",
         :rx.jot/content-text "ul 2",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 3500000},
        "8092623307ed4d8ab037a1cc4d4f1416"
        {:rx.jot/block-id "8092623307ed4d8ab037a1cc4d4f1416",
         :rx.jot/content-text "ol 2",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 5500000},
        "52c170b5e99942d4884823ab5234acf7"
        {:rx.jot/block-id "52c170b5e99942d4884823ab5234acf7",
         :rx.jot/content-text
         "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/index 2500000,
         :rx.jot/offset->deco
         {28 {:rx.jot/styles #{:rx.jot/bold}},
          29 {:rx.jot/styles #{:rx.jot/bold}},
          30 {:rx.jot/styles #{:rx.jot/bold}},
          31 {:rx.jot/styles #{:rx.jot/bold}},
          32 {:rx.jot/styles #{:rx.jot/bold}},
          33 {:rx.jot/styles #{:rx.jot/bold}},
          34 {:rx.jot/styles #{:rx.jot/bold}},
          35 {:rx.jot/styles #{:rx.jot/bold}},
          36 {:rx.jot/styles #{:rx.jot/bold}},
          37 {:rx.jot/styles #{:rx.jot/bold}},
          38 {:rx.jot/styles #{:rx.jot/bold}},
          39 {:rx.jot/styles #{:rx.jot/bold}},
          40 {:rx.jot/styles #{:rx.jot/bold}},
          41 {:rx.jot/styles #{:rx.jot/bold}},
          42 {:rx.jot/styles #{:rx.jot/bold}},
          43 {:rx.jot/styles #{:rx.jot/bold}},
          44 {:rx.jot/styles #{:rx.jot/bold}},
          45 {:rx.jot/styles #{:rx.jot/bold}},
          46 {:rx.jot/styles #{:rx.jot/bold}},
          47 {:rx.jot/styles #{:rx.jot/bold}},
          48 {:rx.jot/styles #{:rx.jot/bold}},
          49 {:rx.jot/styles #{:rx.jot/bold}},
          50 {:rx.jot/styles #{:rx.jot/bold}},
          51 {:rx.jot/styles #{:rx.jot/bold}},
          52 {:rx.jot/styles #{:rx.jot/bold}},
          53 {:rx.jot/styles #{:rx.jot/bold}},
          54 {:rx.jot/styles #{:rx.jot/bold}},
          61 {:rx.jot/styles #{:rx.jot/italic}},
          62 {:rx.jot/styles #{:rx.jot/italic}},
          63 {:rx.jot/styles #{:rx.jot/italic}},
          64 {:rx.jot/styles #{:rx.jot/italic}},
          65 {:rx.jot/styles #{:rx.jot/italic}},
          66 {:rx.jot/styles #{:rx.jot/italic}},
          67 {:rx.jot/styles #{:rx.jot/italic}},
          68 {:rx.jot/styles #{:rx.jot/italic}},
          69 {:rx.jot/styles #{:rx.jot/italic}},
          70 {:rx.jot/styles #{:rx.jot/italic}},
          71 {:rx.jot/styles #{:rx.jot/italic}},
          72 {:rx.jot/styles #{:rx.jot/italic}},
          73 {:rx.jot/styles #{:rx.jot/italic}},
          74 {:rx.jot/styles #{:rx.jot/italic}},
          75 {:rx.jot/styles #{:rx.jot/italic}},
          76 {:rx.jot/styles #{:rx.jot/italic}},
          77 {:rx.jot/styles #{:rx.jot/italic}}}},
        "53efe8fdad6145628816847707cbbabf"
        {:rx.jot/block-id "53efe8fdad6145628816847707cbbabf",
         :rx.jot/content-text
         "The quick brown fox jumps over the lazy dog...",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/index 1500000},
        "e020c38167c74f9a83729eafed4f6516"
        {:rx.jot/block-id "e020c38167c74f9a83729eafed4f6516",
         :rx.jot/content-text "ul 3",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 4000000},
        "2f49766c78a14ad7a75085fdebbbd997"
        {:rx.jot/block-id "2f49766c78a14ad7a75085fdebbbd997",
         :rx.jot/content-text "ol 1",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 5000000,
         :rx.jot/reverse-expansion-text "1. ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph},
        "2b8a3e5b2853464e8d2408d5a5bdbbf0"
        {:rx.jot/block-id "2b8a3e5b2853464e8d2408d5a5bdbbf0",
         :rx.jot/content-text "ol 3",
         :rx.jot/type :rx.jot/ordered-list-item,
         :rx.jot/index 6000000},
        "fc12e0d887cd46a3b9b33bc44950c0a6"
        {:rx.jot/block-id "fc12e0d887cd46a3b9b33bc44950c0a6",
         :rx.jot/content-text "ul 1",
         :rx.jot/type :rx.jot/unordered-list-item,
         :rx.jot/index 3000000,
         :rx.jot/reverse-expansion-text "* ",
         :rx.jot/reverse-expansion-type :rx.jot/paragraph}},
       :rx.jot/block-order
       (sorted-map
         1000000 "5f52cb9b3cbb48d3ab44a2517185565f",
         1500000 "53efe8fdad6145628816847707cbbabf",
         2500000 "52c170b5e99942d4884823ab5234acf7",
         3000000 "fc12e0d887cd46a3b9b33bc44950c0a6",
         3500000 "1cef3aa851544482b8e29cf20a4ed74d",
         4000000 "e020c38167c74f9a83729eafed4f6516",
         5000000 "2f49766c78a14ad7a75085fdebbbd997",
         5500000 "8092623307ed4d8ab037a1cc4d4f1416",
         6000000 "2b8a3e5b2853464e8d2408d5a5bdbbf0"),
       :rx.jot/selection
       {:rx.jot/start ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/end ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/anchor ["fc12e0d887cd46a3b9b33bc44950c0a6" 0],
        :rx.jot/focus ["fc12e0d887cd46a3b9b33bc44950c0a6" 0]}}
      #_(-> (jot/base-doc)
            (jot/append-block
              (let [embed-id (ks/uuid)]
                (-> (jot/create-block
                      {::jot/type ::jot/paragraph
                       ::jot/content-text "Hello world"})))))
      :right-gutter-width 100
      :render-right-gutter
      (fn [ed]
        (let [{:keys [top]} (first (bjot/sel-layout ed))]
          (when top
            (when-not (bjot/sel-collapsed? ed)
              [:div
               {:style {:position 'absolute
                        :width "100%"
                        :height 10
                        :top top
                        :left 10
                        :background-color 'red}}]))))}]))

(defn initial-selected []
  (browser/<set-root!
    [root
     {:initial-doc
      {:rx.jot/block-id->block
       {"25ab38d727824a77b167b7d20c431c91"
        {:rx.jot/block-id "25ab38d727824a77b167b7d20c431c91",
         :rx.jot/type :rx.jot/heading-two,
         :rx.jot/content-text "The Last Question (2 Paragraphs)"},
        "6e1f85f1fe474560bc67691b6417eb5a"
        {:rx.jot/block-id "6e1f85f1fe474560bc67691b6417eb5a",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/content-text "The Last Question by Isaac Asimov © 1956"},
        "090e3277787244178f73bc3c1ef1ad8b"
        {:rx.jot/block-id "090e3277787244178f73bc3c1ef1ad8b",
         :rx.jot/type :rx.jot/paragraph,
         :rx.jot/content-text
         "The last question was asked for the first time, half in jest, on May 21, 2061, at a time when humanity first stepped into the light. The question came about as a result of a five dollar bet over highballs, and it happened this way:"}},
       :rx.jot/block-order
       {0 "25ab38d727824a77b167b7d20c431c91",
        1000 "6e1f85f1fe474560bc67691b6417eb5a",
        2000 "090e3277787244178f73bc3c1ef1ad8b"},
       :rx.jot/selection
       {:rx.jot/start ["090e3277787244178f73bc3c1ef1ad8b" 112],
        :rx.jot/end ["090e3277787244178f73bc3c1ef1ad8b" 132],
        :rx.jot/anchor ["090e3277787244178f73bc3c1ef1ad8b" 112],
        :rx.jot/focus ["090e3277787244178f73bc3c1ef1ad8b" 132]},
       :rx.jot/undo-stack [],
       :rx.jot/redo-stack [], 
       :rx.jot/block-transforms nil}}]))


(comment

  (set! *print-namespace-maps* false)

  (solo)

  (empty-doc)

  (one-line)

  (populated)

  (web-image)

  (placeholder)

  (embed)

  (embed-view)

  (deco-basic)

  (custom-theme)

  (merge-inline-embeds)

  (unordered-list)

  (nested-list)

  (inline-embed-period)

  (single-block-embed)

  (multiblock-inline-embed)

  (docception)

  (gutter)

  (initial-selected)

  )



(comment

  (ks/pp
    (ee/get-edits
      (es/diff)))

  )

