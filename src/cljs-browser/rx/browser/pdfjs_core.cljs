(ns rx.browser.pdfjs-core
  (:require [rx.kitchen-sink :as ks
             :refer-macros [<&]]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [rx.browser.canvas :as cvs]
            [rx.browser.css :as bcss]
            [rx.browser.ui :as ui]
            [rx.browser.buttons :as btn]
            [rx.browser.forms :as forms]
            [rx.browser.feather-icons :as fi]
            [reagent.core :as r]
            [clojure.core.async
             :as async
             :refer [go <! timeout chan sliding-buffer put! close!]]))

(defonce !PJ (atom nil))

(defn PJ []
  (when-not @!PJ
    (anom/throw-anom
      {:desc "PDF.js hasn't been loaded."}))
  @!PJ)

(def !debug-categories
  (atom #{#_:page-rendering
          #_:text-rendering
          #_:event-channel
          #_:document-events
          #_:user-content
          #_:doc-selection}))

(defn debug-enabled? [category]
  (get @!debug-categories category))

(defn dprintln [category & ss]
  (when (debug-enabled? category)
    (apply println ss)))

(defn init-pdfjs [PJ {:keys [worker-src]}]
  (reset! !PJ PJ)
  (set! (.. PJ -GlobalWorkerOptions -workerSrc) worker-src))

(def css-text
  "/* Copyright 2014 Mozilla Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*.textLayer {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  opacity: 0.2;
  line-height: 1.0;
}
  */

.textLayer > span {
  /*color: transparent;*/
  position: absolute;
  white-space: pre;
  cursor: text;
  transform-origin: 0% 0%;
  pointer-events: auto;
}

.textLayer .highlight {
  margin: -1px;
  padding: 1px;
  background-color: rgba(180, 0, 170, 1);
  border-radius: 4px;
}

.textLayer .highlight.begin {
  border-radius: 4px 0px 0px 4px;
}

.textLayer .highlight.end {
  border-radius: 0px 4px 4px 0px;
}

.textLayer .highlight.middle {
  border-radius: 0px;
}

.textLayer .highlight.selected {
  background-color: rgba(0, 100, 0, 1);
}

.textLayer ::-moz-selection {
  background: rgba(0, 0, 255, 1);
}

.textLayer ::selection {
  background: rgba(0, 0, 255, 1);
}

.textLayer .endOfContent {
  display: block;
  position: absolute;
  left: 0px;
  top: 100%;
  right: 0px;
  bottom: 0px;
  z-index: -1;
  cursor: default;
  -webkit-user-select: none;
     -moz-user-select: none;
      -ms-user-select: none;
          user-select: none;
}

.textLayer .endOfContent.active {
  top: 0px;
}


.annotationLayer section {
  position: absolute;
}

.annotationLayer .linkAnnotation > a,
.annotationLayer .buttonWidgetAnnotation.pushButton > a {
  position: absolute;
  font-size: 1em;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.annotationLayer .linkAnnotation > a:hover,
.annotationLayer .buttonWidgetAnnotation.pushButton > a:hover {
  opacity: 0.2;
  background: rgba(255, 255, 0, 1);
  box-shadow: 0px 2px 10px rgba(255, 255, 0, 1);
}

.annotationLayer .textAnnotation img {
  position: absolute;
  cursor: pointer;
}

.annotationLayer .textWidgetAnnotation input,
.annotationLayer .textWidgetAnnotation textarea,
.annotationLayer .choiceWidgetAnnotation select,
.annotationLayer .buttonWidgetAnnotation.checkBox input,
.annotationLayer .buttonWidgetAnnotation.radioButton input {
  background-color: rgba(0, 54, 255, 0.13);
  border: 1px solid transparent;
  box-sizing: border-box;
  font-size: 9px;
  height: 100%;
  margin: 0;
  padding: 0 3px;
  vertical-align: top;
  width: 100%;
}

.annotationLayer .choiceWidgetAnnotation select option {
  padding: 0;
}

.annotationLayer .buttonWidgetAnnotation.radioButton input {
  border-radius: 50%;
}

.annotationLayer .textWidgetAnnotation textarea {
  font: message-box;
  font-size: 9px;
  resize: none;
}

.annotationLayer .textWidgetAnnotation input[disabled],
.annotationLayer .textWidgetAnnotation textarea[disabled],
.annotationLayer .choiceWidgetAnnotation select[disabled],
.annotationLayer .buttonWidgetAnnotation.checkBox input[disabled],
.annotationLayer .buttonWidgetAnnotation.radioButton input[disabled] {
  background: none;
  border: 1px solid transparent;
  cursor: not-allowed;
}

.annotationLayer .textWidgetAnnotation input:hover,
.annotationLayer .textWidgetAnnotation textarea:hover,
.annotationLayer .choiceWidgetAnnotation select:hover,
.annotationLayer .buttonWidgetAnnotation.checkBox input:hover,
.annotationLayer .buttonWidgetAnnotation.radioButton input:hover {
  border: 1px solid rgba(0, 0, 0, 1);
}

.annotationLayer .textWidgetAnnotation input:focus,
.annotationLayer .textWidgetAnnotation textarea:focus,
.annotationLayer .choiceWidgetAnnotation select:focus {
  background: none;
  border: 1px solid transparent;
}

.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:before,
.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:after,
.annotationLayer .buttonWidgetAnnotation.radioButton input:checked:before {
  background-color: rgba(0, 0, 0, 1);
  content: '';
  display: block;
  position: absolute;
}

.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:before,
.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:after {
  height: 80%;
  left: 45%;
  width: 1px;
}

.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:before {
  transform: rotate(45deg);
}

.annotationLayer .buttonWidgetAnnotation.checkBox input:checked:after {
  transform: rotate(-45deg);
}

.annotationLayer .buttonWidgetAnnotation.radioButton input:checked:before {
  border-radius: 50%;
  height: 50%;
  left: 30%;
  top: 20%;
  width: 50%;
}

.annotationLayer .textWidgetAnnotation input.comb {
  font-family: monospace;
  padding-left: 2px;
  padding-right: 0;
}

.annotationLayer .textWidgetAnnotation input.comb:focus {
  /*
   * Letter spacing is placed on the right side of each character. Hence, the
   * letter spacing of the last character may be placed outside the visible
   * area, causing horizontal scrolling. We avoid this by extending the width
   * when the element has focus and revert this when it loses focus.
   */
  width: 115%;
}

.annotationLayer .buttonWidgetAnnotation.checkBox input,
.annotationLayer .buttonWidgetAnnotation.radioButton input {
  -webkit-appearance: none;
     -moz-appearance: none;
          appearance: none;
  padding: 0;
}

.annotationLayer .popupWrapper {
  position: absolute;
  width: 20em;
}

.annotationLayer .popup {
  position: absolute;
  z-index: 200;
  max-width: 20em;
  background-color: rgba(255, 255, 153, 1);
  box-shadow: 0px 2px 5px rgba(136, 136, 136, 1);
  border-radius: 2px;
  padding: 6px;
  margin-left: 5px;
  cursor: pointer;
  font: message-box;
  font-size: 9px;
  word-wrap: break-word;
}

.annotationLayer .popup > * {
  font-size: 9px;
}

.annotationLayer .popup h1 {
  display: inline-block;
}

.annotationLayer .popup span {
  display: inline-block;
  margin-left: 5px;
}

.annotationLayer .popup p {
  border-top: 1px solid rgba(51, 51, 51, 1);
  margin-top: 2px;
  padding-top: 2px;
}

.annotationLayer .highlightAnnotation,
.annotationLayer .underlineAnnotation,
.annotationLayer .squigglyAnnotation,
.annotationLayer .strikeoutAnnotation,
.annotationLayer .freeTextAnnotation,
.annotationLayer .lineAnnotation svg line,
.annotationLayer .squareAnnotation svg rect,
.annotationLayer .circleAnnotation svg ellipse,
.annotationLayer .polylineAnnotation svg polyline,
.annotationLayer .polygonAnnotation svg polygon,
.annotationLayer .caretAnnotation,
.annotationLayer .inkAnnotation svg polyline,
.annotationLayer .stampAnnotation,
.annotationLayer .fileAttachmentAnnotation {
  cursor: pointer;
}

.pdfViewer .canvasWrapper {
  overflow: hidden;
}

.pdfViewer .page {
  direction: ltr;
  width: 816px;
  height: 1056px;
  margin: 1px auto -8px auto;
  position: relative;
  overflow: visible;
  border: 9px solid transparent;
  background-clip: content-box;
  -o-border-image: url(images/shadow.png) 9 9 repeat;
     border-image: url(images/shadow.png) 9 9 repeat;
  background-color: rgba(255, 255, 255, 1);
}

.pdfViewer.removePageBorders .page {
  margin: 0px auto 10px auto;
  border: none;
}

.pdfViewer.singlePageView {
  display: inline-block;
}

.pdfViewer.singlePageView .page {
  margin: 0;
  border: none;
}

.pdfViewer.scrollHorizontal, .pdfViewer.scrollWrapped, .spread {
  margin-left: 3.5px;
  margin-right: 3.5px;
  text-align: center;
}

.pdfViewer.scrollHorizontal, .spread {
  white-space: nowrap;
}

.pdfViewer.removePageBorders,
.pdfViewer.scrollHorizontal .spread,
.pdfViewer.scrollWrapped .spread {
  margin-left: 0;
  margin-right: 0;
}

.spread .page,
.pdfViewer.scrollHorizontal .page,
.pdfViewer.scrollWrapped .page,
.pdfViewer.scrollHorizontal .spread,
.pdfViewer.scrollWrapped .spread {
  display: inline-block;
  vertical-align: middle;
}

.spread .page,
.pdfViewer.scrollHorizontal .page,
.pdfViewer.scrollWrapped .page {
  margin-left: -3.5px;
  margin-right: -3.5px;
}

.pdfViewer.removePageBorders .spread .page,
.pdfViewer.removePageBorders.scrollHorizontal .page,
.pdfViewer.removePageBorders.scrollWrapped .page {
  margin-left: 5px;
  margin-right: 5px;
}

.pdfViewer .page canvas {
  margin: 0;
  display: block;
}

.pdfViewer .page canvas[hidden] {
  display: none;
}

.pdfViewer .page .loadingIcon {
  position: absolute;
  display: block;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: url('images/loading-icon.gif') center no-repeat;
}

.pdfPresentationMode .pdfViewer {
  margin-left: 0;
  margin-right: 0;
}

.pdfPresentationMode .pdfViewer .page,
.pdfPresentationMode .pdfViewer .spread {
  display: block;
}

.pdfPresentationMode .pdfViewer .page,
.pdfPresentationMode .pdfViewer.removePageBorders .page {
  margin-left: auto;
  margin-right: auto;
}

.pdfPresentationMode:-ms-fullscreen .pdfViewer .page {
  margin-bottom: 100% !important;
}

.pdfPresentationMode:-webkit-full-screen .pdfViewer .page {
  margin-bottom: 100%;
  border: 0;
}

.pdfPresentationMode:-moz-full-screen .pdfViewer .page {
  margin-bottom: 100%;
  border: 0;
}

.pdfPresentationMode:fullscreen .pdfViewer .page {
  margin-bottom: 100%;
  border: 0;
}
")

(defonce style-text
  (bcss/set-style-text
    "rx-browser-pdfjs-style"
    css-text))

;; PDFJS api

(<defn <pdf-obj [url-or-data]
  (<? (ks/<promise
        (.-promise
          (.getDocument (PJ) url-or-data)))))

(<defn <pdf-page-obj [pdf page-index]
  (<? (ks/<promise
        (.getPage pdf (inc page-index)))))

(defn pdf-start-render [ctx page viewport]
  (let [res
        (.render page
          (clj->js
            {:canvasContext ctx
             :viewport viewport
             :enableWebGL true}))]
    res))

(defn pdf-page-viewport [page scale]
  (.getViewport page
    (clj->js
      {:scale scale})))

(defn pdf-num-pages [pdf]
  (.. pdf -_pdfInfo -numPages))

(defn viewport-obj-rect [vp]
  {:width (.-width vp)
   :height (.-height vp)})

(defn page-obj-rect [p]
  (viewport-obj-rect
    (pdf-page-viewport p 1)))

(defn get-page-index [page]
  (when page
    (.-_pageIndex page)))

(defn page-aspect-ratio [page]
  (let [pr (page-obj-rect page)]
    (/
      (:width pr)
      (:height pr))))


;;; Page Rendering

(defn pdf-render-page-img [page width]
  (let [ch (chan)
        !render-obj (atom nil)
        !cancelled? (atom nil)
        ctl {:ch ch
             :!render-obj !render-obj
             :!cancelled? !cancelled?}
        start-ts (ks/now)]
    (go
      (try
        (let [width (/ width 1)

              canvas (.createElement js/document "canvas")
              pixel-ratio (.-devicePixelRatio js/window)

              pr (page-obj-rect page)
              
              aspect-ratio (/
                             (:height pr)
                             (:width pr))

              height (* width aspect-ratio)

              _ (set! (.-width canvas) (* width pixel-ratio))
              _ (set! (.-height canvas) (* height pixel-ratio))

              _ (set! (.-width (.-style canvas)) width)
              _ (set! (.-height (.-style canvas)) height)
              
              ctx (.getContext canvas "2d")
              
              ;;_ (.scale ctx pixel-ratio pixel-ratio)

              scale (/
                      width
                      (:width pr))

              vp (pdf-page-viewport page scale)

              viewport-rect (viewport-obj-rect vp)

              _ (dprintln
                  :page-rendering
                  (str "[" (get-page-index page) "]")
                  "Start render"
                  viewport-rect)]

          (if @!cancelled?
            (do
              (println "Render pre-cancelled")
              (close! ch))
            (let [render-obj (pdf-start-render ctx page vp)]

              (reset! !render-obj render-obj)
              (.scale ctx pixel-ratio pixel-ratio)
              
              (let [res (<! (ks/<promise (.-promise render-obj)))]
                (if (anom/? res)
                  (do
                    (put! ch res)
                    (close! ch))
                  (do
                    (dprintln
                      :page-rendering
                      (str "[" (get-page-index page) "]")
                      "Render complete" (- (ks/now) start-ts) "ms")
                    (let [start-ts (ks/now)]
                      (.toBlob canvas
                        (fn [blob]
                          (dprintln
                            :page-rendering
                            (str "[" (get-page-index page) "]")
                            "Blob complete" (- (ks/now) start-ts) "ms")
                          (put! ch (.createObjectURL js/URL blob))
                          (close! ch))
                        "image/jpeg"
                        0.5))))))))
        (catch js/Error e
          (put! ch (anom/from-err e))
          (close! ch))))
    ctl))

(defn cancel-render [{:keys [!render-obj
                             !cancelled?]}]
  (try
    (reset! !cancelled? true)
    (when @!render-obj
      (.cancel @!render-obj))
    (catch js/Error e
      (prn (anom/from-err e)))))

(defn resolve-page-img [{:keys [index->img-src
                                index->img-viewport-rect]
                         :as cache}
                        page width]
  (let [ch (chan)
        !render-obj (atom nil)
        !cancelled? (atom nil)
        page-index (get-page-index page)
        cached-width (:width (get index->img-viewport-rect page-index))]
    (go
      (try
        (if (>= cached-width width)
          (do
            (dprintln
              :page-rendering
              (str "[" page-index "]")
              "Retreiving page image from cache"
              width)
            (put! ch
              (merge
                cache
                {:img-data-url (get index->img-src page-index)})))
          (let [render-job (pdf-render-page-img page width)
                _ (do (reset! !render-obj @(:!render-obj render-job))
                      (add-watch
                        (:!render-obj render-job)
                        :sync-cancelled
                        (fn [_ _ _ render-obj]
                          (reset! !render-obj render-obj))))
                _ (do
                    (reset! !cancelled? @(:!cancelled? render-job))
                    (add-watch
                      (:!cancelled? render-job)
                      :sync-cancelled
                      (fn [_ _ _ cancelled?]
                        (reset! !cancelled? cancelled?))))
                img-data-url (<? (:ch render-job))]
            (put! ch
              (merge
                cache
                {:index->img-src
                 (assoc
                   index->img-src
                   page-index
                   img-data-url)
                 :index->img-viewport-rect
                 (assoc
                   index->img-viewport-rect
                   page-index
                   {:width width})}))))
        (catch js/Error e
          (prn "ERR" e)
          (close! ch))))
    {:ch ch
     :!render-obj !render-obj
     :!cancelled? !cancelled?}))


;;; Text layer rendering

(defn render-text-layer? [opts]
  (:text-layer? opts))

(<defn <pdf-render-text-layer [page viewport layer-el]
  (let [tc (<? (ks/<promise
                 (.getTextContent page)))]
    (browser/remove-all-children layer-el)
    (.renderTextLayer
      (PJ)
      (clj->js
        {:textContent tc
         :container layer-el
         :viewport viewport
         :enhanceTextSelection true}))))

(defn text-layer-opts [{:keys [!index->text-layer-el
                               !index->text-layer-rect
                               render-text-layer-ch
                               !index->mouse-down?
                               !index->page
                               !interaction]}
                       page-index]
  (let [layer-rect (get @!index->text-layer-rect page-index)
        page-rect (page-obj-rect (get @!index->page page-index))

        scale-x (/ (:width layer-rect) (:width page-rect))
        scale-y (/ (:height layer-rect) (:height page-rect))]
    {:ref (fn [el]
            (when el
              (when-not (get @!index->text-layer-el page-index)
                (swap! !index->text-layer-el assoc page-index el)
                #_(put! render-text-layer-ch page-index))))
     :zoom-active? (:zoom-active? @!interaction)
     :scale {:x scale-x :y scale-y}
     :mouse-down? (get @!index->mouse-down? page-index)
     :page-index page-index}))

(defn render-text-layer [{:keys [ref
                                 mouse-down?
                                 page-index
                                 scale
                                 zoom-active?]}]
  (let [debug? (debug-enabled? :text-rendering)]
    [:div.page-content-text-layer
     {:style {:position 'absolute
              :top 0
              :left 0
              :right 0
              :bottom 0}}
     [:div
      {:style {:position 'relative
               :width "100%"
               :height "100%"}}
      [:div
       {:class "textLayer"
        :ref ref
        :style (merge
                 {:position 'absolute
                  :display (if zoom-active? "none" "block")
                  :top 0
                  :left 0
                  :right 0
                  :bottom 0
                  :overflow 'visible?
                  :opacity 0.2
                  :color 'transparent
                  :line-height 1.0
                  :transform (str "translate3d(0,0,0) scale3d("
                                  (or (:x scale) 1)
                                  ","
                                  (or (:y scale) 1)
                                  ","
                                  1
                                  ")")
                  :transform-origin "0 0"}
                 (when debug?
                   {:color "black"
                    :opacity 0.5
                    :background-color "rgba(255,0,0,0.2)"}))}]
      (when debug?
        [:div
         {:style {:position 'absolute
                  :top 0
                  :left 0}}
         [ui/g
          {:gap 8}
          [ui/g
           {:gap 4}
           (->> ["zoom scale" scale]
                (partition 2)
                (map (fn [[s v]]
                       [:div s " " v])))]]])]]))

(defn text-layer-start-loop [opts {:keys [!index->text-layer-el
                                          !index->text-layer-rect
                                          !index->page-content-el
                                          !index->page
                                          !viewing-ref
                                          render-text-layer-ch
                                          !zoom
                                          !opts]}]
  (gol
    (loop [page-index (<! render-text-layer-ch)]
      (when (:text-layer? @!opts)
        (let [page (get @!index->page page-index)
              page-rect (page-obj-rect page)
              el (get @!index->page-content-el page-index)

              el-rect (browser/bounding-client-rect el)

              scale 1

              vp (pdf-page-viewport page scale)]

          (when (not (get @!index->text-layer-rect page-index))
            (dprintln
              :text-rendering
              (str "[" page-index "]")
              "Start text rendering")
            (let [start-ts (ks/now)]
              (<? (<pdf-render-text-layer
                    page
                    vp
                    (get @!index->text-layer-el page-index)))
              (dprintln
                :text-rendering
                (str "[" page-index "]")
                "Complete text rendering"
                (- (ks/now) start-ts)
                "ms")))

          (when (not= el-rect (get @!index->text-layer-rect page-index))
            (swap! !index->text-layer-rect
              assoc
              page-index
              el-rect))))
      (recur (<! render-text-layer-ch)))))

(defn scroll-offset [ref]
  (let [left (if (= js/window ref)
               (.-scrollX ref)
               (.-scrollLeft ref)) 
        top (if (= js/window ref)
              (.-scrollY ref)
              (.-scrollTop ref))]
    {:left left
     :top top}))

(defn scroller-rect [ref]
  (if (= js/window ref)
    {:width (.-innerWidth js/window)
     :height (.-innerHeight js/window)}
    (select-keys (browser/bounding-client-rect ref) [:width :height])))

(defn viewing-rect [ref]
  (merge
    (scroller-rect ref)
    (scroll-offset ref)))

(defn opts [state] @(:!opts state))

(defn scale-rect [scale rect]
  (->> rect
       (map (fn [[k v]]
              [k (* scale v)]))
       (into {})))

;;; Page rendering service

(defn queue-render [{:keys [render-queue-ch
                            render-text-layer-ch
                            !index->page]}
                    page-indexes
                    width]
  (put! render-queue-ch
    (->> page-indexes
         (map #(get @!index->page %))
         (map (fn [page]
                [page width]))))
  (doseq [page-index page-indexes]
    (put! render-text-layer-ch page-index)))

(defn pages-in-order [{:keys [!index->page]}]
  (->> @!index->page
       (sort-by first)
       (map second)
       vec))

(defn clear-render-lifo [{:keys [render-queue-ch !lifo]}]
  (reset! !lifo []))

(<defn load-page-fill-rect [{:keys [!index->page
                                    !index->viewport-rect
                                    !index->img-viewport-rect
                                    !index->img-src
                                    !loaded-page-indexes
                                    !viewing-ref
                                    !resizing-so
                                    !resizing-scale
                                    !resizing?
                                    !content-ref
                                    !zoom
                                    !opts
                                    render-queue-ch]
                             :as state}]

  (let [{:keys [pdf]} (opts state)

        resizing-scale (or @!resizing-scale 1)
        
        resizing? @!resizing?

        so (if resizing?
             @!resizing-so
             (scroll-offset @!viewing-ref))

        viewing-rect (merge
                       (scroller-rect @!viewing-ref)
                       so)

        max-index (max 0 (dec (pdf-num-pages pdf)))

        visible-top (* (:top so) resizing-scale)
        visible-bot (+ visible-top (:height viewing-rect))

        loading-top (- visible-top 1000)
        loading-bot (+ visible-bot 1000)

        [index->page
         to-load-page-indexes
         index->viewport-rect
         page+widths
         current-page-index]
        (loop [page-index 0
               accu-top 0
               index->page {}
               to-load-page-indexes #{}
               index->viewport-rect {}
               page+widths #{}
               current-page-index nil]
          (if (or
                (>= accu-top loading-bot)
                (> page-index max-index))
            [index->page
             to-load-page-indexes
             index->viewport-rect
             page+widths
             current-page-index]
            (let [page (or (get @!index->page page-index)
                           (<? (<pdf-page-obj
                                 pdf
                                 page-index)))
                  pr (scale-rect
                       (/ 1 (or (:scale @!zoom) 1))
                       (page-obj-rect page))
                  scale (/
                          (/
                            (:width viewing-rect)
                            (:width pr))
                          resizing-scale)

                  vp (pdf-page-viewport page scale)
                  vpr (viewport-obj-rect vp)
                  vpr (scale-rect resizing-scale vpr)
                  vpr (merge
                        vpr
                        {:top accu-top})
                  new-accu-top (+ accu-top (:height vpr))

                  page-vpr (scale-rect
                             resizing-scale
                             (merge
                               (viewport-obj-rect vp)
                               {:top accu-top}))

                  to-load? (or
                             #_(>= accu-top loading-top)
                             (>= new-accu-top loading-top))

                  existing-vpr (get @!index->img-viewport-rect page-index)

                  page+width [page (:width vpr)]

                  center-px (+ (:top viewing-rect)
                               (/ (:height viewing-rect) 2))

                  current-page-index? (and (>= center-px (:top page-vpr))
                                           (<= center-px (+ (:top page-vpr)
                                                            (:height page-vpr))))

                  new-current-page-index (when current-page-index?
                                           page-index)]

              (recur
                (inc page-index)
                new-accu-top
                (merge index->page {page-index page})
                (if to-load?
                  (conj to-load-page-indexes page-index)
                  to-load-page-indexes)
                (merge
                  index->viewport-rect
                  {page-index vpr})
                (if (and to-load?
                         (not= (select-keys vpr [:width])
                               existing-vpr))
                  (conj page+widths page+width)
                  page+widths)
                (if current-page-index?
                  new-current-page-index
                  current-page-index)))))]

    (swap! !loaded-page-indexes
      (fn [vpis]
        (set
          (apply
            conj
            vpis
            to-load-page-indexes))))

    (when-let [f (:on-change-page-index @!opts)]
      (f current-page-index))

    (swap! !index->page merge index->page)
    
    (swap! !index->viewport-rect merge index->viewport-rect)

    (put! render-queue-ch page+widths)))

(defn current-page-index [{:keys [!viewing-ref
                                  !index->viewport-rect]}]
  (let [so (scroll-offset @!viewing-ref)
        page-index (->> @!index->viewport-rect
                        (sort-by first)
                        (some
                          (fn [[page-index {:keys [top]}]]
                            (when (<= (:top so) top)
                              page-index))))]
    page-index))

(<defn <load-pages [pdf]
  (let [page-count (pdf-num-pages pdf)]
    (loop [page-index 0
           out []]
      (if (= page-count page-index)
        out
        (let [page (<? (<pdf-page-obj
                         pdf
                         page-index))]
          (recur
            (inc page-index)
            (conj
              out
              page)))))))

(defn render-cache-state []
  (let [render-ch (chan 1000)
        !cancel-ch (atom nil)
        render-queue-ch (chan 1000)
        !lifo (atom [])

        !index->img-viewport-rect (atom nil)
        !index->img-src (r/atom nil)

        !index->text-layer-el (atom nil)
        !index->text-layer-rect (r/atom nil)
        

        state
        {:render-ch render-ch
         :render-queue-ch render-queue-ch
         :!lifo !lifo
         :!render-cancel-ch !cancel-ch

         :!index->img-viewport-rect !index->img-viewport-rect
         :!index->img-src !index->img-src

         :!index->text-layer-el !index->text-layer-el
         :!index->text-layer-rect !index->text-layer-rect}]
    (gol
      (loop [page+widths (<! render-queue-ch)]
        (when page+widths
          (swap! !lifo
            (fn [xs]
              (vec
                (concat
                  xs
                  (reverse page+widths)))))
          (put! render-ch :next)
          (recur
            (<! render-queue-ch)))))

    (gol
      (loop [o (<! render-ch)]
        (when o
          (when-not (empty? @!lifo)
            (loop [[page width] (peek @!lifo)]
              
              (swap! !lifo pop)
              (let [page-index (get-page-index page)
                    vpr-width (:width (get @!index->img-viewport-rect page-index))]
                (when-not (= width vpr-width)
                  (reset! !cancel-ch (chan (sliding-buffer 1)))
                  (let [render-job (resolve-page-img
                                     {:index->img-src @!index->img-src
                                      :index->img-viewport-rect
                                      @!index->img-viewport-rect}
                                     page
                                     width)

                        [pl ch] (async/alts! [@!cancel-ch (:ch render-job)])]

                    (if (= ch @!cancel-ch)
                      (cancel-render render-job)
                      (let [{:keys [index->img-viewport-rect
                                    index->img-src]
                             :as res}
                            pl]
                        (if (anom/? res)
                          (ks/pp res)
                          (do
                            (reset! !index->img-src index->img-src)
                            (reset!
                              !index->img-viewport-rect
                              index->img-viewport-rect))))))))
              (when-not (empty? @!lifo)
                (recur (peek @!lifo)))))
          (recur
            (<! render-ch)))))

    state))

(defn create-document-state [{:keys [pdf] :as opts}]
  (merge
    {:!opts (atom opts)
     :!content-ref (atom nil)
     :!content-rect (r/atom nil)
     :!viewing-ref (atom nil)
     :!viewing-rect (r/atom nil)
     :!initial-content-rect (atom nil)
     :!index->page (r/atom nil)
     :!index->viewport-rect (r/atom nil)
     :!index->page-content-el (atom nil)
     :!index->page-content-rect (atom nil)
     :!index->initial-page-rect (atom nil)
     :!index->page-wrapper-el (atom nil)
     :!index->mouse-down? (atom nil)
     :!pdf (atom pdf)
     :!loaded-page-indexes (r/atom nil)
     :!visible-page-indexes (r/atom nil)

     :!resizing-timeout (atom nil)
     :!resizing? (r/atom nil)
     :!resizing-offset (r/atom {:top "0%" :left "0%"})
     :!resizing-so (r/atom {:top 0 :left 0})
     :!resizing-scale (r/atom 1)
     :!resizing-viewing-rect (atom nil)
     :!resizing-content-rect (atom nil)
     :!resizing-pct (atom {:top 1 :left 1})

     :!interaction (r/atom 
                     {:scale 1})

     :!zoom (r/atom {:scale 1})
     :!timeouts (atom nil)

     :!last-sel (atom nil)

     :render-text-layer-ch (chan 100)}
    (render-cache-state)))


;;; Event Machinery

(defn track-event-lifecycle [in-ch timeout & [out-ch]]
  (let [!timeout (atom nil)
        out-ch (or out-ch (chan))]
    (gol
      (loop [pl (<? in-ch)]
        (when pl
          (let [existing-timeout @!timeout]
            (if existing-timeout
              (do
                (js/clearTimeout existing-timeout))
              (put! out-ch [:start pl]))

            (put! out-ch [:in-progress pl])

            (reset! !timeout
              (js/setTimeout
                (fn []
                  (put! out-ch [:end pl])
                  (reset! !timeout nil))
                timeout)))
          (recur (<? in-ch)))))
    out-ch))

(defn zoom-event-handler [zoom-ch event-ch min-zoom max-zoom]
  (let [!zoom (atom {:zoom-scale 1
                     :starting-scale 1
                     :delta-scale 1
                     :final-scale 1})

        min-zoom (or min-zoom 0.5)
        max-zoom (or max-zoom 3)]

    (gol
      (let [ch (track-event-lifecycle zoom-ch 250)]
        (loop [[event-type delta-value :as event] (<? ch)]
          (when event
            (let [{:keys [zoom-scale
                          starting-scale
                          final-scale
                          delta-scale]} @!zoom]
              (condp = event-type
                :start
                (swap! !zoom assoc
                  :starting-scale zoom-scale
                  :delta-scale 0
                  :final-scale zoom-scale)
                :in-progress 
                (let [tick-scale (/ (- delta-value) 100)
                      new-final-scale
                      (+ final-scale tick-scale)]
                  (swap! !zoom
                    merge
                    (cond
                      (> new-final-scale max-zoom)
                      {:final-scale max-zoom
                       :delta-scale (- max-zoom starting-scale)
                       :tick-scale 0}
                      (< new-final-scale min-zoom)
                      {:final-scale min-zoom
                       :delta-scale (- min-zoom starting-scale)
                       :tick-scale 0}
                      :else
                      {:final-scale new-final-scale
                       :delta-scale (- new-final-scale starting-scale)
                       :tick-scale tick-scale})))
                :end
                (swap! !zoom assoc
                  :zoom-scale final-scale))
              (let [{:keys [starting-scale delta-scale tick-scale]} @!zoom] 
                (>! event-ch
                  (condp = event-type
                    :start
                    [:zoom-start {:initial starting-scale
                                  :delta delta-scale
                                  :aggregate final-scale
                                  :tick tick-scale}]
                    :in-progress
                    [:zoom {:initial starting-scale
                            :delta delta-scale
                            :aggregate final-scale
                            :tick tick-scale}]
                    :end
                    [:zoom-end {:initial starting-scale
                                :delta delta-scale
                                :aggregate final-scale
                                :tick tick-scale}]))))
            (recur (<? ch))))))))

#_(defn zoom-event-handler [zoom-ch event-ch min-zoom max-zoom]
  (let [!zoom (atom {:zoom-scale 1
                     :starting-scale 1
                     :delta-scale 1
                     :final-scale 1})

        min-zoom (or min-zoom 0.5)
        max-zoom (or max-zoom 3)]

    (gol
      (let [in-ch zoom-ch]
        (loop [delta-value (<? in-ch)]
          (when delta-value
            (let [{:keys [zoom-scale
                          starting-scale
                          final-scale
                          delta-scale]} @!zoom
                  new-tick-scale (/ (- delta-value) 1000)
                  new-final-scale (+ zoom-scale new-tick-scale)
                  new-delta-scale (- new-final-scale starting-scale)]
              
              (>! event-ch [:zoom-start {:initial zoom-scale
                                         :delta-scale 0
                                         :final-scale zoom-scale}])
              
              (>! event-ch [:zoom {:initial zoom-scale
                                   :delta new-delta-scale
                                   :aggregate new-final-scale
                                   :tick new-tick-scale}])
              (swap! !zoom assoc
                :starting-scale zoom-scale
                :delta-scale new-delta-scale
                :final-scale new-final-scale)

              (loop [agg-timeout-ch (timeout 50)] ; agg timeout
                (when agg-timeout-ch
                  (let [inactive-timeout-ch (timeout 250)
                        [v ch] (async/alts!
                                 [in-ch
                                  inactive-timeout-ch
                                  agg-timeout-ch]
                                 :priority true)]
                    
                    (condp = ch
                      inactive-timeout-ch
                      (let [{:keys [zoom-scale
                                    starting-scale
                                    final-scale
                                    delta-scale]} @!zoom]
                        (>! event-ch [:zoom-end {:initial starting-scale
                                                 :delta delta-scale
                                                 :aggregate final-scale}])
                        (reset! !zoom
                          {:zoom-scale final-scale
                           :tick-scale 0
                           :starting-scale final-scale
                           :delta-scale 0}))
                      
                      in-ch
                      (let [delta-value v
                            {:keys [zoom-scale
                                    starting-scale
                                    final-scale
                                    delta-scale
                                    tick-scale]} @!zoom
                            new-tick-scale (+ tick-scale (/ (- delta-value) 1000))
                            new-final-scale (+ final-scale new-tick-scale)
                            [new-final-scale
                             new-delta-scale
                             new-tick-scale]
                            
                            (cond
                              (> new-final-scale max-zoom)
                              [max-zoom
                               (- max-zoom starting-scale)
                               0]
                              (< new-final-scale min-zoom)
                              [min-zoom
                               (- max-zoom starting-scale)
                               0]
                              :else
                              [new-final-scale
                               (- new-final-scale starting-scale)
                               new-tick-scale])]
                        (swap! !zoom
                          merge
                          {:final-scale new-final-scale
                           :delta-scale new-delta-scale
                           :tick-scale new-tick-scale})
                        (recur agg-timeout-ch))
                      
                      agg-timeout-ch
                      (let [{:keys [zoom-scale
                                    starting-scale
                                    final-scale
                                    delta-scale
                                    tick-scale]} @!zoom]
                        (>! event-ch [:zoom {:initial starting-scale
                                             :delta delta-scale
                                             :aggregate final-scale
                                             :tick tick-scale}])
                        (recur (timeout 250)))))))

              (recur (<? in-ch)))))))))

(defn resize-event-handler [in-ch event-ch]
  (let [!state (atom {:initial nil
                      :delta nil
                      :final nil})]

    (gol
      (let [ch (track-event-lifecycle in-ch 500)]
        (loop [[event-type {:keys [width height] :as pl} :as event] (<? ch)]
          (when event
            (let [{:keys [initial delta final]} @!state]
              (condp = event-type
                :start
                (swap! !state
                  assoc
                  :initial {:width width :height height}
                  :delta {:width 0 :height 0}
                  :final {:width width :height height})
                :in-progress 
                (let [new-width width
                      new-height height]
                  (swap! !state
                    merge
                    {:delta {:width (- new-width (:width initial))
                             :height (- new-height (:height initial))}
                     :final {:width width
                             :height height}}))
                :end nil)
              (>! event-ch
                (condp = event-type
                  :start
                  [:resize-start @!state]
                  :in-progress
                  [:resize @!state]
                  :end
                  [:resize-end @!state])))
            (recur (<? ch))))))))

(defn scroll-event-handler [in-ch event-ch]
  (let [!state (atom {:initial nil
                      :delta nil
                      :final nil})]

    (gol
      (let [ch (track-event-lifecycle in-ch 500)]
        (loop [[event-type {:keys [scroll-top scroll-left] :as pl} :as event] (<? ch)]
          (when event
            (let [{:keys [initial delta final]} @!state]
              (condp = event-type
                :start
                (swap! !state
                  assoc
                  :initial {:scroll-top scroll-top :scroll-left scroll-left}
                  :delta {:scroll-top 0 :scroll-left 0}
                  :final {:scroll-top scroll-top :scroll-left scroll-left})
                :in-progress 
                (let [new-scroll-top scroll-top
                      new-scroll-left scroll-left]
                  (swap! !state
                    merge
                    {:delta {:scroll-top (- new-scroll-top (:scroll-top initial))
                             :scroll-left (- new-scroll-left (:scroll-left initial))}
                     :final {:scroll-top scroll-top
                             :scroll-left scroll-left}}))
                :end nil)
              (>! event-ch
                (condp = event-type
                  :start
                  [:scroll-start @!state]
                  :in-progress
                  [:scroll @!state]
                  :end
                  [:scroll-end @!state])))
            (recur (<? ch))))))))

(<defn <exec-event [handler payload]
  (when handler
    (<& (handler payload))))

(defn <handle-document-event [opts [event-type payload]]
  (dprintln
    :event-channel
    [event-type payload])
  (<exec-event
    (get opts
      (keyword
        (str "on-" (name event-type))))
    payload))

(defn setup-event-handling [{:keys [min-zoom
                                    max-zoom
                                    zoom-enabled?]
                             :as opts}]
  (let [zoom-ch (chan 100)
        scroll-ch (chan 100)
        resize-ch (chan 100)
        event-ch (chan 100)

        !timeouts (atom nil)
        !zoom (atom {})]

    (gol
      (let [ch (ks/throttle event-ch 8)]
        (loop [[event-type :as pl] (<? ch)]
          (<? (<handle-document-event opts pl))
          (recur (<? ch)))))

    (when zoom-enabled?
      (zoom-event-handler zoom-ch event-ch min-zoom max-zoom))
    (resize-event-handler resize-ch event-ch)
    (scroll-event-handler scroll-ch event-ch)

    {:zoom-ch zoom-ch
     :scroll-ch scroll-ch
     :resize-ch resize-ch}))

(defn -visible-page-indexes [pages
                             content-rect
                             viewing-rect]
  (let [visible-top (:top viewing-rect)
        visible-bot (+ visible-top (:height viewing-rect))
        page-count (count pages)]
    (loop [page-index 0
           accu-top 0
           visible-page-indexes []]
      (if (or (>= accu-top visible-bot)
              (>= page-index page-count))
        visible-page-indexes
        (let [page (nth pages page-index)
              pr (page-obj-rect page)
              scale (/
                      (:width content-rect #_viewing-rect)
                      (:width pr))

              vp (pdf-page-viewport page scale)
              vpr (viewport-obj-rect vp)
              vpr (merge
                    vpr
                    {:top accu-top})
              new-accu-top (+ accu-top (:height vpr))

              visible? (>= new-accu-top visible-top)

              page-vpr (merge
                         (viewport-obj-rect vp)
                         {:top accu-top})]

          (recur
            (inc page-index)
            new-accu-top
            (if (and visible? (< page-index page-count))
              (conj visible-page-indexes page-index)
              visible-page-indexes)))))))

(defn -visible-page-indexes-rects [page-rects
                                   content-rect
                                   viewing-rect]
  (let [visible-top (:top viewing-rect)
        visible-bot (+ visible-top (:height viewing-rect))
        page-count (count page-rects)]
    (loop [page-index 0
           accu-top 0
           visible-page-indexes []]
      (if (or (> accu-top visible-bot)
              (>= page-index page-count))
        visible-page-indexes
        (let [pr (nth page-rects page-index)
              new-accu-top (+ accu-top (:height pr))

              visible? (>= new-accu-top visible-top)]

          (recur
            (inc page-index)
            new-accu-top
            (if (and visible? (< page-index page-count))
              (conj visible-page-indexes page-index)
              visible-page-indexes)))))))

#_(defn -visible-page-indexes [page-wrapper-rects
                            content-rect
                            viewing-rect]
  (let [visible-top (:top viewing-rect)
        visible-bot (+ visible-top (:height viewing-rect))
        page-count (count page-wrapper-rects)]
    (loop [page-index 0
           accu-top 0
           visible-page-indexes []]
      (if (or (>= accu-top visible-bot)
              (>= page-index page-count))
        visible-page-indexes
        (let [pr (nth page-wrapper-rects page-index)
              new-accu-top (+ accu-top (:height pr))
              visible? (>= new-accu-top visible-top)]

          (recur
            (inc page-index)
            new-accu-top
            (if (and visible? (< page-index page-count))
              (conj visible-page-indexes page-index)
              visible-page-indexes)))))))


;;; Document API

(defn jump-to-page-index
  "Scrolls to the top of the specified page's wrapper, includes header and footer."
  [{:keys [!viewing-ref
           !index->page-wrapper-el]
    :as state}
   page-index
   & [{:keys [offset]}]]
  (when (and state !viewing-ref @!viewing-ref)
    (let [top (:top
               (browser/bounding-client-rect
                 (get @!index->page-wrapper-el page-index)))
          so (:top (viewing-rect @!viewing-ref))

          scroll-top (ks/spy (+ top so 1))]
      (.scrollTo
        @!viewing-ref
        (clj->js {:top scroll-top})))))

(defn visible-page-indexes [{:keys [!content-ref !viewing-ref]
                             :as state}]
  
  (let [pages (pages-in-order state)
        content-rect (browser/bounding-client-rect @!content-ref)
        viewing-rect (viewing-rect @!viewing-ref)]
    (-visible-page-indexes pages content-rect viewing-rect)))

(defn ensure-page-index-visible
  "If specified page index is off screen will instantly scroll to on screen. If page is below screen will set scroll so that the bottom of the page is in line with the bottom of the container. If above, will scroll so that top is at the top of the container."
  [{:keys [!viewing-ref
           !index->page-wrapper-el]}
   page-index
   & [{:keys [offset]}]]
  (when (and !viewing-ref @!viewing-ref)
    (let [el-rect (browser/bounding-client-rect
                    (get @!index->page-wrapper-el page-index))

          viewing-rect (scroller-rect @!viewing-ref)

          visible? (and (>= (:top el-rect) (:top viewing-rect))
                        (<=
                          (+ (:top el-rect) (:height el-rect))
                          (+ (:top viewing-rect) (:height viewing-rect))))]

      (when-not visible?
        (let [above? (< (:top el-rect) (:top viewing-rect))
              el-top (:top el-rect)
              so-top (:top (scroll-offset @!viewing-ref))]

          (.scrollTo
            @!viewing-ref
            (clj->js
              (if above?
                {:top (+ el-top so-top)}
                {:top (- (+ el-top so-top)
                         (- (:height viewing-rect)
                            (+ (:height el-rect) 30)))}))))))))

(defn zooming? [{:keys [!interaction]}]
  (:zoom-active? @!interaction))

(defn calc-transform-origin [viewing-rect
                             content-rect]
  [(+ (/ (min
           (:width viewing-rect)
           (:width content-rect))
        2)
      (:left viewing-rect))
   (+ (/ (:height viewing-rect) 2)
      (:top viewing-rect))])

(defn page-index-from-rect [{:keys [!index->page-content-rect
                                    !pdf]}
                            {:keys [top]}]
  (loop [page-indexes (range (pdf-num-pages @!pdf))
         out nil]
    (if (or out (empty? page-indexes))
      out
      (let [page-index (first page-indexes)
            rect (get @!index->page-content-rect page-index)
            rect-top (:top rect)
            rect-height (:height rect)
            next-out (when (and (>= top rect-top)
                                (< top (+ rect-top rect-height)))
                       page-index)]
        (recur (rest page-indexes) next-out)))))

(defn page-scale [{:keys [!index->viewport-rect]} page-index]
  (let [{:keys [w-scale]} (get @!index->viewport-rect page-index)]
    w-scale))

(defn offset-rect [offset rect]
  (->> rect
       (map (fn [[k v]]
              (let [offset (get offset k 0)]
                [k (+ offset v)])))
       (into {})))

(defn rects->bounds [rects]
  (->> rects
       (reduce
         (fn [r1 r2]
           {:left (min (:left r1) (:left r2))
            :top (min (:top r1) (:top r2))
            :width (let [left (min (:left r1) (:left r2))
                         right (max
                                 (+ (:left r1) (:width r1))
                                 (+ (:left r2) (:width r2)))]
                     (- right left))
            :height (let [top (min (:top r1) (:top r2))
                          bottom (max
                                   (+ (:top r1) (:height r1))
                                   (+ (:top r2) (:height r2)))]
                      (- bottom top))}))))

#_(defn handle-sel-change [{:keys [!viewing-ref
                                 !content-ref
                                 !index->page-content-rect
                                 !last-sel
                                 !opts] :as state}]
  (let [{scroll-top :top
         scroll-left :left} (viewing-rect @!viewing-ref)
        
        pdf-el-rect (browser/bounding-client-rect @!content-ref)
        pdf-el-rect (offset-rect
                      pdf-el-rect
                      {:top scroll-top
                       :y scroll-top
                       :left scroll-left
                       :x scroll-left})
        
        rects (->> (browser/sel-bounding-rects)
                   ks/spy
                   (remove #(or (= 0 (:width %))
                                (= 0 (:height %))))
                   (mapv #(offset-rect
                            {:top (+ scroll-top (- (:top pdf-el-rect)))
                             :y (+ scroll-top (- (:top pdf-el-rect)))
                             :left (+ scroll-left (- (:left pdf-el-rect)))
                             :x (+ scroll-left (- (:left pdf-el-rect)))}
                            %)))

        _ (dprintln
            :doc-selection
            "Selection rects count:"
            (count rects))

        index->pct-rects
        (->> rects
             distinct
             (map
               (fn [rect]
                 (when-let [page-index (page-index-from-rect state rect)]
                   [page-index rect])))
             (remove nil?)
             (group-by first)
             (map (fn [[page-index page-index+rects]]
                                 
                    [page-index (mapv second page-index+rects)]))
             (map (fn [[page-index rects]]
                    [page-index
                     (->> rects
                          (mapv (fn [sel-rect]
                                  (let [pcr (offset-rect
                                              {:top (+ scroll-top (- (:top pdf-el-rect)))
                                               :y (+ scroll-top (- (:top pdf-el-rect)))
                                               :left (+ scroll-left (- (:left pdf-el-rect)))
                                               :x (+ scroll-left (- (:left pdf-el-rect)))}
                                              (get @!index->page-content-rect page-index))

                                        pct-width (/
                                                    (:width sel-rect)
                                                    (:width pcr))
                                        pct-height (/
                                                     (:height sel-rect)
                                                     (:height pcr))

                                        pct-left (/
                                                   (- (:left sel-rect) (:left pcr))
                                                   (:width pcr))
                                        pct-top (/
                                                  (- (:top sel-rect) (:top pcr))
                                                  (:height pcr))
                                        sel-pct-rect {:width pct-width
                                                      :height pct-height
                                                      :left pct-left
                                                      :top pct-top}]

                                    sel-pct-rect))))]))
             (remove nil?)
             (into {}))

        last-sel {:collapsed? (browser/sel-collapsed?)
                  :rects (->> index->pct-rects
                              (sort-by first)
                              (mapcat
                                (fn [[page-index rects]]
                                  (->> rects
                                       (map (fn [rect]
                                              (merge
                                                rect
                                                {:page-index page-index}))))))
                              vec)
                  :bounds (->> index->pct-rects
                               (sort-by first)
                               (map (fn [[page-index rects]]
                                      (merge
                                        {:page-index page-index}
                                        (rects->bounds rects)))))}]

    (reset! !last-sel last-sel)

    (when-let [f (:on-change-selection @!opts)]
      (f last-sel state))))

