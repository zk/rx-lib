(ns rx.browser.notebook-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg]
            [rx.jot :as jot]
            [rx.notebook :as nb]
            [rx.browser.notebook :as bnb]
            [rx.browser.box :as box]
            [reagent.core :as r]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:url "https://www.oldbookillustrations.com/wp-content/high-res/1893/warren-bookplate-768.jpg"
     :height 350}
    [:h1 {:id "notebook-intro"} "Notebook"]
    [:p "Notebook is a document editing, storage, and organizational system. It is similar to a traditional notebook in that it provides the ability to create and edit documents and organize those documents into a heirarchical structure. Additionally Notebook supports linking between documents and full-text search."]
    [:p "Notebook uses Jot for editing and Box for storage and search."]
    [:div {:style {:clear "both"}}]
    [:h2 {:id "app-vs-components"} "App vs. Components"]
    [:p "Notebook is a collection of components that can be reused, but should be looked at more like a standalone app."]
    [sg/checkerboard
     [:div
      {:style {:height 300
               :display 'flex}}
      [sg/async-mount
       (fn []
         (go
           [bnb/app
            {:box/conn
             (<! (box/<conn
                   (nb/box-schema
                     {:box/local-db-name
                      "notebook_styleguide_db_2"})))}]))]]]]
   [sg/section
    [:h2 {:id "notebook-editor"} "Editor"]
    [sg/checkerboard
     [:div
      {:style {:display 'flex
               :height 400}}
      [bnb/editor {:theme {::nb/editor-padding 20}}]]]]
   [sg/section
    [:h2 {:id "notebook-items-list"} "Items List"]
    [:p "Displays documents and folders in list form. Typically used to show the contents of a folder, or the root level folders."]
    [sg/checkerboard
     [bnb/items-list
      {:items [{::nb/folder-id "folder1"
                ::nb/title "Folder 1"}
               {::nb/doc-id "doc1"
                ::nb/title "Doc 1"}]
       :on-choose-item (fn [item]
                         (ks/pp item))}]]]
   [sg/section
    [:h2 {:id "notebook-doc-links"} "Linking Between Documents"]
    [sg/checkerboard
     [sg/async-mount
      (fn []
        (go
          (let [docs [(let [embed-id (ks/uuid)]
                        (-> (jot/base-doc)
                            (assoc ::nb/doc-id "doc-one")
                            (jot/append-block
                              (-> (jot/create-block
                                    {::jot/type ::jot/paragraph
                                     ::jot/content-text "Here's a document link. 1"})
                                  (jot/set-decos
                                    9
                                    (->> (range 13)
                                         (map (fn [i]
                                                {::jot/embed-id embed-id}))))))
                            (jot/set-embed-data
                              {::jot/embed-id embed-id
                               ::jot/embed-type ::nb/doc-link
                               ::nb/doc-id "doc-two"})))
                      (let [embed-id (ks/uuid)]
                        (-> (jot/base-doc)
                            (assoc ::nb/doc-id "doc-two")
                            (jot/append-block
                              (-> (jot/create-block
                                    {::jot/type ::jot/paragraph
                                     ::jot/content-text "Here's a document link. 2"})
                                  (jot/set-decos
                                    9
                                    (->> (range 13)
                                         (map (fn [i]
                                                {::jot/embed-id embed-id}))))))
                            (jot/set-embed-data
                              {::jot/embed-id embed-id
                               ::jot/embed-type ::nb/doc-link
                               ::nb/doc-id "doc-one"})))]]
            [:div {:style {:height 500
                           :display 'flex}}
             [bnb/app
              {:box/conn (<! (box/<conn
                               (nb/box-schema
                                 {:box/local-db-name
                                  "notebook_styleguide_db"})))
               :initial-view-key :doc
               :initial-view-opts
               {:initial-doc (first docs)}
               :initial-docs docs}]])))]]]])

(comment

  (browser/<show-route!
    [sg/standalone {:component sections}])

  )

