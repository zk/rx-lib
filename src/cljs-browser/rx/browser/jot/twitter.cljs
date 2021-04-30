(ns rx.browser.jot.twitter
  (:require [rx.kitchen-sink :as ks]
            [rx.jot :as jot]
            [rx.media :as media]
            [goog.object :as gobj]
            [dommy.core :as dommy
             :refer-macros [by-id]]
            [cljs.core.async :as async
             :refer [<! put! chan close!]
             :refer-macros [go]]))

(defn <inject-twitter-js []
  (let [ch (chan)]
    (gobj/set
      js/window
      "twttr"
      (fn [d s id]
        (let [twttr (gobj/get js/window "twttr")]
          (if (by-id id)
            (close! ch)))))
    ch))


(defn render-embed [_ {:keys [::tweet-id] :as block}]
  (let [{:keys [::rows
                ::aspect-ratio]
         :or {rows 3
              aspect-ratio 1}} block

        media-width-pct (/ 1 rows)
        aspect-ratio 0.69
        medias (::media/medias block)]
    [:div
     {:style {:display 'flex
              :flex-direction 'row
              :flex-wrap 'wrap
              :overflow 'hidden
              :border-radius 3}}
     (->> medias
          (map-indexed
            (fn [i {:keys [::media/id] :as media}]
              [:div
               {:key (or id i)
                :style {:display 'flex
                        :flex (str "0 0 " (* media-width-pct 100) "%")
                        :overflow 'hidden
                        :background-image (str "url('" (media/remote-uri media) "')")
                        :background-size 'cover
                        :background-position 'center
                        :cursor 'pointer
                        :max-width (str (* media-width-pct 100) "%")}
                :on-click (fn [e]
                            (.preventDefault e)
                            nil)}
               [:div
                {:style {:width "100%"
                         :padding-top (str (* aspect-ratio 100) "%")}}]
               #_[:img
                  {:style {:flex 1
                           :display 'block}
                   :key (or id i)
                   :src uri}]])))]))

(defn embed []
  {::jot/type ::jot/gallery
   ::jot/render render-embed})
