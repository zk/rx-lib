(ns rx.browser.forms
  (:require [rx.kitchen-sink :as ks
             :refer-macros [<&]]
            [rx.theme :as th]
            [rx.anom :as anom :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [rx.browser.components :as cmp]
            [rx.browser.buttons :as btn]
            [rx.browser.css :as css]
            [rx.browser.ui :as ui]
            [rx.browser.popper :as pop]
            [rx.browser.keybindings :as kb]
            [reagent.core :as r]
            [cljsjs.react :as react]
            [clojure.string :as str]
            [clojure.core.async
             :refer [go chan sliding-buffer <! alts! put!]])
  (:refer-clojure :exclude [range]))

(def css-rules
  [[".rx-browser-forms-group > *:last-child"
     {:margin-right "0 !important"
      :margin-bottom "0 !important"}]])

(def common-theme-docs
  [{:rule [:bg-color ::bg-color :color/bg-0]}
   {:rule [:fg-color ::fg-color :color/fg-0]}
   {:rule [:border-color ::border-color :color/bg-3]}
   {:rule [:hpad ::input-horizontal-padding 8]}
   {:rule [:vpad ::input-vertical-padding 4]}
   {:rule [:vspace ::vertical-spacing (th/size-mult 1)]}
   {:rule [:label-font-size ::label-font-size 13]}
   {:rule [:help-text-font-size ::help-text-font-size 13]}
   {:rule [:help-text-font-color ::help-text-font-color :color/fg-1]}
   {:rule [:error-text-font-size ::error-text-font-size 13]}
   {:rule [:error-text-font-color ::error-text-font-color "red"]}
   {:rule [:error-border-color ::error-border-color "red"]}])

(defn create-state [& [{:keys [initial-data
                               initial-errors
                               initial-submit-failed?]}]]
  {:!data (r/atom initial-data)
   :!errors (r/atom initial-errors)
   :!state (r/atom {:submit-failed? initial-submit-failed?})
   :!validating (r/atom nil)
   :!prop-validations (atom nil)})

(defn data-atom [{:keys [!data]}] !data)

(defn form-data [{:keys [!data]}]
  (when !data
    (ks/remove-nil-values @!data)))

(defn merge-data [{:keys [!data]} m]
  (when !data
    (swap! !data
      merge
      m)))

(defn errors-atom [{:keys [!errors]}] !errors)

(defn errors [{:keys [!errors]}] @!errors)

(defn set-error [{:keys [!errors]} data-key v]
  (when !errors
    (swap! !errors assoc data-key v)))

(defn clear-error [{:keys [!errors]} data-key]
  (when !errors
    (swap! !errors dissoc data-key)))

(defn get-error [{:keys [!errors]} data-key]
  (when !errors
    (get @!errors data-key)))

(defn clear-all-errors [{:keys [!errors]}]
  (reset! !errors {}))

(defn set-submitting [{:keys [!state]}]
  (when !state
    (swap! !state assoc :submitting? true)))

(defn clear-submitting [{:keys [!state]}]
  (when !state
    (swap! !state dissoc :submitting?)))

(defn submitting? [{:keys [!state]}]
  (when !state (get @!state :submitting?)))

(defn set-submit-failed [{:keys [!state]}]
  (when !state (swap! !state assoc :submit-failed? true)))

(defn clear-submit-failed [{:keys [!state]}]
  (when !state (swap! !state dissoc :submit-failed?)))

(defn submit-failed? [{:keys [!state]}]
  (when !state (get @!state :submit-failed?)))

(defn set-validating [{:keys [!state]} data-key]
  (when !state
    (swap! !state assoc-in [:validating data-key] true)))

(defn clear-validating [{:keys [!state]} data-key]
  (when !state
    (swap! !state update :validating dissoc data-key)))

(defn validating? [{:keys [!state]}]
  (when !state
    (not (empty? (:validating @!state)))))

(defn validating-data-key? [{:keys [!state]} data-key]
  (when !state
    (get-in @!state [:validating data-key])))

(defn get-context [comp]
  (when-let [o (.-context comp)]
    (.-wrapped o)))

(def form-context
  (.createContext js/React nil))

(def form-provider (.-Provider form-context))

(defn apply-react-context [m k]
  (let [rr (:reagent-render m)
        oc (:on-context m)]
    (merge
      m
      (when rr
        {:component-did-mount
         (fn [this]
           (when oc
             (oc (get-context this)))
           (when-let [f (:component-did-mount m)]
             (f this)))
         :reagent-render
         (fn [& args]
           (let [[opts & children] (ks/ensure-opts args)]
             (apply rr
               (merge
                 opts
                 {k (when-let [o (.-context
                                   (r/current-component))]
                      (.-wrapped o))})
               children)))}))))

(defn input [{:keys [full-width? label
                     help-text error
                     prepend append
                     render-input-control
                     form-state
                     data-key]
              :as opts}]
  (let [{:keys [::bg-color
                ::border-color
                ::input-horizontal-padding
                ::input-vertical-padding
                ::vertical-spacing
                ::label-font-size
                ::help-text-font-size
                ::help-text-font-color
                ::error-text-font-size
                ::error-text-font-color]}
        (th/des opts
          (map :rule common-theme-docs))

        error (or error
                  (when (and form-state data-key)
                    (get (errors form-state) data-key)))]
    [:div
     {:style (merge
               (:style opts)
               {:width "100%"
                :max-width 400}
               (when full-width?
                 {:max-width "100%"}))}
     [ui/group
      {:horizontal? true
       :justify-content 'space-between
       :align-items 'center}
      (when label
        [:label
         {:style {:line-height "110%"
                  :margin-bottom vertical-spacing
                  :font-size label-font-size
                  :font-weight 'bold}}
         label])
      (when (validating-data-key? form-state data-key)
        [ui/text {:scale "label"} "..."])]
     [:div
      {:style {:display 'flex
               :align-items 'center
               :width "100%"}}
      #_(when prepend prepend
              #_[:div
                 {:style {:padding-left input-horizontal-padding}}
                 prepend])
      [render-input-control
       (dissoc opts :style)]
      #_(when append append
              #_[:div
                 {:style {:padding-right input-horizontal-padding}}
                 append])]
     (when error
       [:div
        {:style {:margin-top vertical-spacing
                 :color 'red
                 :font-size error-text-font-size}}
        error])
     (when help-text
       [:div
        {:style {:font-size help-text-font-size
                 :margin-top vertical-spacing
                 :color help-text-font-color}}
        help-text])]))

