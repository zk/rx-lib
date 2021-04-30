(ns rx.browser.box2.inspector
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.box2 :as box]
            [rx.browser.ui :as ui]
            [rx.theme :as th]
            [reagent.core :as r]
            [clojure.core.async :as async
             :refer [go <!]]))

(defn inspector [{:keys [:box/conn]}]
  (let [!datoms (r/atom nil)]
    (go
      (reset! !datoms
        (<! (box/<datoms conn))))
    (r/create-class
      {:reagent-render
       (fn [opts]
         (let [{:keys [fg-color
                       bg-color]}
               (th/resolve opts
                 [[:fg-color :color/fg-0]
                  [:bg-color :color/bg-0]])]
           [:div
            {:style {:background-color bg-color
                     :color fg-color
                     :overflow-y 'scroll
                     :width 1000
                     :height "80%"}}
            [ui/group
             {:gap 40}
             [ui/group
              {:gap 8}
              [ui/text {:scale "title"} "Conn"]
              [:pre (ks/pp-str conn)]]
             [ui/group
              {:gap 8}
              [ui/text {:scale "title"} "Datoms"]
              [:pre (ks/pp-str @!datoms)]]]]))})))
