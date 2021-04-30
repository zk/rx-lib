(ns rx.browser.notebook_otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser.notebook]
            [rx.jot :as jot]
            [rx.notebook :as nb]
            [rx.browser :as browser]
            [rx.browser.notebook :as bnb]
            [rx.box :as box]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]))

(defn doc-link []
  (go
    (browser/<show-component!
      [bnb/app
       {:theme {::bnb/editor-padding 50}
        :box/conn
        (<! (box/<conn
              (nb/box-schema
                {:box/local-db-name "totebook_db_otto"})))

        :initial-view-key :doc
        :initial-view-opts
        {:initial-doc
         (let [embed-id (ks/uuid)]
           (-> (jot/base-doc)
               (assoc ::nb/doc-id "doc-one")
               (jot/append-block
                 (-> (jot/create-block
                       {::jot/type ::jot/paragraph
                        ::jot/content-text "Here's a document link"})
                     (jot/set-decos
                       9
                       (->> (range 13)
                            (map (fn [i]
                                   {::jot/embed-id embed-id}))))))
               (jot/set-embed-data
                 {::jot/embed-id embed-id
                  ::jot/embed-type ::nb/doc-link
                  ::jot/atomic? true
                  ::nb/doc-id "doc-two"})))}}])))

(comment

  (doc-link)

  )
