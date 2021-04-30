(ns rx.browser.logview
  (:require [rx.kitchen-sink :as ks]
            [rx.theme :as th]
            [rx.res :as res]
            [rx.browser.components :as cmp]
            [rx.box :as box]
            [reagent.core :as r]
            #_[rx.log-ingress :as ingress]
            [rx.log-client :as lc]
            [rx.trace :as trace]
            [rx.browser.fast-list :as fl]
            [dommy.core :as dommy]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [cljs.core.async
             :as async
             :refer [put! close! chan sliding-buffer
                     timeout]
             :refer-macros [go]]))

(defn dom-scroll-at-bottom? [el]
  (= (+ (.-scrollTop el) (.-clientHeight el))
     (.-scrollHeight el) ))

(defn handle-user-scroll [el !stuck-to-bottom?]
  (reset! !stuck-to-bottom?
    (dom-scroll-at-bottom? el)))

(defn le-matches? [le filter-text]
  (or (not filter-text)
      (= :empty filter-text)
      (str/includes? (:rx.log/search-value le) filter-text)))

(defn add-basic-info [le]
  (-> le
      (assoc
        :rx.log/event-id
        (or (:rx.log/event-id le) (ks/uuid)))
      (assoc
        :rx.log/ingress-ts
        (or (:rx.log/ingress-ts le)
            (ks/now)))))

(defn populate-search-value [le]
  (let [{:keys [:rx.log/search-value
                :rx.log/body-str
                :rx.log/body-edn]} le]
    (assoc le :rx.log/search-value
      (or search-value
          body-str
          body-edn))))

(defn read-edn [edn-str]
  (try
    (ks/edn-read-string
      {:default
       (fn [tag value]
         (str "#" tag "" value))}
      edn-str)
    (catch js/Error e
      (prn "ERR" e)
      nil)))

(defn decode-body-edn [le]
  (if-let [body-edn (:rx.log/body-edn le)]
    (-> le
        (assoc
          :rx.log/body-clj
          (read-edn body-edn)))
    le))

(defn process-ingress-log-event [le]
  (-> le
      add-basic-info
      populate-search-value
      decode-body-edn))

(defn collapse-duplicates [les]
  (let [same? (fn [le0 le1]
                (= (if (:rx.log/body-str le0)
                     (:rx.log/body-str le0)
                     (:rx.log/body-edn le0))
                   (if (:rx.log/body-str le1)
                     (:rx.log/body-str le1)
                     (:rx.log/body-edn le1))))]

    (loop [les les
           out []]
      (if (empty? les)
        out
        (let [le (first les)
              match? (same? le (last out))]
          (recur
            (rest les)
            (if match?
              (vec
                (concat
                  (butlast out)
                  [(assoc le :rx.log/duplicate-count (inc (or (:rx.log/duplicate-count (last out)) 0)))]))
              (conj out le))))))))

(defn scroll-height [{:keys [log-event-count]}]
  (* log-event-count 500))

(declare render-clj)

