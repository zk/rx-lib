(ns rx.browser.jot.example-embed
  (:require [rx.kitchen-sink :as ks]
            [rx.jot :as jot]
            [rx.browser.jot :as bjot]
            [rx.view :as view]
            [rx.browser.jot.react-native-embed :as rne]
            [rx.browser.jot.react-native.gallery :as gallery]))

(defn editor-options []
  {:bridge-callbacks []
   :embeds
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
            (ks/uuid)])})]}
    {::jot/type :gallery
     ::view/route [gallery/block-view]}]})

(defn otto []
  (rne/init-embedded-editor
    {:initial-doc (-> (jot/base-doc)
                      (jot/append-block
                        (jot/para
                          {:text "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."}))
                      (jot/append-block
                        (jot/para
                          {:text "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."}))
                      (jot/append-block
                        (jot/para
                          {:text "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."})))
     :theme {::bjot/font-size 18}
     :embeds
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
              (ks/uuid)])})]}]}))

(comment

  (ks/prn "hi")

  (js/alert "hi")

  (otto)

  )