(defn handle-sel-change [{:keys [!viewing-ref
                                 !content-ref
                                 !index->page-content-rect
                                 !last-sel
                                 !opts] :as state}]
  
  (let [vr (viewing-rect @!viewing-ref)
        cr (browser/bounding-client-rect @!content-ref)
        
        rects (->> (browser/sel-bounding-rects)
                   (remove #(or (= 0 (:width %))
                                (= 0 (:height %)))))

        _ (dprintln
            :doc-selection
            "Selection rects count:"
            (count rects))

        index->pct-rects
        (->> rects
             distinct
             (map
               (fn [rect]
                 (when-let [page-index (page-index-from-rect state rect)]
                   [page-index rect])))
             (remove nil?)
             (group-by first)
             (map (fn [[page-index page-index+rects]]
                    [page-index (mapv second page-index+rects)]))
             (map (fn [[page-index rects]]
                    [page-index
                     (->> rects
                          (mapv (fn [sel-rect]
                                  (let [pcr (get @!index->page-content-rect page-index)

                                        pct-width (/
                                                    (:width sel-rect)
                                                    (:width pcr))
                                        
                                        pct-height (/
                                                     (:height sel-rect)
                                                     (:height pcr))

                                        pct-left (/
                                                   (- (:left sel-rect) (:left pcr))
                                                   (:width pcr))
                                        pct-top (/
                                                  (- (:top sel-rect) (:top pcr))
                                                  (:height pcr))
                                        sel-pct-rect {:width pct-width
                                                      :height pct-height
                                                      :left pct-left
                                                      :top pct-top}]

                                    sel-pct-rect))))]))
             (filter #(and (>= (:left %) 0)
                           (>= (:top %) 0)))
             (remove nil?)
             (into {}))

        last-sel {:collapsed? (browser/sel-collapsed?)
                  :rects (->> index->pct-rects
                              (sort-by first)
                              (mapcat
                                (fn [[page-index rects]]
                                  (->> rects
                                       (map (fn [rect]
                                              (merge
                                                rect
                                                {:page-index page-index}))))))
                              vec)
                  :bounds (->> index->pct-rects
                               (sort-by first)
                               (map (fn [[page-index rects]]
                                      (merge
                                        {:page-index page-index}
                                        (rects->bounds rects)))))}]

    (reset! !last-sel last-sel)

    (when-let [f (:on-change-selection @!opts)]
      (f last-sel state))))

