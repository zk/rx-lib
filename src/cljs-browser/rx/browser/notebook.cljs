(ns rx.browser.notebook
  (:require [rx.kitchen-sink :as ks]
            [rx.res :as res]
            [rx.notebook :as nb]
            [rx.browser.components :as cmp]
            [rx.browser.jot :as bjot]
            [rx.jot :as jot]
            [rx.box :as box]
            [rx.browser.modal :as modal]
            [rx.theme :as th]
            [rx.icons.feather :as feather]
            [rx.browser.buttons :as btn]
            [rx.browser.popover :as po]
            [reagent.core :as r]
            [clojure.string :as str]
            [clojure.core.async :as async
             :refer [<! chan put! sliding-buffer timeout]
             :refer-macros [go go-loop]]))

#_(defn button [{:keys [on-action
                      style]} & children]
  (into
    [:div
     {:style
      (merge
        {:position 'relative
         :line-height "100%"
         :display 'inline-block
         :cursor 'pointer
         :padding 5}
        style)}
     [cmp/hover
      {:style {:border "solid transparent 1px"
               :position 'absolute
               :top 0
               :left 0
               :right 0
               :bottom 0}
       :on-click on-action
       :style-over
       {:border "solid #eee 1px"
        :border-right "solid black 1px"
        :border-top "solid black 1px"}
       :style-down
       {:border "solid #eee 2px"
        :border-right "solid black 2px"
        :border-top "solid black 2px"}}]]
    children))

