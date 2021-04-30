(ns rx.styleguide.sgui
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [rx.browser.components :as bc]
            [rx.styleguide.common :as com]
            [rx.css :as css]
            [rx.css.colors :as css-colors]
            [garden.color :as gc]
            [cljs.core.async :as async
             :refer [<! >! chan close! put! take! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn $units []
  [com/$section
   {:title "Units"
    :id "units"}
   [:div.row
    (->> ["xs" css/xs
          "sm" css/sm
          "md" css/md
          "lg" css/lg
          "xl" css/xl]
         (partition 2)
         (map (fn [[title size]]
                [:div.col-sm-4
                 {:key size}
                 [:div.flex-left-top.mg-bot-md
                  [:div.sg-row-label
                   title " " size " px"]
                  [:div
                   {:style {:height (css/px size)
                            :flex 1
                            :background-color (css/blgr :50)}}]]])))]])

(defn $buttons []
  (let [contexts ["primary"
                  "secondary"
                  "success"
                  "warning"
                  "danger"
                  "default"]
        types [nil
               "alt"]]
    (into
      [com/$section
       {:title "Buttons"
        :id "buttons"}]
      (->> types
           (map (fn [type]
                  [:div.row
                   [:div.col-sm-12
                    [:div.mg-bot-sm.bold.code
                     "[:(button|a).btn-*"
                     (when type
                       (str "-" type))
                     "]"]
                    (into
                      [:div.row]
                      (->> contexts
                           (map (fn [ctx]
                                  (let [class (str "btn-" ctx
                                                   (when type
                                                     (str "-" type)))]
                                    [:div.col-sm-4.mg-bot-md
                                     [:button
                                      {:class class
                                       :style {:width "100%"}}
                                      (str/capitalize ctx)]])))))]]))))))



(defn $typography []
  [com/$section
   {:title "Typography"
    :id "typography"}

   [:p "Yadda yadda."]

   [:h4.sg-subsection-header "Selected Fonts"]
   [:div.mg-bot-md
    (->> ["Header" css/header-font
          "Copy" css/copy-font
          "Impact" css/impact-font]
         (partition 2)
         (map (fn [[k v]]
                [:div.flex-left.mg-bot-sm
                 {:key k}
                 [:div.sg-row-label
                  {:style {:width 50
                           :text-align 'right}}
                  k]
                 [:div
                  {:style {:font-family v
                           :font-size 18}}
                  v]])))]



   (into
     [:div
      [:h4.sg-subsection-header "Headings"]]
     (->> [:h1 :h2 :h3 :h4 :h5 :h6]
          (map (fn [$el]
                 [:div.flex-left.mg-bot-md
                  [:div.sg-row-label
                   (name $el)
                   ". "]
                  [:div.ellipsis
                   [$el {:class "ellipsis mg-none bold"}
                    "The quick brown fox jumps over the lazy dog."]
                   [:div.spacer-sm]
                   [$el {:class "ellipsis mg-none"}
                    "The quick brown fox jumps over the lazy dog."]]]))))

   (into
     [:div
      [:h4.sg-subsection-header "Body Copy"]]
     (->> ["p" [:p "I’ve been drunk while swimming in the river, at noon the temperature of water, air, and body all the same, so that I can’t tell where body ends and water begins and it’s a melding of the universe, with the river curling round our bodies, cool and warm rushes intermingling in lost patterns, filling all times and all depths."]
           "ul" [:ul
                 (->> ["CIA Realizes It’s Been Using Black Highlighters All These Years"
                       "https://politics.theonion.com/cia-realizes-its-been-using-black-highlighters-all-thes-1819568147"

                       "Jurisprudence Fetishist Gets Off On Technicality"
                       "https://www.theonion.com/jurisprudence-fetishist-gets-off-on-technicality-1819586446"

                       "Man Unknowingly Purchases Lifetime Supply of Condoms"
                       "https://local.theonion.com/man-unknowingly-purchases-lifetime-supply-of-condoms-1819591526"

                       "Sometimes I Feel Like I'm The Only One Trying To Gentrify This Neighborhood"
                       "https://local.theonion.com/sometimes-i-feel-like-im-the-only-one-trying-to-gentrif-1819584310"
                       "Man Says ‘Fuck It,’ Eats Lunch At 10:58 A.M."
                       "https://local.theonion.com/man-says-fuck-it-eats-lunch-at-10-58-a-m-1819574888"]
                      (partition 2)
                      (map (fn [[title href]]
                             [:li {:key href}
                              [:a {:style {:color css/copy-color
                                           :font-weight 'normal}
                                   :href href}
                               title]])))]
           "ol" [:ol
                 [:li "Gomers don't die"]
                 [:li "Gomers go to ground"]
                 [:li "At a cardiac arrest, the first procedure is to take your own pulse"]
                 [:li "The patient is the one with the disease"]
                 [:li "Placement comes first"]]]
          (partition 2)
          (map (fn [[title comp]]
                 [:div.mg-bot-md.flex-left-top
                  [:div.sg-row-label
                   {:style {:width 25
                            :text-align 'right}}
                   title "."]
                  [:div
                   {:style {:flex 1}}
                   comp]]))))])

(defn $progress []
  [com/$section
   {:title "Progress Indicators"
    :id "progress"}

   [:p "Attach " [:span.pad-xs.bold.code.br-sm
                  {:style {:background-color 'white}}
                  ".loading"] " to top-level elements to show."]
   [:div
    (->> ["prog-rot" "[:div.prog-rot-* [:div.box]]"]
         (partition 2)
         (map (fn [[class-root spec]]
                [:div
                 {:key class-root}
                 [:h4.code.bold.lh120 spec]
                 #_[page/$interpose-children
                    {:separator [:div {:style {:width css/mdpx}}]
                     :class "flex-left-top"}
                    (->> ["sm" "md" "lg"]
                         (map (fn [k]
                                [:div.text-center
                                 [:div.mg-bot-sm k]
                                 [:div.loading
                                  {:key k
                                   :class (str class-root "-" k)}
                                  [:div.box]]])))]])))]])

(defn $box-shadows []
  [com/$section
   {:title "Box Shadows"
    :id "box-shadows"}

   [:div.row
    [:div.col-sm-6
     [:div.code ".shadow-sm"]
     [com/$checkerboard
      {:style {:width 200
               :height 200
               :margin-bottom css/mdpx}}
      [:div.shadow-sm
       {:style {:width 100
                :height 100
                :background-color 'white}}]]]
    [:div.col-sm-6
     [:div.code ".shadow-md"]
     [com/$checkerboard
      {:style {:width 200
               :height 200
               :margin-bottom css/mdpx}}
      [:div.shadow-md
       {:style {:width 100
                :height 100
                :background-color 'white}}]]]
    [:div.col-sm-6
     [:div.code ".shadow-lg"]
     [com/$checkerboard
      {:style {:width 200
               :height 200
               :margin-bottom css/mdpx}}
      [:div.shadow-lg
       {:style {:width 100
                :height 100
                :background-color 'white}}]]]

    [:div.col-sm-6
     [:div.code ".shadow-inner"]
     [com/$checkerboard
      {:style {:width 200
               :height 200
               :margin-bottom css/mdpx}}
      [:div.shadow-inner
       {:style {:width 100
                :height 100
                :background-color 'white}}]]]]])

(defn $colors []
  [com/$section
   {:title "Colors"
    :id "colors"}

   [com/$subsection
    {:title "Google Colors"}

    [:div.row
     (->> css-colors/google-colors-in-order
          (partition 2)
          (map (fn [[color-key vs]]
                 [:div.col-sm-3
                  {:key color-key}
                  [:h4.bold (str color-key)]
                  [:div.mg-bot-lg
                   (->> vs
                        (partition 2)
                        (map (fn [[shade-key v]]
                               [:div.flex-apart
                                {:key shade-key
                                 :style {:background-color v
                                         :padding "5px 10px"
                                         :font-weight 'bold
                                         :color (if
                                                    true #_(> (css-colors/lightness v) 60)
                                                  'black 'white)
                                         :text-align 'center}}
                                (pr-str shade-key)

                                [bc/$copy-button
                                 {:rest-fg (if
                                               true #_(> (css-colors/lightness v) 60)
                                             'black 'white)
                                  :text (str
                                          "(com/gco "
                                          (str color-key)
                                          " "
                                          (str shade-key)
                                          ")")}]])))]])))]]])


(defn $images []
  [com/$section
   {:title "Images"
    :id "images"}
   [bc/$image {:src "https://cdn.dribbble.com/users/13307/screenshots/4429342/packaging_illustration_chocolate_design_minimal.jpg"
               :style {:width "100%"}}]])

(def nav
  {:title "UI"
   :href "/sg/ui"
   :route-key ::route
   :links (->> [{:href "/sg/ui#units"
                 :title "Units"}
                {:href "/sg/ui#colors"
                 :title "Colors"}
                {:href "/sg/ui#typography"
                 :title "Typography"}
                {:href "/sg/ui#buttons"
                 :title "Buttons"}
                {:href "/sg/ui#progress"
                 :title "Progress"}
                {:href "/sg/ui#box-shadows"
                 :title "Box Shadows"}
                {:href "/sg/ui#images"
                 :title "Images"}]
               (mapv (fn [m]
                       (merge
                         m
                         {:route [::route
                                  {:section-id
                                   (-> m
                                       :href
                                       (str/split "#")
                                       last)}]}))))})

(defn $content []
  [:div.pad-md
   [bc/$callout
    {:paras
     [["Edit namespace "
       [:strong "rx.css c/inject-css-defs"]
       " to change these defaults"]]}]
   [$units]
   [$colors]
   [$typography]
   [$buttons]
   [$progress]
   [$box-shadows]
   [$images]])
