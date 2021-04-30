(ns rx.browser.jot.gallery
  (:require [rx.kitchen-sink :as ks]
            [rx.jot :as jot]
            [rx.media :as media]))

(defn render-embed [{:keys [::on-choose-media] :as opts} block]
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
                            (when on-choose-media
                              (on-choose-media medias i))
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
