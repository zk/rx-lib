(ns rx.browser.jot-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.styleguide :as sg]
            [rx.browser.jot :as bjot]
            [rx.browser :as browser]
            [rx.browser.util :as bu]
            [rx.browser.youtube :as yt]
            [rx.browser.components :as cmp]
            [rx.browser.test-ui :as test-ui]
            [rx.theme :as th]
            [rx.view :as view]
            [rx.jot :as jot]
            [rx.browser.youtube :as yt]
            [dommy.core :as dommy]
            [reagent.core :as r]
            [rx.jot-test]
            [clojure.string :as str]
            [cljs.core.async
             :as async
             :refer [<! put! chan sliding-buffer timeout]
             :refer-macros [go go-loop]]))

(defn tap-to-interact [_ _]
  (let [!interacting? (r/atom false)
        !plate-mounted? (r/atom true)
        interacting-ch (chan (sliding-buffer 1))]
    (go-loop []
      (let [interacting? (<! interacting-ch)]
        (when (not= interacting? @!interacting?)
          (if interacting?
            (do
              (reset! !interacting? true)
              (<! (timeout 150))
              (reset! !plate-mounted? false))
            (do
              (reset! !plate-mounted? true)
              (<! (timeout 17))
              (reset! !interacting? false))))
        (recur)))
    (fn [{:keys [style] :as opts} child]
      (let [[comp child-opts & rest] child
            child-opts (merge
                         child-opts
                         {:interacting? @!interacting?})]
        [:div
         {:class (bu/kw->class ::scroll-preventer-wrapper)
          :style (merge
                   {:position 'relative}
                   style)}
         [:div
          {:on-key-down (fn [e]
                          (when (= "Escape" (.-key e))
                            (put! interacting-ch false)))
           :on-blur (fn [e]
                      (put! interacting-ch false))
           :style {:width "100%"
                   :height "100%"
                   :overflow-y 'scroll}}
          (into
            [comp child-opts]
            rest)]
         (when @!plate-mounted?
           [cmp/hover
            {:on-mouse-down (fn []
                              (put! interacting-ch true))
             :style
             {:position 'absolute
              :top 0
              :left 0
              :right 0
              :bottom 0
              :display 'flex
              :justify-content 'center
              :align-items 'center
              :transition "opacity 150ms ease"
              :opacity (if @!interacting? 0 1)
              :background-color "rgba(255,255,255,0.95)"
              :cursor 'pointer}}
            "Tap To Focus"])]))))

(declare last-question-text)

(def headings
  [{:key ::jot
    :level 0
    :title "Jot"}
   {:key ::inline-styling
    :level 1
    :title "Inline Styling"}
   {:key ::inline-custom-embeds
    :level 1
    :title "Inline Custom Embeds"}
   {:key ::text-expansion
    :level 1
    :title "Text Expansion"}
   {:key ::toolbar
    :level 1
    :title "Toolbar"}
   {:key ::block-transforms
    :level 1
    :title "Block Transforms"}
   {:key ::custom-block-embeds
    :level 1
    :title "Custom Block Embeds"}
   {:key ::defining-custom-embeds
    :level 2
    :title "Defining Custom Embeds"}
   {:key ::render-lifecycle
    :level 2
    :title "Render Lifecycle"}
   {:key ::placeholder-component
    :level 1
    :title "Placeholder Component"}
   {:key ::text-input
    :level 1
    :title "Text Input"}
   {:key ::usage
    :level 1
    :title "Usage"}
   {:key ::component-opts
    :level 2
    :title "Component Options"}
   {:key ::document-api
    :level 2
    :title "Document API"}
   {:key ::theming
    :level 1
    :title "Theming"}
   {:key ::theme-rules
    :level 2
    :title "Theme Rules"}
   {:key ::included-custom-block-types
    :level 1
    :title "Included Custom Block Types"}
   {:key ::testing
    :level 1
    :title "Testing"}
   {:key ::editor-stress-test
    :level 1
    :title "Editor Stress Test"}])


(defn sections [opts]
  (let [{:keys [::bg-color
                ::fg-color
                ::border-color
                ::embed-bg-color]}
        (th/des opts
          [[::bg-color :color/bg-0]
           [::border-color :color/bg-2]
           [::embed-bg-color :color/bg-2]])]
    [:div
     {:class [(bu/kw->class ::sections)
              (bu/kw->class :rx.browser.jot-styleguide)]}
     [sg/section-container

      [sg/section-intro
       {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1867/grandville-magpie-writer-768.jpg"
                :height 300}}
       [sg/heading headings ::jot]
       [:p "Rich text editing for the browser and React Native. Many ideas taken from "
        [:a {:href "https://draftjs.org/" :target "_blank"} "draftjs"]
        "."]
       [:p "Jot documents can contain both block level elements, like paragraphs, lists, and headers, inline styling like italics, and inline elements like links. Theming."]
       [:p "Jot is built to be extensible, with support for user-defined block and inline elements."]
       [:p "An example:"]
       [sg/checkerboard
        [bjot/editor
         {:style {:border-radius 3
                  :padding (th/pad opts 4)}
          :initial-doc
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
             :rx.jot/indent-level 1
             :rx.jot/index 3500000},
            "8092623307ed4d8ab037a1cc4d4f1416"
            {:rx.jot/block-id "8092623307ed4d8ab037a1cc4d4f1416",
             :rx.jot/content-text "ol 2",
             :rx.jot/type :rx.jot/ordered-list-item,
             :rx.jot/indent-level 1
             :rx.jot/index 5500000},
            "52c170b5e99942d4884823ab5234acf7"
            {:rx.jot/block-id "52c170b5e99942d4884823ab5234acf7",
             :rx.jot/content-text
             "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
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
            :rx.jot/focus ["fc12e0d887cd46a3b9b33bc44950c0a6" 0]}}}]]]

      [sg/section opts
       [sg/heading headings ::inline-styling]
       [:p "Jot supports typical inline styles like bold / italic / underline. User-provided inline components are supported via a decoration system that supports uses cases like links and mentions."]

       [sg/checkerboard
        [bjot/editor
         {:style {:padding (th/pad opts 4)}
          :initial-doc (-> (jot/base-doc)
                           (jot/append-block
                             (-> (jot/create-block
                                   {::jot/type ::jot/paragraph
                                    ::jot/content-text "Inline text styling"})
                                 (jot/set-decos
                                   0
                                   (->> (range 6)
                                        (map (fn [i]
                                               {::jot/styles #{::jot/bold}}))))
                                 (jot/set-decos
                                   7
                                   (->> (range 4)
                                        (map (fn [i]
                                               {::jot/styles #{::jot/italic}}))))
                                 (jot/set-decos
                                   12
                                   (->> (range 7)
                                        (map (fn [i]
                                               {::jot/styles #{::jot/underline}}))))))
                           )}]]]
      [sg/section opts
       [sg/heading headings ::inline-custom-embeds]
       [:p "Links, tweets, etc."]
       [sg/checkerboard
        [bjot/editor
         {:style {:padding (th/pad opts 4)}
          :initial-doc (let [embed-id (ks/uuid)]
                         (-> (jot/base-doc)
                             (jot/append-block
                               (-> (jot/create-block
                                     {::jot/type ::jot/paragraph
                                      ::jot/content-text "Here's a link embed (meta-click to open)."})
                                   (jot/set-decos
                                     9
                                     (->> (range 4)
                                          (map (fn [i]
                                                 {::jot/embed-id embed-id}))))))
                             (jot/set-embed-data
                               {::jot/embed-id embed-id
                                ::jot/embed-type ::jot/link
                                :url "https://google.com"})))}]]]
      [sg/section opts
       [sg/heading headings ::text-expansion]
       [:p "Some block types and inline styles can be applied using text expansion. Most markdown elements and styles are supproted."]
       [:p "For example, prefixing a line in a paragraph block with `# ` or `* ` will change the current block to a heading or list respectively. **Earmuffs** for bold / italic. Backspace to reverse the expansion for block elements."]
       [sg/checkerboard
        [bjot/editor
         {:style {:padding (th/pad opts 4)}
          :initial-doc (-> (jot/base-doc)
                           (jot/append-block
                             (jot/create-block
                               {::jot/type ::jot/heading-three
                                ::jot/content-text "heading"
                                ::jot/reverse-expansion-text "### "
                                ::jot/reverse-expansion-type ::jot/paragraph}))
                           (jot/append-block
                             (-> (jot/create-block
                                   {::jot/type ::jot/paragraph
                                    ::jot/content-text "bold"})
                                 (jot/set-decos 0
                                   (->> (range 4)
                                        (map (fn []
                                               {::jot/styles #{::jot/bold}}))))))
                           (jot/append-block
                             (-> (jot/create-block
                                   {::jot/type ::jot/paragraph
                                    ::jot/content-text "italic"})
                                 (jot/set-decos 0
                                   (->> (range 6)
                                        (map (fn []
                                               {::jot/styles #{::jot/italic}})))))))}]]]
      [sg/section opts
       [sg/heading headings ::toolbar]
       [:p "Jot ships with a toolbar that you can choose to use to affect inline styles and embeds."]]
      [sg/section opts
       [sg/heading headings ::block-transforms]
       [:p "Block transforms provide a way for you to participate in changes to blocks, like when the user types a character, to change the content of that block based on behvaior you provide. For example, you could use a content transform to automatically turn an entered url into a link via a regex match."]
       [:p "Block transforms are run after the default editor behavior has been applied, in the order that the transforms are provided."]
       [:p "See the " [:code ":block-transforms"] " config option for more info."]]
      [sg/section opts
       [sg/heading headings ::custom-block-embeds]
       [:p "Embeds are a way for you to render custom UI components in your documents, in and around built-in blocks like headings and paragraphs. Embeds can be anything that's able to be rendered in a browser -- images, videos, charts, tweets, anything."]
       [:p "Embed participation in the editing lifecycle is up to you to implement, most editing commands like movement, insertion, and deletion are noops when focus is on a block embed, and it's up to you to provide controls within the embed to acomplish these types of actions."]
       [:p "Example:"]
       [sg/checkerboard
        [bjot/editor
         {:style {:padding (th/pad opts 4)}
          :initial-doc
          (-> (jot/base-doc)
              (jot/append-block
                (jot/create-block
                  {::jot/type ::jot/heading-two
                   ::jot/content-text "Custom Block Embed"}))
              (jot/append-block
                (jot/create-block
                  {::jot/embed? true
                   ::jot/type :test-embed}))
              (jot/append-block
                (jot/para)))
          :embeds [{::jot/type :test-embed
                    ::jot/render
                    (fn []
                      (r/create-class
                        {:reagent-render
                         (fn []
                           [:div
                            {:style {:padding 30
                                     :text-align 'center
                                     :background-color embed-bg-color}}
                            [:div "Custom rendered div, see editor option " [:strong [:code ":embeds"]] "."]
                            (ks/uuid)])}))}]}]]
       [sg/heading headings ::defining-custom-embeds]
       [:p "Custom embeds are identified via a custom block type chosen by you, found in the doc's block map at key " [:code ":rx.jot/type"] ". The block will also have " [:code ":rx.jot/embed?"] " set to " [:code "true"] ". For example:"]
       [sg/code-block
        (ks/pp-str
          {::jot/block-id (ks/uuid)
           ::jot/type :my/custom-embed
           ::jot/embed? true})]
       [:p "Embed-specific data can also be attached to this map and will be passed to the render method you provide via the " [:code ":embeds"] " option."]
       [sg/heading headings ::render-lifecycle]
       [:p "Jot does its best to not unecessarily re-render your custom blocks, but there are a few cases where this is unavoidable. Re-rendering will trigger when the block map representing the embed changes."]]

      [sg/section
       [sg/heading headings ::placeholder-component]
       [:p "Jot give you the option to render a custom component when the user's cursor is on an empty paragraph. You can use this to give the user a way to insert custom blocks."]
       [:p "The placeholder will disappear as soon as you enter text into the paragraph."]
       [sg/checkerboard
        [bjot/editor
         {:style {:padding (th/pad opts 4)}
          :initial-doc (jot/empty-doc)
          :render-placeholder
          (fn []
            [:div
             {:style {:padding-left 5}}
             [:a {:href "#"
                  :style {:color "#888"}
                  :on-click (fn [e]
                              (.preventDefault e)
                              (js/alert "Hello World!")
                              nil)}
              "Insert Custom Block"]])}]]
       [sg/code-block
        (ks/pp-str
          '[bjot/editor
            {:initial-doc (jot/empty-doc)
             :render-placeholder
             (fn []
               [:div
                {:style {:padding-left 5}}
                [:a {:href "#"
                     :style {:color "#888"}
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (js/alert "Hello World!")
                                 nil)}
                 "Insert Custom Block"]])}])]]
      [sg/section
       [sg/heading headings ::text-input]
       [:p "Jot can be used as a replacement for the native text input in cases where you'd like to support inline styling or embeds."]
       [sg/checkerboard
        [bjot/editor
         {:initial-doc (-> (jot/base-doc)
                           (jot/append-block
                             (jot/para
                               {:text "hello world"})))}]]]
      [sg/section
       [:div {:style {:float 'right}}
        [sg/section-callout
         [yt/video
          {:video-id "BMegu18G19Q"
           :style {:height 400
                   :width 220}}]]]
       [:h2 {:id "mobile-embedding"} "Mobile Embedding"]
       [:p "Jot can be used to support rich text editing in react native apps. See namespace "
        [:code "vee.jot"]
        ". All jot features are supported including custom block and inline embeds."]
       [:p "This is accomplished by bundling the jot web assets (html, css, js) with the mobile app and loading them into a web view. However this all happens under the hood. The API used create and interact with the component feels the same as any other React Native component."]
       [:h3
        {:id "jot-creating-an-embedded-distro"}
        "Creating an Embedded Distro"]
       [:p "Instead of providing a single embedded distribution, rx makes it easy to configure and create your own. This is necessary to support your custom block and inline embed functionality."]
       [:p "Code that provides custom embed UI has to live and run in the browser."]
       [:p "Creating a embedded distro takes three steps:"]
       [:ol
        [:li "Create the custom embed initialization namespace."]
        [:li "Compile and write the web assets to the filesystem"]
        [:li "Pass custom embed info to RN component"]]
       [:p "Here's an example custom embed entry point:"]
       [sg/code-block
        (ks/pp-str
          '(rne/init-embedded-editor
             {:embeds
              [{::jot/type :test-embed
                ::view/route
                [(fn []
                   {:render
                    (fn []
                      [:div
                       {:style {:padding 30
                                :font-size 12
                                :text-align 'center
                                :background-color "#eee"}}
                       [:div "Custom Block"]
                       (ks/uuid)])})]}]}))]
       [:div {:style {:clear 'both}}]]
      

      [sg/section opts
       [sg/heading headings ::usage]
       [:p [:code "[rx.browser.jot/editor {opts}]"]]
       [sg/heading headings ::component-opts]
       [sg/options-list
        {:options
         [[:initial-doc
           "The initial doc state used to initalize the jot editor. State will be maintained internally, and participation in updates can be achived via " [:code ":on-change."]]
          [:state
           [:span "Editor state that is used in many api calls to imperitivelyaffect changes to the editor. Can be created outside of the editor via " [:code "(rx.browser.jot/editor-state)"] " or received via option " [:code ":with-state"] "."]]
          [:with-state
           "State map will be passed as the only argument to this function."]
          [:on-change-doc
           "Called with edn doc state when doc state changes"]
          [:on-change-content
           "Called with edn doc state when doc content changes. Ignores things like selection changes."]
          [:render-placeholder
           "Component shown when the cursor is on an empty paragraph. Can be used for UI around changing block type and inserting custom blocks."]
          [:embeds
           [:span
            "Seq of maps, where each map represents a custom embed. Each map must provide " [:code ":type"] " and " [:code ":render"] "."]]]}]
       [sg/heading headings ::document-api]
       [:p "You may want to modify the document programmatically."]
       [sg/options-list
        {:options
         [['(rx.jot/empty-doc)
           "Creates an jot document with a single empty paragraph."]
          ['(rx.jot/append-block doc block)
           "Appends a block to the end of the document"]]}]]
      (let [theme {:bg-color "black"
                   :fg-color "white"
                   :header-fg-color "#aaa"
                   :font-family "monospace"
                   :font-size 20}]
        [sg/section opts
         [sg/heading headings ::theming]
         [:p "Look and feel of the editor can be customized via " [:code "rx.theme"] "."]
         [sg/code-block
          (str
            ":theme\n"
            (ks/pp-str theme))]
         [sg/checkerboard
          [bjot/editor
           {:style {:padding (th/pad opts 4)}
            :theme theme
            :initial-doc (-> (jot/base-doc)
                             (jot/append-block
                               (jot/create-block
                                 {::jot/type ::jot/heading-three
                                  ::jot/content-text "Custom Themed Document"
                                  ::jot/reverse-expansion-text "## "
                                  ::jot/reverse-expansion-type ::jot/paragraph}))
                             (jot/append-block
                               (jot/para
                                 {:text "Black background, white text, grey headers, monospaced font."})))}]]
         [sg/heading headings ::theme-rules]
         [sg/theme-rules-list
          {:theme-rules bjot/theme-info}]])
      [sg/section
       [sg/heading headings ::included-custom-block-types]
       [:p "Jot provides several custom block types, like media embedding."]
       [sg/checkerboard
        [bjot/editor
         {:style {:padding 20}
          :rx.browser.jot.gallery/on-choose-media
          (fn [medias i]
            (js/alert (str "Chose " (inc i) " of " (count medias))))
          :initial-doc
          (-> (jot/base-doc)
              (jot/append-block
                (jot/create-block
                  {::jot/type ::jot/heading-one
                   ::jot/content-text "Built-In Custom Block Types"}))
              (jot/append-block
                (jot/create-block
                  {::jot/type ::jot/heading-two
                   ::jot/content-text "Gallery of #fashion on Unsplash"}))
              (jot/append-block
                (jot/create-block
                  {::jot/type ::jot/gallery
                   ::jot/embed? true
                   :rx.media/medias
                   [{:rx.media/uri "https://images.unsplash.com/photo-1496747611176-843222e1e57c?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=800&q=80"}
                    {:rx.media/uri "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=800&q=80"}
                    {:rx.media/uri "https://images.unsplash.com/photo-1485968579580-b6d095142e6e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=800&q=60"}
                    {:rx.media/uri "https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=800&q=80"}
                    {:rx.media/uri "https://images.unsplash.com/photo-1475180098004-ca77a66827be?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=933&q=80"}]})))}]]]
      [sg/section opts
       [sg/heading headings ::testing]
       [test-ui/run-and-report-detail
        {:test-context
         {:namespaces [:rx.jot-test]}}]]
      [sg/section opts
       [sg/heading headings ::editor-stress-test]
       [:p "It's important that the editor remain as performant as possible, even in the face of large documents. Much of this comes down to the rendering code found in "
        [:code "rx.browser.jot"]
        "."]
       [sg/checkerboard
        [:div {:style {:height 300
                       :overflow-y 'scroll}}
         [sg/async-mount
          (fn []
            (go
              (<! (timeout 500))
              [bjot/editor
               {:style {:padding (th/pad opts 4)}
                :initial-doc (let [paras (->> (str/split
                                                last-question-text
                                                #"\n")
                                              (remove empty?)
                                              (mapv (fn [s]
                                                      (jot/para
                                                        {:text s})))
                                              (take 200))
                                   doc (-> (jot/base-doc)
                                           (jot/append-block
                                             (jot/create-block
                                               {::jot/type ::jot/heading-two
                                                ::jot/content-text
                                                (str "The Last Question"
                                                     " ("
                                                     (count paras)
                                                     " Paragraphs)")
                                                #_"WUTHERING HEIGHTS"})))]

                               (->> paras
                                    (reduce
                                      (fn [doc block]
                                        (jot/append-block doc block))
                                      doc)))}]))]]]]]]))

(defn init []
  (browser/<set-root!
    [sg/standalone
     {:component sections
      :headings headings}]))


(def last-question-text
  "The Last Question by Isaac Asimov © 1956

The last question was asked for the first time, half in jest, on May 21, 2061, at a time when humanity first stepped into the light. The question came about as a result of a five dollar bet over highballs, and it happened this way:
Alexander Adell and Bertram Lupov were two of the faithful attendants of Multivac. As well as any human beings could, they knew what lay behind the cold, clicking, flashing face -- miles and miles of face -- of that giant computer. They had at least a vague notion of the general plan of relays and circuits that had long since grown past the point where any single human could possibly have a firm grasp of the whole.

Multivac was self-adjusting and self-correcting. It had to be, for nothing human could adjust and correct it quickly enough or even adequately enough -- so Adell and Lupov attended the monstrous giant only lightly and superficially, yet as well as any men could. They fed it data, adjusted questions to its needs and translated the answers that were issued. Certainly they, and all others like them, were fully entitled to share In the glory that was Multivac's.

For decades, Multivac had helped design the ships and plot the trajectories that enabled man to reach the Moon, Mars, and Venus, but past that, Earth's poor resources could not support the ships. Too much energy was needed for the long trips. Earth exploited its coal and uranium with increasing efficiency, but there was only so much of both.

But slowly Multivac learned enough to answer deeper questions more fundamentally, and on May 14, 2061, what had been theory, became fact.

The energy of the sun was stored, converted, and utilized directly on a planet-wide scale. All Earth turned off its burning coal, its fissioning uranium, and flipped the switch that connected all of it to a small station, one mile in diameter, circling the Earth at half the distance of the Moon. All Earth ran by invisible beams of sunpower.

Seven days had not sufficed to dim the glory of it and Adell and Lupov finally managed to escape from the public function, and to meet in quiet where no one would think of looking for them, in the deserted underground chambers, where portions of the mighty buried body of Multivac showed. Unattended, idling, sorting data with contented lazy clickings, Multivac, too, had earned its vacation and the boys appreciated that. They had no intention, originally, of disturbing it.

They had brought a bottle with them, and their only concern at the moment was to relax in the company of each other and the bottle.

\"It's amazing when you think of it,\" said Adell. His broad face had lines of weariness in it, and he stirred his drink slowly with a glass rod, watching the cubes of ice slur clumsily about. \"All the energy we can possibly ever use for free. Enough energy, if we wanted to draw on it, to melt all Earth into a big drop of impure liquid iron, and still never miss the energy so used. All the energy we could ever use, forever and forever and forever.\"

Lupov cocked his head sideways. He had a trick of doing that when he wanted to be contrary, and he wanted to be contrary now, partly because he had had to carry the ice and glassware. \"Not forever,\" he said.

\"Oh, hell, just about forever. Till the sun runs down, Bert.\"

\"That's not forever.\"

\"All right, then. Billions and billions of years. Twenty billion, maybe. Are you satisfied?\"

Lupov put his fingers through his thinning hair as though to reassure himself that some was still left and sipped gently at his own drink. \"Twenty billion years isn't forever.\"

\"Will, it will last our time, won't it?\"

\"So would the coal and uranium.\"

\"All right, but now we can hook up each individual spaceship to the Solar Station, and it can go to Pluto and back a million times without ever worrying about fuel. You can't do THAT on coal and uranium. Ask Multivac, if you don't believe me.\"

\"I don't have to ask Multivac. I know that.\"

\"Then stop running down what Multivac's done for us,\" said Adell, blazing up. \"It did all right.\"

\"Who says it didn't? What I say is that a sun won't last forever. That's all I'm saying. We're safe for twenty billion years, but then what?\" Lupov pointed a slightly shaky finger at the other. \"And don't say we'll switch to another sun.\"

There was silence for a while. Adell put his glass to his lips only occasionally, and Lupov's eyes slowly closed. They rested.

Then Lupov's eyes snapped open. \"You're thinking we'll switch to another sun when ours is done, aren't you?\"

\"I'm not thinking.\"

\"Sure you are. You're weak on logic, that's the trouble with you. You're like the guy in the story who was caught in a sudden shower and Who ran to a grove of trees and got under one. He wasn't worried, you see, because he figured when one tree got wet through, he would just get under another one.\"

\"I get it,\" said Adell. \"Don't shout. When the sun is done, the other stars will be gone, too.\"

\"Darn right they will,\" muttered Lupov. \"It all had a beginning in the original cosmic explosion, whatever that was, and it'll all have an end when all the stars run down. Some run down faster than others. Hell, the giants won't last a hundred million years. The sun will last twenty billion years and maybe the dwarfs will last a hundred billion for all the good they are. But just give us a trillion years and everything will be dark. Entropy has to increase to maximum, that's all.\"

\"I know all about entropy,\" said Adell, standing on his dignity.

\"The hell you do.\"

\"I know as much as you do.\"

\"Then you know everything's got to run down someday.\"

\"All right. Who says they won't?\"

\"You did, you poor sap. You said we had all the energy we needed, forever. You said 'forever.'\"

\"It was Adell's turn to be contrary. \"Maybe we can build things up again someday,\" he said.

\"Never.\"

\"Why not? Someday.\"

\"Never.\"

\"Ask Multivac.\"

\"You ask Multivac. I dare you. Five dollars says it can't be done.\"

Adell was just drunk enough to try, just sober enough to be able to phrase the necessary symbols and operations into a question which, in words, might have corresponded to this: Will mankind one day without the net expenditure of energy be able to restore the sun to its full youthfulness even after it had died of old age?

Or maybe it could be put more simply like this: How can the net amount of entropy of the universe be massively decreased?

Multivac fell dead and silent. The slow flashing of lights ceased, the distant sounds of clicking relays ended.

Then, just as the frightened technicians felt they could hold their breath no longer, there was a sudden springing to life of the teletype attached to that portion of Multivac. Five words were printed: INSUFFICIENT DATA FOR MEANINGFUL ANSWER.

\"No bet,\" whispered Lupov. They left hurriedly.

By next morning, the two, plagued with throbbing head and cottony mouth, had forgotten about the incident.

Jerrodd, Jerrodine, and Jerrodette I and II watched the starry picture in the visiplate change as the passage through hyperspace was completed in its non-time lapse. At once, the even powdering of stars gave way to the predominance of a single bright marble-disk, centered.
\"That's X-23,\" said Jerrodd confidently. His thin hands clamped tightly behind his back and the knuckles whitened.

The little Jerrodettes, both girls, had experienced the hyperspace passage for the first time in their lives and were self-conscious over the momentary sensation of inside-outness. They buried their giggles and chased one another wildly about their mother, screaming, \"We've reached X-23 -- we've reached X-23 -- we've ----\"

\"Quiet, children,\" said Jerrodine sharply. \"Are you sure, Jerrodd?\"

\"What is there to be but sure?\" asked Jerrodd, glancing up at the bulge of featureless metal just under the ceiling. It ran the length of the room, disappearing through the wall at either end. It was as long as the ship.

Jerrodd scarcely knew a thing about the thick rod of metal except that it was called a Microvac, that one asked it questions if one wished; that if one did not it still had its task of guiding the ship to a preordered destination; of feeding on energies from the various Sub-galactic Power Stations; of computing the equations for the hyperspacial jumps.

Jerrodd and his family had only to wait and live in the comfortable residence quarters of the ship.

Someone had once told Jerrodd that the \"ac\" at the end of \"Microvac\" stood for \"analog computer\" in ancient English, but he was on the edge of forgetting even that.

Jerrodine's eyes were moist as she watched the visiplate. \"I can't help it. I feel funny about leaving Earth.\"

\"Why for Pete's sake?\" demanded Jerrodd. \"We had nothing there. We'll have everything on X-23. You won't be alone. You won't be a pioneer. There are over a million people on the planet already. Good Lord, our great grandchildren will be looking for new worlds because X-23 will be overcrowded.\"

Then, after a reflective pause, \"I tell you, it's a lucky thing the computers worked out interstellar travel the way the race is growing.\"

\"I know, I know,\" said Jerrodine miserably.

Jerrodette I said promptly, \"Our Microvac is the best Microvac in the world.\"

\"I think so, too,\" said Jerrodd, tousling her hair.

It was a nice feeling to have a Microvac of your own and Jerrodd was glad he was part of his generation and no other. In his father's youth, the only computers had been tremendous machines taking up a hundred square miles of land. There was only one to a planet. Planetary ACs they were called. They had been growing in size steadily for a thousand years and then, all at once, came refinement. In place of transistors had come molecular valves so that even the largest Planetary AC could be put into a space only half the volume of a spaceship.

Jerrodd felt uplifted, as he always did when he thought that his own personal Microvac was many times more complicated than the ancient and primitive Multivac that had first tamed the Sun, and almost as complicated as Earth's Planetary AC (the largest) that had first solved the problem of hyperspatial travel and had made trips to the stars possible.

\"So many stars, so many planets,\" sighed Jerrodine, busy with her own thoughts. \"I suppose families will be going out to new planets forever, the way we are now.\"

\"Not forever,\" said Jerrodd, with a smile. \"It will all stop someday, but not for billions of years. Many billions. Even the stars run down, you know. Entropy must increase.\"

\"What's entropy, daddy?\" shrilled Jerrodette II.

\"Entropy, little sweet, is just a word which means the amount of running-down of the universe. Everything runs down, you know, like your little walkie-talkie robot, remember?\"

\"Can't you just put in a new power-unit, like with my robot?\"

The stars are the power-units, dear. Once they're gone, there are no more power-units.\"

Jerrodette I at once set up a howl. \"Don't let them, daddy. Don't let the stars run down.\"

\"Now look what you've done, \" whispered Jerrodine, exasperated.

\"How was I to know it would frighten them?\" Jerrodd whispered back.

\"Ask the Microvac,\" wailed Jerrodette I. \"Ask him how to turn the stars on again.\"

\"Go ahead,\" said Jerrodine. \"It will quiet them down.\" (Jerrodette II was beginning to cry, also.)

Jarrodd shrugged. \"Now, now, honeys. I'll ask Microvac. Don't worry, he'll tell us.\"

He asked the Microvac, adding quickly, \"Print the answer.\"

Jerrodd cupped the strip of thin cellufilm and said cheerfully, \"See now, the Microvac says it will take care of everything when the time comes so don't worry.\"

Jerrodine said, \"and now children, it's time for bed. We'll be in our new home soon.\"

Jerrodd read the words on the cellufilm again before destroying it: INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.

He shrugged and looked at the visiplate. X-23 was just ahead.

VJ-23X of Lameth stared into the black depths of the three-dimensional, small-scale map of the Galaxy and said, \"Are we ridiculous, I wonder, in being so concerned about the matter?\"
MQ-17J of Nicron shook his head. \"I think not. You know the Galaxy will be filled in five years at the present rate of expansion.\"

Both seemed in their early twenties, both were tall and perfectly formed.

\"Still,\" said VJ-23X, \"I hesitate to submit a pessimistic report to the Galactic Council.\"

\"I wouldn't consider any other kind of report. Stir them up a bit. We've got to stir them up.\"

VJ-23X sighed. \"Space is infinite. A hundred billion Galaxies are there for the taking. More.\"

\"A hundred billion is not infinite and it's getting less infinite all the time. Consider! Twenty thousand years ago, mankind first solved the problem of utilizing stellar energy, and a few centuries later, interstellar travel became possible. It took mankind a million years to fill one small world and then only fifteen thousand years to fill the rest of the Galaxy. Now the population doubles every ten years --\"

VJ-23X interrupted. \"We can thank immortality for that.\"

\"Very well. Immortality exists and we have to take it into account. I admit it has its seamy side, this immortality. The Galactic AC has solved many problems for us, but in solving the problems of preventing old age and death, it has undone all its other solutions.\"

\"Yet you wouldn't want to abandon life, I suppose.\"

\"Not at all,\" snapped MQ-17J, softening it at once to, \"Not yet. I'm by no means old enough. How old are you?\"

\"Two hundred twenty-three. And you?\"

\"I'm still under two hundred. --But to get back to my point. Population doubles every ten years. Once this Galaxy is filled, we'll have another filled in ten years. Another ten years and we'll have filled two more. Another decade, four more. In a hundred years, we'll have filled a thousand Galaxies. In a thousand years, a million Galaxies. In ten thousand years, the entire known Universe. Then what?\"

VJ-23X said, \"As a side issue, there's a problem of transportation. I wonder how many sunpower units it will take to move Galaxies of individuals from one Galaxy to the next.\"

\"A very good point. Already, mankind consumes two sunpower units per year.\"

\"Most of it's wasted. After all, our own Galaxy alone pours out a thousand sunpower units a year and we only use two of those.\"

\"Granted, but even with a hundred per cent efficiency, we can only stave off the end. Our energy requirements are going up in geometric progression even faster than our population. We'll run out of energy even sooner than we run out of Galaxies. A good point. A very good point.\"

\"We'll just have to build new stars out of interstellar gas.\"

\"Or out of dissipated heat?\" asked MQ-17J, sarcastically.

\"There may be some way to reverse entropy. We ought to ask the Galactic AC.\"

VJ-23X was not really serious, but MQ-17J pulled out his AC-contact from his pocket and placed it on the table before him.

\"I've half a mind to,\" he said. \"It's something the human race will have to face someday.\"

He stared somberly at his small AC-contact. It was only two inches cubed and nothing in itself, but it was connected through hyperspace with the great Galactic AC that served all mankind. Hyperspace considered, it was an integral part of the Galactic AC.

MQ-17J paused to wonder if someday in his immortal life he would get to see the Galactic AC. It was on a little world of its own, a spider webbing of force-beams holding the matter within which surges of sub-mesons took the place of the old clumsy molecular valves. Yet despite it's sub-etheric workings, the Galactic AC was known to be a full thousand feet across.

MQ-17J asked suddenly of his AC-contact, \"Can entropy ever be reversed?\"

VJ-23X looked startled and said at once, \"Oh, say, I didn't really mean to have you ask that.\"

\"Why not?\"

\"We both know entropy can't be reversed. You can't turn smoke and ash back into a tree.\"

\"Do you have trees on your world?\" asked MQ-17J.

The sound of the Galactic AC startled them into silence. Its voice came thin and beautiful out of the small AC-contact on the desk. It said: THERE IS INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.

VJ-23X said, \"See!\"

The two men thereupon returned to the question of the report they were to make to the Galactic Council.

Zee Prime's mind spanned the new Galaxy with a faint interest in the countless twists of stars that powdered it. He had never seen this one before. Would he ever see them all? So many of them, each with its load of humanity - but a load that was almost a dead weight. More and more, the real essence of men was to be found out here, in space.
Minds, not bodies! The immortal bodies remained back on the planets, in suspension over the eons. Sometimes they roused for material activity but that was growing rarer. Few new individuals were coming into existence to join the incredibly mighty throng, but what matter? There was little room in the Universe for new individuals.

Zee Prime was roused out of his reverie upon coming across the wispy tendrils of another mind.

\"I am Zee Prime,\" said Zee Prime. \"And you?\"

\"I am Dee Sub Wun. Your Galaxy?\"

\"We call it only the Galaxy. And you?\"

\"We call ours the same. All men call their Galaxy their Galaxy and nothing more. Why not?\"

\"True. Since all Galaxies are the same.\"

\"Not all Galaxies. On one particular Galaxy the race of man must have originated. That makes it different.\"

Zee Prime said, \"On which one?\"

\"I cannot say. The Universal AC would know.\"

\"Shall we ask him? I am suddenly curious.\"

Zee Prime's perceptions broadened until the Galaxies themselves shrunk and became a new, more diffuse powdering on a much larger background. So many hundreds of billions of them, all with their immortal beings, all carrying their load of intelligences with minds that drifted freely through space. And yet one of them was unique among them all in being the originals Galaxy. One of them had, in its vague and distant past, a period when it was the only Galaxy populated by man.

Zee Prime was consumed with curiosity to see this Galaxy and called, out: \"Universal AC! On which Galaxy did mankind originate?\"

The Universal AC heard, for on every world and throughout space, it had its receptors ready, and each receptor lead through hyperspace to some unknown point where the Universal AC kept itself aloof.

Zee Prime knew of only one man whose thoughts had penetrated within sensing distance of Universal AC, and he reported only a shining globe, two feet across, difficult to see.

\"But how can that be all of Universal AC?\" Zee Prime had asked.

\"Most of it, \" had been the answer, \"is in hyperspace. In what form it is there I cannot imagine.\"

Nor could anyone, for the day had long since passed, Zee Prime knew, when any man had any part of the making of a universal AC. Each Universal AC designed and constructed its successor. Each, during its existence of a million years or more accumulated the necessary data to build a better and more intricate, more capable successor in which its own store of data and individuality would be submerged.

The Universal AC interrupted Zee Prime's wandering thoughts, not with words, but with guidance. Zee Prime's mentality was guided into the dim sea of Galaxies and one in particular enlarged into stars.

A thought came, infinitely distant, but infinitely clear. \"THIS IS THE ORIGINAL GALAXY OF MAN.\"

But it was the same after all, the same as any other, and Zee Prime stifled his disappointment.

Dee Sub Wun, whose mind had accompanied the other, said suddenly, \"And Is one of these stars the original star of Man?\"

The Universal AC said, \"MAN'S ORIGINAL STAR HAS GONE NOVA. IT IS NOW A WHITE DWARF.\"

\"Did the men upon it die?\" asked Zee Prime, startled and without thinking.

The Universal AC said, \"A NEW WORLD, AS IN SUCH CASES, WAS CONSTRUCTED FOR THEIR PHYSICAL BODIES IN TIME.\"

\"Yes, of course,\" said Zee Prime, but a sense of loss overwhelmed him even so. His mind released its hold on the original Galaxy of Man, let it spring back and lose itself among the blurred pin points. He never wanted to see it again.

Dee Sub Wun said, \"What is wrong?\"

\"The stars are dying. The original star is dead.\"

\"They must all die. Why not?\"

\"But when all energy is gone, our bodies will finally die, and you and I with them.\"

\"It will take billions of years.\"

\"I do not wish it to happen even after billions of years. Universal AC! How may stars be kept from dying?\"

Dee sub Wun said in amusement, \"You're asking how entropy might be reversed in direction.\"

And the Universal AC answered. \"THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.\"

Zee Prime's thoughts fled back to his own Galaxy. He gave no further thought to Dee Sub Wun, whose body might be waiting on a galaxy a trillion light-years away, or on the star next to Zee Prime's own. It didn't matter.

Unhappily, Zee Prime began collecting interstellar hydrogen out of which to build a small star of his own. If the stars must someday die, at least some could yet be built.

Man considered with himself, for in a way, Man, mentally, was one. He consisted of a trillion, trillion, trillion ageless bodies, each in its place, each resting quiet and incorruptible, each cared for by perfect automatons, equally incorruptible, while the minds of all the bodies freely melted one into the other, indistinguishable.
Man said, \"The Universe is dying.\"

Man looked about at the dimming Galaxies. The giant stars, spendthrifts, were gone long ago, back in the dimmest of the dim far past. Almost all stars were white dwarfs, fading to the end.

New stars had been built of the dust between the stars, some by natural processes, some by Man himself, and those were going, too. White dwarfs might yet be crashed together and of the mighty forces so released, new stars built, but only one star for every thousand white dwarfs destroyed, and those would come to an end, too.

Man said, \"Carefully husbanded, as directed by the Cosmic AC, the energy that is even yet left in all the Universe will last for billions of years.\"

\"But even so,\" said Man, \"eventually it will all come to an end. However it may be husbanded, however stretched out, the energy once expended is gone and cannot be restored. Entropy must increase to the maximum.\"

Man said, \"Can entropy not be reversed? Let us ask the Cosmic AC.\"

The Cosmic AC surrounded them but not in space. Not a fragment of it was in space. It was in hyperspace and made of something that was neither matter nor energy. The question of its size and Nature no longer had meaning to any terms that Man could comprehend.

\"Cosmic AC,\" said Man, \"How may entropy be reversed?\"

The Cosmic AC said, \"THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.\"

Man said, \"Collect additional data.\"

The Cosmic AC said, \"I WILL DO SO. I HAVE BEEN DOING SO FOR A HUNDRED BILLION YEARS. MY PREDECESSORS AND I HAVE BEEN ASKED THIS QUESTION MANY TIMES. ALL THE DATA I HAVE REMAINS INSUFFICIENT.\"

\"Will there come a time,\" said Man, \"when data will be sufficient or is the problem insoluble in all conceivable circumstances?\"

The Cosmic AC said, \"NO PROBLEM IS INSOLUBLE IN ALL CONCEIVABLE CIRCUMSTANCES.\"

Man said, \"When will you have enough data to answer the question?\"

\"THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.\"

\"Will you keep working on it?\" asked Man.

The Cosmic AC said, \"I WILL.\"

Man said, \"We shall wait.\"

\"The stars and Galaxies died and snuffed out, and space grew black after ten trillion years of running down.
One by one Man fused with AC, each physical body losing its mental identity in a manner that was somehow not a loss but a gain.

Man's last mind paused before fusion, looking over a space that included nothing but the dregs of one last dark star and nothing besides but incredibly thin matter, agitated randomly by the tag ends of heat wearing out, asymptotically, to the absolute zero.

Man said, \"AC, is this the end? Can this chaos not be reversed into the Universe once more? Can that not be done?\"

AC said, \"THERE IS AS YET INSUFFICIENT DATA FOR A MEANINGFUL ANSWER.\"

Man's last mind fused and only AC existed -- and that in hyperspace.

Matter and energy had ended and with it, space and time. Even AC existed only for the sake of the one last question that it had never answered from the time a half-drunken computer ten trillion years before had asked the question of a computer that was to AC far less than was a man to Man.
All other questions had been answered, and until this last question was answered also, AC might not release his consciousness.

All collected data had come to a final end. Nothing was left to be collected.

But all collected data had yet to be completely correlated and put together in all possible relationships.

A timeless interval was spent in doing that.

And it came to pass that AC learned how to reverse the direction of entropy.

But there was now no man to whom AC might give the answer of the last question. No matter. The answer -- by demonstration -- would take care of that, too.

For another timeless interval, AC thought how best to do this. Carefully, AC organized the program.

The consciousness of AC encompassed all of what had once been a Universe and brooded over what was now Chaos. Step by step, it must be done.

And AC said, \"LET THERE BE LIGHT!\"

And there was light----")



(comment

  (brow)

  (ks/pp
    (parse-toc
      (sections {})))

  (init)

  )


