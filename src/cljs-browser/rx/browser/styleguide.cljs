(ns rx.browser.styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.theme :as th]
            [rx.browser :as browser]
            [rx.browser.modal :as modal]
            [rx.browser.popbar :as popbar]
            [rx.browser.popover :as po]
            [rx.browser.feather-icons :as feather]
            [rx.browser.buttons :as btn]
            [rx.browser.forms :as forms]
            [rx.browser.tabs :as tabs]
            [rx.browser.ui :as ui]
            [markdown-to-hiccup.core :as mth]
            [rx.css :as css]
            [rx.browser.css :as bcss]
            [dommy.core :as dommy]
            [clojure.walk :as walk]
            [reagent.core :as r]
            [clojure.string :as str]
            [cljs.core.async :as async
             :refer [<!]
             :refer-macros [go]]
            [cljfmt.core :as cljfmt]))

(defn md-to-reagent [s]
  (-> s
      mth/md->hiccup
      mth/component))

(defn key->level [k]
  (get
    {:h1 0 :h2 1 :h3 2
     :h4 3 :h5 4 :h6 5}
    k))

(defn heading-key->id [k]
  (when k
    (str (namespace k) "_" (name k))))

(defn normalize-heading [heading]
  (if (map? heading)
    [(:key heading)
     (condp = (:level heading)
       1 :h2
       2 :h3
       3 :h4
       4 :h5
       5 :h6
       :h1)
     (:title heading)]
    heading))

(defn heading->toc [heading]
  (let [[heading-key tag title] (normalize-heading heading)]
    {:level (key->level tag)
     :id (heading-key->id heading-key)
     :title title}))