(<defn <initialize-document [{:keys [!viewing-ref
                                     !content-ref
                                     !interaction
                                     !index->page-content-el
                                     !index->page-content-rect
                                     render-text-layer-ch]
                              :as state} event]
  (let [content-rect (browser/bounding-client-rect @!content-ref)
        viewing-rect (viewing-rect @!viewing-ref)
        
        [origin-left-px origin-top-px]
        (calc-transform-origin viewing-rect content-rect)

        pages (pages-in-order state)
        page-indexes (-visible-page-indexes pages content-rect viewing-rect)

        index->page-content-rect
        (->> @!index->page-content-el
             (map (fn [[index el]]
                    [index (browser/bounding-client-rect el)]))
             (into {}))]
    
    (swap! !interaction assoc
      :origin-left-px origin-left-px
      :origin-top-px origin-top-px)

    (reset! !index->page-content-rect index->page-content-rect)
    
    (<? (load-page-fill-rect state))

    (doseq [pi page-indexes]
      (put! render-text-layer-ch pi))))

(<defn <handle-zoom-start [_
                           {:keys [!content-ref
                                   !viewing-ref
                                   !interaction]
                            :as state}]
  (let [vr (viewing-rect @!viewing-ref)
        cr (browser/bounding-client-rect @!content-ref)
        [origin-left-px origin-top-px]
        (calc-transform-origin vr cr)

        [left-pct top-pct]
        [(/ origin-left-px (:width cr))
         (/ origin-top-px (:height cr))]

        pages (pages-in-order state)
        page-indexes (-visible-page-indexes pages cr vr)]

    (swap! !interaction assoc
      :zoom-left-pct left-pct
      :zoom-top-pct top-pct
      :starting-width (:width cr)
      :starting-height (:height cr)
      :origin-left-px origin-left-px
      :origin-top-px origin-top-px
      :zoom-active? true)))


