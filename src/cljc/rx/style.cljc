(ns rx.style)

(def dark-bg-color-2 "#2A2A2A")
(def dark-fg-color-0 "rgba(255,255,255,0.87)")
(def dark-fg-color-1 "#C2C3C1")
(def dark-fg-color-2 "#a2a3a1")
(def dark-fg-color-selected-0 "#2664D6")

(def form-label-color "#333")
(def form-label-disabled-color "#aaa")
(def ios-button-fg-color "#0D94FC")
(def ios-button-fg-disabled-color "#bbb")
(def ios-button-font-size 17)

(def edge-pad 5)
(def list-chevron-color "#ccc")
(def list-chevron-disabled-color "#eee")


(def dark-fg-0 "#E8E8E8")
(def dark-fg-1 "#C2C3C1")
(def dark-fg-2 "#8A8A8A")
(def dark-fg-3 "#6A6A6A")
(def dark-bg-0 "#121212")
(def dark-bg-1 "#1F1F1F")
(def dark-bg-2 "#2a2a2a")
(def dark-bg-3 "#3a3a3a")

(def dark-colors
  {:color/bg-0 dark-bg-0
   :color/bg-1 dark-bg-1
   :color/bg-2 dark-bg-2
   :color/bg-3 dark-bg-3
   :color/fg-0 dark-fg-0
   :color/fg-1 dark-fg-1
   :color/fg-2 dark-fg-2
   :color/fg-3 dark-fg-3
   :color/fg-hl-0 "#2664D6"
   :color/action-fg-0 "#eceff1"
   :color/action-fg-1 "#eceff1"
   :color/list-separator dark-bg-3
   :color/forms-label-color dark-fg-1
   :color/forms-label-disabled-color dark-fg-2})

(def dark-fonts
  {:font/monospace "'Inconsolata', monospace"
   :font/header "'Roboto', 'Helvetica Neue', sans-serif"
   :font/copy "'Inter', 'Roboto', 'Helvetica Neue', sans-serif"
   :font/impact "'Roboto Condensed', 'Helvetica Neue', sans-serif"})

(def dark-theme
  (merge
    dark-colors
    dark-fonts
    {:size/base-pad 5
     
     :style/primary-text {:color dark-fg-0}
     :style/secondary-text {:color dark-fg-2}
     :style/list {:background-color dark-bg-0}

     :style/text-input {:color dark-fg-0}
     :style/ios {:keyboard-appearance "dark"}}))

(def !theme (atom dark-theme))

(defn v [& path]
  (get-in @!theme path))

(defn pad [& [n]]
  (* (v :size/base-pad) (or n 1)))

