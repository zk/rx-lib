(ns rx.browser
  (:require [clojure.string :as str]
            [rx.kitchen-sink :as ks]
            [rx.view :as view]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [dommy.core :as dommy]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [goog.object :as gobj]
            [bidi.bidi :as bidi]
            [clojure.string :as str]
            [cljs.core.async :as async
             :refer [close! chan put! timeout <!]
             :refer-macros [go]]))

(defn page-query-params []
  (let [s js/window.location.search
        s (when (and s (not (empty? s)))
            (apply str (drop 1 s)))]
    (when s
      (->> (str/split s "&")
           (map (fn [s]
                  (str/split s "=")))
           (into {})))))

(defn <raf []
  (let [ch (chan)]
    (.requestAnimationFrame js/window
      (fn []
        (close! ch)))
    ch))

(defn bind-error-boundary [reagent-map]
  (let [!err-state (r/atom nil)]
    (merge
      reagent-map
      {:component-did-catch
       (fn [this err info]
         (reset! !err-state
           {:err err
            :info info}))
       :reagent-render
       (fn [& args]
         (if @!err-state
           [:div
            {:style {:padding 30}}
            (let [err (:err @!err-state)]
              (when (and (ex-data err)
                         (anom/? (ex-data err)))
                [:div
                 [:h4 "Anom"]
                 [:pre (ks/pp-str (ex-data err))]]))
            [:h4 "Err"]
            [:pre (.-stack (:err @!err-state))]
            [:h4 "Info"]
            [:pre (str/trim (.-componentStack (:info @!err-state)))]]
           (into [(:reagent-render reagent-map)] args)))})))