(defn run-validation [{:keys [data-key]} form-state]
  (go
    (when data-key
      (when-let [!prop-validations (:!prop-validations form-state)]
        (let [validate (get @!prop-validations data-key)]
          (when validate
            (swap! (errors-atom form-state) dissoc data-key)
            (set-validating form-state data-key)
            (let [message (<& (validate (get (form-data form-state) data-key)))]
              (if message
                (swap! (errors-atom form-state)
                  assoc
                  data-key
                  message)
                (swap! (errors-atom form-state)
                  dissoc data-key)))
            (clear-validating form-state data-key)))))))

(defn <run-all-validations [{:keys [!prop-validations]
                             :as form-state}]
  (go
    (loop [data-key-valdiation-tuples @!prop-validations]
      (if (empty? data-key-valdiation-tuples)
        nil
        (let [[data-key validate] (first data-key-valdiation-tuples)]
          (when validate
            (swap! (errors-atom form-state) dissoc data-key)
            (set-validating form-state data-key)
            (let [message (<& (validate (get (form-data form-state) data-key)))]
              (when message
                (swap! (errors-atom form-state)
                  assoc
                  data-key
                  message)))
            (clear-validating form-state data-key))
          (recur (rest data-key-valdiation-tuples)))))))

(defn resolve-error [{:keys [error form-state data-key error?] :as opts}]
  (or error?
      error
      (when (and form-state data-key)
        (get (errors form-state) data-key))))

