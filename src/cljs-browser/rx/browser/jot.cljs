(ns rx.browser.jot
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.jot :as jot]
            [rx.view :as view]
            [rx.theme :as th]
            [rx.media :as media]
            [rx.browser.jot.gallery :as gallery]
            [rx.browser.clipboard :as cb]
            [rx.browser.components :as cmp]
            [clojure.data :as data]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [goog.object :as gobj]
            [goog.string :as gstring]
            [hickory.core :as hic]
            [clojure.walk :as walk]
            [dommy.core :as dommy
             :include-macros true]
            [cljs.core.async :as async
             :refer [<! put! chan sliding-buffer timeout alts!
                     close!]
             :refer-macros [go go-loop]]))

(def theme-info
  [{:rule [:bg-color ::bg-color :color/bg-0]
    :doc "Default background color used for the editor and more"}
   {:rule [:fg-color ::fg-color :color/fg-0]
    :doc "Default text color. Used in paragraphs, headings, etc"}
   {:rule [:font-family ::font-family]
    :doc "Default font family"}
   {:rule [:font-size ::font-size 16]
    :doc "Base font size"}
   {:rule [:block-space-bottom
           ::block-space-bottom (th/size-mult 3)]
    :doc "Default space below all rendered blocks. Can be overridden by block-specific theme rules."}
   {:rule [:header-fg-color ::header-fg-color :color/fg-0]
    :doc "Header text color"}
   {:rule [:header-space-top
           ::header-space-top
           ::block-space-bottom
           (th/size-mult 5)]
    :doc "Margin over header"}
   {:rule [:header-space-bottom
           ::header-space-bottom
           ::block-space-bottom
           (th/size-mult 3)]
    :doc "Margin under header"}
   {:rule [:list-item-space-bottom
           ::list-item-space-bottom (th/size-mult 1)]
    :doc "Space between list items"}
   {:rule [:ul-list-style-type ::ul-list-style-type "decimal"]
    :doc "Unordered list css list-style-type"}
   {:rule [:ul-item-space-left ::ul-item-space-left ::list-item-space-left (th/size-mult 6)]
    :doc "Left offset of unordered list items"}
   {:rule [:ol-list-style-type ::ol-list-style-type "decimal"]
    :doc "Ordered list css list-style-type"}
   {:rule [:ol-space-bottom
           ::ol-space-bottom
           ::block-space-bottom
           (th/size-mult 3)]}
   {:rule [:ul-space-bottom
           ::ul-space-bottom
           ::block-space-bottom
           (th/size-mult 3)]}
   {:rule [:ol-item-space-left
           ::ol-item-space-left
           ::list-item-space-left
           (th/size-mult 2)]
    :doc "Left offset of ordered list items"}
   {:rule [:blockquote-space-bottom
           ::blockquote-space-bottom
           ::block-space-bottom (th/size-mult 3)]}])

(def theme-rules
  (->> theme-info
       (map :rule)))

(defn backspace? [e]
  (and
    (not (.-metaKey e))
    (= "Backspace" (.-key e))))

(defn enter? [e]
  (= "Enter" (.-key e)))

