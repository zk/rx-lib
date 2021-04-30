(ns rx.browser.fast-list
  (:require [rx.kitchen-sink :as ks]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [rx.theme :as th]
            [rx.browser :as browser]
            [dommy.core :as dommy]
            [clojure.set :as set]
            [cljs.core.async :as async
             :refer [chan <! >! timeout close! put! sliding-buffer alts!]
             :refer-macros [go]]))

(defn <rendered-height [width component]
  (let [ch (chan)
        el (.createElement js/document "div")]
    (dommy/set-style! el
      :width (str width "px")
      :position 'absolute
      :visibility 'hidden
      :top -10000
      :left -10000
      :transform "translate3d(-100%,-100%,0)")
    (.appendChild (.-body js/document) el)
    (rdom/render component el
      (fn []
        (when-let [height
                   (:height (dommy/bounding-client-rect el))]
          (rdom/unmount-component-at-node el)
          (.removeChild (.-body js/document) el)
          (put! ch height)
          (close! ch))))
    ch))

(defn item-ident [{:keys [id-key id-fn]} item]
  (cond
    id-key (get item id-key item)
    id-fn (id-fn item)
    :else item))

(defn item-sort-value [{:keys [sort-key sort-fn]} item]
  (cond
    sort-key (get item sort-key)
    sort-fn (sort-fn item)
    :else nil))

(defn update-visible-indexes [{:keys [!visible-indexes
                                      !tops
                                      !top->id
                                      !id->index
                                      !scroll-offset
                                      !scroller
                                      !container-layout]}]

  (let [{:keys [height]} @!container-layout]
    (when height
      (let [min-top (- @!scroll-offset height)
            max-top (+ @!scroll-offset height height)
            matching-tops (subseq
                            @!tops
                            >=
                            min-top
                            <
                            max-top)]

        #_(ks/spy
          (swap! !visible-indexes
            into
            (->> matching-tops
                 (map #(get @!top->id %))
                 (map #(get @!id->index %))
                 (remove nil?)
                 doall)))

        (reset! !visible-indexes
          (->> matching-tops
               (map #(get @!top->id %))
               (map #(get @!id->index %))
               (remove nil?)
               doall))))))

(defn total-height [{:keys [!item-count
                            !index->id
                            !id->layout]}]

  (let [index (max 0 (dec @!item-count))
        id (get @!index->id index)
        layout (get @!id->layout id)]
    (+ (:top layout) (:height layout))))

(defn ensure-valid-items [ctl items]
  (let [invalid-items (->> items
                           (map (fn [item]
                                  [(item-ident ctl item)
                                   (item-sort-value ctl item)
                                   item]))
                           (remove #(and (first %) (second %)))
                           (map last))]
    (when-not (empty? invalid-items)
      (throw
        (js/Error.
          (str "Invald items found, missing id or sort: "
               (pr-str invalid-items)))))))

(defn append-items [{:keys [!id->item
                            !id->layout
                            !index->id
                            !id->index
                            !index->height
                            !item-count
                            !tops
                            !top->id
                            !visible-indexes
                            !render-item
                            !scroller
                            !items-sorted

                            !ordered-ids

                            default-item-height

                            item-batch-ch

                            debug-log?]
                     :as ctl}
                    in-items]


  #_(ks/pp
      (->> in-items
           (map #(dissoc % :rx.log/body-str :rx.log/body-edn
                   :rx.log/body-clj
                   :rx.log/search-value))))


  (ensure-valid-items ctl in-items)
  
  (go
    (when (and in-items (not (empty? in-items)))
      (let [debug-log? false

            ;; dedupe
            items (->> in-items
                       (remove #(get @!id->item
                                  (item-ident ctl %)))
                       (sort-by #(item-sort-value ctl %)))

            _ (when debug-log?
                (println "Deduped:" (count items) "in:" (count in-items)))

            new-id->item (merge
                           @!id->item
                           (->> items
                                (map (fn [item]
                                       [(item-ident ctl item) item]))
                                (into {})))

            _ (when debug-log?
                (ks/spy
                  "new-id->item"
                  new-id->item))

            lowest-sort-val (item-sort-value ctl (first items))
            lowest-existing-item (when lowest-sort-val
                                   (->> (subseq
                                          @!items-sorted
                                          >
                                          lowest-sort-val)
                                        first))
            lowest-index (get @!id->index (:id lowest-existing-item))

            new-items-sorted (into
                               @!items-sorted
                               (->> items
                                    (map (fn [item]
                                           {:id (item-ident ctl item)
                                            :sort-val (item-sort-value ctl item)}))))

            _ (when debug-log?
                (ks/spy "new-items-sorted" new-items-sorted))

            start-index-to-recalc
            (or lowest-index 0)


            _ (when debug-log?
                (ks/spy "start-index-to-recalc" start-index-to-recalc))

            update-items-subseq (drop start-index-to-recalc new-items-sorted)

            _ (when debug-log?
                (ks/spy "update-items-subseq" update-items-subseq))
            
            item-count (count new-items-sorted)
            last-index start-index-to-recalc

            new-index->id (merge
                            @!index->id
                            (->> update-items-subseq
                                 (map #(get new-id->item (:id %)))
                                 (map-indexed
                                   (fn [i item]
                                     [(+ start-index-to-recalc i)
                                      (item-ident ctl item)]))
                                 (into {})))

            start-top-offset (or (let [layout
                                       (get @!id->layout
                                         (get new-index->id
                                           last-index))]
                                   (+ (:top layout)
                                      (:height layout)))
                                 0)

            new-id->index (set/map-invert new-index->id)

            _ (when debug-log?
                (ks/spy "start-top-offset" start-top-offset)
                (ks/spy "new-index->id" new-index->id)
                (ks/spy "new-id->item" new-id->item))

            new-id->layout (loop [index start-index-to-recalc
                                  id->layout @!id->layout
                                  next-top-offset start-top-offset]
                             (if (>= index (count new-items-sorted))
                               id->layout
                               (let [id (get new-index->id index)
                                     item (get new-id->item id)
                                     
                                     {:keys [height]}
                                     (get id->layout id)
                                     
                                     height (or height
                                                (<! (<rendered-height
                                                      (.-clientWidth @!scroller)
                                                      [@!render-item
                                                       {}
                                                       item])))

                                     new-layout {:top next-top-offset
                                                 :height height}]
                                 (recur
                                   (inc index)
                                   (merge
                                     id->layout
                                     {id new-layout})
                                   (+ next-top-offset height)))))

            _ (when debug-log?
                (ks/spy "new-id->layout" new-id->layout))

            new-tops (apply
                       conj
                       @!tops
                       (->> new-id->layout
                            (map (fn [[id layout]]
                                   (:top layout)))))

            new-top->id (->> new-id->layout
                             (map (fn [[id layout]]
                                    [(:top layout) id]))
                             (into {}))]


        

        (reset! !id->layout new-id->layout)
        (reset! !index->id new-index->id)
        (reset! !id->index new-id->index)
        (reset! !id->item new-id->item)
        (reset! !id->index new-id->index)
        (reset! !item-count item-count)
        (reset! !tops new-tops)
        (reset! !top->id new-top->id)
        (reset! !items-sorted new-items-sorted)

        (update-visible-indexes ctl)))))

(defn dom-scroll-at-bottom? [el]
  (= (+ (.-scrollTop el) (.-clientHeight el))
     (.-scrollHeight el) ))

(defn throttle*
  ([in msecs]
   (throttle* in msecs (chan)))
  ([in msecs out]
   (throttle* in msecs out (chan)))
  ([in msecs out control]
   (go
     (loop [state ::init last nil cs [in control]]
       (let [[_ _ sync] cs]
         (let [[v sc] (alts! cs)]
           (condp = sc
             in (condp = state
                  ::init (do (>! out v)
                             (>! out [::throttle v])
                             (recur ::throttling last
                               (conj cs (timeout msecs))))
                  ::throttling (do (>! out v)
                                   (recur state v cs)))
             sync (if last 
                    (do (>! out [::throttle last])
                        (recur state nil
                          (conj (pop cs) (timeout msecs))))
                    (recur ::init last (pop cs)))
             control (recur ::init nil
                       (if (= (count cs) 3)
                         (pop cs)
                         cs)))))))
   out))

(defn map-async [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
              (recur))
            (close! out))))
    out))

