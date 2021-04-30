(ns rx.theme
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [rx.themes.matrix :as matrix]
            #?(:cljs [reagent.core :as r]))
  (:refer-clojure :exclude [resolve]))


;; Colors

(def DEFAULT_THEME_KEY ::light #_::matrix)

(def tailwind-colors
  {:grey {50 "#F9FAFB"
          100 "#F3F4F6"
          200 "#E5E7EB"
          300 "#D1D5DB"
          400 "#9CA3AF"
          500 "#6B7280"
          600 "#4B5563"
          700 "#374151"
          800 "#1F2937"
          900 "#111827"}
   :green {800 "#065F46"
           100 "#D1FAE5"
           900 "#064E3B"
           200 "#A7F3D0"
           300 "#6EE7B7"
           400 "#34D399"
           50 "#ECFDF5" 
           500 "#10B981" 
           600 "#059669" 
           700 "#047857"}
   :blue {50 "#EFF6FF"
          100 "#DBEAFE"
          200 "#BFDBFE"
          300 "#93C5FD"
          400 "#60A5FA"
          500 "#3B82F6"
          600 "#2563EB"
          700 "#1D4ED8"
          800 "#1E40AF"
          900 "#1E3A8A"}
   :red {50
         "#FEF2F2"
         100
         "#FEE2E2"
         200
         "#FECACA"
         300
         "#FCA5A5"
         400
         "#F87171"
         500
         "#EF4444"
         600
         "#DC2626"
         700
         "#B91C1C"
         800
         "#991B1B"
         900
         "#7F1D1D"}})

#_{:fg (->> tailwind-colors
          :grey
          (sort-by first)
          reverse
          (mapv second))
 :bg (->> tailwind-colors
          :grey
          (sort-by first)
          reverse
          (mapv second))}

(defn gen-colors [key->colors]
  (->> key->colors
       (sort-by first)
       
       (mapcat (fn [[k colors]]
                 (->> colors
                      (map-indexed
                        (fn [i v]
                          [(keyword
                             "color"
                             (str (name k)
                                  "-"
                                  i))
                           v])))))
       (into {})))

(def light-base-size 5)

(defn light-rnav-default-style []
  {:statusBar
   {:style "light"
    :visible true}
   :modalPresentationStyle "overFullScreen"
   :topBar
   {:title
    {;;:fontFamily "Menlo"
     :color "#333"}
    :noBorder true
    :hideOnScroll false
    :background {:color "white"}
    :backButton
    {:title ""
     :color "#2182F8"}
    :barStyle "default"
    :leftButtonColor "#2182F8"
    :rightButtonColor "#2182F8"}
   :overlay
   {:interceptTouchOutside false}
   :bottomTabs
   {:backgroundColor "white"
    :barStyle "black"}
   #_{:backgroundColor "white"}
   :bottomTab
   #_{:iconColor "#444"
      :selectedIconColor "black"
      :textColor "#444"
      :selectedTextColor "black"}
   {:iconColor "#333"
    :selectedIconColor "#2182F8"
    :textColor "#333"
    :selectedTextColor "#2182F8"}
   :layout {:backgroundColor "white"}})