(defn noop? [e]
  (get #{"Control" "Meta" "Shift" "Alt" "Dead" "CapsLock"} (.-key e)))

(defn key-event->op [e]
  (cond
    (backspace? e) [:backspace-character (.-key e)]
    (and (.-shiftKey e)
         (enter? e))
    [:insert-string "\n"]
    
    (enter? e) [:split-block]
    (noop? e) [:noop]
    ;;(= "ArrowLeft" (.-key e)) [:move-backwards]
    ;;(= "ArrowRight" (.-key e)) [:move-forwards]
    ;;(= "ArrowUp" (.-key e)) [:move-upwards]
    ;;(= "ArrowDown" (.-key e)) [:move-downwards]
    (and (.-ctrlKey e)
         (= "h" (.-key e))) [:debug]
    (and (.-ctrlKey e)
         (= "a" (.-key e))) [:move-to-beginning]
    (and (.-ctrlKey e)
         (= "e" (.-key e))) [:move-to-end]
    (and (.-ctrlKey e)
         (= "k" (.-key e))) [:kill-line]
    (and (.-ctrlKey e)
         (= "p" (.-key e))) [:move-upwards]
    (and (.-ctrlKey e)
         (= "n" (.-key e))) [:move-downwards]

    (and (.-ctrlKey e)
         (= "f" (.-key e))) [:noop]
    (and (.-ctrlKey e)
         (= "b" (.-key e))) [:noop]

    (and (.-metaKey e)
         (.-shiftKey e)
         (= "z" (.-key e))) [:redo]

    (and (.-metaKey e)
         (= "z" (.-key e))) [:undo]

    (and (.-metaKey e)
         (= "a" (.-key e))) [:select-all]

    (and (.-metaKey e)
         (= "Backspace" (.-key e))) [:prevent-default]

    (and (.-shiftKey e)
         (= "Tab" (.-key e))) [:decrease-indent]

    (and (.-metaKey e)
         (= "i" (.-key e))) (do
                              (.preventDefault e)
                              [:noop])

    (and (.-metaKey e)
         (= "b" (.-key e))) (do
                              (.preventDefault e)
                              [:noop])

    (= "Tab" (.-key e)) [:increase-indent]

    (.-metaKey e) [:noop]
    
    :else [:insert-string (.-key e)]))

(defn handle-op [doc [op-key & args :as op] opts]
  #_(when (not (get #{:apply-fn} op-key))
      (prn op))
  (condp = op-key
    :insert-string (jot/insert-string doc (first args) opts)
    :backspace-character (jot/backspace-character doc)
    :split-block (jot/split-at-selection doc)
    :move-to-beginning (jot/move-to-beginning doc)
    :move-to-end (jot/move-to-end doc)
    :move-backwards (apply jot/move-backwards doc args)
    :move-forwards (apply jot/move-forwards doc args)
    :move-upwards (apply jot/move-upwards doc args)
    :move-downwards (apply jot/move-downwards doc args)
    :kill-line (jot/kill-line doc)
    :set-selection (jot/set-selection doc (first args))
    :select-all (jot/select-all doc)
    :undo (jot/undo doc)
    :redo (jot/redo doc)
    :increase-indent (jot/increase-indent doc)
    :decrease-indent (jot/decrease-indent doc)
    :apply-fn ((first args) doc)
    :debug (do (ks/pp
                 (dissoc doc ::jot/undo-stack ::jot/redo-stack)) doc)
    :noop doc
    doc))

(defn id+offset+block->id [id offset block]
  (let [deco (get (::jot/offset->deco block) (dec offset))
        rev-decos (->> (range 0 offset)
                       (map (fn [offset]
                              (get (::jot/offset->deco block) offset)))
                       reverse)

        same-decos (->> rev-decos
                        (take-while #(= deco %)))

        deco-start-offset (- offset (count same-decos))]
    [(str id "-" deco-start-offset)
     deco-start-offset]))

(defn find-node [id offset block]
  (let [[id deco-start-offset] (id+offset+block->id id offset block)
        node (.getElementById js/document id)]
    [(or (and node (.-firstChild node))
         node)
     (- offset deco-start-offset)]))

(defn node-matches? [child-node offset]
  (when (and child-node
             (= 1 (gobj/get child-node "nodeType")))
    (let [start-offset-str
          (.getAttribute child-node "data-start-offset")
          start-offset (ks/parse-long start-offset-str)
          end-offset-str
          (.getAttribute child-node "data-end-offset")
          end-offset (ks/parse-long end-offset-str)]
      (when (and start-offset end-offset)
        (and (>= offset start-offset)
             (<= offset end-offset))))))

(defn text-node? [node]
  (when node
    (= 3 (gobj/get node "nodeType"))))

(defn node->str [node]
  (str "node "
       (.-nodeType node)
       " ["
       (.-textContent node)
       "]"))

(defn drill-down-to-node+boffset [[block-id offset] node]
  (let [match-node
        (->> node
             browser/child-nodes
             (filter #(node-matches? % offset))
             first)
        first-child (first (browser/child-nodes match-node))
        first-child-text? (when first-child
                            (text-node? first-child))]

    (cond
      (and (node-matches? node offset)
           (= 1 (count (browser/child-nodes node)))
           (text-node? (first (browser/child-nodes node))))
      [(first (browser/child-nodes node)) offset]
      
      (and match-node
           first-child-text?)
      (let [start-offset (ks/parse-long
                           (.getAttribute match-node "data-start-offset"))
            boffset (- offset start-offset)]
        [first-child boffset])

      match-node (drill-down-to-node+boffset [block-id offset] match-node)
      
      :else (do
              #_(prn [block-id offset] (node->str node))
              [node 0]))))

(defn dsel-part->node+boffset [[block-id offset] ed-ref]
  
  (let [node (.getElementById js/document block-id)]
    (when node
      (drill-down-to-node+boffset [block-id offset] node))))

(defn dsel->bsel [{:keys [::jot/start ::jot/end ::jot/focus ::jot/anchor]} ed-ref]
  [(dsel-part->node+boffset start ed-ref)
   (dsel-part->node+boffset end ed-ref)
   (dsel-part->node+boffset focus ed-ref)
   (dsel-part->node+boffset anchor ed-ref)])

#_(defn update-document-selection [doc ed-ref]
  (try
    (let [{:keys [::jot/start ::jot/end]} (jot/get-sel doc)
          [start-id start-offset] start
          [end-id end-offset] end
          sel-obj (.getSelection js/window)

          [start-node start-node-offset]
          (find-node start-id start-offset (jot/block-by-id doc start-id))
          
          [end-node end-node-offset]
          (find-node end-id end-offset (jot/block-by-id doc end-id))]
      (.setBaseAndExtent sel-obj start-node start-node-offset end-node end-node-offset))
    (catch js/Error e
      (.error js/console e))))

(defn ensure-cursor-on-screen [ed-ref focus-node]
  (let [ancestor (cmp/find-scrollable-ancestor ed-ref :vertical)
        focus-el (if (text-node? focus-node)
                   (.-parent focus-node)
                   focus-node)
        bounds (cmp/get-bounds
                 focus-el
                 ancestor
                 :vertical
                 {:top 0
                  :bot 0})
        current-position (cmp/get-current-position bounds)]
    (when (and (not= ::cmp/inside current-position)
               (not= ::cmp/invisible current-position))
      (.scrollIntoView focus-el))))

(defn set-browser-sel-from-doc [doc ed-ref]
  (try
    (let [sel-obj (.getSelection js/window)

          [[start-node start-node-offset]
           [end-node end-node-offset]
           [focus-node focus-node-offset]
           [anchor-node anchor-node-offset]]
          (dsel->bsel (jot/get-sel doc) ed-ref)]
      #_(prn "start" (.-id start-node) start-node-offset)
      #_(prn "end" (.-id end-node) end-node-offset)
      (.setBaseAndExtent sel-obj anchor-node anchor-node-offset focus-node focus-node-offset)
      (ensure-cursor-on-screen
        ed-ref
        focus-node))
    (catch js/Error e
      (.error js/console e))))

(defn find-id [node]
  (when node
    (loop [node node]
      (if (or (not node) (not (empty? (.-id node))))
        (.-id node)
        (recur (.-parentNode node))))))

(defn node->block-info [node]
  (cond
    (nil? node) nil
    
    (.-getAttribute node)
    (let [block-id (.getAttribute node "data-block-id")]
      (if (not (empty? block-id))
        {:block-id block-id
         :start-offset (ks/parse-long
                         (.getAttribute node
                           "data-start-offset"))
         :end-offset (ks/parse-long
                       (.getAttribute node
                         "data-end-offset"))}
        (node->block-info (.-parentNode node))))
    
    (.-parentNode node)
    (node->block-info (.-parentNode node))
    
    :else nil))

(defn sel-obj->selection [sel]
  (try
    (let [{anchor-id :block-id
           anchor-start-offset :start-offset}
          (node->block-info (.-anchorNode sel))

          {focus-id :block-id
           focus-start-offset :start-offset}
          (node->block-info (.-focusNode sel))]

      #_(prn "ao" (.-anchorOffset sel))
      #_(prn "fo" (.-focusOffset sel))

      (when (and anchor-id focus-id)
        {:anchor [anchor-id
                  (+ anchor-start-offset
                     (.-anchorOffset sel))]
         :focus [focus-id
                 (+ focus-start-offset
                    (.-focusOffset sel))]}))
    
    #_(let [anchor-node (.-anchorNode sel)
            anchor-id (find-id anchor-node)
            anchor-offset (.-anchorOffset sel)
            focus-node (.-focusNode sel)
            focus-id (find-id focus-node)
            focus-offset (.-focusOffset sel)]
        (when (and anchor-id focus-id)
          {:anchor [anchor-id anchor-offset]
           :focus [focus-id focus-offset]}))
    (catch js/Error e
      (prn e))))

(defn render-default [block]
  [:pre (ks/pp-str block)])

(defn render-style-group [block group offset text]
  (let [{:keys [::jot/styles]} (first group)
        end-offset (+ offset (count group))]
    [:span
     {:id (str (::jot/block-id block) "-" offset)
      :data-block-id (::jot/block-id block)
      :data-start-offset offset
      :data-end-offset end-offset
      :style (merge
               (when (get styles ::jot/bold)
                 {:font-weight 'bold})
               (when (get styles ::jot/italic)
                 {:font-style 'italic})
               (when (get styles ::jot/underline)
                 {:text-decoration 'underline})
               (when (get styles ::jot/code)
                 {:font-family 'monospace}))}
     (subs
       text
       offset
       end-offset)]))

(defn dom-rect->clj [dom-rect]
  (let [r dom-rect]
    {:x (.-x r)
     :y (.-y r)
     :width (.-width r)
     :height (.-height r)
     :top (.-top r)
     :bottom (.-bottom r)
     :left (.-left r)
     :right (.-right r)}))

(defn bounding-client-rect [el]
  (when el
    (dom-rect->clj (.getBoundingClientRect el))))

(defn with-layout [{:keys [on-layout]} child]
  (r/create-class
    {:component-did-mount
     (fn [this]
       (when on-layout
         (let [rect (bounding-client-rect
                      (rdom/dom-node this))]
           (on-layout rect))))
     :component-did-update
     (fn [this]
       (when on-layout
         (let [rect (bounding-client-rect
                      (rdom/dom-node this))]
           (on-layout rect))))
     :component-will-unmount
     (fn [this]
       (when on-layout
         (on-layout nil)))
     :reagent-render
     (fn [_ child]
       child)}))

(defn render-deco-group [{:keys [on-change-embed-layout]
                          :as opts}
                         block
                         group
                         offset
                         text]
  (if (::jot/embed-id (first group))
    (let [embed-data (get-in block [::jot/embed-id->embed-data (::jot/embed-id (first group))])
          render (-> opts
                     :type->render-spec
                     (get (::jot/embed-type embed-data))
                     ::jot/render)]
      (if render
        (let [end-offset (+ offset (count group))
              embed-text (subs text offset end-offset)]
          [:span
           {:key (str (::jot/block-id block) "-" offset)
            :id (str "embed-"
                     (::jot/block-id block)
                     "-"
                     (::jot/embed-id embed-data))
            :data-block-id (::jot/block-id block)
            :data-start-offset offset
            :data-end-offset end-offset}
           [:span
            {:key (str (::jot/block-id block) "-" offset)
             :data-block-id (::jot/block-id block)
             :data-start-offset offset
             :data-end-offset end-offset #_(dec end-offset)}
            [render
             (merge
               opts
               {:key (str (::jot/block-id block) "-" offset)
                :id (str (::jot/block-id block) "-" offset)
                :data-block-id (::jot/block-id block)
                :data-start-offset offset
                :data-end-offset end-offset})
             embed-data embed-text]]
           #_[:span
              {:key (str (::jot/block-id block) "-" (dec end-offset))
               :data-block-id (::jot/block-id block)
               :data-start-offset (dec end-offset)
               :data-end-offset end-offset
               :dangerouslySetInnerHTML {:__html "&#65279"}}]])
        [:span "MISSING EMBED RENDER"]))
    (render-style-group block group offset text)))

(defn render-deco [opts {:keys [::jot/content-text
                                ::jot/offset->deco
                                ::jot/block-id]
                         :as block}]
  (let [groups (->> (range (count content-text))
                    (map (fn [i]
                           (try
                             (get offset->deco i)
                             (catch js/Error e nil))))
                    (partition-by identity))
        els 
        (loop [groups (if (empty? groups)
                        [(repeat (count content-text) nil)]
                        groups)
               offset 0
               children []]
          (if (empty? groups)
            children
            (let [group (first groups)
                  el (render-deco-group opts block group offset content-text)]
              (recur
                (rest groups)
                (+ offset (count group))
                (conj children el)))))]
    (if (empty? (::jot/content-text block))
      [:span
       {:id (str block-id "-0")
        :data-block-id block-id
        :style (merge
                 {:line-height "1.25"
                  :min-height "1.25em"
                  :overflow-wrap 'anywhere})
        :dangerouslySetInnerHTML {:__html "&#65279"}}]
      (into [:span
             {:data-block-id block-id
              :data-start-offset 0
              :data-end-offset (count content-text)}]
        els))))

(defn render-paragraph [_
                        {:keys [::jot/block-id]}]
  (r/create-class
    {:reagent-render
     (fn [{:keys [style
                  selected?
                  render-placeholder
                  render-sel-gutter]
           :as opts}
          {:keys [::jot/content-text
                  ::jot/block-id
                  ::jot/last-block?
                  ::jot/content-text
                  ::jot/selected?]
           :as block}]
       (let [{:keys [block-space-bottom]}
             (th/resolve opts theme-rules)]
         (if (empty? content-text)
           [:div
            {:style {:position 'relative}}
            [:p.jot-block
             {:class (when last-block? "last-block")
              :id block-id
              :data-block-id block-id
              :data-start-offset 0
              :data-end-offset 0
              :style (merge
                       {:line-height "1.5"
                        :min-height "1.25em"
                        :margin-bottom (when-not last-block? block-space-bottom)}
                       style)
              ;;:dangerouslySetInnerHTML {:__html "&#65279"}
              }]
            (when (and selected?
                       (empty? content-text)
                       render-placeholder)
              [:div {:contentEditable false
                     :on-mouse-down (fn [e]
                                      (.stopPropagation e)
                                      #_(.preventDefault e)
                                      nil)
                     :on-mouse-up (fn [e]
                                    (.stopPropagation e)
                                    #_(.preventDefault e)
                                    nil)
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 #_(.preventDefault e)
                                 nil)
                     :style {:position 'absolute
                             :top 0
                             :left 0
                             :line-height "1.25"
                             :min-height "1.25em"}}
               [render-placeholder opts]])]
           [:p.jot-block
            {:class (when last-block? "last-block")
             :id block-id
             :style (merge
                      {:line-height "1.5"
                       :min-height "1.25em"
                       :overflow-wrap 'anywhere
                       :margin-bottom block-space-bottom}
                      style)}
            [render-deco opts block]])))}))

(defn render-header [opts {:keys [::jot/content-text
                                  ::jot/block-id
                                  ::jot/type
                                  ::jot/last-block?
                                  ::jot/first-block?]
                           :as block}]
  (let [h (condp = type
            ::jot/heading-one :h1
            ::jot/heading-two :h2
            ::jot/heading-three :h3)
        {:keys [header-fg-color
                header-space-top
                header-space-bottom]}
        (th/resolve opts theme-rules)]
    [h {:class (str "jot-block " (when last-block? "last-block"))
        :id block-id
        :data-block-id block-id
        :data-start-offset 0
        :data-end-offset (count content-text)
        :style (merge
                 {:line-height "1.25"
                  :min-height "1.25em"
                  :margin-bottom header-space-bottom
                  :color header-fg-color}
                 (when-not first-block?
                   {:margin-top header-space-top}))}
     (if (empty? content-text)
       [:span {:class "empty"}]
       #_content-text
       [render-deco opts block])]))

(defn indent-level->unordered-list-style-type [level]
  (nth (cycle ["disc" "circle" "square"]) (or level 0)))

(defn indent-level->ordered-list-style-type [level]
  (nth (cycle ["decimal" "lower-latin" "upper-latin" "lower-roman" "upper-roman"]) (or level 0)))

(defn render-unordered-list [opts blocks]
  (let [{:keys [ul-item-space-left
                list-item-space-left
                ul-space-bottom]}
        (th/resolve opts theme-rules)

        last-block? (::jot/last-block? (last blocks))]
    [:ul.jot-block
     {:class (when last-block? "last-block")
      :style {:list-style-position 'outside
              :padding-left ul-item-space-left
              :margin-bottom ul-space-bottom}}
     (->> blocks
          (map (fn [{:keys [::jot/block-id
                            ::jot/indent-level]
                     :as block}]
                 (let [indent-level (or indent-level 0)]
                   [:li {:id block-id
                         :data-block-id block-id
                         :key block-id
                         :style {:line-height "1.25"
                                 :min-height "1.25em"
                                 :margin-left (* indent-level 20)
                                 :list-style-type
                                 (indent-level->unordered-list-style-type
                                   indent-level)}}
                    [render-deco opts block]]))))]))

(defn render-ordered-list [opts blocks]
  (let [{:keys [ol-item-space-left
                ol-list-style-type
                ol-space-bottom
                list-item-space-bottom]}
        (th/resolve opts theme-rules)

        last-block? (::jot/last-block? (last blocks))]
    [:ol.jot-block
     {:class (when last-block? "last-block")
      :style {:list-style-type ol-list-style-type
              :list-style-position 'outside
              :padding-left ol-item-space-left
              :margin-bottom ol-space-bottom}}
     (->> blocks
          (map (fn [{:keys [::jot/block-id
                            ::jot/indent-level]
                     :as block}]
                 (let [indent-level (or indent-level 0)]
                   [:li {:id block-id
                         :class (str "indent" indent-level)
                         :data-block-id block-id
                         :key block-id
                         :style {:line-height "1.25"
                                 :min-height "1.25em"
                                 :margin-bottom list-item-space-bottom
                                 :margin-left (* indent-level 20)
                                 :list-style-type
                                 "none"
                                 #_(indent-level->ordered-list-style-type
                                     indent-level)
                                 :counter-increment (str "indent" indent-level)}}
                    [render-deco opts block]]))))]))

(defn render-blockquote [opts blocks]
  (let [{:keys [blockquote-space-bottom]}
        (th/resolve opts theme-rules)

        last-block? (::jot/last-block? (first blocks))]
    [:div
     {:class (when last-block? "last-block")
      :style {:border-left "solid #888 2px"
              :padding-left 14
              :color "#888"}}
     (->> blocks
          (map (fn [{:keys [::jot/block-id] :as block}]
                 [:div {:key block-id}
                  [render-paragraph
                   (merge
                     opts
                     {:style {:margin-bottom blockquote-space-bottom}})
                   block]])))]))

(defn custom-render [{:keys [::jot/render
                             ::view/route 
                             ::jot/grouped?]
                      :as embed-spec}
                     opts
                     blocks]
  (let [full-opts (merge
                    (::jot/opts embed-spec)
                    opts)
        render-vec
        (or
          (let [rendered-route
                (view/render-route
                  route
                  (merge
                    full-opts
                    {::jot/block (first blocks)}))]
            rendered-route)
          [render
           full-opts
           (if grouped?
             blocks
             (first blocks))])]
    (let [block (first blocks)
          {:keys [::jot/block-id ::jot/embed?]} block]
      (if embed?
        [:div
         {:id (str block-id "-0")
          :data-block-id block-id
          :key block-id
          :contentEditable false
          :style {:margin-bottom 16}}
         render-vec]
        render-vec))))

(defn route-wrapper [realized-route]
  (r/create-class
    {:should-component-update
     (fn [_ [_ _ old-opts] [_ _ new-opts]]
       #_(ks/spy "route-spec"
           (data/diff old new))
       #_(ks/spy "opts"
           (data/diff old-opts new-opts))
       #_(or (not= (dissoc old ::view/spec ::view/id)
                   (dissoc new ::view/spec ::view/id))
             (not= old-opts new-opts))
       (let [old-block (::jot/block old-opts)
             new-block (::jot/block new-opts)]
         (not= (dissoc old-block ::jot/index)
               (dissoc new-block ::jot/index))))
     :reagent-render
     (fn [rr opts]
       (let [block (::jot/block opts)]
         [:div
          {:id (str (::jot/block-id block) "-0")
           :data-block-id (::jot/block-id block)
           :key (::jot/block-id block)
           :style {:margin-bottom 16}
           :contentEditable false}
          (view/render-realized-route rr opts)]))}))

(defn render-route [route render-spec opts blocks
                    !block-id->realized-route]
  (let [block (first blocks)
        realized-route (or (get @!block-id->realized-route (::jot/block-id block))
                           (let [rr (view/realize-route route
                                      (merge
                                        opts
                                        {::jot/block (first blocks)
                                         :remove-block (partial (:remove-block-by-id opts) (::jot/block-id block))}))]
                             (swap! !block-id->realized-route
                               assoc
                               (::jot/block-id block)
                               rr)
                             rr))]
    [route-wrapper
     realized-route
     (merge
       opts
       {::jot/block (first blocks)})]))

(defn render-blocks [opts blocks type->render-spec
                     !block-id->realized-route]
  (let [{block-type ::jot/type} (first blocks)
        {:keys [::jot/render
                ::view/route]
         :as render-spec}
        (get type->render-spec block-type)

        {:keys [::jot/block-id]} (first blocks)]

    (cond
      route (render-route
              route
              render-spec
              opts
              blocks
              !block-id->realized-route)

      render
      (custom-render
        render-spec
        opts
        blocks)
      
      (jot/para? (first blocks))
      [render-paragraph
       opts
       (first blocks)]

      (get #{::jot/heading-one
             ::jot/heading-two
             ::jot/heading-three}
        block-type)
      [render-header opts (first blocks)]

      (= ::jot/unordered-list-item block-type)
      [render-unordered-list opts blocks]

      (= ::jot/ordered-list-item block-type)
      [render-ordered-list opts blocks]

      (= ::jot/blockquote block-type)
      [render-blockquote opts blocks]
      
      :else [render-default blocks])))

(def grouped-block-types
  #{::jot/unordered-list-item
    ::jot/ordered-list-item
    ::jot/blockquote})

(defn partition-blocks [block]
  (if (get grouped-block-types
        (::jot/type block))
    (::jot/type block)
    (::jot/block-id block)))

(defn skip-key-event? [e]
  (let [s (.-key e)]
    (or
      (get #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"} s)
      (and (.-ctrlKey e)
           (get #{"p" "n"} s))
      (and (.-metaKey e)
           (get #{"c" "v"} s)))))

(defn paste-in-editor? [e ed-ref]
  (->> (.-path e)
       (filter #(= % ed-ref))
       empty?
       not))

(defn parent? [child-node parent-node]
  (loop [child-node child-node]
    (cond
      (not child-node) nil
      (= child-node parent-node) true
      :else (recur (.-parentNode child-node)))))

(defn load-doc [{:keys [!doc]} doc]
  (reset! !doc
    (-> doc
        (update ::jot/block-order
          (fn [sm]
            (into (sorted-map) sm))))))

(defn blur [{:keys [!ed-ref]}]
  (.blur @!ed-ref))

(defn clear [{:keys [!doc] :as state}]
  (ks/spy "clear" state)
  (reset! !doc (jot/empty-doc)))


;; Editor State API

(defn editor-state [] 
  {:!doc (r/atom nil)
   :!ed-ref (atom nil)
   :paste-ch (chan 100)
   :op-ch (chan 100)
   :!sel-layout (r/atom nil)
   :!block-id->layout (r/atom {})
   :bsel-ch (chan (sliding-buffer 1))
   :cdu-ch (chan (sliding-buffer 1))
   :!selecting? (atom false)
   :!focused? (atom false)
   :!ignore-selection-event? (atom false)
   :!editor-layout (r/atom {:scroll-height 0})
   :!sel-change-handler (atom nil)})

(defn sel-layout [{:keys [!sel-layout]}]
  (when !sel-layout
    @!sel-layout))

(defn bounding-box [rects]
  (when (> (count rects) 0)
    (let [x (->> rects
                 (map :x)
                 (remove nil?)
                 (apply min))
          y (->> rects
                 (map :y)
                 (remove nil?)
                 (apply min))
          top (->> rects
                   (map :top)
                   (remove nil?)
                   (apply min))
          bottom (->> rects
                      (map :bottom)
                      (remove nil?)
                      (apply max))

          left (->> rects
                    (map :left)
                    (remove nil?)
                    (apply min))

          right (->> rects
                     (map :right)
                     (remove nil?)
                     (apply max))

          width (- (->> rects
                        (map (fn [{rx :x
                                   rwidth :width}]
                               (+ rx rwidth)))
                        (apply max))
                   x)

          height (- (->> rects
                         (map (fn [{ry :y
                                    rheight :height}]
                                (+ ry rheight)))
                         (apply max))
                    y)]
      {:x x
       :y y 
       :top top
       :bottom bottom
       :left left
       :right right
       :width width
       :height height})))

(defn sel-bounding-box [ed-state]
  (when-let [ranges (sel-layout ed-state)]
    (bounding-box ranges)))

(defn sel-collapsed? [{:keys [!doc]}]
  (when !doc
    (jot/sel-collapsed? @!doc)))

(defn calc-layout [{:keys [!ed-ref]}]
  (try
    (merge
      (dommy/bounding-client-rect @!ed-ref)
      {:scroll-height (.-scrollHeight @!ed-ref)})
    (catch js/Error e nil)))

(declare editor)

(defn default-embeds []
  [(gallery/embed)
   {::jot/type ::jot/link
    ::jot/render
    (fn [opts embed-data text]
      [:a (merge
            #_opts
            {:target "_blank"
             :href (:url embed-data)
             :on-click (fn [e]
                         (when (.-metaKey e)
                           (.open js/window
                             (:url embed-data)
                             "_blank")))})
       text])}
   {::jot/type ::jot/docception
    ::jot/render
    (fn [opts block]
      [:div
       [editor
        {:style {:background-color 'green
                 :padding 10}
         :initial-doc
         (::jot/doc block)}]])}])

(defn <raf []
  (let [ch (chan)]
    (.requestAnimationFrame
      js/window
      (fn []
        (close! ch)))
    ch))

(defn outer-wrapper-gutter-style [{:keys [right-gutter-width
                                          left-gutter-width]}]
  (merge
    (when right-gutter-width
      #_{:padding-right right-gutter-width})
    (when left-gutter-width
      #_{:padding-left left-gutter-width}))
  )

(defn update-sel-layout [{:keys [!sel-layout
                                 !ed-ref]}]
  (when @!ed-ref
    (let [{:keys [top]} (dom-rect->clj (.getBoundingClientRect @!ed-ref))
          sel (.getSelection js/document)]
      (when (> (.-rangeCount sel) 0)
        (reset! !sel-layout
          (->> (.getClientRects (.getRangeAt (.getSelection js/document) 0))
               (mapv dom-rect->clj)
               (map (fn [m]
                      (merge
                        m
                        {:top (- (:top m) top)})))))))))

(defn left-gutter [{:keys [render-left-gutter
                           left-gutter-width
                           max-left-gutter-width
                           min-left-gutter-width]}
                   ed-state]
  [:div
   {:style {:flex 1
            :max-width (or max-left-gutter-width 0)
            :min-width (or min-left-gutter-width 0)}}
   (when render-left-gutter
     [render-left-gutter ed-state])])

(defn right-gutter [{:keys [render-right-gutter
                            right-gutter-width
                            max-right-gutter-width
                            min-right-gutter-width]}
                    ed-state]
  [:div
   {:style {:flex 1
            :max-width (or max-right-gutter-width 0)
            :min-width (or min-right-gutter-width 0)}}
   (when render-right-gutter
     [render-right-gutter ed-state])])

(defn update-editor-layout [{:keys [on-layout]}
                            {:keys [!editor-layout]
                             :as state}]
  (let [layout (calc-layout state)]
    (when (not= layout @!editor-layout)
      (reset! !editor-layout (calc-layout state))
      (when on-layout
        (on-layout @!editor-layout)))))

(defn init-document [{:keys [initial-doc
                                   block-transforms]}
                           state]
  (when (not @(:!doc state))
    (reset! (:!doc state)
      (-> (if initial-doc
            (jot/edn->doc initial-doc)
            (jot/empty-doc))
          (jot/set-block-transforms block-transforms)))))

(defn handle-selection-change [{:keys [bsel-ch
                                       !ignore-selection-event?]}]
  (when-not @!ignore-selection-event?
    (put! bsel-ch :sel-change))
  
  #_(.log js/console e)
  #_(put! bsel-ch :sel-change)
  
  #_(.log
      js/console
      (.getSelection js/document)))

(defn init-selection-listener [{:keys [!sel-change-handler]
                                :as state}]
  (go
    ;; Otherwise selection event will fire when editor is mounted even
    ;; though no selection change happened
    (<! (browser/<raf))
    (let [listener (fn [e]
                     (handle-selection-change state))]
      (browser/listen!
        js/document
        :selectionchange
        listener)
      (reset! !sel-change-handler listener))))

(defn init-doc-selection [{:keys [!doc !ed-ref]}]
  (set-browser-sel-from-doc
    @!doc
    @!ed-ref))

(defn initialize-editor [{:keys [autofocus?]
                          :as ed-opts}
                         {:keys [!ed-ref]
                          :as state}]
  (update-editor-layout ed-opts state)
  (init-doc-selection state)
  (init-selection-listener state)
  (when autofocus?
    (.focus @!ed-ref))
  (update-sel-layout state))

(defn teardown-editor [{:keys [!sel-change-handler]}]
  (browser/unlisten!
    js/document
    :selectionchange
    @!sel-change-handler))

(defn update-blocks-layout [doc]
  (let [blocks (->> doc
                    jot/blocks-in-order
                    (map (fn [{:keys [::jot/block-id] :as block}]
                           (let [el (.getElementById js/document block-id)]
                             (merge
                               block
                               (when el
                                 {::jot/layout (bounding-client-rect el)}))))))]

    (jot/set-blocks doc blocks)))

(defn update-embeds-layout [doc ed-ref]
  (let [scroller-el (browser/find-scrollable-ancestor ed-ref :vertical)
        scroll-top (or (.-scrollTop scroller-el) 0)
        embed-id->embed-data
        (->> doc
             ::jot/embed-id->embed-data
             vals
             (remove nil?)
             (map (fn [embed]
                    (let [{:keys [::jot/block-id->layout
                                  ::jot/embed-id]}
                          embed]
                      (let [block-id->layout
                            (->> block-id->layout
                                 (map (fn [[block-id layout]]
                                        (let [embed-el-id (str "embed-" block-id "-" embed-id)
                                              embed-el (.getElementById js/document
                                                         embed-el-id)
                                              rect (bounding-client-rect embed-el)]
                                          [block-id
                                           (when rect
                                             (update
                                               rect
                                               :top
                                               #(+ % scroll-top)))])))
                                 (into {}))]
                        (merge
                          embed
                          {::jot/block-id->layout
                           block-id->layout

                           ::jot/layout
                           (bounding-box (vals block-id->layout))})))))
             (map (fn [embed]
                    [(::jot/embed-id embed)
                     embed]))
             (into {}))]
    (assoc
      doc
      ::jot/embed-id->embed-data
      embed-id->embed-data)))

(defn remove-embed [{:keys [!doc] :as ed-state} embed-id]
  (swap! !doc
    jot/remove-inline-embed
    embed-id))

(defn hic-tree->blocks [tree]
  [tree #_(walk/postwalk prn tree)])

(defn parse-paste-html-to-blocks [data-items]
  (let [html-item (->> data-items
                       (some #(when (= "text/html" (::cb/type %)) %)))
        html-text (::cb/text html-item)
        blocks (when html-text
                 (->> html-text
                      hic/parse-fragment
                      (map hic/as-hickory)
                      (mapcat hic-tree->blocks)))]
    blocks))

(comment

  (ks/pp
    (parse-paste-html-to-blocks
      [#_{:rx.browser.clipboard/kind "string",
          :rx.browser.clipboard/type "text/html",
          :rx.browser.clipboard/text
          "<meta charset='utf-8'><p data-pm-slice=\"1 1 []\">January 2004<br><br>Have you ever seen an old photo of yourself and been embarrassed at the way you looked?&nbsp;<em>Did we actually dress like that?</em>&nbsp;We did. And we had no idea how silly we looked. It's the nature of fashion to be invisible, in the same way the movement of the earth is invisible to all of us riding on it.<br><br>What scares me is that there are moral fashions too. They're just as arbitrary, and just as invisible to most people. But they're much more dangerous. Fashion is mistaken for good design; moral fashion is mistaken for good. Dressing oddly gets you laughed at. Violating moral fashions can get you fired, ostracized, imprisoned, or even killed.<br><br>If you could travel back in a time machine, one thing would be true no matter where you went: you'd have to watch what you said. Opinions we consider harmless could have gotten you in big trouble. I've already said at least one thing that would have gotten me in big trouble in most of Europe in the seventeenth century, and did get Galileo in big trouble when he said it — that the earth moves. [1]<br><br>It seems to be a constant throughout history: In every period, people believed things that were just ridiculous, and believed them so strongly that you would have gotten in terrible trouble for saying otherwise.</p>"}

       {:rx.browser.clipboard/kind "string",
        :rx.browser.clipboard/type "text/html",
        :rx.browser.clipboard/text
        "<meta charset='utf-8'><p style=\"box-sizing: inherit; margin-bottom: 1.5em; max-width: 830px; margin-left: auto; margin-right: auto; color: rgb(0, 0, 0); font-family: -apple-system, system-ui, system-ui, &quot;Segoe UI&quot;, Roboto, &quot;Helvetica Neue&quot;, Arial, sans-serif; font-size: 18px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">Now let’s assume that we live in a perfect world in which browsers fixed all sanitizer bypasses and no more exist. Does this mean that we are safe when we are pasting data from untrusted websites? The short answer is: no.</p><h2 style=\"box-sizing: inherit; clear: both; font-weight: 500; font-size: 40px; line-height: 1.1; max-width: 830px; margin-left: auto; margin-right: auto; color: rgb(0, 0, 0); font-family: -apple-system, system-ui, system-ui, &quot;Segoe UI&quot;, Roboto, &quot;Helvetica Neue&quot;, Arial, sans-serif; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">Bugs in visual editors</h2><p style=\"box-sizing: inherit; margin-bottom: 1.5em; max-width: 830px; margin-left: auto; margin-right: auto; color: rgb(0, 0, 0); font-family: -apple-system, system-ui, system-ui, &quot;Segoe UI&quot;, Roboto, &quot;Helvetica Neue&quot;, Arial, sans-serif; font-size: 18px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">A JavaScript code can completely ignore the browser’s sanitization process and handle it manually. This approach requires listening to<span> </span><code style=\"box-sizing: inherit; font-family: Consolas, &quot;Andale Mono WT&quot;, &quot;Andale Mono&quot;, &quot;Lucida Console&quot;, &quot;Lucida Sans Typewriter&quot;, &quot;DejaVu Sans Mono&quot;, &quot;Bitstream Vera Sans Mono&quot;, &quot;Liberation Mono&quot;, &quot;Nimbus Mono L&quot;, Monaco, &quot;Courier New&quot;, Courier, monospace; font-size: 0.9375rem;\">paste</code><span> </span>event, for instance:</p>"}])))

(defn handle-paste [opts !doc data-items]
  (ks/spy data-items)
  (let [text (->> data-items
                  (filter #(= "text/plain" (::cb/type %)))
                  first
                  ::cb/text)
        blocks (or #_(parse-paste-html-to-blocks data-items)
                   text)]
    (swap! !doc
      (fn [doc]
        (let [block (jot/sel-start-block doc)
              upd-block (merge
                          block
                          {::jot/content-text
                           (str
                             (::jot/content-text block)
                             text)})]
          (-> doc
              (jot/insert-string text opts)
              (jot/push-undo doc)))))))

(defn editor [{:keys [initial-doc
                      !doc
                      state
                      with-state
                      on-change
                      on-change-doc
                      on-change-content
                      on-change-selection
                      on-layout
                      render-placeholder
                      embeds
                      autofocus?
                      block-transforms]
               :as opts}]
  (let [type->render-spec (->> (concat embeds (default-embeds))
                               (map (fn [{:keys [::jot/type
                                                 ::jot/render]
                                          :as embed}]
                                      {type embed}))
                               (into {}))

        opts (merge
               opts
               {:type->render-spec type->render-spec})

        

        !internal-error (r/atom false)

        !block-id->realized-route (atom nil)

        state (or state (editor-state))

        {:keys [!ed-ref
                op-ch
                paste-ch
                !block-id->layout
                !doc
                bsel-ch
                cdu-ch
                !selecting?
                !focused?
                !ignore-selection-event?
                !editor-layout]} state

        update-block (fn [block {:keys [push-undo?]}]
                       (put! op-ch
                         [:apply-fn (fn [doc]
                                      (let [doc (-> doc
                                                    (jot/set-block
                                                      (::jot/block-id block)
                                                      block)
                                                    (jot/push-undo doc))]
                                        doc))]))

        remove-block-by-id (fn [block-id {:keys [push-undo?]}]
                             (put! op-ch
                               [:apply-fn
                                (fn [doc]
                                  (-> doc
                                      (jot/remove-block-ids
                                        [block-id])
                                      (jot/push-undo doc)))]))

        on-paste (fn [e]
                   (.preventDefault e)
                   (put! paste-ch e))

        on-change-doc (or on-change-doc on-change)

        update-doc (fn [doc]
                     (reset! !doc doc)
                     (when on-change-doc
                       (on-change-doc @!doc)))

        on-change-block-layout
        (fn [block-id rect]
          (if rect
            (swap! !block-id->layout assoc block-id rect)
            (swap! !block-id->layout dissoc block-id rect))

          (when (not= (-> @!doc
                          (jot/block-by-id block-id)
                          ::jot/layout)
                      rect)

            (swap! !doc
              (fn [doc]
                (-> doc
                    (jot/update-block
                      block-id
                      assoc
                      ::jot/layout rect))))

            (when on-change-doc
              (on-change-doc
                (jot/doc->edn
                  @!doc)))))

        on-change-embed-layout
        (fn [block-id embed-id rect]
          (when (not= rect
                      (get-in
                        @!doc
                        [::jot/embed-id->embed-data
                         embed-id
                         ::jot/block-id->layout
                         block-id]))
            (swap! !doc
              (fn [doc]
                (let [doc (if rect
                            (assoc-in doc
                              [::jot/embed-id->embed-data
                               embed-id
                               ::jot/block-id->layout
                               block-id]
                              rect)
                            (update-in doc
                              [::jot/embed-id->embed-data
                               embed-id
                               ::jot/block-id->layout]
                              (fn [m]
                                (dissoc m block-id))))

                      doc (assoc-in
                            doc
                            [::jot/embed-id->embed-data
                             embed-id
                             ::jot/layout]
                            (-> doc
                                ::jot/embed-id->embed-data
                                (get embed-id)
                                ::jot/block-id->layout
                                vals
                                bounding-box))]
                  doc)))
            
            
            (when on-change-doc
              (on-change-doc (jot/doc->edn @!doc)))))]

    (init-document opts state)

    (update-editor-layout opts state)
    
    (when with-state
      (with-state state))

    (go-loop []
      (let [op (<! op-ch)]
        (reset! !ignore-selection-event? true)
        (try
          (swap! !doc
            (fn [doc]
              (-> doc
                  (handle-op op opts)
                  #_update-blocks-layout)))
          (catch js/Error e
            (prn "E" e)
            (.error js/console e)))

        (let [edn-doc (jot/doc->edn @!doc)]
          (when on-change-doc
            (on-change-doc edn-doc))
          (when on-change-content
            (on-change-content edn-doc)))

        (alts! [(<raf)
                #_(timeout 30)])

        (swap! !doc
          update-embeds-layout
          @!ed-ref)

        (set-browser-sel-from-doc
          @!doc
          @!ed-ref)

        (reset! !ignore-selection-event? false)

        (recur)))

    (go-loop []
      (let [op (<! bsel-ch)]
        (let [sel (.getSelection js/document)]
          (when-let [sel-clj (sel-obj->selection sel)]
            #_(ks/spy sel-clj)
            (reset! !doc
              (handle-op
                @!doc
                [:set-selection sel-clj]
                opts))
            #_(when on-change-doc
                (on-change-doc (jot/doc->edn @!doc)))
            (when on-change-selection
              (on-change-selection
                (jot/get-sel @!doc)
                @!doc))))
        (recur)))

    (go-loop []
      (let [{:keys [in-editor? data-items]} (<! paste-ch)]
        (when in-editor?
          (handle-paste opts !doc data-items)
          (on-change-doc (jot/doc->edn @!doc)))
        (recur)))
    
    (r/create-class
      {:component-did-catch
       (fn []
         (reset! !internal-error true))
       :component-did-mount
       (fn []
         (try
           (initialize-editor opts state)
           (catch js/Error e
             (.error js/console e))))
       :component-did-update
       (fn []
         (when (and (not @!selecting?) @!focused?)
           (set-browser-sel-from-doc
             @!doc
             @!ed-ref))
         (when @!focused?
           (update-sel-layout state)))

       :component-will-unmount
       (fn []
         (teardown-editor state))
       :reagent-render
       (fn [{:keys [style top-offset bottom-offset
                    content-width] :as opts}]
         (if @!internal-error
           [:div "INTERNAL ERROR"]
           (let [{:keys [bg-color
                         fg-color
                         font-family
                         font-size
                         render-right-gutter]}
                 (th/resolve opts theme-rules)]
             [:div.rx-jot-editor
              {:style {:width "100%"
                       :min-height "100%"
                       :height "auto"
                       :position 'relative
                       :display 'flex
                       :flex-direction 'row}}
              [left-gutter opts state]
              [:div.jot-outer-wrapper
               {:style (merge
                         { ;;:flex 1
                          :white-space 'pre-wrap
                          :height "auto"
                          :width (or content-width "100%")
                          :min-height "100%"
                          :outline 0
                          :position 'relative}
                         {:background-color bg-color
                          :color fg-color
                          :font-family font-family
                          :font-size font-size
                          :padding-top top-offset
                          :padding-bottom bottom-offset}
                         (outer-wrapper-gutter-style opts)
                         style)
                :ref #(when % (reset! !ed-ref %))
                :on-before-input
                (fn [e]
                  (when (.-data e)
                    (.preventDefault e)
                    (put! op-ch
                      [:insert-string (.-data e)]))
                  )
                :on-change (fn [e]
                             (prn "change")
                             (ks/clog e))
                :on-focus (fn []
                            (reset! !focused? true)
                            (when-let [f (:on-focus opts)]
                              (f)))
                :on-blur (fn []
                           (reset! !focused? false)
                           (when-let [f (:on-blur opts)]
                             (f)))
                :on-key-down (fn [e]
                               (cond
                                 (and (= "ArrowDown" (.-key e))
                                      (jot/embed? (jot/sel-start-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-downwards]))

                                 (and (= "ArrowDown" (.-key e))
                                      (jot/embed? (jot/sel-next-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-downwards]))

                                 (and (= "ArrowUp" (.-key e))
                                      (jot/embed? (jot/sel-start-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-upwards]))

                                 (and (= "ArrowUp" (.-key e))
                                      (jot/embed? (jot/sel-prev-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-upwards]))

                                 (and (= "ArrowLeft" (.-key e))
                                      (jot/embed? (jot/sel-start-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-backwards]))

                                 (and (= "ArrowLeft" (.-key e))
                                      (= 0 (second (jot/get-sel-start @!doc)))
                                      (jot/embed? (jot/sel-prev-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-backwards]))

                                 (and (= "ArrowRight" (.-key e))
                                      (jot/embed?
                                        (jot/sel-start-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-forwards]))

                                 (and (= "ArrowRight" (.-key e))
                                      (= (jot/block-end-offset
                                           (jot/sel-start-block @!doc))
                                         (second (jot/get-sel-start @!doc)))
                                      (jot/embed? (jot/sel-next-block @!doc)))
                                 (do
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (put! op-ch [:move-forwards]))
                                 
                                 (skip-key-event? e) (do
                                                       #_(put! bsel-ch :selection)
                                                       nil)

                                 (= "Escape" (.-key e))
                                 (.blur @!ed-ref)
                                 
                                 :else
                                 (let [[op-key :as op] (key-event->op e)]
                                   (cond
                                     (= :prevent-default op-key)
                                     (do
                                       (.preventDefault e))

                                     (not= :noop op-key)
                                     (do
                                       (.preventDefault e)
                                       (.stopPropagation e)
                                       (put! op-ch op))))))
                :on-key-up (fn [e]
                             #_(prn "ku")
                             #_(when (skip-key-event? e)
                                 (put! bsel-ch :selection)))

                :on-mouse-down (fn [e]
                                 (reset! !selecting? true))
                :on-mouse-up (fn [e]
                               #_(put! bsel-ch :selection)
                               (reset! !selecting? false))

                :on-touch-end (fn [e]
                                (ks/prn "touch end"))

                :on-paste (fn [e]
                            (.preventDefault e)
                            (let [ch (cb/realize-data-items-chan
                                       (cb/event->data-items
                                         (.-nativeEvent e)))
                                  !results (atom [])
                                  in-editor? (paste-in-editor?
                                               (.-nativeEvent e)
                                               @!ed-ref)]
                              (go-loop []
                                (let [res (<! ch)]
                                  (when res
                                    (swap! !results conj res)
                                    (recur)))
                                (put! paste-ch
                                  {:in-editor? in-editor?
                                   :data-items @!results})))
                            #_(ks/spy
                                (cb/realize-data-items
                                  (cb/event->data-items
                                    (.-nativeEvent e))))
                            #_(put! paste-ch
                                (cb/event->data-items
                                  (.-nativeEvent e))))

                :contentEditable true
                :suppressContentEditableWarning true
                :autoCorrect "true"}
               (let [doc @!doc
                     blocks (->> doc
                                 jot/blocks-in-order)
                     block-count (count blocks)
                     blocks (->> blocks
                                 (map-indexed
                                   (fn [i block]
                                     (merge
                                       block
                                       {::jot/embed-id->embed-data
                                        (->> block
                                             ::jot/embed-ids
                                             (map (fn [embed-id]
                                                    [embed-id (jot/embed-data-by-id doc embed-id)]))
                                             (into {}))}
                                       (when (= i 0)
                                         {::jot/first-block? true})
                                       (when (= (inc i) block-count)
                                         {::jot/last-block? true})
                                       (when (and (jot/sel-collapsed? doc)
                                                  (= (::jot/block-id (jot/sel-start-block doc))
                                                     (::jot/block-id block)))
                                         {::jot/selected? true})))))]
                 (->> blocks
                      (partition-by partition-blocks)
                      (map (fn [[{:keys [::jot/block-id]} & _ :as blocks]]
                             [render-blocks
                              (merge
                                (dissoc opts :style)
                                {:type->render-spec type->render-spec}
                                {:key block-id
                                 :update-block update-block
                                 :update-doc update-doc
                                 :remove-block-by-id remove-block-by-id
                                 :render-placeholder render-placeholder
                                 :!block-id->layout !block-id->layout
                                 :on-change-block-layout on-change-block-layout
                                 :on-change-embed-layout on-change-embed-layout})
                              blocks
                              type->render-spec
                              !block-id->realized-route]))
                      doall))]
              [right-gutter opts state]])))})))

(defn render-media [{:keys [update-block
                            update-doc
                            selected?]}
                    blocks doc]
  (let [block (first blocks)
        {:keys [::jot/block-id
                :media/render-width
                :media/render-height
                :media/url
                :media/orig-width
                :media/orig-height]} block

        ar (/
             orig-width
             orig-height)]
    [:div
     {:on-click (fn [e]
                  (.preventDefault e)
                  (.stopPropagation e)
                  (let [{:keys [:media/render-width
                                :media/render-height]}
                        block]
                    (update-doc
                      (jot/set-selection
                        doc
                        {:start [(::jot/block-id block) 1]
                         :end [(::jot/block-id block) 1]}))
                    #_(update-block
                        (merge
                          (first blocks)
                          {:media/render-width
                           (if (= 300 render-width)
                             100
                             300)
                           :media/render-height
                           (if (= 300 render-height)
                             100
                             300)}))))
      :on-mouse-up (fn [e]
                     (.stopPropagation e)
                     (.preventDefault e))}
     [:div
      {:id (str block-id "-0")
       :data-block-id block-id
       :key block-id
       :style {:background-color
               (if selected?
                 "red"
                 "white")}}
      [:div
       {:contentEditable false
        :style {:text-align 'center}}
       [:img {:src url
              :style {:object-fit 'cover
                      :width (or render-width 100)
                      :height (or render-height 100)}}]]]]))