(defn text-input
  {:opts [[:data-key]
          [:form-state]
          [:placeholder]
          [:!val]
          [:on-change-text]
          [:input-type]
          [:style]
          [:autofocus?]]}
  [opts]
  (let [{:keys [throttle-validate-on-change]
         :or {throttle-validate-on-change 200}} opts
        change-validation-ch (chan (sliding-buffer 1))
        blur-ch (chan (sliding-buffer 1))]

    (go
      (let [ch (-> change-validation-ch
                   (ks/debounce throttle-validate-on-change)
                   (ks/throttle throttle-validate-on-change))]
        (loop []
          (let [[_ ch] (alts! [ch blur-ch])]
            (<! (run-validation opts nil))
            (recur)))))
    
    (r/create-class
      {:reagent-render
       (fn [{:keys [placeholder
                    !val
                    on-change-text
                    input-type
                    style
                    autofocus?
                    data-key
                    form-state]
             :as opts}]
         (let [{:keys [:bg-color
                       :border-color
                       :vpad
                       :hpad
                       :vspace
                       :label-font-size
                       :help-text-font-size
                       :help-text-font-color
                       :error-text-font-size
                       :error-text-font-color]}
               (th/resolve opts
                 (map :rule common-theme-docs))

               !val (or !val
                        (when (and form-state data-key)
                          (r/cursor
                            (data-atom form-state)
                            [data-key])))

               opts (merge opts {:!val !val})

               internal-on-change
               (fn [e]
                 (let [value (.. e -target -value)
                       value (if (empty? value)
                               nil
                               value)]
                   (when !val
                     (reset! !val value))
                   (when on-change-text
                     (on-change-text value))

                   (clear-error form-state data-key)

                   (when (:validate-on-change? opts)
                     (put! change-validation-ch :change))))

               on-blur (fn [e]
                         (when (:validate-on-blur? opts)
                           (put! blur-ch :blur)))]
           [input
            (merge
              opts
              {:render-input-control
               (fn [opts]
                 (let [error (resolve-error opts)]
                   [:input
                    (merge
                      {:type (or input-type "text")
                       :placeholder placeholder
                       :style (merge
                                {:flex 1
                                 :outline 0
                                 :padding-left hpad
                                 :padding-right hpad
                                 :padding-top vpad
                                 :padding-bottom vpad
                                 ;;:border 'none
                                 :border-width 1
                                 :border-style "solid"
                                 :border-color (if error error-text-font-color border-color)
                                 :background-color bg-color}
                                (:style opts))
                       :on-change internal-on-change
                       :on-blur on-blur}
                      (when autofocus?
                        {:autoFocus true})
                      (when !val
                        {:value @!val}))]))})]))})))

(defn password-input [opts] [text-input (merge opts {:input-type "password"})])

(defn range-input [{:keys [!val] :as opts}]
  [input
   (merge
     opts
     {:render-input-control
      (fn [{:keys [full-width? style] :as opts}]
        [:input
         (merge
           (dissoc opts
             :full-width?
             :help-text
             :render-input-control
             :!val)
           {:type "range"
            :style (merge
                     (when full-width?
                       {:width "100%"})
                     style)}
           (when !val
             {:value @!val
              :on-change (fn [e]
                           (reset! !val
                             (ks/parse-double
                               (.. e -target -value))))}))])})])

(def search-bar-theme-docs
  [{:rule [:h-inset ::h-inset 8]
    :doc "Horizontal padding of input"}
   {:rule [:v-inset ::v-inset 4]
    :doc "Vertical padding of input"}])