(defn button [{:keys [on-action
                      style]} & children]
  (into
    [cmp/hover
     {:on-click on-action
      :style (merge
               {:line-height "100%"
                :display 'inline-block
                :cursor 'pointer
                :padding 5
                :transition "background-color 0.15s ease, color 0.15s ease"
                :border-radius 2}
               style)
      :style-over
      {:background-color 'black
       :color 'white}}]
    children))

(defn navbar [{:keys [on-choose-new-doc
                      on-choose-items]}]
  [:div
   [:div

    [po/view
     {:show-on-over? true
      :position :right-center
      :render-body
      (fn []
        [button
         {:on-action on-choose-items}
         [feather/layers
          {:stroke-width 1.5}]])
      :render-popover
      (fn []
        [:div
         {:style {:white-space 'nowrap
                  :padding 5
                  :line-height "100%"}}
         [:div "All Docs"]
         [:div {:style {:height 3}}]
         [:div
          {:style {:display 'flex
                   :flex-direction 'row
                   :align-items 'center
                   :font-size 12
                   :color "#ccc"}}
          [feather/keyboard
           {:size 14
            :stroke-width 1.5
            :style {:margin-right 3}}]
          "⌘+O"]])}]]
   [:div {:style {:height 5}}]
   [:div
    [po/view
     {:show-on-over? true
      :position :right-center
      :render-body
      (fn []
        [button
         {:on-action on-choose-new-doc}
         [feather/file-plus
          {:stroke-width 1.5}]])
      :render-popover
      (fn []
        [:div
         {:style {:white-space 'nowrap
                  :padding 5
                  :line-height "100%"}}
         [:div "New Doc"]
         [:div {:style {:height 3}}]
         [:div
          {:style {:display 'flex
                   :flex-direction 'row
                   :align-items 'center
                   :font-size 11
                   :color "#ccc"}}
          [feather/keyboard
           {:size 14
            :stroke-width 1.5
            :style {:margin-right 3}}]
          "⌘+N"]])}]]])

(defn items-list [{:keys [items
                          on-choose-item]}]
  [:div
   {:style {:flex 1
            :padding 10}}
   (cmp/interpose-children
     {:separator [:div {:style {:height 5}}]}
     (->> items
          (map (fn [{:keys [::nb/folder-id
                            ::nb/doc-id
                            ::nb/title
                            ::nb/last-updated-ts]
                     :as item}]
                 [button
                  {:style {:padding 10
                           :display 'flex
                           :flex-direction 'row
                           :justify-content 'space-between}
                   :on-action (fn [e]
                                (.preventDefault e)
                                (on-choose-item item)
                                nil)}
                  
                  [:div
                   {:style {:display 'flex
                            :flex-direction 'row}}

                   [:div
                    [feather/file
                     {:style {:margin-right 10}
                      :size 17}]]
                   [:div
                    (or (and (not (empty? title))
                             title)
                        doc-id
                        folder-id)]]
                  [:div
                   (ks/date-format
                     last-updated-ts
                     "MMM d, yyyy / h:mm a")]]))
          doall))])

(defn embeds []
  [{::jot/type ::nb/doc-link
    ::jot/render (fn [{:keys [::on-choose-doc-link] :as opts} embed-data text]
                   [:a (merge
                         #_(select-keys opts [:id
                                              :key
                                              :data-block-id
                                              :data-deco-start-offset
                                              :data-deco-end-offset])
                         {:href "#"
                          :contentEditable false
                          :on-click
                          (fn [e]
                            (on-choose-doc-link embed-data))})
                    text])}])

(defn text->title-key [s]
  (-> s
      str/trim
      str/lower-case
      (str/replace #"\s+" "-")))

(defn doc-link-text->embed-data [s]
  {::jot/embed-type ::nb/doc-link
   ::jot/atomic? true
   ::nb/doc-link-text s
   ::nb/doc-link-title-key (text->title-key s)
   ::jot/embed-id (ks/uuid)})

(defn doc-link-transform [block]
  (let [text (jot/block-text block)
        [full-match group-match :as r] (re-find #"\[([^\]]*)\]" text)]
    (if full-match
      (let [full-start-offset (str/index-of text full-match)
            full-end-offset (+ full-start-offset (count full-match))
            embed-data (doc-link-text->embed-data group-match)]
        (-> block
            (jot/block-delete-range full-start-offset full-end-offset)
            (jot/block-insert-string full-start-offset group-match)
            (jot/set-inline-embed
              full-start-offset
              (+ full-start-offset (count group-match))
              embed-data)))
      block)))

(defn block-transforms []
  [doc-link-transform])

(defn editor [{:keys [on-toggle-focus-mode
                      on-choose-delete-doc] :as opts}]
  (let [{:keys [::editor-padding
                ::editor-max-width]}
        (th/des opts
          [[::editor-padding 20]
           [::editor-max-width 800]])]
    [:div
     {:style {:flex 1
              :display 'flex
              :position 'relative}}
     [:div
      {:style {:flex 1}}
      [bjot/editor
       (merge
         opts
         {:theme {::bjot/editor-bottom-space 200}
          :embeds (embeds)
          :block-transforms (block-transforms)
          :style
          (merge
            {:max-width editor-max-width
             :margin-left 'auto
             :margin-right 'auto
             :padding editor-padding})})]]
     #_[:div
        {:style {:position 'absolute
                 :top 0
                 :left 30}}
        "TOC"]]))

(defn admin [opts]
  [:div
   [button
    {:on-action (:on-delete-all opts)}
    [feather/trash-2 {:stroke-width 1.5}]]])

(defn confirm-modal-view [{:keys [::modal/state title]}]
  {:render
   (fn []
     [:div
      {:style {:padding 20}}
      [:div
       {:style {:margin-bottom 10
                :font-weight 'bold}}
       title]
      [:div
       {:style {:display 'flex
                :flex-direction 'row
                :justify-content 'flex-end
                :align-items 'flex-start}}
       [:div
        [btn/hollow
         {:label "Cancel"
          :keybinding "Esc"
          :on-action (fn []
                       (modal/<hide state false))}]]
       [:div
        {:style {:margin-left 20}}
        [btn/solid
         {:label "Ok"
          :style {:width 60}
          :keybinding "Enter"
          :on-action
          (fn []
            (modal/<hide state false))}]]]])})

(defn app [{:keys [:box/conn
                   initial-view-key
                   initial-view-opts
                   initial-docs] :as opts}]
  (let [!content-component-call (r/atom nil)
        !focus-mode? (r/atom false)

        
        !view-key (r/atom (or initial-view-key :items))
        !view-opts (r/atom initial-view-opts)
        !items (r/atom nil)

        modal-state (modal/create-state)
        
        refresh-ch (chan)
        
        _ (put! refresh-ch :refresh)

        _ (go-loop []
            (<! refresh-ch)
            (reset! !items
              (res/data
                (<! (box/<query
                      conn
                      {:where [:and
                               [:ident-key ::nb/doc-id]]
                       :sort [[::nb/last-updated-ts :desc]]
                       :limit 1000}
                      {:debug-log? false}))))

            (recur))

        change-ch (chan (sliding-buffer 1))

        _ (let [in-ch (ks/debounce change-ch 500)]
            (go-loop []
              (let [doc (<! in-ch)
                    title (-> doc
                              jot/get-first-block
                              ::jot/content-text)
                    title-key (text->title-key title)]
                (<! (box/<transact
                      conn
                      [(merge
                         doc
                         {::nb/title title
                          ::nb/doc-link-title-key title-key
                          ::nb/last-updated-ts (ks/now)})]))
                (<! (timeout 250)))
              (recur)))
        
        on-doc-change (fn [doc]
                        (put! change-ch doc))

        on-choose-new-doc (fn []
                            (go
                              (reset! !view-key nil)
                              (<! (timeout 17))
                              (reset! !view-opts {:initial-doc
                                                  (merge
                                                    (jot/empty-doc)
                                                    {::nb/doc-id (ks/uuid)})})
                              (reset! !view-key :doc)))

        on-choose-items (fn []
                          (reset! !view-key :items)
                          (put! refresh-ch :refresh))

        on-choose-delete-doc (fn [doc]
                               (go
                                 (when (<! (modal/<show
                                             modal-state
                                             [confirm-modal-view
                                              {:title "Delete Document?"}]))
                                   (<! (box/<transact
                                         conn
                                         [[:box/delete
                                           [::nb/doc-id (::nb/doc-id doc)]]]))
                                   (put! refresh-ch :refresh)
                                   (reset! !view-key :items))))
        
        opts
        (merge
          opts
          {:on-choose-new-doc on-choose-new-doc
           :on-choose-items on-choose-items
           :on-delete-all
           (fn []
             (go
               (when (<! (modal/<show
                           modal-state
                           [confirm-modal-view
                            {:title "Delete All Documents?"}]))
                 
                 (reset! !view-key :items)
                 
                 (let [docs (res/data
                              (<! (box/<query
                                    conn
                                    {:where [[:ident-key ::nb/doc-id]]})))]
                   (when-not (empty? docs)
                     (<! (box/<transact
                           conn
                           (->> docs
                                (map (fn [{:keys [::nb/doc-id]}]
                                       [:box/delete [::nb/doc-id doc-id]])))))
                     (put! refresh-ch :refresh))))))})

        !component (r/atom
                     [items-list
                      {:on-choose-items nil}])]

    (when initial-docs
      (go
        (<! (box/<transact conn initial-docs))))
    
    (r/create-class
      {:component-will-unmount
       (fn [])
       :reagent-render
       (fn []
         [:div {:style {:flex 1
                        :display 'flex
                        :flex-direction 'row}}
          (when (not @!focus-mode?)
            [:div {:style
                   {:display 'flex
                    :flex-direction 'column
                    :justify-content 'space-between
                    :padding 10}}
             
             [navbar opts]
             #_[admin opts]])
          [:div {:style {:flex 1
                         :display 'flex
                         :overflow-y 'scroll}}
           (condp = @!view-key
             :items [items-list
                     {:items @!items
                      :on-choose-item
                      (fn [item]
                        (when (::nb/doc-id item)
                          (reset! !view-key :doc)
                          (reset! !view-opts
                            {:initial-doc
                             (jot/edn->doc item)})))}]
             :doc [editor
                   (merge
                     opts
                     @!view-opts
                     {:on-change-content on-doc-change
                      :on-choose-delete-doc on-choose-delete-doc
                      :on-toggle-focus-mode
                      (fn []
                        (swap! !focus-mode? not))
                      ::on-choose-doc-link
                      (fn [{:keys [::nb/doc-id
                                   ::nb/doc-link-title-key
                                   ::nb/doc-link-text]}]
                        
                        (go
                          (let [doc (or
                                      (when doc-id
                                        (<! (box/<pull conn [::nb/doc-id doc-id])))
                                      (first
                                        (res/data
                                          (<! (box/<query
                                                conn
                                                {:where [[:=
                                                          ::nb/doc-link-title-key
                                                          doc-link-title-key]]}))))
                                      (merge
                                        (-> (jot/base-doc)
                                            (jot/append-block
                                              (jot/create-block
                                                {::jot/type ::jot/heading-one
                                                 ::jot/content-text doc-link-text})))
                                        {::nb/doc-id (ks/uuid)
                                         ::nb/title doc-link-text}))]
                            (reset! !view-key nil)
                            (<! (timeout 17))
                            (reset! !view-opts {:initial-doc doc})
                            (reset! !view-key :doc))))})]
             nil)
           [modal/component
            {:state modal-state}]]])})))
