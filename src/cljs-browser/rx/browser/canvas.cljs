(ns rx.browser.canvas
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.styleguide :as sg
             :refer-macros [example]]
            [rx.browser.ui :as ui]
            [reagent.core :as r]
            [reagent.ratom
             :refer-macros [run!]]))

(defn setup-ctx [{:keys [context-type
                         context-attributes
                         disable-pixel-ratio-scaling?]}
                 canvas]
  (let [pixel-ratio (.-devicePixelRatio js/window)
        rect (.getBoundingClientRect canvas)
        ctx (.getContext canvas
              (or context-type "2d")
              context-attributes)]
    (when-not disable-pixel-ratio-scaling?
      (set! (.-width canvas) (* (.-width rect) pixel-ratio))
      (set! (.-height canvas) (* (.-height rect) pixel-ratio))
      (when (or (= context-type "2d")
                  (not context-type))
          (.scale ctx pixel-ratio pixel-ratio)))
    {::pixel-ratio pixel-ratio
     ::width (.-width rect)
     ::height (.-height rect)
     ::ctx ctx}))

(defn fill-style
  "Set fill color for subsequent drawing ops."
  [ctx v]
  (set! (.-fillStyle ctx) v))

(defn fill-rect
  "Draw rectangle at `x,y` width width `w` and height `h`."
  [ctx x y w h]
  (.fillRect ctx x y w h))

(defn clear-rect [ctx x y w h]
  (.clearRect ctx x y w h))

(defn stroke-rect [ctx x y w h]
  (.strokeRect ctx x y w h))

(defn fill-text [ctx text x y & [max-width]]
  (.fillText ctx text x y max-width))


;; WebGL

(defn clear [ctx mask]
  (.clear ctx mask))

(defn clear-color [ctx r g b a]
  (.clearColor ctx r g b a))

(defn color-buffer-bit [ctx]
  (.-COLOR_BUFFER_BIT ctx))

(defn depth-buffer-bit [ctx]
  (.-DEPTH_BUFFER_BIT ctx))

(defn stencil-buffer-bit [ctx]
  (.-STENCIL_BUFFER_BIT ctx))



(defn canvas
  {:doc "HTML5 Canvas component"
   :opts
   [[:context-type
     "Type of drawing context to generate. One of `2d`, `webgl`, `webgl2`, `bitmaprenderer`. Defaults to `2d`."]
    [:context-attributes
     "Drawing context [attributes](https://developer.mozilla.org/en-US/docs/Web/API/HTMLCanvasElement/getContext)."]
    [:style "Canvas style attributes."]
    [:render "Function called to draw to the canvas."]]}
  
  [opts]
  (let [{:keys [render
                context-type
                context-attributes]} opts
        !comp (atom {})
        !canvas (atom nil)
        !render (atom render)]
    (r/create-class
      (-> {:on-window-resize
           (fn []
             (reset! !comp (setup-ctx opts @!canvas))
             (when (::ctx @!comp)
               (render @!comp)))
           :component-did-mount
           (fn []
             (reset! !comp (setup-ctx opts @!canvas))
             (run!
               (when (::ctx @!comp)
                 (render @!comp))))
           :component-did-update
           (browser/cdu-diff
             (fn [_ [{:keys [render]}]]
               #_(ks/prn "CDU")
               #_(when render
                   (render @!comp))))
           :reagent-render
           (fn [{:keys [render style]}]
             [:canvas
              (merge
                (dissoc opts
                  :render
                  :style
                  :context-type
                  :context-attributes
                  :disable-pixel-ratio-scaling?)
                {:ref #(when % (reset! !canvas %))
                 :style style})])}
          browser/bind-window-resize))))


(def headings
  [[::canvas :h1 "Canvas"]
   [::example :h2 "Example"]
   [::components :h2 "Components"]
   [::twod-api :h2 "2D API"]])

(def !clicks (r/atom 0))

(defn sections []
  [sg/section-container
   [sg/section-intro
    {}
    [sg/heading headings ::canvas]
    [:p "Wrapper around the HTML5 canvas element that provides easy access to properties typically used in scene drawing like width and height, and setup like dpi"]
    [:h3 "Features"]
    [:ul
     {:style {:list-style-type 'disc
              :padding-left 20}}
     [:li "Canvas width and height passed to your render function"]
     [:li "DPI setup"]
     [:li "Automatic rerender on window resize"]]
    [:p "The canvas component re-renderes when either it's properties change, or when the value of an ratom dereffed in it's render function changes."]]
   [sg/section
    [sg/heading headings ::example]
    (sg/example
      {:form
       [canvas
        {:style {:width 200
                 :height 200}
         :on-click (fn []
                     (swap! !clicks inc))
         :render (fn [{:keys [::width ::height ::ctx]}]
                   (fill-style ctx "#ccc")
                   (fill-rect ctx 0 0 width height)
                   (fill-style ctx (if (= (mod @!clicks 2) 0) "blue" "green"))
                   (fill-rect ctx
                     (/ width 4) (/ height 4)
                     (/ width 2) (/ height 2)))}]})]
   [sg/heading headings ::components]
   [sg/component-doc
    {:var #'canvas}]
   [sg/section
    {:gap 12}
    [sg/heading headings ::twod-api]
    #_(sg/example
        {:form
         [canvas
          {:style {:width 200
                   :height 200}
           :on-click (fn []
                       (swap! !clicks inc))
           :context-type "webgl"
           :render (fn [{:keys [::width ::height ::ctx]}]
                     (clear-color ctx 0.0 0.0 0.0 1.0)
                     (clear ctx (color-buffer-bit ctx)))}]})
    [:p
     "These canvas drawing operations use the context object passed as the first argument to the canvas' "
     [:code "render"]
     " function."]
    [sg/vardocs
     {:vars [#'fill-style #'clear-rect #'fill-rect #'stroke-rect #'fill-text]}]]])


(comment

  (meta #'fill-style)

  (resolve 'fill-style)

  (browser/<set-root!
    [sg/standalone
     {:component sections
      :headings headings}])

  )