(defn render-map [o opts]
  [:div
   {:style {:display 'flex
            :flex-direction 'row}}
   [:div "{"]
   [:div
    {:style {:flex 1
             :overflow 'hidden}}
    (->> o
         (map-indexed
           (fn [i [k v]]
             (let [{:keys [render collapsed?]} (get opts k)]
               [:div
                {:key i
                 :style {:display 'flex
                         :flex-direction 'row}}
                [:div
                 {:style {:white-space 'nowrap}}
                 (str k)]
                [:div
                 {:style {:width 5
                          :flex-shrink 0}}
                 " "]

                (if collapsed?
                  "..."
                  (if render
                    [render v opts]
                    [render-clj v opts]))
                
                (when (= i (max 0 (dec (count o))))
                  [:div
                   {:style {:display 'flex
                            :align-items 'flex-end}}
                   "}"])]))))]])

(defn render-string [o opts]
  [:div
   {:style {:display 'flex
            :flex-direction 'row
            :overflow 'hidden}}
   [:div
    "\""]
   [:div
    {:style {:white-space 'nowrap
             :overflow 'hidden
             :text-overflow 'ellipsis}}
    [:span
     {:style {:flex 1}}
     o
     "\""]]])

(defn render-vector [o opts]
  [:div
   {:style
    {:display 'flex
     :flex-direction 'row
     :overflow 'hidden}}
   [:div "["]
   [:div
    {:style {:display 'flex
             :flex-direction 'column
             :overflow 'hidden}}
    (->> o
         (map-indexed
           (fn [i v]
             [:div
              {:key i
               :style {:overflow 'hidden
                       :display 'flex
                       :flex-direction 'row}}
              [render-clj v opts]
              (when (= i (max 0 (dec (count o))))
                [:div
                 {:style {:display 'flex
                          :align-items 'flex-end}}
                 "]"])])))

    (when (= 0 (count o))
      [:div
       {:style {:display 'flex
                :align-items 'flex-end}}
       "]"])]])

(defn render-list [o opts]
  [:div
   {:style
    {:display 'flex
     :flex-direction 'row
     :overflow 'hidden}}
   [:div "("]
   [:div
    {:style {:display 'flex
             :flex-direction 'column
             :overflow 'hidden}}
    (->> o
         (map-indexed
           (fn [i v]
             [:div
              {:key i
               :style {:overflow 'hidden
                       :display 'flex
                       :flex-direction 'row}}
              [render-clj v opts]
              (when (= i (max 0 (dec (count o))))
                [:div
                 {:style {:display 'flex
                          :align-items 'flex-end}}
                 "]"])])))

    (when (= 0 (count o))
      [:div
       {:style {:display 'flex
                :align-items 'flex-end}}
       ")"])]])

(defn render-trace-var [{:keys [ns name] :as var} opts]
  [:div
   [:a {:href "#"
        :on-click (fn [e]
                    (trace/call
                      {:rx.trace/var #'render-trace-var
                       :rx.trace/loc :on-click
                       :rx.trace/args [var opts]}))}
    (if (or ns name)
      (str ns "/" name)
      (pr-str var))]])

(defn render-pl-sql [_ _]
  "PL SQL")

(defn render-clj [o opts]
  (let [opts (merge
               opts
               {:rx.trace/var {:render render-trace-var}
                :box.pl/SQL {:collapsed? true}
                :box.pl/AWS {:collapsed? true}
                :box/schema {:collapsed? true}})]
    (cond
      (map? o) [render-map o opts]
      (string? o) [render-string o opts]
      (vector? o) [render-vector o opts]
      (list? o) [render-list o opts]
      :else [:div (pr-str o)])))

(defn render-body-clj [{:keys [body-clj] :as opts}]
  (let [{:keys [::fg-color
                ::bg-color
                ::font]}
        (th/des opts
          [[::fg-color :color/fg-1]
           [::bg-color :color/bg-0]
           [::font :font/monospace]])]

    [:div
     {:style {:display 'flex
              :overflow 'hidden
              :font-family font}}
     [render-clj body-clj]]))

#_(defn log-event []
  (let [!expanded? (r/atom nil)]
    (fn [{:keys [log-event] :as opts}]
      (let [{:keys [:rx.log/body-str
                    :rx.log/body-edn
                    :rx.log/body-clj
                    :rx.log/ingress-ts
                    :rx.log/duplicate-count]} log-event

            {:keys [::secondary-fg-color
                    ::accent-color
                    ::font-size
                    ::font-family]}
            (th/des opts
              [[::secondary-fg-color :color/fg-3]
               [::accent-color :color/bg-2]
               [::font-family :font/copy]
               [::font-size 14]])]

        [:div
         {:style {:padding 10
                  :position 'relative
                  :font-size font-size
                  :display 'flex
                  :flex-direction 'row
                  :overflow 'hidden}}
         [:div
          {:style {:margin-right 5
                   :font-family "monospace"
                   :font-size font-size}}
          (when (and duplicate-count
                     (> duplicate-count 1))
            (str "(" duplicate-count ") "))]

         [:div
          {:style {:display 'flex
                   :flex-direction 'column
                   :overflow 'hidden}}
          [:div
           {:style (merge
                     {:display 'flex
                      :flex-direction 'column
                      :overflow 'hidden
                      :max-height 100}
                     (when @!expanded?
                       {:max-height 'none}))}
           (if body-clj
             [render-body-clj {:body-clj body-clj}]
             [:pre
              {:style {:white-space 'pre-wrap
                       :margin 0
                       :max-height 100
                       :overflow 'hidden}}
              (or body-str
                  body-edn)])]

          [:div
           {:style {:font-size 10
                    :color secondary-fg-color}}
           (ks/date-format
             ingress-ts
             "hh:mm:ss aa -- MM/dd/yy")
           " -- "
           (ks/ms-delta-desc-short (- (ks/now) ingress-ts))
           " -- "
           [cmp/$button
            {:title (if @!expanded?
                      "collapse"
                      "expand")
             :style {:display 'inline}
             :on-action
             (fn []
               (swap! !expanded? not))}]
           " -- "
           [cmp/$button
            {:title "Print"
             :style {:display 'inline}
             :on-action (fn []
                          (prn body-edn))}]]

          [:div
           {:style
            {:position 'absolute
             :top 0
             :left 0
             :width 3
             :bottom 0
             :background-color accent-color}}]]]))))

(def !colors (atom
               (cycle
                 ["red" "green" "blue" "white" "yellow"])))

(defn debounce [in ms]
  (let [out (chan)]
    (go
      (loop [last-val nil]
        (let [val   (if (nil? last-val) (<! in) last-val)
              timer (timeout ms)
              [new-val ch] (alts! [in timer])]
          (condp = ch
            timer (do (when-not
                          (>! out val)
                          (close! in))
                      (recur nil))
            in (if new-val (recur new-val))))))
    out))

(defn filter-text [text-input-ch
                   <handle-text]
  (let [users-ch-ch (chan (sliding-buffer 1))
        users-ch (chan)
        trace-ctx trace/*ctx*]
    (go
      (let [ch (debounce text-input-ch 100)]
        (loop []
          (let [text (<! ch)]
            (go
              (binding [trace/*ctx* trace-ctx]
                (let [ch (chan)]
                  (>! users-ch-ch ch)
                  (>! ch (<! (<handle-text text))))))
            (recur)))))

    (go
      (loop []
        (let [ch (<! users-ch-ch)
              users (<! ch)]
          (>! users-ch users)
          (recur))))
    
    users-ch))

(defn handle-log-events [conn !filter fl-control
                         !offset-ts les]
  (let [{:keys [filter-text]} @!filter

        with-ids (->> les
                      (map process-ingress-log-event))
        
        matching-les (->> with-ids
                          (filter #(le-matches? % filter-text)))]

    (fl/append-items fl-control matching-les)

    (when-let [ts (:rx.log/created-ts (last matching-les))]
      (reset! !offset-ts ts))

    (box/<transact
      conn
      with-ids)))

(defn <handle-text [conn text]
  #_(let [trace-ctx trace/*ctx*]
      (go
        (let [res (<! (box/<query
                        conn
                        (trace/value
                          {:honeysql
                           {:where (vec
                                     (concat
                                       [:and
                                        [:= :ident_key (box/col :rx.log/event-id)]]
                                       (when-not (= :empty text)
                                         [[:like
                                           (box/col :rx.log/search-value)
                                           (str "%" text "%")]])))
                            :limit 30
                            :order-by [[(box/col :rx.log/ingress-ts) :desc]]}}
                          (merge
                            {:rx.trace/noop "hi"}
                            trace-ctx
                            {:rx.trace/var #'<handle-text}))))]
          res))))

(defn start-filter-text-processor [conn
                                   filter-text-ch
                                   !filter
                                   fl-control]

  (lc/log
    {:group "rx.browser.logview"
     :event "start-filter-text-processor"}
    {:rx.trace/var #'start-filter-text-processor
     :rx.trace/args [conn filter-text-ch !filter fl-control]})
  
  (go
    (let [ch (filter-text filter-text-ch (partial <handle-text conn))]
      (loop []
        (let [res (<! ch)]
          (when (res/data res)
            (fl/append-items
              fl-control
              (->> res
                   res/data
                   (sort-by :rx.log/created-ts)
                   collapse-duplicates)))
          (recur))))))

(defn time-line []
  (let [!run? (atom true)
        !count (r/atom 0)]
    (r/create-class
      {:component-did-mount
       (fn []
         (go
           (loop []
             (<! (timeout 1000))
             (swap! !count inc)
             (when @!run?
               (recur)))))
       :component-will-unmount
       (fn []
         (reset! !run? false))
       :reagent-render
       (fn [{:keys [created-ts offset-ts last-item?]}]
         [:div
          {:style {:white-space 'nowrap
                   :flex-shrink 0}}
          (when @!count nil)
          (when created-ts
            (ks/date-format
              created-ts
              "hh:mm:ss.SSS aa"))
          (when created-ts
            " / ")
          (when created-ts
            (if last-item?
              (let [delta (- (ks/now) created-ts)]
                (if (< delta (* 1000 60))
                  (str (ks/round (/ delta 1000)) "s")
                  (ks/ms-delta-desc-short delta)))
              (let [delta (- offset-ts created-ts)]
                (str "+"
                     (if (< delta (* 1000 60))
                       (str (ks/round (/ delta 1000)) "s")
                       (ks/ms-delta-desc-short delta))))))])})))

(defn render-log-event [_ log-event]
  (let [{:keys [:rx.log/app-name
                :rx.log/event-name
                :rx.log/group-name
                :rx.log/body-edn]} log-event]
    (r/create-class
      {:reagent-render
       (fn 
         [{:keys [on-choose-log-event
                  offset-ts
                  last-item?
                  selected?] :as opts}
          log-event]

         (let [{:keys [::selected-bg-color]}
               (th/des opts
                 [[::selected-bg-color :color/bg-2]])

               {:keys [:rx.log/created-ts]} log-event]
           [:div
            {:on-click (fn [e]
                         (.preventDefault e)
                         (on-choose-log-event log-event)
                         nil)
             :style (merge
                      {:cursor 'pointer
                       :display 'flex
                       :flex-direction 'row
                       :justify-content 'space-between
                       :font-family "monospace"
                       :font-size 14
                       :padding (th/pad opts 1)}
                      (when selected?
                        {:background-color selected-bg-color}))}
            (if (or app-name event-name)
              [cmp/interpose-children
               {:separator [:div {:style {:width 10}}]
                :style {:display 'flex
                        :flex-direction 'row
                        :justify-content 'flex-start
                        :overflow 'hidden
                        :flex-wrap 'nowrap
                        :align-items 'center
                        :flex 1}}
               (->> [(when event-name
                       [:div
                        {:style {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis}}
                        event-name])
                     (when group-name
                       [:div
                        {:style {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis
                                 :opacity 0.5}}
                        group-name])
                     (when app-name
                       [:div
                        {:style {:white-space 'nowrap
                                 :overflow 'hidden
                                 :text-overflow 'ellipsis
                                 :opacity 0.5}}
                        app-name])
                     #_(when body-edn
                         [:div
                          {:style {:white-space 'nowrap
                                   :overflow 'hidden
                                   :text-overflow 'ellipsis
                                   :opacity 0.5
                                   :flex 1}}
                          body-edn])]
                    (remove nil?))]
              [:div
               {:style {:white-space 'nowrap
                        :overflow 'hidden
                        :text-overflow 'ellipsis}}
               (pr-str log-event)])
            

            [:div {:style {:width (th/pad opts 1)}}]
            [time-line {:created-ts created-ts
                        :offset-ts offset-ts
                        :last-item? last-item?}]]))})))

(defn log-stream-comp [{:keys [conn
                               skip-initial-load?
                               initial-log-events
                               initial-filter]
                        :as opts}]

  (lc/log
    {:event "log-stream-comp.call"
     :group "rx.browser.logview"}
    (select-keys opts [:skip-initial-load? :initial-log-events :initial-filter]))

  (let [!filter (r/atom nil)

        fl-control (fl/create-state
                     {:initial-items initial-log-events
                      :id-key :rx.log/event-id
                      :sort-fn #(or (:rx.log/created-ts %) 0)})

        !offset-ts (r/atom (ks/now))
        
        on-log-event (fn [le]
                       (handle-log-events conn !filter
                         fl-control
                         !offset-ts
                         [le]))

        filter-text-ch (chan)

        on-change-text (fn [text]
                         (put! filter-text-ch (or text :empty)))


        !selected-log-event (r/atom nil)

        on-choose-log-event (fn [le]
                              (reset!
                                !selected-log-event
                                le))

        copy-selected-body (fn []
                             (cmp/copy-to-clipboard
                               (:rx.log/body-clj @!selected-log-event)))

        {:keys [::bg-color
                ::fg-color
                ::separator-color
                ::secondary-bg-color]}
        (th/des opts
          [[::bg-color :color/bg-0]
           [::fg-color :color/fg-1]
           [::separator-color :list/separator-color]
           [::secondary-bg-color :color/bg-1]])]

    (start-filter-text-processor conn filter-text-ch !filter fl-control)
    
    (when-not skip-initial-load?
      (on-change-text nil))
    
    {:render (fn []
               (r/create-class
                 {:component-did-mount
                  (fn []
                    #_(ingress/listen! on-log-event))
                  
                  :component-will-unmount
                  (fn []
                    #_(ingress/unlisten! on-log-event))

                  :reagent-render
                  (fn []
                    [:div
                     {:style {:flex 1
                              :display 'flex
                              :flex-direction 'column
                              :overflow 'hidden
                              :background-color bg-color
                              :color fg-color}}
                     [:div
                      {:style {:flex 1
                               :width "100%"
                               :display 'flex
                               :flex-direction 'column
                               :overflow 'hidden
                               :border-right (str
                                               "solid "
                                               separator-color
                                               " 1px")}}
                      [fl/view
                       {:control fl-control
                        :render-item
                        (fn [opts le]
                          [render-log-event
                           (merge
                             opts
                             {:selected? (= (:rx.log/event-id le)
                                            (:rx.log/event-id @!selected-log-event))
                              :offset-ts @!offset-ts
                              :on-choose-log-event on-choose-log-event})
                           le])}]

                      [:div
                       [cmp/searchbar
                        {:placeholder "Filter Log Events"
                         :on-change-text on-change-text}]]]


                     (let [body-edn (when @!selected-log-event
                                      (:rx.log/body-clj @!selected-log-event))]

                       [:div
                        {:style {:flex 1
                                 :width "100%"
                                 :display 'flex
                                 :flex-direction 'column
                                 :overflow-y 'scroll}}

                        [:div
                         {:style {:flex 1
                                  :padding (th/pad opts 1)}}
                         (if @!selected-log-event
                           [render-clj (or body-edn @!selected-log-event) {}]
                           [:div "No Event"])]

                        [:div
                         [cmp/list-separator opts]
                         [:div
                          {:style {:padding (th/pad opts 1)
                                   :background-color secondary-bg-color}}
                          [cmp/$button
                           {:title "Copy"
                            :style {:display 'inline}
                            :on-action copy-selected-body}]]]])])}))
     :rx.tabs/render
     (fn []
       [:div "Log Events"])}))