(defn <show-component! [component-or-vec
                        & [{:keys [mount-el-selector
                                   mount-el
                                   preserve-scroll-top?]}]]
  (let [ch (chan)
        el (or mount-el
               (dommy/sel1
                 (or mount-el-selector :#rx-root-mount-point)))]
    
    (when-not el
      (ks/throw-str
        "Couldn't find root element by :root-el, :root-selector, or #rx-root-mount-point"))
    (go
      (try
        (let [scroll-top (.-scrollY js/window)]
          (rdom/unmount-component-at-node el)
          #_(<! (<raf))
          (if component-or-vec
            (rdom/render
              [(r/create-class
                 (bind-error-boundary
                   {:reagent-render
                    (if (fn? component-or-vec)
                      component-or-vec
                      (fn []
                        component-or-vec))}))]
              el
              (fn []
                (go
                  (close! ch)
                  (<! (<raf))
                  (.scrollTo js/window 0 scroll-top))))
            (close! ch)))
        (catch js/Error e
          (.error js/console e))))
    ch))

(defn <show-route! [route & [opts]]
  (<show-component! (view/render-route route) opts))

(defn <set-root!
  [route & [{:keys [root-el
                    root-selector
                    scroll-to-top?]
             :as opts}]]
  (go
    (let [res (<! (<show-route! route (merge
                                        {:mount-el-selector root-selector
                                         :mount-el root-el}
                                        opts)))]
      (<! (<raf))
      (when scroll-to-top?
        (.scrollTo js/window #js {:left 0 :top 0}))
      res)))

(defn location-href []
  (.. js/window -location -href))

(defn location-pathname []
  (.. js/window -location -pathname))

(defn history-push-state [url]
  (.pushState
    (.. js/window -history)
    nil "" url))

(defn ancestor? [an-el child-el]
  (loop [child-el child-el]
    (cond
      (= nil (.-parentNode child-el)) nil
      (= an-el (.-parentNode child-el)) true
      :else (recur (.-parentNode child-el)))))

(defn scroll-node? [node direction
                    {:keys [ignore-same-height?]}]
  (when node
    (let [direction (or direction :vertical)
          style (.getComputedStyle js/window node)
          overflow-direc (if (= :vertical direction)
                           (.getPropertyValue style "overflow-y")
                           (.getPropertyValue style "overflow-x"))
          overflow (or overflow-direc
                       (.getPropertyValue style "overflow"))]
      (and (or (= "auto" overflow)
               (= "scroll" overflow))
           (if ignore-same-height?
             true
             (< (.-offsetHeight node) (.-scrollHeight node)))))))

(defn find-scrollable-ancestor [node direction & [opts]]
  (loop [current-node node]
    (cond
      (= current-node (.-body js/document))
      js/window

      (scroll-node? current-node direction opts)
      current-node

      (and current-node
           (.-parentNode current-node))
      (recur (.-parentNode current-node))

      :else current-node)))

(defn scrollable-ancestor [node & [opts]]
  (find-scrollable-ancestor
    node
    (or (:direction opts) :vertical)
    (merge
      {:ignore-same-height? true}
      opts)))

(defn replace-state-hash [s]
  (.replaceState
    (.-history js/window)
    ""
    ""
    (str
      (if (str/includes? (location-href) "#")
        (->> (location-href)
             reverse
             (drop-while #(not= % \#))
             reverse
             (apply str))
        (str (location-href) "#"))
      s)))

(defn listen! [el kw-or-string f]
  (.addEventListener el (name kw-or-string) f))

(defn unlisten! [el kw-or-string f]
  (.removeEventListener el (name kw-or-string) f))

(defn children [node]
  (when node
    (let [children (gobj/get node "children")
          len (gobj/get children "length")]
      (->> (range len)
           (map (fn [i]
                  (aget children i)))))))

(defn child-nodes [node]
  (when node
    (let [child-nodes (gobj/get node "childNodes")
          len (gobj/get child-nodes "length")]
      (->> (range len)
           (map (fn [i]
                  (aget child-nodes i)))))))

(defn drag-handler
  [{:keys [on-click
           on-down
           on-up
           on-drag
           on-move]}]
  (let [!m (atom nil)]
    (letfn [(internal-drag [e]
              #_(.preventDefault e)
              #_(.stopPropagation e)
              (let [start (:start @!m)
                    last (:last @!m)
                    {:keys [x y]} start
                    {lx :x ly :y} last
                    up-x (.-clientX e)
                    up-y (.-clientY e)
                    dx (- up-x x)
                    dy (- up-y y)]
                (swap! !m assoc :last {:x up-x :y up-y})
                (when (and on-drag
                           (or (> (ks/abs dx) 2)
                               (> (ks/abs dy) 2)))
                  (on-drag
                    {:start-client-x x
                     :start-client-y y
                     :end-client-x up-x
                     :end-client-y up-y
                     :delta-x dx
                     :delta-y dy
                     :last-client-x lx
                     :last-client-y ly}
                    e)))
              nil)

            (internal-mm [e]
              #_(.preventDefault e)
              #_(.stopPropagation e)
              (let [x (.-clientX e)
                    y (.-clientY e)]
                (when on-move
                  (on-move
                    {:x x
                     :y y}
                    e)))
              nil)
            (internal-mu [e]
              #_(.preventDefault e)
              #_(.stopPropagation e)
              (let [start (:start @!m)
                    {:keys [x y]} start
                    up-x (.-clientX e)
                    up-y (.-clientY e)
                    dx (- up-x x)
                    dy (- up-y y)]
                (if (and (<= (ks/abs dx) 1)
                         (<= (ks/abs dy) 1))
                  (when on-click
                    (try
                      (on-click e)
                      (catch js/Error e
                        (.error js/console e)
                        nil)))
                  (when on-up
                    (on-up
                      {:sx x
                       :sy y
                       :ex up-x
                       :ey up-y
                       :dx dx
                       :dy dy}
                      e))))
              (unlisten!
                js/window
                :mouseup
                internal-mu)
              (unlisten!
                js/window
                :mousemove
                internal-drag)
              nil)
            (internal-md [e]
              #_(.preventDefault e)
              #_(.stopPropagation e)
              (let [client-x (.-clientX e)
                    client-y (.-clientY e)]
                (swap! !m assoc
                  :start {:x client-x
                          :y client-y}
                  :last {:x client-x
                         :y client-y})
                (listen!
                  js/window
                  :mouseup
                  internal-mu)
                (listen!
                  js/window
                  :mousemove
                  internal-drag)
                (when on-down
                  (on-down
                    {:client-x client-x
                     :client-y client-y}
                    e)))
              nil)]
      {:on-mouse-down internal-md
       :on-mouse-move internal-mm
       #_:on-click
       #_(fn [e]
           (prn "click")
           #_(.preventDefault e)
           #_(.stopPropagation e)
           nil)})))

(defn location-query-params []
  (let [qs (subs (.. js/window -location -search) 1)]
    (when (and qs (not (empty? qs)))
      (->> qs
           (#(str/split % "&"))
           (map (fn [s]
                  (let [[k v] (str/split s "=")]
                    [(keyword (ks/url-decode k))
                     (ks/url-decode v)])))
           (into {})))))

(defn location-root []
  (let [l (.. js/window -location)]
    (str
      (.-protocol l)
      "//"
      (.-host l))))

(defn location-hash []
  js/window.location.hash)

(defn location-hash-params []
  (let [qs (subs (location-hash) 1)]
    (when (and qs (not (empty? qs)))
      (->> qs
           (#(str/split % "&"))
           (map (fn [s]
                  (let [[k v] (str/split s "=")]
                    [(keyword (ks/url-decode k))
                     (ks/url-decode v)])))
           (into {})))))

(defn dev-host? []
  (let [l (.. js/window -location)
        h (.-hostname l)]
    (when h
      (get #{"localhost" "127.0.0.1"}
        (str/lower-case h)))))

(defn resolve-bidi-route
  [bidi-routes & [{:keys [path
                          query-params
                          debug?]}]]
  (let [path (or path (location-pathname))
        route (bidi/match-route bidi-routes path)]
    (when debug?
      (println "Checking routes:" (pr-str bidi-routes))
      (println "Using path:" (pr-str path)))
    (when route
      (let [{:keys [handler route-params]} route
            query-params (or query-params
                             (location-query-params))]
        (merge
          route
          {:query-params query-params})))))

(defn dispatch-current-location
  [{:keys [routes
           comps
           mount-selector
           comp-opts
           debug?]
    :as opts}]
  
  (assert mount-selector)
  (assert comps)
  (assert routes)
  
  (let [{:keys [handler route-params query-params]
         :as bidi-route}
        (resolve-bidi-route routes opts)
        comp-fn (get comps handler)]

    (when debug?
      (println "Resolved route:" (pr-str bidi-route))
      (println "Comp fn:" (pr-str comp-fn)))
    (when comp-fn
      (let [el (dommy/sel1 mount-selector)]
        (when debug?
          (println "Mount point:" (pr-str el)))
        (rdom/render
          [comp-fn (merge
                     comp-opts
                     route-params
                     query-params)]
          el)))))

(defn dispatch-location [{:keys [routes handlers handler-opts
                                 debug?]
                          :as opts}]
  (assert handlers)
  (assert routes)
  
  (let [{:keys [handler route-params query-params]
         :as bidi-route}
        (resolve-bidi-route routes opts)
        handler-fn (get handlers handler)]

    (when debug?
      (println "Resolved route:" (pr-str bidi-route))
      (println "Handler fn:" (pr-str handler-fn)))
    (when handler-fn
      (handler-fn
        (merge
          handler-opts
          route-params
          query-params)))))

(defn cdu-diff [f]
  (fn [this [_ & old-args]]
    (let [new-args (rest (r/argv this))]
      (f old-args new-args this))))

(defn interpose-children [& args]
  (let [[{:keys [separator] :as opts} children]
        (ks/ensure-opts args)]
    (into
      [:div
       (dissoc opts :separator)]
      (->> children
           (remove nil?)
           (interpose separator)))))

(defn dom-rect->clj [dom-rect]
  (let [r dom-rect]
    {:x (.-x r)
     :y (.-y r)
     :width (.-width r)
     :height (.-height r)
     :top (.-top r)
     :bottom (.-bottom r)
     :left (.-left r)
     :right (.-right r)}))

(defn bounding-client-rect [el]
  (when el
    (dom-rect->clj (.getBoundingClientRect el))))

(defn add-ancestor-scroll-listener [node on-scroll & [{:keys [direction]}]]
  (let [ancestor (gobj/get
                   node
                   "__rx-scrollable-ancestor"
                   (let [anc (find-scrollable-ancestor node (or direction :vertical))]
                     (gobj/set node "__rx-scrollable-ancestor" anc)
                     anc))]
    (.addEventListener
      ancestor
      "scroll"
      on-scroll)))

(defn remove-ancestor-scroll-listener [node on-scroll]
  (let [ancestor (gobj/get
                   node
                   "__rx-scrollable-ancestor")]
    (when ancestor
      (.removeEventListener
        ancestor
        "scroll"
        on-scroll))))

(defn bind-ancestor-scroll [component-map & [opts]]
  (let [{:keys [direction]} opts
        
        {:keys [component-did-mount
                component-will-unmount]}
        component-map
        
        !ancestor (atom nil)

        on-scroll (:on-ancestor-scroll component-map)]
    (when-not on-scroll
      (println "Warning, no :on-ancestor-scroll defined"))
    (merge
      component-map
      {:component-did-mount
       (fn [this]
         (add-ancestor-scroll-listener
           (rdom/dom-node this)
           on-scroll)
         (when component-did-mount
           (component-did-mount this)))
       :component-will-unmount
       (fn [this]
         (remove-ancestor-scroll-listener
           (rdom/dom-node this)
           on-scroll)
         (when component-will-unmount
           (component-will-unmount this)))})))

(defn bind-window-resize [{:keys [component-did-mount
                                  component-will-unmount
                                  component-did-update
                                  reagent-render
                                  on-window-resize]
                           :as reagent-class-map}]
  (let [!layout (r/atom
                  {::width (.-innerWidth js/window)
                   ::window-width (.-innerWidth js/window)
                   ::height (.-innerHeight js/window)
                   ::window-height (.-innerHeight js/window)})
        
        handle-resize
        (fn [e]
          (reset! !layout
            {::width (.-innerWidth js/window)
             ::window-width (.-innerWidth js/window)
             ::height (.-innerHeight js/window)
             ::window-height (.-innerHeight js/window)})
          (when on-window-resize
            (on-window-resize @!layout e)))]
    (merge
      reagent-class-map
      {:component-did-mount
       (fn [& args]

         (.addEventListener
           js/window
           "resize"
           handle-resize)
         
         (when component-did-mount
           (apply component-did-mount args)))
       :component-will-unmount
       (fn [& args]
         (.removeEventListener
           js/window
           "resize"
           handle-resize)
         (when component-will-unmount
           (apply component-will-unmount args)))

       #_:component-did-update
       #_(fn [this old-argv]
           (when component-did-update
             (let [[opts & xs] (ks/ensure-opts (rest old-argv))]
               (component-did-update
                 this
                 (into
                   [(first old-argv)
                    (merge
                      opts
                      @!layout)]
                   xs)))))

       :reagent-render
       (fn [& args]
         (when reagent-render
           (let [[opts & children] (ks/ensure-opts args)]
             (into
               [reagent-render
                (merge opts @!layout)]
               children)
             #_(apply
                 reagent-render
                 (merge opts @!layout)
                 children))))})))

(defn add-layout-listener [node on-layout & [opts]]
  (let [!on-layout->listeners
        (gobj/get node "__rx-on-layout->listeners"
          (let [!m (atom {})]
            (gobj/set node "__rx-on-layout->listeners" !m)
            !m))
        on-ancestor-scroll (fn []
                             (let [rect (bounding-client-rect node)]
                               (on-layout rect)))
        on-window-resize (fn []
                           (let [rect (bounding-client-rect node)]
                             (on-layout rect)))]
    (swap! !on-layout->listeners
      assoc
      on-layout
      {:on-ancestor-scroll on-ancestor-scroll
       :on-window-resize on-window-resize})
    (add-ancestor-scroll-listener
      node
      on-ancestor-scroll opts)
    (.addEventListener js/window "resize"
      on-window-resize)
    on-layout))

(defn remove-layout-listener [node on-layout]
  (let [!on-layout->listeners (gobj/get node "__rx-on-layout->listeners")
        {:keys [on-ancestor-scroll
                on-window-resize]}
        (get @!on-layout->listeners on-layout)]
    (remove-ancestor-scroll-listener
      node
      on-ancestor-scroll)
    (.removeEventListener js/window "resize"
      on-window-resize)))

(defn bind-layout [component-map & [opts]]
  (let [{:keys [component-did-mount
                component-did-update
                component-will-unmount
                on-ancestor-scroll
                on-window-resize
                on-layout]}
        component-map

        !layout (atom nil)
        !node (atom nil)

        update-layout (fn []
                        (let [rect (bounding-client-rect @!node)]
                          (when (not= rect @!layout)
                            (reset! !layout rect)
                            (on-layout @!layout))))]

    (-> (merge
          component-map
          {:component-did-mount
           (fn [this]
             (reset! !node (rdom/dom-node this))
             (update-layout)
             (when component-did-mount
               (component-did-mount this)))
           :component-did-update
           (fn [& args]
             (update-layout)
             (when component-did-update
               (apply component-did-update args)))
           :on-ancestor-scroll
           (fn [& args]
             (update-layout)
             (when on-ancestor-scroll
               (apply on-ancestor-scroll args)))
           :on-window-resize
           (fn [& args]
             (update-layout)
             (when on-window-resize
               (apply on-window-resize args)))})
        bind-window-resize
        (bind-ancestor-scroll opts))))

#_(defn bind-layout [component-map & [opts]]
  (let [{:keys [component-did-mount
                component-did-update
                component-will-unmount
                on-ancestor-scroll
                on-window-resize
                on-layout]}
        component-map

        !layout (atom nil)
        !node (atom nil)

        update-layout (fn []
                        (let [rect (bounding-client-rect @!node)]
                          (when (not= rect @!layout)
                            (reset! !layout rect)
                            (on-layout @!layout))))]

    (-> (merge
          component-map
          {:component-did-mount
           (fn [this]
             (reset! !node (rdom/dom-node this))
             (update-layout)
             (when component-did-mount
               (component-did-mount this)))
           :component-did-update
           (fn [& args]
             (update-layout)
             (when component-did-update
               (apply component-did-update args)))
           :on-ancestor-scroll
           (fn [& args]
             (update-layout)
             (when on-ancestor-scroll
               (apply on-ancestor-scroll args)))
           :on-window-resize
           (fn [& args]
             (update-layout)
             (when on-window-resize
               (apply on-window-resize args)))})
        bind-window-resize
        (bind-ancestor-scroll opts))))


(defn bind-lifecycle-callbacks [m opts]
  (merge
    m
    (->> [[:on-did-mount :component-did-mount]
          [:on-did-update :component-did-update]
          [:on-will-unmount :component-will-unmount]]
         (map (fn [[opts-key comp-key]]
                (when (get opts opts-key)
                  [comp-key
                   (fn [& args]
                     (apply (get opts opts-key) args)
                     (when (get m comp-key)
                       (apply (get m comp-key) args)))])))
         (remove nil?)
         (into {}))))

(defn bind-comp [m comp]
  (merge
    m
    {:reagent-render
     (fn [& args]
       (let [[opts & _] (ks/ensure-opts args)]
         (when-let [f (:on-comp opts)]
           (f comp)))
       (apply (:reagent-render m) args))}))

(defn update-opts [vec-component f]
  (if (vector? vec-component)
    (let [component-fn (first vec-component)
          opts (if (map? (second vec-component))
                 (second vec-component)
                 {})
          opts (f opts)
          children (if (map? (second vec-component))
                     (drop 2 vec-component)
                     (drop 1 vec-component))]
      (into
        [component-fn opts]
        children))
    vec-component))

(defn throttle [f delta]
  (let [last (atom nil)
        to (atom nil)]
    (fn [& args]
      (cond
        (not @last) (do
                      (reset! last (ks/now))
                      (reset! to nil)
                      (apply f args))
        (> @last 0) (let [now (ks/now)]
                      (if (> (- now @last) delta)
                        (do
                          (reset! last now)
                          (apply f args))
                        (do
                          (js/clearTimeout @to)
                          (reset! to
                            (js/setTimeout
                              (fn []
                                (reset! last (+ delta @last))
                                (apply f args))
                              (- delta (- now @last)))))))))))

(defn debounce [f delay]
  (let [last (atom nil)
        to (atom nil)]
    (fn [& args]
      (when @to
        (js/clearTimeout @to))
      (reset! to
        (js/setTimeout
          (fn []
            (reset! to nil)
            (apply f args))
          delay)))))

(defn scroll-top [el]
  (when el
    (if (= js/window el)
      (.-scrollY el)
      (.-scrollTop el))))

(defn scroll-left [el]
  (when el
    (if (= js/window el)
      (.-scrollX el)
      (.-scrollLeft el))))

(defn doc-sel []
  (.getSelection js/document))

(defn sel-bounding-rects []
  (let [sel (.getSelection js/document)]
    (when (> (.-rangeCount sel) 0)
      (->> (.getClientRects (.getRangeAt (.getSelection js/document) 0))
           (mapv dom-rect->clj)))))

(defn sel-collapsed? []
  (.-isCollapsed (.getSelection js/document)))

(defn sel-focus-node []
  (.-focusNode (doc-sel)))

(defn sel-anchor-node []
  (.-anchorNode (doc-sel)))

(defn sel-in-el? [el]
  (let [focus-node (sel-focus-node)
        anchor-node (sel-anchor-node)]
    (and focus-node
         anchor-node
         (ancestor? el focus-node)
         (ancestor? el anchor-node))))

(defn bind-drop-target [m]
  (let [!drag-over? (r/atom nil)
        !opts (atom nil)
        on-drag-over (fn [e]
                       ;; https://stackoverflow.com/questions/21339924/drop-event-not-firing-in-chrome
                       (.preventDefault e)
                       (reset! !drag-over? true))
        on-drag-leave (fn []
                        (reset! !drag-over? false))
        on-drop (fn [e]
                  (when-let [f (:on-drop m)]
                    (let [transit-str (.getData (.-dataTransfer e) "application/transit+json")]
                      (f @!opts (ks/from-transit transit-str)))))]
    (merge
      m
      {:component-did-mount
       (fn [this]
         (when-let [f (:component-did-mount m)]
           (f this))
         (let [node (rdom/dom-node this)]
           (.addEventListener node
             "dragover"
             on-drag-over)
           (.addEventListener node
             "dragleave"
             on-drag-leave)
           (.addEventListener node
             "drop"
             on-drop)))
       :component-will-unmount
       (fn [this]
         (when-let [f (:component-will-unmount m)]
           (f this))
         (let [node (rdom/dom-node this)]
           (.removeEventListener node
             "dragover"
             on-drag-over)
           (.removeEventListener node
             "dragleave"
             on-drag-leave)
           (.removeEventListener node
             "drop"
             on-drop)))
       :reagent-render
       (fn [opts]
         (reset! !opts opts)
         (when-let [f (:reagent-render m)]
           (f (merge
                opts
                {:dnd-drag-over? @!drag-over?}))))})))

(defn set-drag-data [e v]
  (.setData
    (.-dataTransfer e)
    "application/transit+json"
    (ks/to-transit v)))


;; Broken
#_(defn <scroll-to [el {:keys [top left] :as opts}]
  (let [ch (chan)
        fixed-top (if top
                    (max (long top) 0)
                    0)
        fixed-left (if left
                     (max (long left) 0)
                     0)]
    (letfn [(on-scroll []
              (let [scroll-top (if fixed-top
                                 (long (.-pageYOffset js/window))
                                 0)
                    scroll-left (if fixed-left
                                  (long (.-pageXOffset js/window))
                                  0)]
                (prn "F" top left fixed-top scroll-top fixed-left scroll-left)
                (when (and (< (ks/abs (- fixed-top scroll-top)) 2)
                           (< (ks/abs (- fixed-left scroll-left)) 2))
                  (.removeEventListener js/window "scroll" on-scroll)
                  (close! ch))))]
      (.addEventListener js/window "scroll" on-scroll)
      (on-scroll)
      (.scrollTo
        el
        (clj->js opts))
      ch)))

(defn file-obj->clj [o]
  {:file-obj o
   :last-modified (.-lastModified o)
   :name (.-name o)
   :size (.-size o)
   :type (.-type o)})

(defn <input-file-list
  "Warning: there is no indication when the user chooses to cancel the
  file selection dialog. The returned channel will never close."
  [& [opts]]
  (let [{:keys [multiple?
                accept]} opts
        el (.createElement js/document "input")
        ch (chan)]
    (set! (.-type el) "file")
    (when multiple?
      (set! (.-multiple el) "multiple"))
    (when accept
      (set! (.-accept el) accept))
    (set! (.-onchange el)
      (fn [e]
        (let [file-list-obj (.. e -target -files)
              files (->> (range (.-length file-list-obj))
                         (map (fn [i]
                                (.item file-list-obj i)))
                         (map file-obj->clj))]
          (put! ch files)
          (close! ch))))
    (.click el)
    ch))

(defn <file-as-data-url
  "Requires browser interaction from user (click on page) to work."
  [file]
  (let [ch (chan)
        reader (js/FileReader.)]
    (if file
      (do
        (.addEventListener reader
          "load"
          (fn [e]
            (put! ch (.-result reader))
            (close! ch)))

        (.addEventListener reader
          "abort"
          (fn [e]
            (put! ch (anom/anom {:desc "File read aborted"}))
            (close! ch)))
        
        (.addEventListener reader
          "error"
          (fn [e]
            (put! ch (anom/anom {:desc "File read error"}))
            (close! ch)))

        (.readAsDataURL reader (:file-obj file)))
      (close! ch))
    ch))

(defn remove-all-children [node]
  (set! (.-innerHTML node) ""))

(defn <ensure-script [src]
  (let [ch (chan)
        id (ks/md5 src)
        existing (.getElementById js/document id)]
    (if existing
      (close! ch)
      (let [script-el (.createElement js/document "script")]
        (set! (.-src script-el) src)
        (set! (.-id script-el) id)
        (.addEventListener script-el "load"
          (fn []
            (close! ch)))
        (.appendChild
          (.-body js/document)
          script-el)))
    ch))


(comment

  (defonce !file (atom nil))

  (selection-bounding-rects)

  (selection-collapsed?)

  (go
    (reset! !file (first (<! (<input-file-list {:multiple? true})))))

  (prn @!file)
    
  (let [el (.createElement js/document "input")]
    (set! (.-type el) "file")
    (set! (.-onchange el)
      (fn [e]
        (.log js/console (.. e -target -files))))
    (.click el))
  
  (bidi/match-route "/index.html")
 
  (dispatch-current-location
    {:path "/foo/123/bar"
     :query-params {:asdf "qewr"}
     :mount-selector :#rx-root-mount-point
     :routes ["/" {["foo/" :id "/bar"] :index}]
     :comps
     {:index (fn [opts]
               [:div "id2"
                (ks/pp-str opts)])}})

  (location-query-params)


  (.. js/window -location -search)

  (location-href)

  (location-pathname)

  

  )