(def default-light
  (merge
    { ;;:type/default-font-family "Menlo"
     :type/display-font-size 32
     :type/display-line-height 48
     :type/display-font-weight "bold"
     :type/display-font-family "'Rubik'"
     
     :type/headline-font-size 25
     :type/headline-font-weight "bold"
     :type/headline-font-family "'Rubik'"
     :type/headline-line-height 32
     :type/headline-letter-spacing 1
     
     :type/title-font-size 19
     :type/title-line-height 20
     :type/title-font-weight "bold"
     ;;:type/title-font-family "Menlo"
     :type/header-font-size 17
     :type/header-line-height 20
     :type/header-font-weight "600"
     ;;:type/header-font-family "Menlo"
     :type/body-font-size 17
     :type/body-line-height 22
     :type/body-font-weight "normal"
     :type/body-font-family "'Roboto'"

     :type/subtext-font-size 15
     :type/subtext-line-height 16
     :type/subtext-font-weight "normal"

     :type/label-font-size 12
     :type/label-line-height 12
     :type/label-font-weight "normal"
     
     :type/action-font-weight "bold"}
    {:list/separator-color "#eee"
     :list/separator-size 1
     :list/separator-inset 0
     
     :list/chevron-color "#ccc"
     :list/chevron-disabled-color "#eee"

     :list/item-icon-color "#222"}
    {:vee.bottom-tabs/fg-color "#888"
     :vee.bottom-tabs/active-fg-color "#126DD8"}
    {:vee.nav/default-header-opts
     {:hideShadow true
      :backTitle ""}}
    {:vee.ui/solid-button-border-radius 8}
    {:vee.forms/section-inset-hpad 3}

    #_{:buttons/font-size {:sm 12 :md 17 :lg 22}
       :buttons/border-radius {[:intent "default" :size "sm"] 12}
       :buttons/hollow-colors {:default ""}}

    {:theme/key ::light
     ::id ::light

     :color/action "#718AC0"
     :color/action-disabled "lightblue"
     :color/success "#4ED9BE"
     :color/warning "#F39555"
     :color/error "#D62A3B"
     :color/text-primary "black"
     :color/text-secondary "#888"

     :rx.browser.buttons/color-intent-default "#718AC0"
     :rx.browser.buttons/color-intent-danger "#D62A3B"
     :rx.browser.buttons/color-intent-none "#888"

     :color/bg0 "white"

     :space/baseline 4
     :space/xxs 4
     :space/xs 8
     :space/sm 16
     :space/md 24
     :space/xl 32
     :space/xxl 40
     
     :size/base light-base-size
     :size/sm (* light-base-size 2)
     :size/md (* light-base-size 4)
     :size/lg (* light-base-size 8)
     
     :forms/label-fg-color "#333"
     :forms/label-disabled-fg-color "#aaa"
     :forms/input-fg-color "#333"
     :forms/input-bg-color "white"
     :forms/text-input-padding (* light-base-size 2)
     :forms/input-disabled-fg-color "#aaa"
     :forms/placeholder-fg-color "#888"
     :forms/bg-color-0 "white"
     :forms/fg-color-0 "#333"

     :comps/action-fg-color "#1F7CED"
     :comps/action-disabled-fg-color "#eceff1"
     :comps/button-font-size 16
     
     :rnav/default-options (light-rnav-default-style)

     :calendars/theme {:backgroundColor "white"
                       :calendarBackground "white"
                       :selectedDayBackgroundColor "#000000"
                       :dotColor "#aaa"
                       :dayTextColor "#555"
                       :selectedDayTextColor "#ffffff"
                       "stylesheet.day.basic" {:todayText {:fontWeight "bold"}}}

     :color/bg-0 "white"
     :color/bg-1 "#eee"
     :color/bg-2 "#ddd"
     :color/bg-3 "#ccc"
     :color/fg-0 "#333"
     :color/fg-1 "#777"
     :color/fg-2 "#999"
     :color/fg-3 "#bbb"
     :color/action-fg-0 "#1F7CED"
     :color/action-disabled-fg-0 "#eceff1"

     :evry.entry2/border-size 0
     :box-shadow/sm {:box-shadow "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)"}}
    (->> tailwind-colors
         (mapcat (fn [[name-key weight->hex]]
                   (->> weight->hex
                        (map (fn [[weight hex]]
                               [(keyword
                                  "color"
                                  (str (name name-key) "-" weight))
                                hex])))))
         (into {}))))

(def light-mobile
  (merge
    default-light
    {:type/display-font-family "System"
     :type/headline-font-family "System"
     :type/title-font-family "System"
     :type/body-font-family "System"
     :vee.fast-list/separator-inset 12
     :vee.ui/list-row-vpad 12
     :vee.ui/list-row-hpad 0
     :vee.forms/form-row-inset 12
     :vee.forms/section-heading-inset 12}))

(def dark-fg-0 "#E8E8E8")
(def dark-fg-1 "#C2C3C1")
(def dark-fg-2 "#8A8A8A")
(def dark-fg-3 "#6A6A6A")
(def dark-bg-0 "#0F0F10")
(def dark-bg-1 "#151516")
(def dark-bg-2 "#2a2a2a")
(def dark-bg-3 "#3a3a3a")

(def dark-action-fg-0 "#bed9eb")