(defn parse-toc [structure & [headings]]
  (if headings
    (map heading->toc headings)
    (let [!headers (atom [])]
      (walk/postwalk
        (fn [form]
          (if (and (vector? form)
                   (get #{:h1 :h2 :h3 :h4 :h5 :h6} (first form))
                   (and (:id (second form))))
            (swap! !headers conj form)
            form))
        structure)
      (->> @!headers
           (map (fn [[k & children] form]
                  (merge
                    {:level (key->level k)
                     :title (->> children
                                 (drop-while #(not (string? %)))
                                 (take-while string?)
                                 (apply str))}
                    (when-let [id (:id (first children))]
                      {:id id}))))))))

(defn toc [{:keys [toc
                   on-choose-id] :as opts}]
  (let [{:keys [::toc-horizontal-space]}
        (th/des opts
          [[::toc-horizontal-space (th/size-mult 5)]])]
    [:div
     {:style
      #_{:position 'fixed
         :transform "translate3d(-100%,0,0)"
         :padding-right toc-horizontal-space
         :font-size 14}
      {:font-size 14}}
     [:ul
      {:style {:list-style-type 'none
               :margin 0
               :padding 0}}
      (->> toc
           (map (fn [{:keys [level title id]}]
                  (let [title-el (if (= level 0)
                                   [:strong title]
                                   title)]
                    [:li
                     {:key (or id title)
                      :style {:margin-left (if (= level 0)
                                             0
                                             (* (dec level) 10))
                              :padding-bottom 5}}
                     (if id
                       [:a {:href (str "#" id)
                            :on-click
                            (fn [e]
                              (when on-choose-id
                                (.preventDefault e)
                                (on-choose-id id)
                                nil))
                            :style {:color (if (= level 0)
                                             "black"
                                             "#636363")}}
                        title-el]
                       title-el)]))))]]))

(defn standalone [{:keys [component headings] :as opts}]
  {:render
   (fn []
     (let [{:keys [::bg-color
                   ::fg-color]}
           (th/des opts
             [[::bg-color :color/bg-0]
              [::fg-color :color/fg-0]])]
       (dommy/set-style!
         (dommy/sel1 "body")
         :background-color bg-color)
       [:div
        {:style {:padding 20
                 :flex 1
                 :background-color bg-color
                 :color fg-color
                 :max-width 760
                 :margin-left 'auto
                 :margin-right 'auto}}
        
        [:div {:style {:position 'fixed
                       :transform "translate3d(-100%,0,0)"
                       :padding-right 30}}
         [toc (merge
                opts
                {:toc (parse-toc (component opts) headings)})]]
        [component opts]
        [:div {:style {:padding-bottom 200}}]
        [modal/global-component]]))})

(defn group [& args]
  (let [[opts & children] (ks/ensure-opts args)
        children (ks/unwrap-children children)
        {:keys [horizontal? gap pad style]} opts]
    (into
      [:div
       {:style (merge
                 {:display 'flex
                  :flex-direction 'column}
                 (when pad
                   {:padding pad})
                 (if horizontal?
                   {:flex-direction 'row})
                 (select-keys opts css/flex-keys)
                 style)}]
      (->> children
           (interpose
             [:div {:style (if horizontal?
                             {:width gap}
                             {:height gap})}])))))

(defn section [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    (into
      [group
       (merge
         {:gap 20}
         opts
         {:class (th/kw->class-name ::section)})]
      children)))

(defn section-image [{:keys [url] :as opts}]
  [:img
   (merge-with ks/deep-merge
     {:style {:display 'block
              :margin-left 'auto
              :margin-right 'auto}
      :src url}
     opts)])

(defn section-intro [{:keys [image] :as opts} & children]
  (r/create-class
    (-> {:reagent-render
         (fn [{:keys [::browser/window-width]}]
           [:div
            [section-image
             {:url (:url image)
              :style (merge
                       {:height (:height image)
                        :width (:width image)
                        :padding-bottom 20}
                       (when (> window-width 1120)
                         {:float 'right
                          :padding-right 20}))}]
            [section (ks/deep-merge opts {:style {:display 'block}}) children]])}
        browser/bind-window-resize)))

(defn section-callout [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [::section-callout-space]}
        (th/des opts
          [[::section-callout-space (th/size-mult 5)]])]
    (into
      [:div
       (merge-with ks/deep-merge
         {:style {:padding-left section-callout-space
                  :padding-bottom section-callout-space}})]
      children)))

(defn options-list [{:keys [options style]}]
  [ui/group
   {:gap 8}
   (->> options
        (map (fn [[k content]]
               [ui/group
                {:gap 4}
                [:div
                 [:code (pr-str k)]]
                [:div
                 (if (string? content)
                   (md-to-reagent content)
                   content)]])))])

(defn theme-rules-list [{:keys [theme-rules]}]
  [options-list
   {:options
    (->> theme-rules
         (map (fn [{:keys [doc rule] :as rule-entry}]
                (let [rule (if (map? rule-entry)
                             rule
                             rule-entry)]
                  [rule doc]))))}])

(defn code-block [& args]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [::code-block-space-left]}
        (th/des opts
          [[::code-block-space-left (th/size-mult 3)]])]
    (into
      [:pre
       (merge-with ks/deep-merge
         {:style {:padding-left code-block-space-left
                  :border-left "solid #eee 3px"}}
         opts)]
      children)))

(defn checkerboard [& args]
  (let [[opts & children] (ks/ensure-opts args)
        fg "#fafafa"
        bg "#f0f0f0"
        size 50]
    (into
      [:div
       {:style (merge
                 {:padding 8
                  :margin-bottom 20
                  :background-color bg
                  :background-image
                  (str "linear-gradient(45deg, " fg " 25%, transparent 25%), linear-gradient(-45deg, " fg " 25%, transparent 25%), linear-gradient(45deg, transparent 75%, " fg " 75%), linear-gradient(-45deg, transparent 75%, " fg " 75%)")
                  :background-size (str size "px " size "px")
                  :background-position (str "0 0, 0 " (/ size 2) "px, " (/ size 2) "px -" (/ size 2) "px, -" (/ size 2) "px 0px")}
                 (:style opts))}]
      children)))

(defn dots [& args]
  (let [[opts & children] (ks/ensure-opts args)
        size 4]
    (into
      [:div
       {:style (merge
                 {:padding 20
                  :background-image
                  "radial-gradient(#E4E0E4 1px, transparent 1px)"
                  :background-size (str size "px " size "px")
                  :background-position "0 0"}
                 (:style opts))}]
      children)))

(defn async-mount [<f]
  (let [!component (r/atom nil)]
    (go (reset! !component (<! (<f))))
    (r/create-class
      {:reagent-render
       (fn [opts]
         (if @!component
           @!component
           [:div "Loading..."]))})))

(defn sections [& args]
  (let [[opts & children] (ks/ensure-opts args)]
    [group
     {:gap 40}
     children]))

(def section-container sections)

(defn theme-doc-control [{:keys [doc-entry
                                 theme-map
                                 on-change-theme]}]
  (let [!val (r/atom (get theme-map (first (:rule doc-entry))))]
    (r/create-class
      {:reagent-render
       (fn [{:keys [doc-entry]}]
         (let [{:keys [doc type rule]} doc-entry
               rule-name-key (first rule)]
           [group
            [:div
             [:code (pr-str (first rule))]]
            [:div
             "Type: "
             (pr-str type)]
            [forms/text-input
             {:!val !val
              :on-change-text (fn [t]
                                (on-change-theme
                                  {rule-name-key @!val}))}]]))})))

(defn theme-controls-view
  [{:keys [theme-docs] :as opts}]
  {::popbar/stick-to :left
   :render (fn []
             [:div
              {:style {:flex 1
                       :background-color 'black
                       :color 'white
                       :padding 10
                       :width 300}}
              [group
               {:gap 12}
               [:div {:style {:text-align 'right}}
                [btn/bare
                 {:on-click (fn [] (popbar/<hide!))
                  :label [feather/x {:size 20}]
                  :theme {:fg-color "white"
                          :bg-color "black"}}]]
               [group
                (->> theme-docs
                     (map (fn [doc-entry]
                            [theme-doc-control
                             (merge
                               opts
                               {:doc-entry doc-entry})])))]]])})

(defn render-boolean-option [{:keys [option !val]}]
  (let [[k type] option]
    [ui/group
     [forms/label {:text (pr-str k)
                   :style {:display 'flex
                           :align-items 'center}}
      (when (= :boolean type)
        [:div {:style {:margin-left 22
                       :display 'inline-block}}
         [forms/checkbox
          {:!val !val}]])]]))

(defn render-string-option [{:keys [option !val]}]
  (let [[k type] option]
    [ui/group
     {:horizontal? true
      :gap 12
      :align-items 'center}
     [forms/label
      {:style {:width 75}
       :text (pr-str k)}]
     [forms/text
      {:!val !val}]]))

(defn render-form-option [{:keys [!val]}]
  (let [!text (r/atom (pr-str @!val))
        !error? (r/atom nil)
        on-change-text (fn [v]
                         (reset! !text (or v ""))
                         (let [form (try
                                      (let [res (if v
                                                  (ks/edn-read-string v)
                                                  nil)]
                                        (reset! !error? false)
                                        res)
                                      (catch js/Error e
                                        (reset! !error? true)
                                        nil))]
                           (reset! !val form)))]
    (r/create-class
      {:reagent-render
       (fn [{:keys [option !val]}]
         (let [[k type] option]
           [ui/group
            {:horizontal? true
             :gap 12
             :align-items 'center}
            [forms/label
             {:style {:width 75}
              :text (pr-str k)}]
            [forms/text
             {:value @!text
              :error? @!error?
              :style {:flex 1}
              :on-change-text on-change-text}]]))})))

(defn render-long-option [{:keys [!val]}]
  (let [!text (r/atom (pr-str @!val))
        !error? (r/atom nil)
        on-change-text (fn [v]
                         (reset! !text (or v ""))
                         (let [form (try
                                      (let [res (if v
                                                  (ks/edn-read-string v)
                                                  nil)]
                                        (reset! !error? false)
                                        res)
                                      (catch js/Error e
                                        (reset! !error? true)
                                        nil))]
                           (reset! !val form)))]
    (r/create-class
      {:reagent-render
       (fn [{:keys [option !val]}]
         (let [[k type] option]
           [ui/group
            {:horizontal? true
             :gap 12
             :align-items 'center}
            [forms/label
             {:style {:width 75}
              :text (pr-str k)}]
            [forms/text
             {:value @!text
              :error? @!error?
              :style {:flex 1}
              :on-change-text on-change-text}]]))})))

(defn render-example-option [{:keys [option !val] :as opts}]
  (condp = (second option)
    :boolean [render-boolean-option opts]
    :string [render-string-option opts]
    :form [render-form-option opts]
    :long [render-long-option opts]))

(defn example-container [{:keys [initial form-str render]}]
  (let [!opts (r/atom initial)
        !error? (r/atom nil)
        render-expander-label (fn [{:keys [open?]}]
                                [ui/group
                                 {:horizontal? true
                                  :pad 4
                                  :gap 2
                                  :style {:border-top "solid #ccc 1px"
                                          :border-bottom "solid #ccc 1px"}}
                                 (if open?
                                   [feather/chevron-down
                                    {:size 17}]
                                   [feather/chevron-right
                                    {:size 17}])
                                 [ui/text {:scale "subtext"} "Options"]])
        render-option (fn [option]
                        [render-example-option
                         {:key (first option)
                          :option option
                          :!val (r/cursor !opts [(first option)])}])]
    (r/create-class
      {:component-did-mount
       (fn []
         (add-watch !opts :change
           (fn []
             (reset! !error? nil))))
       :component-will-unmount
       (fn []
         (remove-watch !opts :change))
       :reagent-render
       (fn [{:keys [form-str options
                    initial-options-open?]}]
         [ui/group
          {:style {:position 'relative}}
          [tabs/view
           {:style-selected-tab {:border-bottom "solid #ccc 2px"}
            :style-tab-title {:color "#607d8b"}
            :style-selected-tab-title
            {:text-shadow "0 0 .2px #607d8b, 0 0 .2px #607d8b"}
            :initial-routes
            [[(fn []
                {:render
                 (fn []
                   (r/create-class
                     {:get-derived-state-from-error
                      (fn []
                        (reset! !error? true))
                      :reagent-render
                      (fn []
                        [:div {:style {:flex 1}}
                         [#_checkerboard
                          dots
                          (if @!error?
                            [:div "ERROR RENDERING"]
                            [render @!opts])]])}))
                 ::tabs/title "Example"})]
             [(fn []
                {:render
                 (fn []
                   [:div {:style {:flex 1
                                  :padding 20}}
                    [:pre form-str]])
                 ::tabs/title "Code"})]]}]
          (when-not (empty? options)
            [ui/group
             [ui/expander
              {:initial-open? initial-options-open?
               :render-label render-expander-label
               :content
               [ui/group
                {:hpad 24
                 :vpad 12
                 :gap 12
                 :style {:border-bottom "solid #ccc 1px"}}
                (->> options
                     (map render-option))]}]])])})))

(defn solid-container [child]
  [:div
   {:style {:padding 8
            :border "solid #eee 1px"
            :border-radius 8}}
   child])

(defn heading [headings k]
  (let [[k tag title] (->> headings
                           (map normalize-heading)
                           (filter #(= k (first %)))
                           first)]
    [tag {:id (heading-key->id k)} title]))

(defn render-signature [{:keys [sig sigs] :as opts}]
  (when (or sig sigs)
    (let [sigs (if sig
                 [sig]
                 sigs)]
      [ui/group
       (merge
         {:gap 8}
         opts)
       (->> sigs
            (map (fn [sig]
                   (into
                     [ui/text
                      {:style {:font-family 'monospace}}]
                     (->> (if (string? sig)
                            [sig]
                            sig))))))])))

(defn func [{:keys [name sig sigs desc] :as opts}]
  [ui/group
   (merge {:gap 8} opts)
   [ui/text {:scale "title"} name]
   (render-signature opts)
   (into
     [ui/text]
     (if (vector? desc)
       desc
       [desc]))])

(defn vardoc [{:keys [var] :as opts}]
  (let [{:keys [ns name arglists doc] :as m} (meta var)]
    [ui/group
     (merge {:gap 8}
       (dissoc opts :var))
     [ui/text {:scale "title"} name]
     [ui/group
      (->> arglists
           (map (fn [arglist]
                  [ui/text
                   {:style {:font-family 'monospace}}
                   (str "("
                        name
                        " "
                        (->> arglist
                             (interpose " ")
                             (apply str))
                        ")")])))]
     [:div
      (md-to-reagent doc)]]))

(defn vardocs [{:keys [vars] :as opts}]
  [ui/group
   (merge
     {:gap 16}
     (dissoc opts :vars))
   (->> vars
        (map (fn [var]
               [vardoc (merge {:var var} (dissoc opts :vars))])))])

(defn component-doc [{:keys [var] :as opts}
                     & [opts & children :as args]]
  (let [[opts & children] (ks/ensure-opts args)
        {:keys [ns name doc opts arglists]} (meta var)]
    (into
      [ui/group
       {:gap 20
        :css [[:p {:margin-bottom "8px"}]
              [:*:last-child {:margin-bottom 0}]
              [:.component-doc
               [:pre {:padding-left "10px"
                      :border-left "solid #eee 3px"}]]]}
       [ui/group
        {:gap 12}
        [ui/text {:scale "title"} name]
        [ui/group
         {:gap 8}
         (->> arglists
              (map (fn [arglist]
                     [ui/text {:style {:font-family 'monospace}}
                      (pr-str arglist)])))]
        [:div.component-doc
         (md-to-reagent doc)]]
       (when opts
         [ui/group
          {:gap 8}
          [:h5
           "Options"]
          [options-list
           {:options opts}]])]
      children)))

(defn components-section
  [{:keys [vars]}]
  [ui/group
   {:gap 20}
   [:h2 "Components"]
   [ui/group
    {:gap 32}
    (->> vars
         (map (fn [var]
                [component-doc {:var var}])))]])



(comment

  standalone

  (test-theme-stuff)

  )
