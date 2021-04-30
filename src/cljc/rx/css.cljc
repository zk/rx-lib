(ns rx.css
  (:require [clojure.string :as str]
            [rx.css.colors :as rc]
            [rx.kitchen-sink :as ks]
            [garden.units :as u]
            [garden.color :as co]
            [garden.stylesheet :refer [at-media]]
            [garden.core :as garden]
            [garden.stylesheet :as gs]
            #?(:clj [rx.forms :as forms])))

(def flex-keys [:flex
                :flex-direction
                :align-items
                :justify-content])

(def cream "#F7F8F1")
(def light-blue "#65C6EA")
(def green "#3CB477")
(def orange "#FB6732")
(def light-grey "#CFD8D7")
(def red "#BD5448")
(def black "#181818")
(def light-black (co/as-hex (co/lighten black 3)))
(def dark-grey "#404B48")

(def med-grey (co/as-hex (co/lighten dark-grey 20)))

(def page-background black)

(def dark-font-color black)


(def grey-100 "#F0FBFB")
(def grey-200 "#EEEEEE")
(def grey-300 "#E1E8E8")
(def grey-400 "#D1D7D7")
(def grey-500 "#B9CACA")
(def grey-600 "#929D9D")
(def grey-700 "#5D5D5D")
(def grey-800 "#505555")
(def grey-900 "#1F2831")


(def road-background black)
(def road-label-text-color light-grey)
(def road-label-background (co/as-hex (co/lighten black 5)))
(def road-border dark-grey)

(def road-tick-line-color dark-grey)

(def road-loop-background-color "rgba(255,255,255,0.2)")
(def road-current-time-color light-grey)

(def lower-content-padding "0px")

(def font-hrefs ["https://fonts.googleapis.com/css?family=Roboto+Condensed:300,400,700|Roboto:100,300,400,500,700|Inconsolata:400,700|Cardo:400,400i,700"])

#_(c/inject-css-defs
  {:sizes {:xs 5 :sm 10 :md 26 :lg 42 :xl 110}
   :fonts {:header "'Roboto', 'Helvetica Neue', sans-serif"
           :copy "'Inter', 'Roboto', 'Helvetica Neue', sans-serif"
           :impact "'Roboto Condensed', 'Helvetica Neue', sans-serif"
           :monospace "'Inconsolata', monospace"}
   :buttons {:primary {:base-bg-color light-blue}
             :secondary {:base-bg-color light-blue}
             :success {:base-bg-color green}
             :warning {:base-bg-color orange}
             :danger {:base-bg-color red}}})

(declare
  mg-bot-md mg-none xs sm md lg xl header-font
  copy-font impact-font mdpx xspx)

(def source-sans-pro "'Source Sans Pro','Helvetica Neue', Helvetica, sans-serif")

(def red2 rc/red)
(def pnk rc/pnk)
(def prp rc/prp)
(def dprp rc/dprp)
(def ind rc/ind)
(def blu rc/blu)
(def lb rc/lb)
(def cyn rc/cyn)
(def teal rc/teal)
(def gr rc/gr)
(def lgr rc/lgr)
(def lime rc/lime)
(def yl rc/yl)
(def amb rc/amb)
(def org rc/org)
(def dorg rc/dorg)
(def brn rc/brn)
(def blgr rc/blgr)


(defn gco [color shade]
  (get-in rc/google-colors-map
          [color shade]))

(defn px [n]
  (str n "px"))

(def border-radius-sm
  {:border-radius (px 2)})

(def border-radius-md
  {:border-radius (px 3)})

(def border-radius-lg
  {:border-radius (px 5)})

(def border-radius-round
  {:border-radius "999px"})

(def lh100 {:line-height "100%"})
(def lh120 {:line-height "120%"})
(def lh150 {:line-height "150%"})

(def copy-color (blgr :900))

(defn transition [v]
  {:transition v
   :-webkit-transition (if (string? v)
                         (str/replace v #"transform" "-webkit-transform")
                         v)
   :-moz-transition v
   :-ms-transition v
   :-o-transition v})

(def ellipsis
  {:flex 1
   :overflow 'hidden
   :white-space 'nowrap
   :text-overflow 'ellipsis})

(def text-center {:text-align 'center})
(def text-right {:text-align 'right})
(def text-left {:text-align 'left})

(def checkerboard #_ (c/checkerboard-gen {}))

(def layouts
  #_[[:.mag-left
      {:width "100%"
       :height "100%"}
      [:.mag-sidebar
       {:left 0}]
      [:.mag-content
       {:width "70%"
        :min-height "100%"
        :margin-left "30%"
        :position 'relative}]]
     [:.mag-right
      {:width "100%"
       :height "100%"}
      [:.mag-sidebar
       {:right 0}]
      [:.mag-content
       {:width "70%"
        :min-height "100%"
        :margin-right "30%"
        :position 'relative}]]

     [:.mag-left :.mag-right
      [:.mag-sidebar
       (c/at-bp :xs {:display 'none})
       {:position 'fixed
        :top 0
        :bottom 0
        :width "30%"
        :overflow-y 'scroll}]
      [:.mag-content
       (c/at-bp :xs {:width "100%"
                     :margin 0})]]


     [:div.fold-concious
      {:width "100%"
       :height "100%"}]
     [:div.above-fold-static
      {:width "100%"
       :height "100%"
       :position 'relative}]
     [:div.above-fold
      {:min-width "100%"
       :min-height "100%"}]])

(def styleguide
  #_[[:.sg-component
      {:margin 0
       :padding-top mdpx
       :margin-bottom xlpx}]
     [:.sg-checkerboard
      {:color 'black}
      pad-sm
      flex-vcenter
      border-radius-sm
      checkerboard]
     [:.sg-section-header
      flex-apart]
     [:.sg-subsection-header
      {:margin 0
       :margin-top lgpx
       :margin-bottom mdpx
       :font-weight 'bold}]

     [:.sg-row-label
      {:margin-right mdpx
       :font-weight 'bold
       :font-size (px 13)
       :color (blgr :400)
       :text-transform 'uppercase
       :line-height "100%"}
      (c/at-bp :xs text-left)]
     [:.css-garage
      {:color 'black}
      pad-lg
      mg-lg
      border-radius-md
      checkerboard]
     [:.sg-navbar
      [:li [:a
            {:color (lb :700)}
            {:display 'inline-block
             :padding "3px 6px"
             :font-weight '400
             :margin-right "-6px"}
            border-radius-sm
            [:&:hover
             {:background-color (blgr :50)}]]]
      [:.sg-header
       {:font-family copy-font
        :letter-spacing (px 1)
        :text-transform 'uppercase
        :font-size (px 14)
        :font-weight '500
        :color 'black}
       [:.sg-num
        {:font-weight '700}]
       mg-bot-sm]]

     [:.sg-opts-values
      [:pre
       {:background-color 'white}
       border-radius-md]]

     [:.sg-opts-prop
      {:font-family monospace-font
       :font-weight 'bold
       :font-size (px 16)}]

     [:.sg-opts-desc
      {:font-size (px 14)}]])

(def cardo "'Cardo', sans-serif")

(def app
  #_[(c/progress css-spec)]
  #_[(c/defaults css-spec)
     (c/headers
       css-spec
       {:font-weight '400})

     [:h5 {:font-size "14px"
           :font-weight 'bold
           :letter-spacing "1px"
           :text-transform 'uppercase
           :padding 0
           :margin 0}]
     (c/utilities css-spec)
     layouts
     (c/progress css-spec)
     (c/buttons css-spec)

     comps/css
     styleguide
     c/flexbox

     (comps/copy-button-css)

     [:html :body
      {:background-color 'white
       :font-family copy-font}]

     [:#cljs-entry {:width "100%"
                    :height "100%"}]

     [:.mag-sidebar
      {:text-align 'right
       :background-color (blgr :25)}]

     #_[:.mag-content]

     [:.sg-sidebar
      {:max-width (px 200)
       :padding-right "20px"
       :margin-left 'auto}]

     [:iframe {:margin 0 :padding 0}]

     [:.mark-group
      {:padding-top "10px"
       :padding-bottom "10px"}
      [:&:hover {:background-color "rgba(0,0,0,0.05)"}]]
     [:.hsm {:margin-top smpx}]
     [:.rx-editor
      #_[:.ql-editor {:padding "10px 10px 10px 0px"}]
      #_[".ql-editor.ql-blank::before" {:left "0px"}]
      #_[".ql-container.ql-snow" {:border "none"}]
      [:.ql-toolbar.ql-snow {:border-top "none"}]]

     [:.DraftEditor-root
      :.DraftEditor-editorContainer
      :.public-DraftEditor-content
      {:height "100%"
       :flex-grow 1}]

     [:.noscrollbar
      ["&::-webkit-scrollbar"
       {:height "0 !important"
        :width "0 !important"}]]

     [:html :body
      {:min-height "100%"}]
     [:.timeline-container
      {:color road-label-text-color}]
     [:.hover-lighten-bg-01
      [:&:hover {:background-color "rgba(255,255,255,0.1)"}]]
     [:.hover-lighten-bg-02
      [:&:hover {:background-color "rgba(255,255,255,0.2)"}]]
     [:.hover-lighten-bg-05
      [:&:hover {:background-color "rgba(255,255,255,0.5)"}]]
     [:.hover-darken-bg-01
      [:&:hover {:background-color "rgba(0,0,0,0.1)"}]]
     [:.cursor-pointer
      {:cursor 'pointer}]
     #?(:clj
        (forms/css))])


(defn spinner [base-key
               {:keys [size
                       ring-size
                       ring-color
                       bar-color
                       base-pad]
                :or {size 20
                     ring-size 2
                     bar-color "red"
                     ring-color "#f0f0f0"
                     base-pad 5}}]
  (let [base-str (name base-key)]
    [[base-key
      (keyword (str base-str ":after"))
      {:border-radius "50%"
       :width (px size)
       :height (px size)
       :margin (px base-pad)
       :transition "opacity 0.8s ease"
       :opacity 0}]
     [base-key
      {;;:margin (str size "px auto")
       :font-size "10px"
       :position 'relative
       :text-indent "-9999em"
       :border-top (str (px ring-size) " solid " ring-color)
       :border-right (str (px ring-size) " solid " ring-color)
       :border-bottom (str (px ring-size) " solid " ring-color)
       :border-left (str (px ring-size) " solid " bar-color)
       :transform "translateZ(0)"
       :animation "load8 0.7s infinite linear"}
      [:&.active {:opacity 1}]]
     (gs/at-keyframes
       :load8
       ["0%" {:transform "rotate(0deg)"}]
       ["100%" {:transform "rotate(360deg)"}])]))


 