(defn dark-rnav-default-style []
  {:statusBar
   {:style "dark"}
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

(defn default-dark []
  (merge
    default-light
    {:color/bg-0 dark-bg-0
     :color/bg-1 dark-bg-1
     :color/bg-2 dark-bg-2
     :color/bg-3 dark-bg-3
     :color/fg-0 dark-fg-0
     :color/action-fg-0 dark-action-fg-0
     
     :forms/label-fg-color dark-fg-1
     :forms/label-disabled-fg-color dark-fg-3
     :forms/input-fg-color dark-fg-0
     :forms/input-bg-color dark-bg-1
     :forms/input-disabled-fg-color dark-fg-3     
     :forms/placeholder-fg-color dark-fg-3
     :forms/bg-color-0 dark-bg-0
     
     :forms/fg-color-0 dark-fg-0
     :ios/keyboard-appearance "dark"

     :style/primary-text {:font-size 17
                          :color dark-fg-0}

     :style/secondary-text {:color dark-fg-2}
     :style/list {:background-color dark-bg-0}

     :rnav/default-options (dark-rnav-default-style)}))

(defn matrix-theme []
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
      (default-dark)
      {::id ::matrix
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
       
       :forms/label-fg-color fg-2
       :forms/label-disabled-fg-color fg-3
       :forms/input-fg-color fg-0
       :forms/input-bg-color bg-1
       :forms/input-disabled-fg-color fg-3     
       :forms/placeholder-fg-color fg-3
       :forms/bg-color-0 bg-0
       :forms/fg-color-0 fg-0
       :forms/text-input-style {:color fg-0}
       :ios/keyboard-appearance "dark"

       :text/font-family "Menlo"

       

       :style/primary-text {:font-size 16
                            :color fg-0
                            :font-family "Menlo"
                            :line-height 22}

       :style/secondary-text {:color fg-2}
       :style/list {:background-color bg-0}

       :list/chevron-color fg-3
       :list/separator-color bg-1

       :rnav/default-options
       {:statusBar
        {:style "dark"}
        :modalPresentationStyle "overFullScreen"
        :topBar
        {:title
         {:fontFamily "Menlo"
          #_"HelveticaNeue-Medium"
          :color fg-1}
         :hideOnScroll false
         :background {:color bg-0}
         :backButton {:title ""
                      :color action-fg-0}
         ;;:barStyle "black"
         :leftButtonColor action-fg-0
         :rightButtonColor action-fg-0
         :borderColor "#ff0000"
         :noBorder false}
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

(def !id->theme
  (#?(:clj atom
      :cljs r/atom)
    {::light default-light
     ::dark (default-dark)
     ::matrix (matrix-theme)}))

(defn register! [k t]
  (swap! !id->theme assoc k t)
  k)

(defn register-multi! [m]
  (swap! !id->theme merge m))

(defn clear! []
  (reset! !id->theme nil))

(defn get-theme [& [arg-k overrides]]
  (let [k (if (map? arg-k)
            (or
              (::id arg-k)
              (:theme-id arg-k))
            arg-k)
        overrides (if (map? arg-k)
                    (or
                      (::theme arg-k)
                      (:theme arg-k))
                    overrides)]
    (merge-with ks/deep-merge
      (or (get @!id->theme k)
          (get @!id->theme DEFAULT_THEME_KEY))
      overrides)))

(def get! get-theme)

(defn v [k v]
  (get (get-theme k)
    v))

(defn pad
  ([]
   (pad nil 1))
  ([n]
   (pad nil n))
  ([k n]
   (when-let [x (v k :size/base)]
     (* x (or n 1)))))

(defn from-opts [{:keys [theme]} & [default-theme-key]]
  (get-theme (or theme default-theme-key)))

(defn refresh-themes []
  (reset! !id->theme {::light default-light
                      ::dark default-dark}))

(comment
  (v :foo :bar)
  (pad :foo 0))

(defn kw->class-name [kw]
  (when kw
    (let [ns (namespace kw)
          nm (name kw)]
      (->> [(when ns (str/replace ns #"\." "-"))
            (when ns "--")
            (str/replace nm #"\." "-")]
           (remove nil?)
           (apply str)))))

(defn theme-class [opts]
  (-> opts
      get!
      (get :rx.theme/id)
      kw->class-name))

(defn des [opts rules & [{:keys [debug-log?]}]]
  (let [style (if (:theme-disabled? opts)
                nil
                (get! opts))]
    (merge
      style
      (->> rules
           (map (fn [rule]
                  (let [rule (if (map? rule)
                               (:rule rule)
                               rule)
                        default-or-fn (if (not (keyword? (last rule)))
                                        (last rule)
                                        nil)

                        default (if (fn? default-or-fn)
                                  (default-or-fn style)
                                  default-or-fn)
                        
                        rule (if (not (keyword? (last rule)))
                               (butlast rule)
                               rule)

                        value (or (->> rule
                                       (map #(get style %))
                                       (remove nil?)
                                       first)
                                  default)

                        value (if (fn? value)
                                (value style)
                                value)]

                    {(first rule) value})))
           (into {})))))

(def !default-theme
  (let [theme
        #_(matrix/theme)
        default-light]
    #?(:clj (atom theme)
       :cljs (r/atom theme))))

(defn set-theme! [theme]
  (reset! !default-theme theme))

(comment

  (reset! !default-theme default-light)

  )

(defn current-theme [& [{:keys [::ignore-default?]
                         :as opts}]]
  (merge-with ks/deep-merge
    (when-not ignore-default?
      @!default-theme)
    opts
    (::theme opts)
    (:theme opts)))

(defn resolve [opts
               & [rules {:keys [debug-log?]}]]
  (let [res-theme (current-theme opts)]
    (ks/deep-merge
      res-theme
      (->> rules
           (map (fn [rule]
                  (when-not (sequential? rule)
                    (ks/throw-anom
                      {:code :invalid
                       :desc "Rule must be sequential"
                       ::rule rule}))
                  [(first rule)
                   (loop [segments rule #_(rest rule)
                          val nil]
                     (if (or (empty? segments) val)
                       val
                       (recur
                         (rest segments)
                         (let [segment (first segments)]
                           (cond
                             (keyword? segment) (get res-theme segment)
                             (fn? segment) (segment res-theme)
                             segment segment
                             :else nil)))))]))
           (into {})))))

(defn pad-padding [{:keys [style] :as opts} & [default]]
  (let [{:keys [:space/baseline
                :pad
                :vpad
                :hpad]}
        (resolve (merge default opts))

        vpadding (* (or pad vpad (:vpad default)) 1 #_baseline)
        hpadding (* (or pad hpad (:hpad default)) 1 #_baseline)

        style-padding (:padding style)]
    (if style-padding
      {:padding style-padding}
      #_{:padding-left (str hpadding "px")
         :padding-right (str hpadding "px")
         :padding-top (str vpadding "px")
         :padding-bottom (str vpadding "px")}
      {:padding-left hpadding
       :padding-right hpadding
       :padding-top hpadding
       :padding-bottom hpadding})))

(defn pad-margin [opts]
  (let [m (pad-padding opts)]
    {:margin-left (:padding-left m)
     :margin-right (:padding-right m)
     :margin-top (:padding-top m)
     :margin-bottom (:padding-bottom m)}))

(comment

  (resolve
    {::theme {::foo "bar"}}
    [[::foo "baz"]
     [::bar "asdf"]
     [::baz "foo"]])

  )

(defn size-mult [n]
  (fn [theme]
    (* (:size/base theme) n)))

(defn gen-background-styles [{:keys [colors]}]
  (merge
    (->> colors
         (mapcat
           (fn [[k weight->value]]
             (->> weight->value
                  (map (fn [[weight value]]
                         [(keyword
                            (str
                              "bg-"
                              (name k)
                              "-"
                              weight))
                          {:background-color value}])))))
         (into {}))
    {:bg-transparent {:background-color 'transparent}}))

(defn gen-border-styles [{:keys [border-radius]}]
  (->> border-radius
       (map (fn [[k v]]
              [(keyword
                 (str
                   "rounded-"
                   (name k)))
               {:border-radius v}]))
       (into {})))

(defn process-config [config]
  (let [config (merge
                 {:spacing (->> [0 1 2 4 6 8 10 12 14 16 20 24 28 32 36 40 44 48
                                 56 64 80 96 112 128 144 160 176 192 208 224 240
                                 256 288 320 384]
                                (map (fn [n]
                                       [n n]))
                                (into {}))
                  :border-radius {:sm 4
                                  :md 8
                                  :lg 16
                                  :full 9999}}
                 config)]
    (merge
      (gen-background-styles config)
      (gen-border-styles config))))





(comment
  
  (ks/pp (process-config
           {:colors {:pri (:blue tailwind-colors)
                     :grey (:cool-gray tailwind-colors)}}))

  
  (refresh-themes)
  
  (->> (str/split ""
         #"\s+")
       (partition 2)
       (reduce
         (fn [accu [k v]]
           (assoc
             accu
             (ks/parse-long k) v))
         {})
       ks/pp)

  )