(defn search-bar [{:keys [!val on-change-text] :as opts}]
  (let [{:keys [h-inset
                v-inset]}
        (th/resolve opts
          (map :rule search-bar-theme-docs))]
    [:div
     {:style {:display 'flex
              :flex-direction 'row
              :flex 1
              :background-color 'red}}
     [:input {:type "text"
              :style {:flex 1
                      :padding-left h-inset
                      :padding-right h-inset
                      :padding-top v-inset
                      :padding-bottom v-inset}
              :on-change (fn [e]
                           (when on-change-text
                             (on-change-text 
                               (.. e -target -value))))}]]))

(defn process-form-children [opts children]
  (->> children
       ks/unwrap-children
       (map (fn [child]
              (browser/update-opts
                child
                (fn [child-opts]
                  (if (:form-state opts)
                    (merge
                      child-opts
                      {:form-state (:form-state opts)})
                    child-opts)))))))

(defn group
  {:doc "Like `rx.browser.ui/group` for form elements (labels, inputs, errors). Automatically passes `:data-key` to children."}
  [& args]
  (r/create-class
    {:reagent-render
     (fn [& args]
       (let [[{:keys [gap style horizontal? class data-key] :as opts} & children]
             (ks/ensure-opts args)
             form-context (get-context (r/current-component))]
         
         [:> form-provider
          {:value (js-obj "wrapped"
                    (merge
                      form-context
                      {:data-key data-key}))}
          (into
            [ui/g
             (merge
               {:class class}
               opts)]
            children)]))
     :context-type form-context}))

(defn section [& args]
  (let [[{:keys [gap style] :as opts} & children]
        (ks/ensure-opts args)
        
        children (process-form-children opts children)

        {:keys [title]} opts

        children (concat
                   (when title
                     [[ui/text
                       {:scale "title"}
                       title]])
                   children)

        children (->> children
                      (map
                        (fn [child]
                          (browser/update-opts
                            child
                            (fn [child-opts]
                              (ks/deep-merge
                                (select-keys opts [:data-key])
                                child-opts))))))

        children (concat
                   (->> children
                        butlast
                        (map
                          (fn [child]
                            (browser/update-opts
                              child
                              (fn [child-opts]
                                (merge-with ks/deep-merge
                                  {:style {:margin-bottom gap}}
                                  child-opts))))))
                   [(last children)])]
    (into
      [:div
       {:style style}]
      children)))

(defn bind-form-context [m]
  (merge
    m
    {:context-type form-context
     :reagent-render
     (fn [opts]
       (when-let [f (:reagent-render m)]
         (f (merge
              (get-context (r/current-component))
              opts))))}))

(defn error-group [{:keys [form-state data-key style] :as opts} & children]
  (let [children (process-form-children opts children)]
    (when (get-error form-state data-key)
      (into
        [:div {:style style}]
        children))))

(defn error-text [_]
  (r/create-class
    (->
      {:reagent-render
       (fn [{:keys [form-state data-key style text] :as opts} & children]
         (when-let [v (or text (get-error form-state data-key))]
           (let [{:keys [:error-text-font-color
                         :error-text-font-size]}
                 (th/resolve opts (map :rule common-theme-docs))]
             [:p
              {:style (merge
                        {:color error-text-font-color
                         :font-size error-text-font-size}
                        style)}
              v])))}
      bind-form-context)))

(defn handle-submit [{:keys [!errors opts] :as form-state}]
  (go
    (let [{:keys [on-submit-failed
                  on-submit]} opts]
      (clear-submit-failed form-state)
      (clear-all-errors form-state)
      (<! (<run-all-validations form-state))
      (when-not (empty? @!errors)
        (set-submit-failed form-state)
        (when on-submit-failed
          (on-submit-failed
            (errors form-state)
            form-state)))
      (when (and on-submit (empty? @!errors))
        (set-submitting form-state)
        (<& (on-submit
              (form-data form-state)
              form-state))
        (clear-submitting form-state)))))

(defn form
  {:doc "Top level wrapper for form elements. This is where you specify prop validations, default data, and the on-submit handler which is passed a map of the values of all child elements.

```
[forms/form
  {:on-submit
  (fn
   [form-data]
   (js/alert (str \"User entered email: \" (:email form-data))))}
  [forms/text {:data-key :email}]
  [forms/submit-button {:label \"Submit\"}]]
```"
   :opts
   [[:on-submit "Function called on form submit. Only called if validations pass. First argument is a map of `data-key -> value`, second arg is the form state map. If this function returns a channel, post on-submit actions will block until this channel provides a value which can be useful when needing to call out to an external server."]
    [:prop-validations "Map of `data-key -> validation fn`. These valdiations are used to validate user-provided values depending on how you've configured when to run validations (on blur, on submit, etc)."]
    [:on-submit-failed "Function called when the on-submit action results in failed prop validations. Called with a map of the data-key to failed validation message."]
    [:on-submit-reset "Called when "]]}
  [& [opts & children :as args]]
  (r/create-class
    (-> {:reagent-render
         (fn [& [opts & children :as args]]
           (let [[{:keys [form-state
                          on-submit
                          on-submit-failed
                        
                          disable-submit-prevent-default?]
                   :as opts} & children]
                 (ks/ensure-opts args)
                 form-state (or form-state (create-state opts))

                 form-state (merge form-state
                              {:opts opts})

                 opts (merge opts {:form-state form-state})
                 children children #_(process-form-children opts children)

                 {:keys [!prop-validations !errors]} form-state]

             (reset! !prop-validations (:prop-validations opts))
           
             [:form
              {:style (:style opts)
               :on-submit (fn [e]
                            (when-not disable-submit-prevent-default?
                              (.preventDefault e)
                              (.stopPropagation e))
                            (handle-submit form-state))}

              [:> form-provider {:value (js-obj "wrapped" {:form-state form-state})}
               (into
                 [ui/g (dissoc opts :prop-validations :initial-data)]
                 children)]]))}
        (browser/bind-lifecycle-callbacks opts))))

(defn submit-button [_]
  (r/create-class
    (->
      {:reagent-render
       (fn [& args]
         (let [[{:keys [form-state
                        on-submit
                        disable-submit-prevent-default?]
                 :as opts} & children] (ks/ensure-opts args)
               form-state (or form-state (create-state))

               opts (merge opts {:form-state form-state})
               children (process-form-children opts children)]
           [btn/button
            (merge
              opts
              {:type "submit"
               :disabled? (submitting? form-state)})]))}
      bind-form-context)))

(defn submit-button-bare [& args]
  (let [[{:keys [form-state
                 on-submit
                 disable-submit-prevent-default?]
          :as opts} & children] (ks/ensure-opts args)
        form-state (or form-state (create-state))

        opts (merge opts {:form-state form-state})
        children (process-form-children opts children)]
    [btn/bare
     (merge
       opts
       {:type "submit"
        :disabled? (submitting? form-state)})]))

(defn btn-bare [& args]
  (let [[{:keys [form-state
                 on-submit
                 disable-submit-prevent-default?]
          :as opts}] (ks/ensure-opts args)
        form-state (or form-state (create-state))

        opts (merge opts {:form-state form-state})]
    [btn/bare
     (merge
       opts
       {:disabled? (submitting? form-state)})]))

(defn label
  {:doc "Label element whose content is option `:text` and / or `children`"
   :opts [[:text "Text content of the label (or `children`)"]]}
  [& [opts & children :as args]]
  (let [[{:keys [text style] :as opts} & children] (ks/ensure-opts args)
        {:keys [:label-font-size
                :fg-color]}
        (th/resolve opts (map :rule common-theme-docs))
        children (ks/unwrap-children children)]
    (into
      [:label
       {:style (merge
                 {:font-weight 'bold
                  :color fg-color}
                 style)}
       text]
      children)))

(defn initialize-validation-loop
  [{:keys [throttle-validate-on-change !val]
    :or {throttle-validate-on-change 0}
    :as opts}]
  
  (let [change-ch (chan (sliding-buffer 1))
        blur-ch (chan (sliding-buffer 1))]

    (gol
      (let [ch (-> change-ch
                   (ks/debounce throttle-validate-on-change)
                   (ks/throttle throttle-validate-on-change))]
        (loop []
          (let [[[opts form-state e] got-ch] (alts! [ch blur-ch])]
            (when (or (and (= got-ch blur-ch)
                           (:validate-on-blur? opts))
                      (and (= got-ch change-ch)
                           (:validate-on-change? opts)))
              (<! (run-validation
                    opts
                    form-state)))

            (when (= got-ch blur-ch)
              (when-let [f (:on-blur opts)]
                (f e)))
            
            (recur)))))

    {:change-ch change-ch
     :blur-ch blur-ch}))

(defn resolve-!val [{:keys [!val form-state data-key initial-value] :as opts}]
  (let [!val (or !val
                 (when (and form-state data-key)
                   (r/cursor
                     (data-atom form-state)
                     [data-key])))]
    (merge opts {:!val !val})))

(defn handle-change-value [{:keys [!val
                                   on-change-text
                                   on-change-value
                                   form-state
                                   data-key
                                   validate-on-change?]
                            :as opts}
                           {:keys [change-ch]}
                           value
                           e]
  (let [value (if (and (sequential? value)
                       (empty? value))
                nil
                value)]

    (when !val (reset! !val value))

    (when on-change-text
      (on-change-text value))

    (when on-change-value
      (on-change-value value))

    (clear-error form-state data-key)

    (when validate-on-change?
      (put! change-ch :change))))

(defn create-input-state [opts]
  (initialize-validation-loop opts))

(defn text
  {:opts [[:data-key]
          [:form-state]
          [:placeholder]
          [:!val]
          [:on-change-text]
          [:on-focus]
          [:input-type]
          [:style]
          [:autofocus?]]}
  [opts]
  (let [{:keys [blur-ch] :as input-state}
        (create-input-state opts)]
    (r/create-class
      (->
        {:reagent-render
         (fn [{:keys [placeholder
                      value
                      on-change-text
                      on-key-press
                      on-key-down
                      input-type
                      form-state
                      style
                      autofocus?
                      data-key
                      error?
                      ref]
               :as opts}]
           (let [{:keys [:bg-color
                         :fg-color
                         :border-color
                         :error-border-color
                         :vpad
                         :hpad
                         :vspace
                         :label-font-size]}
                 (th/resolve opts
                   (map :rule common-theme-docs))

                 {:keys [!val] :as opts} (resolve-!val opts)

                 internal-on-change
                 (fn [e]
                   (let [value (.. e -target -value)]
                     (handle-change-value opts input-state value e)))

                 on-blur (fn [e]
                           (put! blur-ch [opts form-state e]))

                 error (resolve-error opts)]
             [:input
              (merge
                (select-keys opts [:on-focus :on-blur])
                {:ref ref
                 :type (or input-type "text")
                 :placeholder placeholder
                 :style (merge
                          {:outline 0
                           :padding-left hpad
                           :padding-right hpad
                           :padding-top vpad
                           :padding-bottom vpad
                           :border-width 1
                           :border-style "solid"
                           :border-color (if (or error error?) error-border-color border-color)
                           :background-color bg-color
                           :color fg-color
                           :display 'block}
                          (:style opts))
                 :on-change internal-on-change
                 :on-blur on-blur
                 :on-key-press on-key-press
                 :on-key-down on-key-down}
                (when autofocus?
                  {:autoFocus true})
                (when !val
                  {:value @!val})
                (when value
                  {:value value}))]))}
        (browser/bind-lifecycle-callbacks opts)
        bind-form-context))))

(def textarea-el-keys
  [:autocomplete :autofocus :cols
   :disabled :form :maxlength :minlength
   :name :placeholder :readonly :required :rows
   :spellcheck :wrap

   :on-key-down
   :on-key-up
   :on-key-press])

(defn config-mod-enter-submit [{:keys [mod-enter-submit?
                                       on-key-down
                                       form-state]}]
  (when mod-enter-submit?
    {:on-key-down
     (fn [e]
       (when on-key-down
         (on-key-down e))
       (when (and (= 13 (.-keyCode e)) (.-metaKey e))
         (when form-state
           (handle-submit form-state))))}))

(defn textarea
  {:opts [[:data-key]
          [:form-state]
          [:placeholder]
          [:!val]
          [:on-change-text]
          [:input-type]
          [:style]
          [:autofocus?]
          [:mod-enter-submit?]]}
  [opts]
  (let [{:keys [blur-ch]
         :as input-state}
        (create-input-state opts)]
    
    (r/create-class
      (-> {:reagent-render
           (fn [{:keys [placeholder
                        value
                        on-change-text
                        input-type
                        style
                        autofocus?
                        error?]
                 :as opts}]
             (let [{:keys [:bg-color
                           :fg-color
                           :border-color
                           :error-border-color
                           :vpad
                           :hpad
                           :vspace
                           :label-font-size]}
                   (th/resolve opts
                     (map :rule common-theme-docs))

                   {:keys [!val] :as opts} (resolve-!val opts)

                   internal-on-change
                   (fn [e]
                     (let [value (.. e -target -value)]
                       (handle-change-value opts input-state value e)))

                   on-blur (fn [e]
                             (when (:validate-on-blur? opts)
                               (put! blur-ch :blur)))

                   error (resolve-error opts)]
               [:textarea
                (merge
                  (select-keys opts textarea-el-keys)
                  (config-mod-enter-submit opts)
                  {:type (or input-type "text")
                   :placeholder placeholder
                   :style (merge
                            {:outline 0
                             :padding-left hpad
                             :padding-right hpad
                             :padding-top vpad
                             :padding-bottom vpad
                             :border-width 1
                             :border-style "solid"
                             :border-color (if (or error error?) error-border-color border-color)
                             :background-color bg-color
                             :color fg-color
                             :display 'block
                             :resize 'none}
                            (:style opts))
                   :on-change internal-on-change
                   :on-blur on-blur}
                  (when autofocus?
                    {:autoFocus true
                     :autofocus "autofocus"})
                  (when value
                    {:value value})
                  (when !val
                    {:value @!val}))]))}
          bind-form-context))))

(defn password
  {:opts [[:data-key]
          [:form-state]
          [:placeholder]
          [:!val]
          [:on-change-text]
          [:input-type]
          [:style]
          [:autofocus?]]}
  [opts]
  [text (merge opts {:input-type "password"})])

(defn range
  {:opts [[:min]
          [:max]
          [:step]
          [:data-key]
          [:form-state]
          [:!val]
          [:style]]}
  [opts]
  (let [{:keys [change-ch] :as input-state}
        (create-input-state opts)]
    (r/create-class
      (-> {:reagent-render
           (fn [opts]
             (let [{:keys [!val value style] :as opts} (resolve-!val opts)]
               [:input
                (merge
                  (select-keys
                    opts
                    [:min :max :step])
                  (merge
                    {:type "range"
                     :style style
                     :default-value (or value
                                        (when !val (or @!val 0))
                                        0)
                     :on-change (fn [e]
                                  (let [v (ks/parse-double
                                            (.. e -target -value))]
                                    (handle-change-value opts input-state v e)))}
                    (when !val
                      {:value (or @!val 0)})
                    (when value
                      {:value value})))]))}
          bind-form-context))))

(defn checkbox
  {:opts [[:label "Checkbox label"]
          [:data-key]
          [:form-state]
          [:!val]
          [:style]
          [:label-style]]}
  [opts]
  (let [{:keys [change-ch] :as input-state}
        (create-input-state opts)]
    (r/create-class
      (->
        {:reagent-render
         (fn [{:keys [style label label-style value] :as opts}]
           (let [{:keys [!val] :as opts}
                 (-> opts
                     resolve-!val
                     (merge
                       {:change-ch change-ch}))

                 {:keys [:fg-color]}
                 (th/resolve opts
                   [[:fg-color :color/fg-0]])]
             [:label
              {:style (merge
                        {:display 'flex
                         :flex-direction 'row
                         :align-items 'center
                         :cursor 'pointer
                         :user-select 'none
                         :margin-bottom 0
                         :color fg-color}
                        #_{:cursor 'pointer
                           :user-select 'none
                           :color fg-color}
                        label-style)}
              [:input
               (merge
                 (select-keys
                   opts
                   [:min :max])
                 {:type "checkbox"
                  :style (merge
                           {:border "solid #ccc 1px"
                            :border-raidus 0
                            :width 16
                            :height 16
                            :outline 0
                            :margin-right 6
                            :user-select 'none}
                           style)
                  :checked (or value (when !val @!val))
                  :on-change (fn [e]
                               (let [v (.. e -target -checked)]
                                 (handle-change-value opts input-state v e)))})]
              label]))}
        bind-form-context))))

(defn handle-change-select-value
  [{:keys [!val
           on-change-value
           form-state
           data-key
           change-ch
           validate-on-change?
           options]
    :as opts}
   {:keys [change-ch]}
   input-value
   e]
  (let [selected-option (->> options
                             (some
                               (fn [{:keys [value] :as o}]
                                 (when (= (pr-str value)
                                          input-value)
                                   o))))
        value (:value selected-option)]
                   
    (when !val (reset! !val value))

    (when on-change-value
      (on-change-value value))

    (clear-error form-state data-key)

    (when validate-on-change?
      (put! change-ch :change))))

(defn select
  {:opts [[:data-key]
          [:form-state]
          [:!val]
          [:style]
          [:options]]}
  [opts]
  (let [{:keys [change-ch] :as input-state}
        (create-input-state opts)]
    (r/create-class
      (-> {:reagent-render
           (fn [opts]
             (let [{:keys [!val value style options]
                    :as opts}
                   (resolve-!val opts)]
               (into
                 [:select
                  (merge
                    (merge
                      {:style style
                       :default-value (or value
                                          (when !val (or @!val 0))
                                          0)
                       :on-change (fn [e]
                                    (let [v (.. e -target -value)]
                                      (handle-change-select-value opts input-state v e)))}
                      (when !val
                        {:value (or (pr-str @!val) 0)})
                      (when value
                        {:value value})))]
                 (->> options
                      (map (fn [{:keys [value label disabled?]}]
                             [:option
                              (merge
                                {:value (pr-str value)}
                                (when disabled?
                                  {:disabled "disabled"}))
                              label]))))))}
          bind-form-context))))

(defn filter-select [opts]
  (let [!text (r/atom nil)
        !ti-ref (atom nil)
        !selected-index (r/atom 0)
        kb-id (str "filter-select-" (ks/uuid))
        input-state (initialize-validation-loop opts)]
    (r/create-class
      (-> {:reagent-render
           (fn [opts]
             (let [options (:options opts)
                   bindings [{:key-name "Escape"
                              :handler (fn [e]
                                         (.stopPropagation e)
                                         (.blur @!ti-ref))}
                             {:key-name "ArrowDown"
                              :handler (fn [e]
                                         (.stopPropagation e)
                                         (swap! !selected-index
                                           (fn [i]
                                             (min
                                               (inc i)
                                               (dec (count options))))))}

                             {:key-name "ArrowUp"
                              :handler (fn [e]
                                         (.stopPropagation e)
                                         (swap! !selected-index
                                           (fn [i]
                                             (max
                                               (dec i)
                                               0))))}
                             {:key-name "Enter"
                              :handler (fn [e]
                                         (gol
                                           (.stopPropagation e)
                                           (.blur @!ti-ref)
                                           (<! (browser/<raf))
                                           (reset! !text
                                             (try
                                               (->> options
                                                    (filter (fn [{:keys [label]}]
                                                              (if (and label @!text)
                                                                (str/starts-with?
                                                                  (str/lower-case label)
                                                                  (str/lower-case @!text))
                                                                true)))
                                                    (nth @!selected-index)
                                                    :value)
                                               (catch js/Error e
                                                 nil)))
                                           (handle-change-value
                                             opts
                                             input-state
                                             (:value (nth options @!selected-index))
                                             e)))}]]
               [pop/wrap
                {:render-body
                 (fn [pop]
                   [text
                    (merge
                      opts
                      {:ref #(when % (reset! !ti-ref %))
                       :value @!text
                       :on-change-text
                       (fn [t]
                         (reset! !text t))
                       :on-focus (fn []
                                   (pop/show pop)
                                   (kb/add-bindings!
                                     kb-id
                                     bindings))
                       :on-blur (fn []
                                  (pop/hide pop)
                                  (kb/remove-bindings! kb-id))})])
                 :popper [ui/g
                          {:style {:margin-top 4}}
                          (->> opts
                               :options
                               (filter (fn [{:keys [label]}]
                                         (if (and label @!text)
                                           (str/starts-with?
                                             (str/lower-case label)
                                             (str/lower-case @!text))
                                           true)))
                               (map-indexed
                                 (fn [i {:keys [label value]}]
                                   (let [selected? (= i @!selected-index)]
                                     [ui/g
                                      {:css [(merge
                                               {:cursor 'pointer}
                                               (when selected?
                                                 {:background-color "#ddd"}))
                                             ["&:hover" {:background-color "#eee"}]]
                                       :on-mouse-down (fn []
                                                        (reset! !text label))}
                                      label])))
                               doall)]
                 :position :bottom-left
                 :body-width? true}]))}
          bind-form-context))))

(defn with-form-state [_]
  (r/create-class
    (->
      {:reagent-render
       (fn [& args]
         (let [[opts render] (ks/ensure-opts args)]
           [render
            (merge opts
              (:form-context opts))
            (-> opts :form-context :form-state)]))
       :context-type form-context}
      (apply-react-context :form-context))))

(defn submit-errors [form-state]
  (when (submit-failed? form-state)
    (errors form-state)))

(defn row
  [& args]
  (let [[{:keys [gap style horizontal? class] :as opts} & children]
        (ks/ensure-opts args)
        
        children (process-form-children opts children)

        {:keys [title]} opts

        children (concat
                   (when title
                     [[ui/text
                       {:scale "title"}
                       title]])
                   children)

        children (->> children
                      (map
                        (fn [child]
                          (browser/update-opts
                            child
                            (fn [child-opts]
                              (ks/deep-merge
                                (select-keys opts [:data-key])
                                child-opts))))))

        children (concat
                   (->> children
                        butlast
                        (map
                          (fn [child]
                            (browser/update-opts
                              child
                              (fn [child-opts]
                                (merge-with ks/deep-merge
                                  {:style
                                   (if horizontal?
                                     {:margin-right gap}
                                     {:margin-bottom gap})}
                                  child-opts))))))
                   [(last children)])]
    (into
      [:div.rx-browser-forms-group
       {:class class
        :style style}]
      children)))

(comment

  (let [form-state (forms/create-state)]
    [forms/form
     {:state form-state
      :on-submit (fn [data e]
                   (let [errors (validate-the-thing data)]
                     (if (empty? errors)
                       "SUBMIT!"

                       ;; errors: {:email "There was a problem with your email"
                       ;;          :email-sectoon "Please fix 1 error here"}
                       (forms/set-errors errors))))}
     [forms/section
      {:title "Let's get some data!"
       :error-key :email-section
       :theme {:theme "data here"}}
      [forms/input-row
       {:label "Email"
        :data-key :email
        :on-blur (fn []
                   (let [valid? (email-valid? (:email (forms/data form-state)))]
                     (when-not valid?
                       (forms/update-errors
                         {:email "Invalid!!!!!"}))))}]]])


  


  )