(defn filter-async [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (pred x) (>! out x))
              (recur))
            (close! out))))
    out))

(defn throttle
  ([in msecs] (throttle in msecs (chan)))
  ([in msecs out]
   (->> (throttle* in msecs out)
        (filter-async #(and (vector? %) (= (first %) ::throttle)))
        (map-async second))))

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

(defn scroll-chan [{:keys [!stuck-to-bottom?
                            !scroller
                            !scroll-offset]
                     :as ctl}]
  (let [in-ch (chan)
        e-ch (throttle in-ch 50)
        id (ks/uuid)]
    (go
      (loop []
        (let [e (<! e-ch)]

          (reset! !scroll-offset (.-scrollTop @!scroller))

          (reset! !stuck-to-bottom?
              (dom-scroll-at-bottom? @!scroller))

          (update-visible-indexes ctl)

          (recur))))
    in-ch))

(defn update-dimensions [{:keys [!id->layout
                                 !index->id
                                 !item-count
                                 default-item-height]}
                         id dims]

  (let [starting-index (get @!index->id id 0)
        upd-id->layout (loop [indexes (range starting-index @!item-count)
                              id->layout @!id->layout]
                         (if (empty? indexes)
                           id->layout
                           (let [index (first indexes)
                                 id (get @!index->id index)
                                 prev-id (get @!index->id (dec index))
                                 prev-layout (when prev-id
                                               (get id->layout prev-id))

                                 new-top (+ (get prev-layout :top 0)
                                            (get prev-layout :height
                                              (or default-item-height 0)))]

                             (recur
                               (rest indexes)
                               (assoc
                                 id->layout
                                 id
                                 (merge
                                   (get id->layout id)
                                   {:top new-top
                                    :height (:height dims)}))))))]
    (reset! !id->layout upd-id->layout)))

(defn comparable? [o]
  (or (string? o)
      (number? o)
      (symbol? o)
      (keyword? o)
      (nil? o)))

(defn create-state [& [{:keys [initial-items
                               id-key
                               default-item-height
                               id-fn
                               sort-key
                               sort-fn
                               render-item]
                        :as ctl}]]
  (let [item-batch-ch (chan 100)
        !item-count (atom nil)
        !scroller (atom nil)
        !scroll-offset (atom 0)
        !stuck-to-bottom? (atom true)
        !items-sorted (atom nil)
        !tops (atom (sorted-set))
        !top->id (atom nil)
        !index->id (r/atom nil)
        !id->index (r/atom nil)
        !id->item (r/atom nil)
        !id->layout (r/atom nil)
        !visible-indexes (r/atom #{})
        !container-layout (atom nil)
        !render-item (r/atom render-item)
        
        ctl {:!scroller !scroller
             :!scroll-offset !scroll-offset
             :id-key id-key
             :id-fn id-fn
             :sort-key sort-key
             :sort-fn sort-fn
             :!stuck-to-bottom? !stuck-to-bottom?
             :!item-count !item-count
             :!tops !tops
             :!top->id !top->id
             :!index->id !index->id
             :!id->index !id->index
             :!id->item !id->item
             :!id->layout !id->layout
             :!visible-indexes !visible-indexes
             :!container-layout !container-layout
             :!render-item !render-item
             :item-batch-ch item-batch-ch
             :!items-sorted !items-sorted}
        
        sorted-set-compare 
        (fn [x y]
          (let [c (compare
                    (:sort-val x)
                    (:sort-val y))]
            (if (not= c 0)
              c
              (let [x-id (:id x)
                    x-id (if (comparable? x-id)
                           x-id
                           (str x-id))
                    
                    y-id (:id y)
                    y-id (if (comparable? y-id)
                           y-id
                           (str y-id))]
                (compare x-id y-id)))))]

    (reset! !items-sorted
      (sorted-set-by sorted-set-compare))
    
    ctl))

(defn view [{:keys [initial-items render-item] :as opts}]

  (assert render-item "fast-list -- :render-item required")
  
  (let [ctl (or (:control opts) (create-state opts))

        {:keys [!id->item
                !index->id
                !id->layout
                !id->index
                !stuck-to-bottom?
                !scroller
                !visible-indexes
                !container-layout
                !render-item
                !item-count]} ctl
        
        on-ref (fn [ref]
                 (when ref
                   (reset! !scroller ref)
                   (reset! !container-layout
                     (dommy/bounding-client-rect ref))))

        scroll-ch (scroll-chan ctl)

        on-scroll (fn [e]
                    (put! scroll-ch e))
        
        on-resize (fn []
                    (reset! !container-layout
                      (dommy/bounding-client-rect
                        @!scroller))
                    (update-visible-indexes ctl))

        index->index+id (fn [index]
                          [index (get @!index->id index)])

        render-item-wrapper (fn [[index id]]
                              (let [item (get @!id->item id)
                                    layout (get @!id->layout id)
                                    index (get @!id->index id)
                                    last-item? 
                                    (or
                                      (= 0 @!item-count)
                                      (= (dec @!item-count)
                                         index))]
                                [:div
                                 {:key id
                                  :style {:position 'absolute
                                          :top (:top layout)
                                          :width "100%"}}
                                 [render-item
                                  {:last-item? last-item?
                                   :index index}
                                  item]]))]
    
    (r/create-class
      {:component-did-mount
       (fn []

         (append-items ctl initial-items)

         #_(when @!stuck-to-bottom?
             (set! (.-scrollTop @!scroller)
               (.-scrollHeight @!scroller)))

         (browser/listen! js/window :resize on-resize)

         (prn "mount"))

       :component-will-unmount
       (fn []
         (browser/unlisten! js/window :resize on-resize))
       
       :component-did-update
       (fn []
         #_(when @!stuck-to-bottom?
             (set! (.-scrollTop @!scroller)
               (.-scrollHeight @!scroller)))

         (prn "update"))
       
       :reagent-render
       (fn [{:keys [render-item style] :as opts}]
         (reset! !render-item render-item)
         [:div
          {:ref on-ref
           :style (merge
                    {:flex-direction 'column
                     :flex 1
                     :overflow 'scroll
                     :position 'relative}
                    style)
           :on-scroll on-scroll}
          [:div
           {:style {:height (total-height ctl)}}
           (->> @!visible-indexes
                (map index->index+id)
                (map render-item-wrapper)
                doall)]])})))