(<defn <handle-zoom [_
                     {:keys [!content-ref
                             !content-rect
                             !viewing-ref
                             !index->page
                             !index->img-src
                             !index->initial-page-rect
                             !initial-content-rect
                             !loaded-page-indexes
                             render-queue-ch
                             !interaction
                             !opts
                             !zoom
                             render-text-layer-ch]
                      :as state}
                     {zoom-scale :aggregate
                      zoom-delta :delta
                      zoom-initial :initial
                      :as zoom-data}]

  (let [viewing-rect (viewing-rect @!viewing-ref)
        content-rect (select-keys
                       (browser/bounding-client-rect @!content-ref)
                       [:width :height])

        {:keys [zoom-left-pct zoom-top-pct
                starting-width starting-height]} @!interaction

        pages (pages-in-order state)

        {initial-width :width
         initial-height :height}
        @!initial-content-rect

        [left-pct top-pct]
        [zoom-left-pct zoom-top-pct]

        [scroll-left-px scroll-top-px]
        [(- (* left-pct initial-width zoom-scale)
            (/ (:width viewing-rect) 2))
         (- (* top-pct initial-height zoom-scale)
            (/ (:height viewing-rect) 2))]

        [origin-left-px origin-top-px]
        (calc-transform-origin viewing-rect content-rect)

        page-indexes (-visible-page-indexes
                       pages
                       content-rect
                       viewing-rect)

        missing-image-indexes
        (->> page-indexes
             (remove
               #(get @!index->img-src %)))

        page+widths (->> page-indexes
                         (map (fn [i]
                                [(nth pages i) (:width viewing-rect)])))

        {:keys [on-debug-frame]} @!opts]

    (swap! !loaded-page-indexes
      (fn [vpis]
        (set
          (apply
            conj
            vpis
            page-indexes))))

    (swap! !zoom assoc :scale zoom-scale)

    #_(println zoom-scale "\n" scroll-left-px "\n" scroll-top-px)

    (.scrollTo
      @!viewing-ref
      (clj->js
        {:top scroll-top-px
         :left scroll-left-px
         :behavior "auto"}))

    (swap! !interaction
      merge
      {:origin-left-px origin-left-px
       :origin-top-px origin-top-px
       :scroll-left-px scroll-left-px
       :scroll-top-px scroll-top-px})
    

    #_(set! (.-scrollLeft @!viewing-ref) scroll-left-px)
    #_(set! (.-scrollTop @!viewing-ref) scroll-top-px)

    (doseq [pi page-indexes]
      (put! render-text-layer-ch pi))

    #_(reset! !content-rect content-rect)

    #_(when on-debug-frame
        (on-debug-frame
          (merge
            {:ts (ks/now)
             :viewing-rect viewing-rect
             :content-rect content-rect
             :interaction @!interaction
             :zoom @!zoom
             :event event}
            (when (and scroll? active?)
              {:set-scroll {:scroll-left-px scroll-left-px
                            :scroll-top-px scroll-top-px}}))))

    (when-let [f (:on-change-zoom @!opts)]
      (f (:scale @!zoom) state))

    ))


(<defn <handle-zoom-end [_
                         {:keys [!interaction
                                 !index->page-content-el
                                 !index->page-content-rect]
                          :as state}
                         _]
  (let [index->page-content-rect
        (->> @!index->page-content-el
             (map (fn [[index el]]
                    [index (browser/bounding-client-rect el)]))
             (into {}))]
    (reset! !index->page-content-rect index->page-content-rect)
    (swap! !interaction
      assoc
      :zoom-active? false)

    (<? (browser/<raf))

    (handle-sel-change state)))

(defn set-zoom [{:keys [!content-ref
                        !viewing-ref
                        !interaction
                        !initial-content-rect
                        !zoom
                        !opts]
                 :as state}
                zoom-scale]
  (gol
    (when-not (:zoom-active? @!interaction)
      (let [zoom-scale (ks/clamp zoom-scale
                         (or (:min-zoom @!opts) 0.5)
                         (or (:max-zoom @!opts) 3))
            viewing-rect (viewing-rect @!viewing-ref)
            content-rect (browser/bounding-client-rect @!content-ref)
            
            {initial-width :width
             initial-height :height}
            @!initial-content-rect

            [origin-left-px origin-top-px]
            (calc-transform-origin viewing-rect content-rect)

            [left-pct top-pct]
            [(/ origin-left-px (:width content-rect))
             (/ origin-top-px (:height content-rect))]

            [scroll-left-px scroll-top-px]
            [(- (* left-pct initial-width zoom-scale)
                (/ (:width viewing-rect) 2))
             (- (* top-pct initial-height zoom-scale)
                (/ (:height viewing-rect) 2))]]

        

        (swap! !zoom assoc :scale zoom-scale)
        (swap! !interaction
          merge
          {:origin-left-px origin-left-px
           :origin-top-px origin-top-px
           :zoom-left-pct left-pct
           :zoom-top-pct top-pct
           :starting-width (:width content-rect)
           :starting-height (:height content-rect)
           :scroll-left-px scroll-left-px
           :scroll-top-px scroll-top-px})

        (when-let [f (:on-change-zoom @!opts)]
          (f (:scale @!zoom) state))

        (<! (timeout 1))

        (.scrollTo
          @!viewing-ref
          (clj->js
            {:top scroll-top-px
             :left scroll-left-px
             :behavior "auto"}))))))

(defn zoom-in [{:keys [!zoom] :as state} & [amount]]
  (set-zoom state (+
                    (or (:scale @!zoom) 1)
                    (or amount 0.25))))

(defn zoom-out [{:keys [!zoom] :as state} & [amount]]
  (set-zoom state (-
                    (or (:scale @!zoom) 1)
                    (or amount 0.25))))

(defn page-wrapper-rects [{:keys [!index->page-wrapper-el]}]
  (->> @!index->page-wrapper-el
       (sort-by first)
       (map second)
       (map browser/bounding-client-rect)
       vec))

(<defn <handle-scroll [_
                       {:keys [!content-ref
                               !content-rect
                               !viewing-ref
                               !index->page
                               !index->img-src
                               !loaded-page-indexes
                               !visible-page-indexes
                               render-queue-ch
                               !interaction
                               !opts
                               !zoom]
                        :as state}
                       {zoom-scale :aggregate
                        zoom-delta :delta
                        zoom-initial :initial}]

  (let [viewing-rect (viewing-rect @!viewing-ref)
        content-rect (select-keys
                       (browser/bounding-client-rect @!content-ref)
                       [:width :height])

        pages (pages-in-order state)

        page-rects (page-wrapper-rects state)

        page-indexes (-visible-page-indexes-rects
                       page-rects
                       content-rect
                       viewing-rect)

        missing-image-indexes
        (->> page-indexes
             (remove
               #(get @!index->img-src %)))

        page+widths (->> page-indexes
                         (map (fn [i]
                                [(nth pages i) (:width viewing-rect)])))


        [origin-left-px origin-top-px]
        (calc-transform-origin viewing-rect content-rect)

        [left-pct top-pct]
        [(/ origin-left-px (:width content-rect))
         (/ origin-top-px (:height content-rect))]

        {:keys [on-debug-frame]} @!opts]

    (swap! !loaded-page-indexes
      (fn [vpis]
        (set
          (apply
            conj
            vpis
            page-indexes))))

    (swap! !interaction
      assoc
      :origin-left-px origin-left-px
      :origin-top-px origin-top-px)

    (queue-render state page-indexes
      (:width viewing-rect))

    (let [{:keys [on-change-page-indexes]} @!opts]
      (when on-change-page-indexes
        (on-change-page-indexes page-indexes state)))))

(defn <handle-resize-end [_
                          {:keys [!zoom
                                  !viewing-ref
                                  !content-ref
                                  !initial-content-rect
                                  !visible-page-indexes
                                  !index->page-content-el
                                  !index->page-content-rect]
                           :as state}
                          _]
  (let [zoom-scale (or (:scale @!zoom) 1)
        viewing-rect (viewing-rect @!viewing-ref)
        content-rect (browser/bounding-client-rect @!content-ref)
        page-indexes (-visible-page-indexes-rects
                       (page-wrapper-rects state)
                       content-rect
                       viewing-rect)

        index->page-content-rect
        (->> @!index->page-content-el
             (map (fn [[index el]]
                    [index (browser/bounding-client-rect el)]))
             (into {}))]
    (reset! !index->page-content-rect index->page-content-rect)
    (reset! !visible-page-indexes page-indexes)
    (reset! !initial-content-rect
      (scale-rect
        (/ 1 zoom-scale)
        content-rect))))

(defn create-event-handlers [opts state]
  (->> [:on-zoom <handle-zoom
        :on-zoom-start <handle-zoom-start
        :on-zoom-end <handle-zoom-end
        :on-scroll <handle-scroll
        :on-resize-end <handle-resize-end]
       (partition 2)
       (map (fn [[k f]]
              [k (fn [pl]
                   (f opts state pl))]))
       (into {})))

(defn doc-render-page-header [{:keys [render-page-header]} header-opts]
  (when render-page-header
    [:div.doc-user-page-header
     (when (debug-enabled? :user-content)
       {:style {:border "solid blue 1px"
                :position 'relative}})
     
     [render-page-header header-opts]
     (when (debug-enabled? :user-content)
       [:div {:style {:position 'absolute
                      :top 2
                      :right 2
                      :font-size 11
                      :opacity 0.5
                      :line-height 1}}
        "page header"])]))

(defn doc-render-page-footer [{:keys [render-page-footer]} footer-opts]
  (when render-page-footer
    [:div.doc-user-page-footer
     (when (debug-enabled? :user-content)
       {:style {:border "solid blue 1px"
                :position 'relative}})
     
     [render-page-footer footer-opts]
     (when (debug-enabled? :user-content)
       [:div {:style {:position 'absolute
                      :top 2
                      :right 2
                      :font-size 11
                      :opacity 0.5
                      :line-height 1}}
        "page footer"])]))

(defn doc-render-page-overlay [{:keys [render-page-overlay]}
                               {:keys [!index->page-content-el
                                       !visible-page-indexes
                                       !loaded-page-indexes]
                                :as state}
                               {:keys [page-index] :as opts}]
  (when (and render-page-overlay
             (get (set @!loaded-page-indexes) page-index))
    (let [opts opts]
      [:div
       (when (debug-enabled? :user-content)
         [:div {:style {:position 'absolute
                        :top 0
                        :left 0
                        :right 0
                        :bottom 0
                        :border "solid green 1px"
                        :pointer-events 'none
                        :text-align 'right
                        :font-size 11
                        :color "rgba(0,0,0,0.5"
                        :padding 2}}
          "page overlay"])
       [:div {:style (merge
                       {:position 'absolute
                        :top 0
                        :left 0
                        :right 0
                        :bottom 0
                        :pointer-events 'none})}
        
        [render-page-overlay opts]]])))

(defn doc-render-document-overlay [{:keys [render-document-overlay]} overlay-opts]
  (when render-document-overlay
    [:div {:style {:position 'absolute
                   :top 0
                   :left 0
                   :right 0
                   :bottom 0
                   :pointer-events 'none}}
     [render-document-overlay overlay-opts]]))

(defn doc-render-right-page-gutter [{:keys [render-right-page-gutter
                                            right-page-gutter-style
                                            page-inset
                                            right-gutter-visible?]}
                                    {:keys [!zoom]}
                                    child-opts]
  (when (and render-right-page-gutter
             right-gutter-visible?)
    [:div.doc-user-right-page-gutter
     (ks/deep-merge
       {:style (merge
                 {:margin-top (* page-inset
                                (or (:scale @!zoom) 1))
                  :margin-bottom (* page-inset
                                   (or (:scale @!zoom) 1))
                  :position 'relative}
                 right-page-gutter-style)}
       (when (debug-enabled? :user-content)
         {:style {:border "solid blue 1px"
                  :position 'relative}}))
     [render-right-page-gutter (merge child-opts
                                 {:zoom-scale (:scale @!zoom)})]
     (when (debug-enabled? :user-content)
       [:div {:style {:position 'absolute
                      :top 2
                      :right 2
                      :font-size 11
                      :opacity 0.5
                      :line-height 1}}
        "right gutter"])]))

(defn page-content-event-handlers [{:keys [on-mouse-move
                                           on-click
                                           on-mouse-down
                                           on-mouse-up
                                           on-mouse-over
                                           on-mouse-out]}
                                   {:keys [!index->page-content-rect]}
                                   {:keys [page-index]}]
  (let [event-payload (fn [e]
                        (let [rect (get @!index->page-content-rect page-index)
                              client-x (.-clientX e)
                              client-y (.-clientY e)
                              local-x (- client-x (:left rect))
                              local-y (- client-y (:top rect))

                              left-pct (/ local-x (:width rect))
                              top-pct (/ local-y (:height rect))]
                          {:page-index page-index
                           :page-rect rect
                           :left-pct left-pct
                           :top-pct top-pct
                           :client-x client-x
                           :client-y client-y}))]
    (merge
      (when on-mouse-move
        {:on-mouse-move (fn [e]
                          (on-mouse-move (event-payload e)))})
      (when on-click
        {:on-click (fn [e]
                     (on-click (event-payload e)))})
      (when on-mouse-down
        {:on-mouse-down (fn [e]
                          (on-mouse-down (event-payload e)))})
      (when on-mouse-up
        {:on-mouse-up (fn [e]
                        (on-mouse-up (event-payload e)))})
      (when on-mouse-over
        {:on-mouse-over (fn [e]
                          (on-mouse-over (event-payload e)))})
      (when on-mouse-out
        {:on-mouse-out (fn [e]
                         (on-mouse-out (event-payload e)))}))))

(defn document [{:keys [render-cache-state
                        zoom-enabled?
                        text-layer?
                        on-comp]
                 :as opts}]
  
  (let [{:keys [!content-ref
                !initial-content-rect
                !index->page
                !index->img-src
                !index->img-viewport-rect
                !index->page-content-el
                !resizing-timeout
                !resizing-viewing-rect
                !resizing-content-rect
                !viewing-ref
                
                !loaded-page-indexes
                !lifo

                !zoom
                !interaction
                !timeouts
                !handle-scroll?]
         :as state}
        (merge
          (create-document-state opts)
          render-cache-state)

        internal-event-handlers (create-event-handlers opts state)

        {:keys [zoom-ch
                scroll-ch
                resize-ch]}
        (setup-event-handling
          (merge opts internal-event-handlers))

        on-resize (fn [e]
                    (put! resize-ch
                      {:width (.-innerWidth js/window)
                       :height (.-innerHeight js/window)}))

        on-scroll (fn [e]
                    (put! scroll-ch
                      {:scroll-top (.-scrollTop @!viewing-ref)
                       :scroll-left (.-scrollLeft @!viewing-ref)}))

        on-sel-change (fn [e]
                        (handle-sel-change state))]

    (when on-comp
      (on-comp state))

    (when-let [f (:on-comp opts)]
      (f state))

    (r/create-class
      (->
        {:component-did-mount
         (fn [this]
           (gol
             (let [viewing-rect (scroller-rect @!viewing-ref)
                   so (scroll-offset @!viewing-ref)
                   origin-left-px (+ (/ (:width viewing-rect) 2) (:left so))
                   origin-top-px (+ (/ (:height viewing-rect) 2) (:top so))]

               
                 
               (swap! !interaction
                 merge
                 {:origin-left-px origin-left-px
                  :origin-top-px origin-top-px})

               (reset! !resizing-viewing-rect viewing-rect)

               (reset! !index->page
                 (->> (<? (<load-pages (:pdf opts)))
                      (map-indexed
                        (fn [i page]
                          [i page]))
                      (into {}))))

             (.addEventListener @!viewing-ref "scroll" on-scroll)
             (.addEventListener js/window "resize" on-resize)
             (.addEventListener js/document "selectionchange" on-sel-change)
             
             (<? (browser/<raf))
               
             (reset! !resizing-content-rect (browser/bounding-client-rect @!content-ref))
             (reset! !initial-content-rect (browser/bounding-client-rect @!content-ref))

             (<initialize-document state nil)
             (text-layer-start-loop opts state)))

         :component-did-update
         (browser/cdu-diff
           (fn [[{old-set-zoom :set-zoom
                  old-right-gutter-visible? :right-gutter-visible?}]
                [{new-set-zoom :set-zoom
                  new-right-gutter-visible? :right-gutter-visible?}]]
             (when (not= old-set-zoom new-set-zoom)
               (set-zoom state (:scale new-set-zoom)))
             (when (not= old-right-gutter-visible?
                         new-right-gutter-visible?)
               (<handle-resize-end nil state nil))))
         
         :component-will-unmount
         (fn []
           (.removeEventListener @!viewing-ref "scroll" on-scroll)
           (.removeEventListener js/window "resize" on-resize)
           (.removeEventListener js/document "selectionchange" on-sel-change)
           (reset! !lifo []))
           
         :reagent-render
         (fn [{:keys [debug?
                      style
                      page-style
                      content-style
                      selected-page-style
                      page-inset
                      invert-colors?]
               :as opts}]
           [:div
            {:style {:width "100%"
                     :height "100%"
                     :position 'relative}}
            [:div
             {:style (merge
                       style
                       {:width "100%"
                        :position 'relative}
                       {:height "100%"
                        :overflow 'scroll})
              :on-wheel
              (fn [e]
                (when zoom-enabled?
                  (if (.-ctrlKey e)
                    (put! zoom-ch (* (.-deltaY e) 1.2)))))
              :ref #(when % (reset! !viewing-ref %))}
             (let [{:keys [!index->page
                           !index->img-src
                           !index->mouse-down?
                           !index->viewport-rect
                           !index->page-wrapper-el
                           !index->initial-page-rect
                           !content-ref
                           !loaded-page-indexes
                           !resizing-offset
                           !content-height
                           !content-ref
                           !content-rect
                           !interaction
                           !viewing-ref]}
                   state

                   page-inset (or page-inset 0)

                   selected-page-indexes (set (:selected-page-indexes opts))

                   overlay-opts {}]

               (let [width-pct (if-let [pct (:scale @!zoom)]
                                 (str (* pct 100) "%")
                                 "100%")]
                 (into
                   [:div.pdf-content-ref
                    {:ref #(when % (reset! !content-ref %))
                     :style (merge
                              {:width width-pct
                               :overflow 'hidden
                               :position 'relative}
                              {:margin-left 'auto
                               :margin-right 'auto}
                              #_{:transform-origin
                                 (if (:active? @!interaction)
                                   (str (:origin-left-px @!interaction) "px"
                                        " "
                                        (:origin-top-px @!interaction) "px")
                                   "0 0")}
                              content-style)}]
                   (concat
                     (->> @!index->page
                          (sort-by first)
                          (map (fn [[page-index page]]
                                 (let [aspect-ratio (page-aspect-ratio page)
                                       child-opts {:page-index page-index
                                                   :page page}]
                                   [:div {:style {:display 'flex
                                                  :flex-direction 'row}}
                                    [:div.page-wrapper-el
                                     (merge
                                       {:style (merge
                                                 {:flex 1
                                                  :width "100%"
                                                  :padding (* page-inset
                                                             (or (:scale @!zoom) 1))}
                                                 (when (:on-choose-page opts)
                                                   {:cursor 'pointer})
                                                 (when (get selected-page-indexes page-index)
                                                   selected-page-style)
                                                 (:page-wrapper-style opts))
                                        :ref #(when % (swap! !index->page-wrapper-el assoc page-index %))}
                                       (when-let [f (:on-choose-page opts)]
                                         {:on-click (fn [e]
                                                      (f {:page-index page-index} e))}))
                                     (doc-render-page-header opts child-opts)
                                     [:div
                                      {:key page-index
                                       :style {:width "100%"
                                               :padding-top (str (* (/ 1 aspect-ratio) 100) "%")
                                               :position 'relative
                                               :overflow 'hidden}}
                                      [:div
                                       {:style {:position 'absolute
                                                :top 0
                                                :left 0
                                                :right 0
                                                :bottom 0
                                                :display 'flex}}
                                       [:div.page-content
                                        (merge
                                          {:style (merge
                                                    {:position 'relative
                                                     :width "100%"
                                                     :height "100%"
                                                     :flex 1}
                                                    (:page-content-style opts))
                                           :ref #(when %
                                                   (swap! !index->page-content-el assoc page-index %)
                                                   (swap! !index->initial-page-rect
                                                     assoc
                                                     page-index
                                                     (browser/bounding-client-rect %)))}
                                          (page-content-event-handlers opts state {:page-index page-index}))
                                        (if (and (get @!loaded-page-indexes page-index)
                                                 (get @!index->img-src page-index))
                                          [:div.page-content-render-layer
                                           {:style (merge
                                                     {:position 'absolute
                                                      :top 0
                                                      :left 0
                                                      :right 0
                                                      :bottom 0}
                                                     (when invert-colors?
                                                       {:filter "invert(1)"}))}
                                           (let [img-src (get @!index->img-src page-index)]
                                             [:img {:src img-src
                                                    :style {:display 'block
                                                            :width "100%"
                                                            :height "100%"
                                                            :pointer-events 'none
                                                            :user-select 'none}}])]
                                          [:div {:style {:position 'absolute
                                                         :top 0
                                                         :left 0
                                                         :right 0
                                                         :bottom 0
                                                         :display 'flex
                                                         :align-items 'center
                                                         :justify-content 'center
                                                         :opacity 0.5
                                                         :font-size 12}}
                                           "loading..."])
                                        (when (render-text-layer? opts)
                                          [render-text-layer (text-layer-opts state page-index)])
                                        (doc-render-page-overlay opts state child-opts)]]]
                                     (doc-render-page-footer opts child-opts)]
                                    (doc-render-right-page-gutter opts state child-opts)])))
                          doall)

                     (doc-render-document-overlay opts overlay-opts)

                     (when (debug-enabled? :page-rendering)
                       [[:div {:style {:position 'absolute
                                       :top 0
                                       :left (:origin-left-px @!interaction)
                                       :width 1
                                       :bottom 0
                                       :background-color 'blue}}]
                        [:div {:style {:position 'absolute
                                       :top (:origin-top-px @!interaction)
                                       :left 0
                                       :right 0
                                       :height 1
                                       :background-color 'blue}}]

                        [:div {:style {:position 'absolute
                                       :top 0
                                       :left (str (* 100 (:zoom-left-pct @!interaction)) "%")
                                       :width 1
                                       :bottom 0
                                       :background-color 'green}}]
                        [:div {:style {:position 'absolute
                                       :top (str (* 100 (:zoom-top-pct @!interaction)) "%")
                                       :left 0
                                       :right 0
                                       :height 1
                                       :background-color 'green}}]])))))]
            (when (debug-enabled? :page-rendering)
              [:div {:style {:position 'absolute
                             :bottom 0
                             :left 0
                             :background-color 'white
                             :padding 5
                             :font-size 10
                             :max-width "100%"}}
               [:pre (ks/pp-str @!zoom)]
               [:pre (ks/pp-str @!interaction)]
               ])])}))))

(defn header-btn [opts]
  [btn/button
   (merge
     opts
     {:style {:border-radius 4
              :background-color "#ccc"
              :padding "4px 12px"
              :cursor 'pointer}
      :hover-style {:background-color "#bbb"}
      :active-style {:background-color "#aaa"}})])

(defn viewer-render-header [state]
  (let [{:keys [!opts]} state
        {:keys [header-style]} @!opts]
    [:div
     {:style (merge
               {:background-color "#ddd"
                :border-bottom "solid #ccc 1px"}
               header-style)}
     [ui/g
      {:horizontal? true
       :style {:justify-content 'space-between}}
      [:div ""]
      [ui/g
       {:horizontal? true
        :gap 4
        :style {:padding 8}}
       [header-btn
        {:before [fi/zoom-in
                  {:size 16}]
         :on-click (fn []
                     (prn "CL"))}]
       [header-btn
        {:before [fi/zoom-out
                  {:size 16}]
         :on-click (fn []
                     (prn "CL"))}]]]]))

(defn viewer [opts]
  (let [!selected-page-index (r/atom 0)
        !minimap-comp (atom nil)
        !viewer-comp (atom nil)
        !set-zoom (atom {:scale 1 :ts (ks/now)})
        !opts (atom opts)

        state {:!selected-page-index !selected-page-index
               :!minimap-comp !minimap-comp
               :!viewer-comp !viewer-comp
               :!set-zoom !set-zoom
               :!opts !opts}]
    (r/create-class
      {:reagent-render
       (fn [opts]
         (let [rc-state (render-cache-state)]
           [:div
            {:style {:display 'flex
                     :width "100%"
                     :height "100%"
                     :flex-direction 'column
                     :overflow 'hidden}}
            #_(viewer-render-header state)
            [:div
             {:style {:flex 1
                      :display 'flex
                      :flex-direction 'row
                      :overflow 'hidden}}
             [ui/group
              {:class "rx-minimap-container"
               :style (merge
                        {:width 200
                         :border-right "solid #ccc 1px"}
                        (:minimap-container-style opts))}
              [document
               (merge
                 (select-keys
                   opts
                   [:pdf :invert-colors?])
                 {:on-comp (fn [comp]
                             (reset! !minimap-comp comp))
                  :page-inset 10
                  :content-style
                  {:background-color "#ddd"
                   :padding-left 20
                   :padding-right 20}
                  
                  :render-cache-state rc-state
                  :render-page-footer
                  (fn [{:keys [page-index]}]
                    [:div
                     {:style (merge
                               {:font-size 12
                                :text-align 'center
                                :margin-top 5
                                :margin-bottom 5})}
                     [:div
                      {:style (merge
                                {:display 'inline-block
                                 :width 30
                                 :border-radius 4
                                 :color "#777"}
                                (:minimap-page-number-style opts)
                                (when (= page-index @!selected-page-index)
                                  (merge
                                    {:background-color "#ccc"
                                     :color 'black}
                                    (:minimap-page-number-selected-style opts))))}
                      (inc page-index)]])

                  :on-choose-page (fn [{:keys [page-index]}]
                                    (reset! !selected-page-index page-index)
                                    (jump-to-page-index @!viewer-comp page-index))}
                 (:minimap-opts opts))]]
             [:div
              {:style {:display 'flex
                       :flex 1}}
              [document
               (merge
                 opts
                 {:on-comp (fn [comp]
                             (reset! !viewer-comp comp)
                             (when-let [f (:on-document-comp opts)]
                               (f comp)))
                  :zoom-enabled? true
                  :text-layer? true
                  :style {:background-color "#f0f0f0"}
                  :page-inset 40
                  :content-style {:background-color "#f0f0f0"}
                  :render-cache-state rc-state
                  :set-zoom @!set-zoom
                  :on-change-zoom
                  (fn [v]
                    (swap! !set-zoom assoc :scale v))
                  :on-change-page-indexes
                  (fn [idxs]
                    (when-let [idx (first idxs)]
                      (reset! !selected-page-index idx)
                      (ensure-page-index-visible @!minimap-comp idx)))}
                 (:document-opts opts))]]]]))})))

(defn debug-document-state-frames-list [{:keys [!frames
                                                on-choose-frame]}]
  [ui/g
   {:style {:flex 1
            :overflow-y 'scroll}}
   [ui/list-container
    (->> @!frames
         (map (fn [frame]
                [:div
                 {:style {:padding 10
                          :cursor 'pointer}
                  :on-click (fn [e]
                              (on-choose-frame frame))}
                 (ks/date-format
                   (:ts frame)
                   "hh:mm:ss.SSS")
                 " - "
                 (first (:event frame))])))]])

(defn debug-document-state-frame [{:keys [!current-frame]}]
  (let [{:keys [interaction
                viewing-rect
                content-rect
                set-scroll]}
        @!current-frame]
    [ui/g
     {:style {:padding 8
              :overflow-y 'scroll
              :flex 1}}
     [ui/g
      {:gap 12}

      [ui/g {:gap 4}
       [ui/label "Interaction"]
       [ui/g {:gap 4}
        (->> interaction
             (map (fn [[k v]]
                    [:div (pr-str k) " " v])))]]
      
      [ui/g {:gap 4}
       [ui/label "Viewing rect"]
       [ui/g {:gap 4}
        (->> viewing-rect
             (map (fn [[k v]]
                    [:div (pr-str k) " " v])))]]

      [ui/g {:gap 4}
       [ui/label "Content rect"]
       [ui/g {:gap 4}
        (->> content-rect
             (map (fn [[k v]]
                    [:div (pr-str k) " " v])))]]

      (when set-scroll
        [ui/g {:gap 4}
         [ui/label "Scroll"]
         [ui/g {:gap 4}
          (->> set-scroll
               (map (fn [[k v]]
                      [:div (pr-str k) " " v])))]])
      ]
     [:pre (->> @!current-frame
                keys
                (interpose "\n")
                (apply str))]

     #_[:pre (ks/pp-str @!current-frame)]]))

(defonce !frames (r/atom '()))
(defonce !current-frame (r/atom nil))

(defn test-document [opts]
  (let [state {:!current-frame !current-frame
               :!frames !frames}]

    (reset! !frames '())
    (reset! !current-frame nil)
    
    (r/create-class
      {:reagent-render
       (fn []
         [:div {:style {:width "100%"
                        :height "100%"
                        :display 'flex
                        :flex-direction 'row
                        :overflow 'hidden}}
          [:div
           {:style {:flex 1}}
           [document (merge
                       opts
                       {:on-debug-frame (fn [frame]
                                          (swap! !frames conj frame)
                                          (when-not @!current-frame
                                            (reset! !current-frame frame)))})]]
          [:div
           {:style {:width 300
                    :display 'flex
                    :flex-direction 'column}}
           [:div
            {:style {:padding 5}}
            [btn/button {:label "Clear"
                         :on-click (fn []
                                     (reset! !frames '())
                                     (reset! !current-frame nil))}]]
           [:div {:style {:flex 1
                          :overflow 'hidden
                          :display 'flex}}
            [debug-document-state-frames-list
             (merge
               opts
               state
               {:on-choose-frame (fn [frame] (reset! !current-frame frame))})]]
           [:div {:style {:flex 2
                          :overflow 'hidden
                          :display 'flex}}
            [debug-document-state-frame (merge opts state)]]]])})))


(defn test-basic []
  (gol
    (let [pdf (<? (<pdf-obj
                    (or
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [document
         {:pdf pdf
          :debug? false
          :page-inset 20
          :zoom-enabled? true
          :style {:background-color "#f0f0f0"}
          :content-style {:background-color "#f0f0f0"}}]))))

(defn test-stress []
  (gol
    (let [pdf (<! (<pdf-obj
                    "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                    #_"https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf"))]
      (browser/<show-component!
        [:div
         {:style {:width "100%"
                  :height "100%"}}
         [document {:pdf pdf
                    :debug? false}]]))))

(defn test-viewer []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      "http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [viewer
         {:pdf pdf}]))))

(defn test-viewer-stress []
  (gol
    (let [pdf (<! (<pdf-obj
                    "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                    #_"https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf"))]
      (browser/<show-component!
        [viewer
         {:pdf pdf}]))))

(defn test-text-layer []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [:div
         {:style {:width "100%"
                  :height "100%"}}
         [document {:pdf pdf
                    :debug? false
                    :page-inset 20
                    :zoom-enabled? true
                    :text-layer? true
                    :style {:background-color "#f0f0f0"}
                    :content-style {:background-color "#f0f0f0"}}]]))))


(defn test-user-events []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [:div
         {:style {:width "100%"
                  :height "100%"}}
         [document
          {:pdf pdf
           :debug? false
           :page-inset 20
           :zoom-enabled? true
           :text-layer? true
           :style {:background-color "#f0f0f0"}
           :content-style {:background-color "#f0f0f0"}

           :on-click (partial prn "click")
           :on-mouse-move (partial prn "mouse-move")
           :on-mouse-down (partial prn "mouse-down")
           :on-mouse-up (partial prn "mouse-up")
           :on-mouse-over (partial prn "mouse-over")
           :on-mouse-out (partial prn "mouse-out")}]]))))


(defn test-user-content []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [:div
         {:style {:width "100%"
                  :height "100%"}}
         [document
          {:pdf pdf
           :debug? false
           :page-inset 20
           :zoom-enabled? true
           :text-layer? true
           :style {:background-color "#f0f0f0"}
           :content-style {:background-color "#f0f0f0"}

           :render-page-header
           (fn []
             [:div "header"])
           :render-page-footer
           (fn []
             [:div "footer"])
           :render-page-overlay
           (fn [data]
             [:div {:style {:position 'absolute
                            :top 0
                            :left 0}}
              [:pre (ks/pp-str data)]])}]]))))


(defn test-zoom-api []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))
          !zoom (r/atom {:scale 1 :ts (ks/now)})]
      (browser/<show-component!
        [(fn []
           [:div {:style {:width "100%"
                          :height "100%"
                          :position 'relative}}
            [document
             {:pdf pdf
              :debug? false
              :page-inset 20
              :zoom-enabled? true
              :text-layer? true
              :style {:background-color "#f0f0f0"}
              :content-style {:background-color "#f0f0f0"}
              :set-zoom @!zoom
              :on-change-zoom (fn [v]
                                (swap! !zoom assoc :scale v))}]
            [:div {:style {:position 'absolute
                           :top 40
                           :left 20}}
             [forms/range
              {:min 0.5
               :max 3
               :step 0.25
               :!val (r/cursor !zoom [:scale])}]]])]))))

(defn test-comp-interface []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      "http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))

          !doc (r/atom nil)
          !comp (atom nil)
          !data (r/atom nil)]
      (browser/<show-component!
        [(fn []
           [:div
            {:style {:width "100%"
                     :height "100%"
                     :position 'relative}}
            [document
             (merge
               {:pdf pdf
                :on-comp #(reset! !comp %)
                :debug? false
                :page-inset 20
                :zoom-enabled? true
                :text-layer? true
                :style {:background-color "#f0f0f0"}
                :content-style {:background-color "#f0f0f0"}
                :on-change-zoom (fn [v]
                                  (swap! !doc assoc-in [:set-zoom :scale] v))
                :right-page-gutter-style {:width 100}
                :render-right-page-gutter
                (fn [data]
                  [:div {:style {:position 'absolute
                                 :top 100
                                 :width 100
                                 :font-size 13}}
                   "Right Gutter"])}
               @!doc)]
            [:div {:style {:position 'absolute
                           :top 30
                           :left 30
                           :width 200
                           :background-color 'white
                           :padding 10
                           :border "solid #ccc 1px"}}
             [ui/g {:gap 12}
              [:h5 "Comp Interface"]
              [ui/g
               [ui/label "set-zoom"]
               [forms/range
                {:min 0.5
                 :max 3
                 :step 0.1
                 :!val (r/cursor !doc [:set-zoom :scale])}]]
              [ui/g
               [ui/label "jump-to-page-index"]
               [forms/select
                {:options (->> (range (pdf-num-pages pdf))
                               (map (fn [i]
                                      {:label (str i) :value i})))
                 :on-change-value
                 (fn [v]
                   (jump-to-page-index @!comp v))}]]
              [ui/g
               [ui/label "ensure-page-index-visible"]
               [forms/select
                {:options (->> (range (pdf-num-pages pdf))
                               (map (fn [i]
                                      {:label (str i) :value i})))
                 :on-change-value
                 (fn [v]
                   (ensure-page-index-visible @!comp v))}]]
              [ui/g
               [ui/label "visible-page-indexes"]
               [:pre
                {:on-click
                 (fn []
                   (swap! !data assoc :visible-page-indexes
                     (visible-page-indexes @!comp)))}
                (ks/pp-str (:visible-page-indexes @!data))]]
              [ui/g
               [ui/label "right-gutter-visible"]
               [forms/checkbox
                {:!val (r/cursor !doc [:right-gutter-visible?])}]]]]])]))))

(defn test-selection-callbacks []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))

          !bounds (r/atom nil)]
      (browser/<show-component!
        [(fn []
            [:div
             {:style {:width "100%"
                      :height "100%"
                      :position 'relative}}
             [document {:pdf pdf
                        :debug? false
                        :page-inset 20
                        :zoom-enabled? true
                        :text-layer? true
                        :style {:background-color "#f0f0f0"}
                        :content-style {:background-color "#f0f0f0"}
                        :on-change-selection
                        (fn [sel]
                          (reset! !bounds
                            {:bounds (:bounds sel)
                             :rects-count (count (:rects sel))}))}]

             [:div {:style {:position 'absolute
                            :top 30
                            :left 30
                            :width 300
                            :background-color 'white
                            :padding 10
                            :font-size 12
                            :border "solid #ccc 1px"}}
              [ui/g {:gap 12}
               [:h5 "Selection Bounds"]
               [:pre (ks/pp-str @!bounds)]]]])]))))


(defn test-styling []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [(fn []
            [:div
             {:style {:width "100%"
                      :height "100%"
                      :position 'relative}}
             [document {:pdf pdf
                        :debug? false
                        :page-inset 20
                        :zoom-enabled? true
                        :text-layer? true
                        :style {:background-color "red"}
                        :content-style {:background-color "green"}
                        :page-wrapper-style {:border "solid 4px blue"}
                        :page-content-style {:border "solid 4px black"}
                        :render-page-header (fn []
                                              [:div
                                               {:style {:padding 20}}
                                               "header"])}]])]))))

(defn test-right-page-gutter []
  (gol
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf")))]
      (browser/<show-component!
        [:div
         {:style {:width "100%"
                  :height "100%"}}
         [document {:pdf pdf
                    :debug? false
                    :page-inset 20
                    :zoom-enabled? true
                    :text-layer? true
                    :style {:background-color "#f0f0f0"}
                    :content-style {:background-color "#f0f0f0"}
                    :right-page-gutter-style {:width 100}
                    :right-gutter-visible? true
                    :render-right-page-gutter
                    (fn [data]
                      [:div {:style {:position 'absolute
                                     :top 100
                                     :width 100
                                     :font-size 13}}
                       "Right Gutter"])}]]))))


(comment

  ;; Browser
  (do
    (require 'pdfjs)
    (init-pdfjs
      js/pdfjsLib
      {:worker-src "https://unpkg.com/pdfjs-dist@2.6.347/es5/build/pdf.worker.js"}))


  ;; Electron
  (do
    (init-pdfjs
      (js/window.module.require "pdfjs-dist/es5/build/pdf.js")
      {:worker-src "https://unpkg.com/pdfjs-dist@2.6.347/es5/build/pdf.worker.js"}))

  (test-basic)

  (test-stress)

  (test-viewer)

  (test-viewer-stress)

  (test-standalone)

  (test-text-layer)

  (test-user-events)

  (test-user-content)

  (test-zoom-api)

  (test-comp-interface)

  (test-selection-callbacks)

  (test-styling)

  (test-right-page-gutter)

  (go
    (let [pdf (<! (<pdf-obj
                    (or
                      #_"https://www.antennahouse.com/hubfs/xsl-fo-sample/pdf/basic-link-1.pdf?hsLang=en"
                      #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
                      #_(<! (elec/<read-file "/Users/zk/Downloads/themagicoflight.pdf"))
                      "https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
                      "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf"
                      #_(<! (elec/<read-file "/Users/zk/Downloads/sci-am.pdf")))))]
      (ks/pp pdf)))

  )

