(ns rx.themes.matrix
  (:require [rx.kitchen-sink :as ks]))

(def dark-fg-0 "#E8E8E8")
(def dark-fg-1 "#C2C3C1")
(def dark-fg-2 "#8A8A8A")
(def dark-fg-3 "#6A6A6A")
(def dark-bg-0 "#121212")
(def dark-bg-1 "#1F1F1F")
(def dark-bg-2 "#2a2a2a")
(def dark-bg-3 "#3a3a3a")

(def dark-action-fg-0 "#bed9eb")

(defn dark-rnav-default-style []
  {:statusBar
   {:style "light"}
   :modalPresentationStyle "overFullScreen"
   :topBar
   {:title
    {:fontFamily "HelveticaNeue-Medium"
     :color dark-fg-0}
    :hideOnScroll false
    :background {:color dark-bg-0}
    :backButton
    {:title ""
     :color dark-action-fg-0}
    :barStyle "black"
    :leftButtonColor dark-action-fg-0
    :rightButtonColor dark-action-fg-0}
   :overlay
   {:interceptTouchOutside false}
   :bottomTabs
   {:backgroundColor dark-bg-0
    :barStyle "black"}
   #_{:backgroundColor "white"}
   :bottomTab
   #_{:iconColor "#444"
      :selectedIconColor "black"
      :textColor "#444"
      :selectedTextColor "black"}
   {:iconColor dark-fg-0
    :selectedIconColor dark-action-fg-0
    :textColor dark-fg-2
    :selectedTextColor dark-action-fg-0}
   :layout {:backgroundColor dark-bg-0
            :componentBackgroundColor dark-bg-0}})

(defn theme []
  (let [bg-0 "#0D0208"
        bg-1 "#190F14"
        bg-2 "#241B20"
        bg-3 "#30282C"

        fg-0 "#00FF41"
        fg-1 "#00CC34"
        fg-2 "#009927"
        fg-3 "#00661A"
        fg-4 "#003b0f"
        action-fg-0 "#008F11"]
    (merge
      {::id ::matrix

       :space/baseline 4
       :space/xxs 4
       :space/xs 8
       :space/sm 16
       :space/md 24
       :space/xl 32
       :space/xxl 40

       :color/action fg-0
       :color/action-disabled fg-3
       :color/success "#4ED9BE"
       :color/warning "#F39555"
       :color/error "#D62A3B"
       :color/secondary-text "#68778C"
       :color/primary-text "#39414D"
       :color/white "#FFFFFF"
       :color/black "#272C34"

       :type/heading-fg-color fg-2
       :type/heading-font-family "Menlo"
       :type/body-font-family "Menlo"
       :type/body-font-size 17
       :type/action-font-weight "bold"
       
       :color/bg-0 bg-0
       :color/bg-1 bg-1
       :color/bg-2 bg-2
       :color/bg-3 bg-3
       :color/fg-0 fg-0
       :color/fg-1 fg-1
       :color/fg-2 fg-2
       :color/fg-3 fg-3
       :color/fg-4 fg-4
       :color/action-fg-0 action-fg-0


       :font/monospace "monospace"
       :ios/keyboard-appearance "dark"

       :style/primary-text {:font-size 16
                            :color fg-0
                            :font-family "Menlo"
                            :line-height 22}

       :style/secondary-text {:color fg-2}
       :style/list {:background-color bg-0}

       :list/chevron-color fg-3
       :list/separator-color bg-2
       :list/separator-size 1

       :size/base 5
       :rnav/default-options
       {:statusBar
        {:style "dark"
         :visible false}
        :modalPresentationStyle "overFullScreen"
        :topBar
        {:noBorder true
         :background {:color bg-0}
         :title
         {:fontFamily "Menlo"
          #_"HelveticaNeue-Medium"
          :color fg-1}
         :hideOnScroll false
         
         :backButton {:title ""
                      :color action-fg-0}
         ;;:barStyle "black"
         :leftButtonColor action-fg-0
         :rightButtonColor action-fg-0}
        :overlay
        {:interceptTouchOutside false}
        :bottomTabs
        {:backgroundColor bg-0
         :barStyle "black"}
        #_{:backgroundColor "white"}
        :bottomTab
        #_{:iconColor "#444"
           :selectedIconColor "black"
           :textColor "#444"
           :selectedTextColor "black"}
        {:iconColor fg-0
         :selectedIconColor action-fg-0
         :textColor fg-2
         :selectedTextColor action-fg-0}
        :layout {:backgroundColor bg-0
                 :componentBackgroundColor bg-0}}})))
