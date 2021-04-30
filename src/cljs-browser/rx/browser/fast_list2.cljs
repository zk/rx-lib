(ns rx.browser.fast-list2
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [clojure.core.async
             :as async
             :refer [go <! put! chan sliding-buffer]])
  (:refer-clojure :exclude [list]))

(defn create-state [opts]
  {:!opts (r/atom opts)
   :!ref (atom nil)
   :!scroll-el (atom nil)
   :!rendered-pages (r/atom #{0})
   :!page-height (r/atom nil)
   :!total-count (atom 0)
   :!top-offset (atom 0)})

(defn render-page-item [_]
  (r/create-class
    {:component-did-mount
     (fn [] #_(prn "MOUNT"))
     :should-component-update
     (fn [_ [_ old] [_ new]]
       (not= (dissoc old :render-item)
             (dissoc new :render-item)))
     :component-did-update
     (browser/cdu-diff
       (fn [[old] [new]]
         #_(prn "CDU")
         #_(ks/pp old)
         #_(ks/pp new)
         #_(prn "--")))
     :component-will-unmount
     (fn [] #_(prn "UNM"))
     :reagent-render
     (fn [{:keys [item data-key separator render-item]}]
       [:div
        {:key (get item data-key)
         :style {:overflow 'hidden}}
        [render-item item {}]
        (let [{:keys [size color]} separator]
          [:div
           {:style {:height (or size 0)
                    :background-color (or color "transparent")}}])])}))

(defn render-page [{:keys [page-index]}]
  (r/create-class
    {:component-did-mount
     (fn [])
     :component-will-unmount
     (fn [])
     :reagent-render
     (fn [{:keys [page-index
                  page-data
                  render-item
                  data-key
                  !page-height
                  separator
                  page-size]}]
       (into
         [:div
          {:style {:position 'absolute
                   :flex-direction 'column
                   :left 0
                   :right 0
                   :top (*
                          page-index
                          @!page-height)}}]
         (->> page-data
              (map (fn [d]
                     [render-page-item
                      {:key (get d data-key)
                       :item d
                       :data-key data-key
                       :separator separator
                       :render-item render-item}])))))}))

(defn list [{:keys [page-size scroll-event-throttle] :as opts}]
  (let [state (or (:state opts)
                  (create-state opts))
        {:keys [!opts
                !ref
                !scroll-el
                !rendered-pages
                !total-count
                !page-height
                !top-offset]} state

        update-ref (fn [ref] (when ref (reset! !ref ref)))

        scroll-ch (chan (sliding-buffer 1))

        on-scroll (fn [e]
                    (put! scroll-ch
                      (if (= (.. e -target) js/document)
                        (.-scrollY js/window)
                        (.. e -target -scrollTop))))

        page-size (or (:page-size opts) 20)]

    (go
      (let [ch (ks/throttle scroll-ch
                 (or scroll-event-throttle 17))]
        (loop []
          (let [scroll-top (<! ch)
                bot-px (- scroll-top @!top-offset)
                page-index (ks/ceil (/ bot-px @!page-height))
                max-pages (ks/ceil (/ @!total-count page-size))]

            (swap! !rendered-pages
              into
              (->> (range
                     (- page-index 2)
                     (+ page-index 2))
                   (map #(ks/clamp % 0 max-pages))
                   set))
            (recur)))))
    
    (r/create-class
      {:component-did-mount
       (fn [this]
         (reset! !scroll-el
           (browser/scrollable-ancestor @!ref))
         (reset! !page-height
           (let [el (.item (.-children (.item (.-children @!ref) 0)) 0)]
             (.-offsetHeight el)))
         (.addEventListener
           @!scroll-el
           "scroll"
           on-scroll))
       :component-will-unmount
       (fn []
         (.removeEventListener
           @!scroll-el
           "scroll"
           on-scroll))
       :component-did-update
       (browser/cdu-diff
         (fn [[old] [new]]
           (when (not= old new)
             (reset! !opts new))
           (let [new-scroll-ancestor
                 (browser/find-scrollable-ancestor @!ref :vertical)]
             (when (not= new-scroll-ancestor @!scroll-el)
               (reset! !top-offset
                 (+ (.-pageYOffset js/window)
                    (.-top (.getBoundingClientRect @!ref))))
               (reset! !page-height
                 (let [el (.item (.-children (.item (.-children @!ref) 0)) 0)]
                   (.-offsetHeight el)))
               (.removeEventListener
                 @!scroll-el
                 "scroll"
                 on-scroll)
               (reset! !scroll-el new-scroll-ancestor)
               (.addEventListener
                 @!scroll-el
                 "scroll"
                 on-scroll)
               (put! scroll-ch
                 (if (or (= new-scroll-ancestor js/document)
                         (= new-scroll-ancestor js/window))
                   (.-scrollY js/window)
                   (.. new-scroll-ancestor -scrollTop)))))))
       :reagent-render
       (fn [{:keys [render-item
                    separator
                    data
                    data-key
                    style]}]
         (let [data (vec data)
               page-size (min page-size (count data))]
           (reset! !total-count (count data))
           [:div
            {:ref update-ref
             :style (merge
                      #_{:overflow-y 'scroll}
                      (when separator
                        (let [{:keys [size color]} separator]
                          {:border-top (str "solid "
                                            color
                                            " "
                                            size "px")}))
                      style)}
            (into
              [:div {:style (merge
                              {:position 'relative}
                              (when @!page-height
                                {:height (* @!page-height (ks/ceil (/ (count data) page-size)))}))}]
              (->> @!rendered-pages
                   (map (fn [page-index]
                          [render-page
                           {:key page-index
                            :page-index page-index
                            :page-data (subvec data
                                         (min (* page-index page-size) (count data))
                                         (min (+ (* page-index page-size) page-size) (count data)))
                            :render-item render-item
                            :data-key data-key
                            :!page-height !page-height
                            :separator separator
                            :page-size page-size}])))
              #_(->> @!rendered-pages
                     (map
                       (fn [page-index]
                         [(r/create-class
                            {:component-did-mount
                             (fn []
                               (prn "MN" page-index))
                            
                             :component-will-unmount
                             (fn []
                               (prn "UNM" page-index))
                             :reagent-render
                             (fn [page-index]
                               (into
                                 [:div
                                  {:key page-index
                                   :style {:position 'absolute
                                           :flex-direction 'column
                                           :left 0
                                           :right 0
                                           :top (*
                                                  page-index
                                                  @!page-height)}}]
                                 (->> (subvec data
                                        (min (* page-index page-size) (count data))
                                        (min (+ (* page-index page-size) page-size) (count data)))
                                      (map (fn [d]
                                             [:div
                                              {:key (get d data-key)
                                               :style {:overflow 'hidden}}
                                              [render-item d {}]
                                              (let [{:keys [size color]} separator]
                                                [:div
                                                 {:style {:height (or size 0)
                                                          :background-color (or color "transparent")}}])])))))})
                          page-index]))
                     doall))]))})))

