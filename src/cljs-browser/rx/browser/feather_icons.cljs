(ns rx.browser.feather-icons (:refer-clojure :exclude [list repeat map type hash filter shuffle key divide]))

(defn activity [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "22 12 18 12 15 21 9 3 6 12 2 12"}]])

(defn airplay [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M5 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-1"}]
   [:polygon {:points "12 15 17 21 7 21 12 15"}]])

(defn alert-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "12"}]
   [:line {:x1 "12", :y1 "16", :x2 "12.01", :y2 "16"}]])

(defn alert-octagon [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon
    {:points
     "7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "12"}]
   [:line {:x1 "12", :y1 "16", :x2 "12.01", :y2 "16"}]])

(defn alert-triangle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"}]
   [:line {:x1 "12", :y1 "9", :x2 "12", :y2 "13"}]
   [:line {:x1 "12", :y1 "17", :x2 "12.01", :y2 "17"}]])

(defn align-center [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "18", :y1 "10", :x2 "6", :y2 "10"}]
   [:line {:x1 "21", :y1 "6", :x2 "3", :y2 "6"}]
   [:line {:x1 "21", :y1 "14", :x2 "3", :y2 "14"}]
   [:line {:x1 "18", :y1 "18", :x2 "6", :y2 "18"}]])

(defn align-justify [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "21", :y1 "10", :x2 "3", :y2 "10"}]
   [:line {:x1 "21", :y1 "6", :x2 "3", :y2 "6"}]
   [:line {:x1 "21", :y1 "14", :x2 "3", :y2 "14"}]
   [:line {:x1 "21", :y1 "18", :x2 "3", :y2 "18"}]])

(defn align-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "17", :y1 "10", :x2 "3", :y2 "10"}]
   [:line {:x1 "21", :y1 "6", :x2 "3", :y2 "6"}]
   [:line {:x1 "21", :y1 "14", :x2 "3", :y2 "14"}]
   [:line {:x1 "17", :y1 "18", :x2 "3", :y2 "18"}]])

(defn align-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "21", :y1 "10", :x2 "7", :y2 "10"}]
   [:line {:x1 "21", :y1 "6", :x2 "3", :y2 "6"}]
   [:line {:x1 "21", :y1 "14", :x2 "3", :y2 "14"}]
   [:line {:x1 "21", :y1 "18", :x2 "7", :y2 "18"}]])

(defn anchor [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "5", :r "3"}]
   [:line {:x1 "12", :y1 "22", :x2 "12", :y2 "8"}]
   [:path {:d "M5 12H2a10 10 0 0 0 20 0h-3"}]])

(defn aperture [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "14.31", :y1 "8", :x2 "20.05", :y2 "17.94"}]
   [:line {:x1 "9.69", :y1 "8", :x2 "21.17", :y2 "8"}]
   [:line {:x1 "7.38", :y1 "12", :x2 "13.12", :y2 "2.06"}]
   [:line {:x1 "9.69", :y1 "16", :x2 "3.95", :y2 "6.06"}]
   [:line {:x1 "14.31", :y1 "16", :x2 "2.83", :y2 "16"}]
   [:line {:x1 "16.62", :y1 "12", :x2 "10.88", :y2 "21.94"}]])

(defn archive [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "21 8 21 21 3 21 3 8"}]
   [:rect {:x "1", :y "3", :width "22", :height "5"}]
   [:line {:x1 "10", :y1 "12", :x2 "14", :y2 "12"}]])

(defn arrow-down-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polyline {:points "8 12 12 16 16 12"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "16"}]])

(defn arrow-down-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "17", :y1 "7", :x2 "7", :y2 "17"}]
   [:polyline {:points "17 17 7 17 7 7"}]])

(defn arrow-down-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "7", :y1 "7", :x2 "17", :y2 "17"}]
   [:polyline {:points "17 7 17 17 7 17"}]])

(defn arrow-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "5", :x2 "12", :y2 "19"}]
   [:polyline {:points "19 12 12 19 5 12"}]])

(defn arrow-left-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polyline {:points "12 8 8 12 12 16"}]
   [:line {:x1 "16", :y1 "12", :x2 "8", :y2 "12"}]])

(defn arrow-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "19", :y1 "12", :x2 "5", :y2 "12"}]
   [:polyline {:points "12 19 5 12 12 5"}]])

(defn arrow-right-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polyline {:points "12 16 16 12 12 8"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn arrow-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "5", :y1 "12", :x2 "19", :y2 "12"}]
   [:polyline {:points "12 5 19 12 12 19"}]])

(defn arrow-up-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polyline {:points "16 12 12 8 8 12"}]
   [:line {:x1 "12", :y1 "16", :x2 "12", :y2 "8"}]])

(defn arrow-up-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "17", :y1 "17", :x2 "7", :y2 "7"}]
   [:polyline {:points "7 17 7 7 17 7"}]])

(defn arrow-up-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "7", :y1 "17", :x2 "17", :y2 "7"}]
   [:polyline {:points "7 7 17 7 17 17"}]])

(defn arrow-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "19", :x2 "12", :y2 "5"}]
   [:polyline {:points "5 12 12 5 19 12"}]])

(defn at-sign [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "4"}]
   [:path {:d "M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94"}]])

(defn award [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "8", :r "7"}]
   [:polyline {:points "8.21 13.89 7 23 12 20 17 23 15.79 13.88"}]])

(defn bar-chart-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "18", :y1 "20", :x2 "18", :y2 "10"}]
   [:line {:x1 "12", :y1 "20", :x2 "12", :y2 "4"}]
   [:line {:x1 "6", :y1 "20", :x2 "6", :y2 "14"}]])

(defn bar-chart [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "20", :x2 "12", :y2 "10"}]
   [:line {:x1 "18", :y1 "20", :x2 "18", :y2 "4"}]
   [:line {:x1 "6", :y1 "20", :x2 "6", :y2 "16"}]])

(defn battery-charging [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M5 18H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3.19M15 6h2a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-3.19"}]
   [:line {:x1 "23", :y1 "13", :x2 "23", :y2 "11"}]
   [:polyline {:points "11 6 7 12 13 12 9 18"}]])

(defn battery [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "1", :y "6", :width "18", :height "12", :rx "2", :ry "2"}]
   [:line {:x1 "23", :y1 "13", :x2 "23", :y2 "11"}]])

(defn bell-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M13.73 21a2 2 0 0 1-3.46 0"}]
   [:path {:d "M18.63 13A17.89 17.89 0 0 1 18 8"}]
   [:path {:d "M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14"}]
   [:path {:d "M18 8a6 6 0 0 0-9.33-5"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn bell [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"}]
   [:path {:d "M13.73 21a2 2 0 0 1-3.46 0"}]])

(defn bluetooth [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "6.5 6.5 17.5 17.5 12 23 12 1 17.5 6.5 6.5 17.5"}]])

(defn bold [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"}]
   [:path {:d "M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"}]])

(defn book-open [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"}]
   [:path {:d "M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"}]])

(defn book [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M4 19.5A2.5 2.5 0 0 1 6.5 17H20"}]
   [:path
    {:d
     "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"}]])

(defn bookmark [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"}]])

(defn box [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"}]
   [:polyline {:points "3.27 6.96 12 12.01 20.73 6.96"}]
   [:line {:x1 "12", :y1 "22.08", :x2 "12", :y2 "12"}]])

(defn briefcase [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "2", :y "7", :width "20", :height "14", :rx "2", :ry "2"}]
   [:path {:d "M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"}]])

(defn calendar [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "4", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "16", :y1 "2", :x2 "16", :y2 "6"}]
   [:line {:x1 "8", :y1 "2", :x2 "8", :y2 "6"}]
   [:line {:x1 "3", :y1 "10", :x2 "21", :y2 "10"}]])

(defn camera-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]
   [:path
    {:d
     "M21 21H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3m3-3h6l2 3h4a2 2 0 0 1 2 2v9.34m-7.72-2.06a4 4 0 1 1-5.56-5.56"}]])

(defn camera [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"}]
   [:circle {:cx "12", :cy "13", :r "4"}]])

(defn cast [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M2 16.1A5 5 0 0 1 5.9 20M2 12.05A9 9 0 0 1 9.95 20M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6"}]
   [:line {:x1 "2", :y1 "20", :x2 "2.01", :y2 "20"}]])

(defn check-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M22 11.08V12a10 10 0 1 1-5.93-9.14"}]
   [:polyline {:points "22 4 12 14.01 9 11.01"}]])

(defn check-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "9 11 12 14 22 4"}]
   [:path
    {:d "M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"}]])

(defn check [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "20 6 9 17 4 12"}]])

(defn chevron-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "6 9 12 15 18 9"}]])

(defn chevron-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "15 18 9 12 15 6"}]])

(defn chevron-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "9 18 15 12 9 6"}]])

(defn chevron-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "18 15 12 9 6 15"}]])

(defn chevrons-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "7 13 12 18 17 13"}]
   [:polyline {:points "7 6 12 11 17 6"}]])

(defn chevrons-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "11 17 6 12 11 7"}]
   [:polyline {:points "18 17 13 12 18 7"}]])

(defn chevrons-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "13 17 18 12 13 7"}]
   [:polyline {:points "6 17 11 12 6 7"}]])

(defn chevrons-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "17 11 12 6 7 11"}]
   [:polyline {:points "17 18 12 13 7 18"}]])

(defn chrome [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:circle {:cx "12", :cy "12", :r "4"}]
   [:line {:x1 "21.17", :y1 "8", :x2 "12", :y2 "8"}]
   [:line {:x1 "3.95", :y1 "6.06", :x2 "8.54", :y2 "14"}]
   [:line {:x1 "10.88", :y1 "21.94", :x2 "15.46", :y2 "14"}]])

(defn circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]])

(defn clipboard [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"}]
   [:rect {:x "8", :y "2", :width "8", :height "4", :rx "1", :ry "1"}]])

(defn clock [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polyline {:points "12 6 12 12 16 14"}]])

(defn cloud-drizzle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "8", :y1 "19", :x2 "8", :y2 "21"}]
   [:line {:x1 "8", :y1 "13", :x2 "8", :y2 "15"}]
   [:line {:x1 "16", :y1 "19", :x2 "16", :y2 "21"}]
   [:line {:x1 "16", :y1 "13", :x2 "16", :y2 "15"}]
   [:line {:x1 "12", :y1 "21", :x2 "12", :y2 "23"}]
   [:line {:x1 "12", :y1 "15", :x2 "12", :y2 "17"}]
   [:path {:d "M20 16.58A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.25"}]])

(defn cloud-lightning [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M19 16.9A5 5 0 0 0 18 7h-1.26a8 8 0 1 0-11.62 9"}]
   [:polyline {:points "13 11 9 17 15 17 11 23"}]])

(defn cloud-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22.61 16.95A5 5 0 0 0 18 10h-1.26a8 8 0 0 0-7.05-6M5 5a8 8 0 0 0 4 15h9a5 5 0 0 0 1.7-.3"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn cloud-rain [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "16", :y1 "13", :x2 "16", :y2 "21"}]
   [:line {:x1 "8", :y1 "13", :x2 "8", :y2 "21"}]
   [:line {:x1 "12", :y1 "15", :x2 "12", :y2 "23"}]
   [:path {:d "M20 16.58A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.25"}]])

(defn cloud-snow [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M20 17.58A5 5 0 0 0 18 8h-1.26A8 8 0 1 0 4 16.25"}]
   [:line {:x1 "8", :y1 "16", :x2 "8.01", :y2 "16"}]
   [:line {:x1 "8", :y1 "20", :x2 "8.01", :y2 "20"}]
   [:line {:x1 "12", :y1 "18", :x2 "12.01", :y2 "18"}]
   [:line {:x1 "12", :y1 "22", :x2 "12.01", :y2 "22"}]
   [:line {:x1 "16", :y1 "16", :x2 "16.01", :y2 "16"}]
   [:line {:x1 "16", :y1 "20", :x2 "16.01", :y2 "20"}]])

(defn cloud [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"}]])

(defn code [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "16 18 22 12 16 6"}]
   [:polyline {:points "8 6 2 12 8 18"}]])

(defn codepen [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"}]
   [:line {:x1 "12", :y1 "22", :x2 "12", :y2 "15.5"}]
   [:polyline {:points "22 8.5 12 15.5 2 8.5"}]
   [:polyline {:points "2 15.5 12 8.5 22 15.5"}]
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "8.5"}]])

(defn codesandbox [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"}]
   [:polyline {:points "7.5 4.21 12 6.81 16.5 4.21"}]
   [:polyline {:points "7.5 19.79 7.5 14.6 3 12"}]
   [:polyline {:points "21 12 16.5 14.6 16.5 19.79"}]
   [:polyline {:points "3.27 6.96 12 12.01 20.73 6.96"}]
   [:line {:x1 "12", :y1 "22.08", :x2 "12", :y2 "12"}]])

(defn coffee [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M18 8h1a4 4 0 0 1 0 8h-1"}]
   [:path {:d "M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"}]
   [:line {:x1 "6", :y1 "1", :x2 "6", :y2 "4"}]
   [:line {:x1 "10", :y1 "1", :x2 "10", :y2 "4"}]
   [:line {:x1 "14", :y1 "1", :x2 "14", :y2 "4"}]])

(defn columns [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7m0-18H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h7m0-18v18"}]])

(defn command [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M18 3a3 3 0 0 0-3 3v12a3 3 0 0 0 3 3 3 3 0 0 0 3-3 3 3 0 0 0-3-3H6a3 3 0 0 0-3 3 3 3 0 0 0 3 3 3 3 0 0 0 3-3V6a3 3 0 0 0-3-3 3 3 0 0 0-3 3 3 3 0 0 0 3 3h12a3 3 0 0 0 3-3 3 3 0 0 0-3-3z"}]])

(defn compass [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polygon
    {:points "16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"}]])

(defn copy [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "9", :y "9", :width "13", :height "13", :rx "2", :ry "2"}]
   [:path {:d "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"}]])

(defn corner-down-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "9 10 4 15 9 20"}]
   [:path {:d "M20 4v7a4 4 0 0 1-4 4H4"}]])

(defn corner-down-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "15 10 20 15 15 20"}]
   [:path {:d "M4 4v7a4 4 0 0 0 4 4h12"}]])

(defn corner-left-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "14 15 9 20 4 15"}]
   [:path {:d "M20 4h-7a4 4 0 0 0-4 4v12"}]])

(defn corner-left-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "14 9 9 4 4 9"}]
   [:path {:d "M20 20h-7a4 4 0 0 1-4-4V4"}]])

(defn corner-right-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "10 15 15 20 20 15"}]
   [:path {:d "M4 4h7a4 4 0 0 1 4 4v12"}]])

(defn corner-right-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "10 9 15 4 20 9"}]
   [:path {:d "M4 20h7a4 4 0 0 0 4-4V4"}]])

(defn corner-up-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "9 14 4 9 9 4"}]
   [:path {:d "M20 20v-7a4 4 0 0 0-4-4H4"}]])

(defn corner-up-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "15 14 20 9 15 4"}]
   [:path {:d "M4 20v-7a4 4 0 0 1 4-4h12"}]])

(defn cpu [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "4", :y "4", :width "16", :height "16", :rx "2", :ry "2"}]
   [:rect {:x "9", :y "9", :width "6", :height "6"}]
   [:line {:x1 "9", :y1 "1", :x2 "9", :y2 "4"}]
   [:line {:x1 "15", :y1 "1", :x2 "15", :y2 "4"}]
   [:line {:x1 "9", :y1 "20", :x2 "9", :y2 "23"}]
   [:line {:x1 "15", :y1 "20", :x2 "15", :y2 "23"}]
   [:line {:x1 "20", :y1 "9", :x2 "23", :y2 "9"}]
   [:line {:x1 "20", :y1 "14", :x2 "23", :y2 "14"}]
   [:line {:x1 "1", :y1 "9", :x2 "4", :y2 "9"}]
   [:line {:x1 "1", :y1 "14", :x2 "4", :y2 "14"}]])

(defn credit-card [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "1", :y "4", :width "22", :height "16", :rx "2", :ry "2"}]
   [:line {:x1 "1", :y1 "10", :x2 "23", :y2 "10"}]])

(defn crop [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M6.13 1L6 16a2 2 0 0 0 2 2h15"}]
   [:path {:d "M1 6.13L16 6a2 2 0 0 1 2 2v15"}]])

(defn crosshair [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "22", :y1 "12", :x2 "18", :y2 "12"}]
   [:line {:x1 "6", :y1 "12", :x2 "2", :y2 "12"}]
   [:line {:x1 "12", :y1 "6", :x2 "12", :y2 "2"}]
   [:line {:x1 "12", :y1 "22", :x2 "12", :y2 "18"}]])

(defn database [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:ellipse {:cx "12", :cy "5", :rx "9", :ry "3"}]
   [:path {:d "M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"}]
   [:path {:d "M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"}]])

(defn delete [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 4H8l-7 8 7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z"}]
   [:line {:x1 "18", :y1 "9", :x2 "12", :y2 "15"}]
   [:line {:x1 "12", :y1 "9", :x2 "18", :y2 "15"}]])

(defn disc [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:circle {:cx "12", :cy "12", :r "3"}]])

(defn divide-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]
   [:line {:x1 "12", :y1 "16", :x2 "12", :y2 "16"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "8"}]
   [:circle {:cx "12", :cy "12", :r "10"}]])

(defn divide-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]
   [:line {:x1 "12", :y1 "16", :x2 "12", :y2 "16"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "8"}]])

(defn divide [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "6", :r "2"}]
   [:line {:x1 "5", :y1 "12", :x2 "19", :y2 "12"}]
   [:circle {:cx "12", :cy "18", :r "2"}]])

(defn dollar-sign [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "1", :x2 "12", :y2 "23"}]
   [:path {:d "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"}]])

(defn download-cloud [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "8 17 12 21 16 17"}]
   [:line {:x1 "12", :y1 "12", :x2 "12", :y2 "21"}]
   [:path {:d "M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"}]])

(defn download [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
   [:polyline {:points "7 10 12 15 17 10"}]
   [:line {:x1 "12", :y1 "15", :x2 "12", :y2 "3"}]])

(defn dribbble [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:path
    {:d
     "M8.56 2.75c4.37 6.03 6.02 9.42 8.03 17.72m2.54-15.38c-3.72 4.35-8.94 5.66-16.88 5.85m19.5 1.9c-3.5-.93-6.63-.82-8.94 0-2.58.92-5.01 2.86-7.44 6.32"}]])

(defn droplet [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"}]])

(defn edit-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"}]])

(defn edit-3 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M12 20h9"}]
   [:path {:d "M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"}]])

(defn edit [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"}]
   [:path {:d "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"}]])

(defn external-link [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"}]
   [:polyline {:points "15 3 21 3 21 9"}]
   [:line {:x1 "10", :y1 "14", :x2 "21", :y2 "3"}]])

(defn eye-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn eye [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"}]
   [:circle {:cx "12", :cy "12", :r "3"}]])

(defn facebook [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"}]])

(defn fast-forward [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "13 19 22 12 13 5 13 19"}]
   [:polygon {:points "2 19 11 12 2 5 2 19"}]])

(defn feather [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z"}]
   [:line {:x1 "16", :y1 "8", :x2 "2", :y2 "22"}]
   [:line {:x1 "17.5", :y1 "15", :x2 "9", :y2 "15"}]])

(defn figma [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M5 5.5A3.5 3.5 0 0 1 8.5 2H12v7H8.5A3.5 3.5 0 0 1 5 5.5z"}]
   [:path {:d "M12 2h3.5a3.5 3.5 0 1 1 0 7H12V2z"}]
   [:path {:d "M12 12.5a3.5 3.5 0 1 1 7 0 3.5 3.5 0 1 1-7 0z"}]
   [:path {:d "M5 19.5A3.5 3.5 0 0 1 8.5 16H12v3.5a3.5 3.5 0 1 1-7 0z"}]
   [:path
    {:d "M5 12.5A3.5 3.5 0 0 1 8.5 9H12v7H8.5A3.5 3.5 0 0 1 5 12.5z"}]])

(defn file-minus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "9", :y1 "15", :x2 "15", :y2 "15"}]])

(defn file-plus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "12", :y1 "18", :x2 "12", :y2 "12"}]
   [:line {:x1 "9", :y1 "15", :x2 "15", :y2 "15"}]])

(defn file-text [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16", :y1 "13", :x2 "8", :y2 "13"}]
   [:line {:x1 "16", :y1 "17", :x2 "8", :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn file [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"}]
   [:polyline {:points "13 2 13 9 20 9"}]])

(defn film [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect
    {:x "2", :y "2", :width "20", :height "20", :rx "2.18", :ry "2.18"}]
   [:line {:x1 "7", :y1 "2", :x2 "7", :y2 "22"}]
   [:line {:x1 "17", :y1 "2", :x2 "17", :y2 "22"}]
   [:line {:x1 "2", :y1 "12", :x2 "22", :y2 "12"}]
   [:line {:x1 "2", :y1 "7", :x2 "7", :y2 "7"}]
   [:line {:x1 "2", :y1 "17", :x2 "7", :y2 "17"}]
   [:line {:x1 "17", :y1 "17", :x2 "22", :y2 "17"}]
   [:line {:x1 "17", :y1 "7", :x2 "22", :y2 "7"}]])

(defn filter [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"}]])

(defn flag [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"}]
   [:line {:x1 "4", :y1 "22", :x2 "4", :y2 "15"}]])

(defn folder-minus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"}]
   [:line {:x1 "9", :y1 "14", :x2 "15", :y2 "14"}]])

(defn folder-plus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"}]
   [:line {:x1 "12", :y1 "11", :x2 "12", :y2 "17"}]
   [:line {:x1 "9", :y1 "14", :x2 "15", :y2 "14"}]])

(defn folder [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"}]])

(defn framer [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M5 16V9h14V2H5l14 14h-7m-7 0l7 7v-7m-7 0h7"}]])

(defn frown [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:path {:d "M16 16s-1.5-2-4-2-4 2-4 2"}]
   [:line {:x1 "9", :y1 "9", :x2 "9.01", :y2 "9"}]
   [:line {:x1 "15", :y1 "9", :x2 "15.01", :y2 "9"}]])

(defn gift [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "20 12 20 22 4 22 4 12"}]
   [:rect {:x "2", :y "7", :width "20", :height "5"}]
   [:line {:x1 "12", :y1 "22", :x2 "12", :y2 "7"}]
   [:path {:d "M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"}]
   [:path {:d "M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"}]])

(defn git-branch [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "6", :y1 "3", :x2 "6", :y2 "15"}]
   [:circle {:cx "18", :cy "6", :r "3"}]
   [:circle {:cx "6", :cy "18", :r "3"}]
   [:path {:d "M18 9a9 9 0 0 1-9 9"}]])

(defn git-commit [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "4"}]
   [:line {:x1 "1.05", :y1 "12", :x2 "7", :y2 "12"}]
   [:line {:x1 "17.01", :y1 "12", :x2 "22.96", :y2 "12"}]])

(defn git-merge [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "18", :cy "18", :r "3"}]
   [:circle {:cx "6", :cy "6", :r "3"}]
   [:path {:d "M6 21V9a9 9 0 0 0 9 9"}]])

(defn git-pull-request [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "18", :cy "18", :r "3"}]
   [:circle {:cx "6", :cy "6", :r "3"}]
   [:path {:d "M13 6h3a2 2 0 0 1 2 2v7"}]
   [:line {:x1 "6", :y1 "9", :x2 "6", :y2 "21"}]])

(defn github [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"}]])

(defn gitlab [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22.65 14.39L12 22.13 1.35 14.39a.84.84 0 0 1-.3-.94l1.22-3.78 2.44-7.51A.42.42 0 0 1 4.82 2a.43.43 0 0 1 .58 0 .42.42 0 0 1 .11.18l2.44 7.49h8.1l2.44-7.51A.42.42 0 0 1 18.6 2a.43.43 0 0 1 .58 0 .42.42 0 0 1 .11.18l2.44 7.51L23 13.45a.84.84 0 0 1-.35.94z"}]])

(defn globe [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "2", :y1 "12", :x2 "22", :y2 "12"}]
   [:path
    {:d
     "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"}]])

(defn grid [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "7", :height "7"}]
   [:rect {:x "14", :y "3", :width "7", :height "7"}]
   [:rect {:x "14", :y "14", :width "7", :height "7"}]
   [:rect {:x "3", :y "14", :width "7", :height "7"}]])

(defn hard-drive [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "22", :y1 "12", :x2 "2", :y2 "12"}]
   [:path
    {:d
     "M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"}]
   [:line {:x1 "6", :y1 "16", :x2 "6.01", :y2 "16"}]
   [:line {:x1 "10", :y1 "16", :x2 "10.01", :y2 "16"}]])

(defn hash [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "4", :y1 "9", :x2 "20", :y2 "9"}]
   [:line {:x1 "4", :y1 "15", :x2 "20", :y2 "15"}]
   [:line {:x1 "10", :y1 "3", :x2 "8", :y2 "21"}]
   [:line {:x1 "16", :y1 "3", :x2 "14", :y2 "21"}]])

(defn headphones [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M3 18v-6a9 9 0 0 1 18 0v6"}]
   [:path
    {:d
     "M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"}]])

(defn heart [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"}]])

(defn help-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:path {:d "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"}]
   [:line {:x1 "12", :y1 "17", :x2 "12.01", :y2 "17"}]])

(defn hexagon [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"}]])

(defn home [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"}]
   [:polyline {:points "9 22 9 12 15 12 15 22"}]])

(defn image [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:circle {:cx "8.5", :cy "8.5", :r "1.5"}]
   [:polyline {:points "21 15 16 10 5 21"}]])

(defn inbox [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "22 12 16 12 14 15 10 15 8 12 2 12"}]
   [:path
    {:d
     "M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"}]])

(defn info [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "12", :y1 "16", :x2 "12", :y2 "12"}]
   [:line {:x1 "12", :y1 "8", :x2 "12.01", :y2 "8"}]])

(defn instagram [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "2", :y "2", :width "20", :height "20", :rx "5", :ry "5"}]
   [:path {:d "M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"}]
   [:line {:x1 "17.5", :y1 "6.5", :x2 "17.51", :y2 "6.5"}]])

(defn italic [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "19", :y1 "4", :x2 "10", :y2 "4"}]
   [:line {:x1 "14", :y1 "20", :x2 "5", :y2 "20"}]
   [:line {:x1 "15", :y1 "4", :x2 "9", :y2 "20"}]])

(defn key [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"}]])

(defn layers [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "12 2 2 7 12 12 22 7 12 2"}]
   [:polyline {:points "2 17 12 22 22 17"}]
   [:polyline {:points "2 12 12 17 22 12"}]])

(defn layout [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "3", :y1 "9", :x2 "21", :y2 "9"}]
   [:line {:x1 "9", :y1 "21", :x2 "9", :y2 "9"}]])

(defn life-buoy [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:circle {:cx "12", :cy "12", :r "4"}]
   [:line {:x1 "4.93", :y1 "4.93", :x2 "9.17", :y2 "9.17"}]
   [:line {:x1 "14.83", :y1 "14.83", :x2 "19.07", :y2 "19.07"}]
   [:line {:x1 "14.83", :y1 "9.17", :x2 "19.07", :y2 "4.93"}]
   [:line {:x1 "14.83", :y1 "9.17", :x2 "18.36", :y2 "5.64"}]
   [:line {:x1 "4.93", :y1 "19.07", :x2 "9.17", :y2 "14.83"}]])

(defn link-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M15 7h3a5 5 0 0 1 5 5 5 5 0 0 1-5 5h-3m-6 0H6a5 5 0 0 1-5-5 5 5 0 0 1 5-5h3"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn link [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"}]
   [:path
    {:d "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"}]])

(defn linkedin [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z"}]
   [:rect {:x "2", :y "9", :width "4", :height "12"}]
   [:circle {:cx "4", :cy "4", :r "2"}]])

(defn list [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "8", :y1 "6", :x2 "21", :y2 "6"}]
   [:line {:x1 "8", :y1 "12", :x2 "21", :y2 "12"}]
   [:line {:x1 "8", :y1 "18", :x2 "21", :y2 "18"}]
   [:line {:x1 "3", :y1 "6", :x2 "3.01", :y2 "6"}]
   [:line {:x1 "3", :y1 "12", :x2 "3.01", :y2 "12"}]
   [:line {:x1 "3", :y1 "18", :x2 "3.01", :y2 "18"}]])

(defn loader [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "6"}]
   [:line {:x1 "12", :y1 "18", :x2 "12", :y2 "22"}]
   [:line {:x1 "4.93", :y1 "4.93", :x2 "7.76", :y2 "7.76"}]
   [:line {:x1 "16.24", :y1 "16.24", :x2 "19.07", :y2 "19.07"}]
   [:line {:x1 "2", :y1 "12", :x2 "6", :y2 "12"}]
   [:line {:x1 "18", :y1 "12", :x2 "22", :y2 "12"}]
   [:line {:x1 "4.93", :y1 "19.07", :x2 "7.76", :y2 "16.24"}]
   [:line {:x1 "16.24", :y1 "7.76", :x2 "19.07", :y2 "4.93"}]])

(defn lock [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "11", :width "18", :height "11", :rx "2", :ry "2"}]
   [:path {:d "M7 11V7a5 5 0 0 1 10 0v4"}]])

(defn log-in [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"}]
   [:polyline {:points "10 17 15 12 10 7"}]
   [:line {:x1 "15", :y1 "12", :x2 "3", :y2 "12"}]])

(defn log-out [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"}]
   [:polyline {:points "16 17 21 12 16 7"}]
   [:line {:x1 "21", :y1 "12", :x2 "9", :y2 "12"}]])

(defn mail [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"}]
   [:polyline {:points "22,6 12,13 2,6"}]])

(defn map-pin [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"}]
   [:circle {:cx "12", :cy "10", :r "3"}]])

(defn map [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"}]
   [:line {:x1 "8", :y1 "2", :x2 "8", :y2 "18"}]
   [:line {:x1 "16", :y1 "6", :x2 "16", :y2 "22"}]])

(defn maximize-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "15 3 21 3 21 9"}]
   [:polyline {:points "9 21 3 21 3 15"}]
   [:line {:x1 "21", :y1 "3", :x2 "14", :y2 "10"}]
   [:line {:x1 "3", :y1 "21", :x2 "10", :y2 "14"}]])

(defn maximize [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"}]])

(defn meh [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "8", :y1 "15", :x2 "16", :y2 "15"}]
   [:line {:x1 "9", :y1 "9", :x2 "9.01", :y2 "9"}]
   [:line {:x1 "15", :y1 "9", :x2 "15.01", :y2 "9"}]])

(defn menu [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "3", :y1 "12", :x2 "21", :y2 "12"}]
   [:line {:x1 "3", :y1 "6", :x2 "21", :y2 "6"}]
   [:line {:x1 "3", :y1 "18", :x2 "21", :y2 "18"}]])

(defn message-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"}]])

(defn message-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"}]])

(defn mic-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]
   [:path {:d "M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"}]
   [:path {:d "M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"}]
   [:line {:x1 "12", :y1 "19", :x2 "12", :y2 "23"}]
   [:line {:x1 "8", :y1 "23", :x2 "16", :y2 "23"}]])

(defn mic [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"}]
   [:path {:d "M19 10v2a7 7 0 0 1-14 0v-2"}]
   [:line {:x1 "12", :y1 "19", :x2 "12", :y2 "23"}]
   [:line {:x1 "8", :y1 "23", :x2 "16", :y2 "23"}]])

(defn minimize-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "4 14 10 14 10 20"}]
   [:polyline {:points "20 10 14 10 14 4"}]
   [:line {:x1 "14", :y1 "10", :x2 "21", :y2 "3"}]
   [:line {:x1 "3", :y1 "21", :x2 "10", :y2 "14"}]])

(defn minimize [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"}]])

(defn minus-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn minus-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn minus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "5", :y1 "12", :x2 "19", :y2 "12"}]])

(defn monitor [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "2", :y "3", :width "20", :height "14", :rx "2", :ry "2"}]
   [:line {:x1 "8", :y1 "21", :x2 "16", :y2 "21"}]
   [:line {:x1 "12", :y1 "17", :x2 "12", :y2 "21"}]])

(defn moon [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"}]])

(defn more-horizontal [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "1"}]
   [:circle {:cx "19", :cy "12", :r "1"}]
   [:circle {:cx "5", :cy "12", :r "1"}]])

(defn more-vertical [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "1"}]
   [:circle {:cx "12", :cy "5", :r "1"}]
   [:circle {:cx "12", :cy "19", :r "1"}]])

(defn mouse-pointer [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M3 3l7.07 16.97 2.51-7.39 7.39-2.51L3 3z"}]
   [:path {:d "M13 13l6 6"}]])

(defn move [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "5 9 2 12 5 15"}]
   [:polyline {:points "9 5 12 2 15 5"}]
   [:polyline {:points "15 19 12 22 9 19"}]
   [:polyline {:points "19 9 22 12 19 15"}]
   [:line {:x1 "2", :y1 "12", :x2 "22", :y2 "12"}]
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "22"}]])

(defn music [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M9 18V5l12-2v13"}]
   [:circle {:cx "6", :cy "18", :r "3"}]
   [:circle {:cx "18", :cy "16", :r "3"}]])

(defn navigation-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "12 2 19 21 12 17 5 21 12 2"}]])

(defn navigation [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "3 11 22 2 13 21 11 13 3 11"}]])

(defn octagon [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon
    {:points
     "7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"}]])

(defn package [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "16.5", :y1 "9.4", :x2 "7.5", :y2 "4.21"}]
   [:path
    {:d
     "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"}]
   [:polyline {:points "3.27 6.96 12 12.01 20.73 6.96"}]
   [:line {:x1 "12", :y1 "22.08", :x2 "12", :y2 "12"}]])

(defn paperclip [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"}]])

(defn pause-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "10", :y1 "15", :x2 "10", :y2 "9"}]
   [:line {:x1 "14", :y1 "15", :x2 "14", :y2 "9"}]])

(defn pause [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "6", :y "4", :width "4", :height "16"}]
   [:rect {:x "14", :y "4", :width "4", :height "16"}]])

(defn pen-tool [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M12 19l7-7 3 3-7 7-3-3z"}]
   [:path {:d "M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"}]
   [:path {:d "M2 2l7.586 7.586"}]
   [:circle {:cx "11", :cy "11", :r "2"}]])

(defn percent [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "19", :y1 "5", :x2 "5", :y2 "19"}]
   [:circle {:cx "6.5", :cy "6.5", :r "2.5"}]
   [:circle {:cx "17.5", :cy "17.5", :r "2.5"}]])

(defn phone-call [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M15.05 5A5 5 0 0 1 19 8.95M15.05 1A9 9 0 0 1 23 8.94m-1 7.98v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn phone-forwarded [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "19 1 23 5 19 9"}]
   [:line {:x1 "15", :y1 "5", :x2 "23", :y2 "5"}]
   [:path
    {:d
     "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn phone-incoming [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "16 2 16 8 22 8"}]
   [:line {:x1 "23", :y1 "1", :x2 "16", :y2 "8"}]
   [:path
    {:d
     "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn phone-missed [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "23", :y1 "1", :x2 "17", :y2 "7"}]
   [:line {:x1 "17", :y1 "1", :x2 "23", :y2 "7"}]
   [:path
    {:d
     "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn phone-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.42 19.42 0 0 1-3.33-2.67m-2.67-3.34a19.79 19.79 0 0 1-3.07-8.63A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"}]
   [:line {:x1 "23", :y1 "1", :x2 "1", :y2 "23"}]])

(defn phone-outgoing [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "23 7 23 1 17 1"}]
   [:line {:x1 "16", :y1 "8", :x2 "23", :y2 "1"}]
   [:path
    {:d
     "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn phone [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"}]])

(defn pie-chart [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21.21 15.89A10 10 0 1 1 8 2.83"}]
   [:path {:d "M22 12A10 10 0 0 0 12 2v10z"}]])

(defn play-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:polygon {:points "10 8 16 12 10 16 10 8"}]])

(defn play [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "5 3 19 12 5 21 5 3"}]])

(defn plus-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "16"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn plus-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "16"}]
   [:line {:x1 "8", :y1 "12", :x2 "16", :y2 "12"}]])

(defn plus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "12", :y1 "5", :x2 "12", :y2 "19"}]
   [:line {:x1 "5", :y1 "12", :x2 "19", :y2 "12"}]])

(defn pocket [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M4 3h16a2 2 0 0 1 2 2v6a10 10 0 0 1-10 10A10 10 0 0 1 2 11V5a2 2 0 0 1 2-2z"}]
   [:polyline {:points "8 10 12 14 16 10"}]])

(defn power [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M18.36 6.64a9 9 0 1 1-12.73 0"}]
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "12"}]])

(defn printer [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "6 9 6 2 18 2 18 9"}]
   [:path
    {:d
     "M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"}]
   [:rect {:x "6", :y "14", :width "12", :height "8"}]])

(defn radio [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "2"}]
   [:path
    {:d
     "M16.24 7.76a6 6 0 0 1 0 8.49m-8.48-.01a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"}]])

(defn refresh-ccw [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "1 4 1 10 7 10"}]
   [:polyline {:points "23 20 23 14 17 14"}]
   [:path
    {:d
     "M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"}]])

(defn refresh-cw [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "23 4 23 10 17 10"}]
   [:polyline {:points "1 20 1 14 7 14"}]
   [:path
    {:d
     "M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"}]])

(defn repeat [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "17 1 21 5 17 9"}]
   [:path {:d "M3 11V9a4 4 0 0 1 4-4h14"}]
   [:polyline {:points "7 23 3 19 7 15"}]
   [:path {:d "M21 13v2a4 4 0 0 1-4 4H3"}]])

(defn rewind [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "11 19 2 12 11 5 11 19"}]
   [:polygon {:points "22 19 13 12 22 5 22 19"}]])

(defn rotate-ccw [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "1 4 1 10 7 10"}]
   [:path {:d "M3.51 15a9 9 0 1 0 2.13-9.36L1 10"}]])

(defn rotate-cw [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "23 4 23 10 17 10"}]
   [:path {:d "M20.49 15a9 9 0 1 1-2.12-9.36L23 10"}]])

(defn rss [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M4 11a9 9 0 0 1 9 9"}]
   [:path {:d "M4 4a16 16 0 0 1 16 16"}]
   [:circle {:cx "5", :cy "19", :r "1"}]])

(defn save [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"}]
   [:polyline {:points "17 21 17 13 7 13 7 21"}]
   [:polyline {:points "7 3 7 8 15 8"}]])

(defn scissors [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "6", :cy "6", :r "3"}]
   [:circle {:cx "6", :cy "18", :r "3"}]
   [:line {:x1 "20", :y1 "4", :x2 "8.12", :y2 "15.88"}]
   [:line {:x1 "14.47", :y1 "14.48", :x2 "20", :y2 "20"}]
   [:line {:x1 "8.12", :y1 "8.12", :x2 "12", :y2 "12"}]])

(defn search [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "11", :cy "11", :r "8"}]
   [:line {:x1 "21", :y1 "21", :x2 "16.65", :y2 "16.65"}]])

(defn send [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "22", :y1 "2", :x2 "11", :y2 "13"}]
   [:polygon {:points "22 2 15 22 11 13 2 9 22 2"}]])

(defn server [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "2", :y "2", :width "20", :height "8", :rx "2", :ry "2"}]
   [:rect {:x "2", :y "14", :width "20", :height "8", :rx "2", :ry "2"}]
   [:line {:x1 "6", :y1 "6", :x2 "6.01", :y2 "6"}]
   [:line {:x1 "6", :y1 "18", :x2 "6.01", :y2 "18"}]])

(defn settings [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "3"}]
   [:path
    {:d
     "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"}]])

(defn share-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "18", :cy "5", :r "3"}]
   [:circle {:cx "6", :cy "12", :r "3"}]
   [:circle {:cx "18", :cy "19", :r "3"}]
   [:line {:x1 "8.59", :y1 "13.51", :x2 "15.42", :y2 "17.49"}]
   [:line {:x1 "15.41", :y1 "6.51", :x2 "8.59", :y2 "10.49"}]])

(defn share [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"}]
   [:polyline {:points "16 6 12 2 8 6"}]
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "15"}]])

(defn shield-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M19.69 14a6.9 6.9 0 0 0 .31-2V5l-8-3-3.16 1.18"}]
   [:path
    {:d "M4.73 4.73L4 5v7c0 6 8 10 8 10a20.29 20.29 0 0 0 5.62-4.38"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn shield [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}]])

(defn shopping-bag [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"}]
   [:line {:x1 "3", :y1 "6", :x2 "21", :y2 "6"}]
   [:path {:d "M16 10a4 4 0 0 1-8 0"}]])

(defn shopping-cart [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "9", :cy "21", :r "1"}]
   [:circle {:cx "20", :cy "21", :r "1"}]
   [:path
    {:d
     "M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"}]])

(defn shuffle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "16 3 21 3 21 8"}]
   [:line {:x1 "4", :y1 "20", :x2 "21", :y2 "3"}]
   [:polyline {:points "21 16 21 21 16 21"}]
   [:line {:x1 "15", :y1 "15", :x2 "21", :y2 "21"}]
   [:line {:x1 "4", :y1 "4", :x2 "9", :y2 "9"}]])

(defn sidebar [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "9", :y1 "3", :x2 "9", :y2 "21"}]])

(defn skip-back [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "19 20 9 12 19 4 19 20"}]
   [:line {:x1 "5", :y1 "19", :x2 "5", :y2 "5"}]])

(defn skip-forward [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "5 4 15 12 5 20 5 4"}]
   [:line {:x1 "19", :y1 "5", :x2 "19", :y2 "19"}]])

(defn slack [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M14.5 10c-.83 0-1.5-.67-1.5-1.5v-5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5v5c0 .83-.67 1.5-1.5 1.5z"}]
   [:path
    {:d
     "M20.5 10H19V8.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"}]
   [:path
    {:d
     "M9.5 14c.83 0 1.5.67 1.5 1.5v5c0 .83-.67 1.5-1.5 1.5S8 21.33 8 20.5v-5c0-.83.67-1.5 1.5-1.5z"}]
   [:path
    {:d
     "M3.5 14H5v1.5c0 .83-.67 1.5-1.5 1.5S2 16.33 2 15.5 2.67 14 3.5 14z"}]
   [:path
    {:d
     "M14 14.5c0-.83.67-1.5 1.5-1.5h5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5h-5c-.83 0-1.5-.67-1.5-1.5z"}]
   [:path
    {:d
     "M15.5 19H14v1.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5-.67-1.5-1.5-1.5z"}]
   [:path
    {:d
     "M10 9.5C10 8.67 9.33 8 8.5 8h-5C2.67 8 2 8.67 2 9.5S2.67 11 3.5 11h5c.83 0 1.5-.67 1.5-1.5z"}]
   [:path
    {:d "M8.5 5H10V3.5C10 2.67 9.33 2 8.5 2S7 2.67 7 3.5 7.67 5 8.5 5z"}]])

(defn slash [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "4.93", :y1 "4.93", :x2 "19.07", :y2 "19.07"}]])

(defn sliders [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "4", :y1 "21", :x2 "4", :y2 "14"}]
   [:line {:x1 "4", :y1 "10", :x2 "4", :y2 "3"}]
   [:line {:x1 "12", :y1 "21", :x2 "12", :y2 "12"}]
   [:line {:x1 "12", :y1 "8", :x2 "12", :y2 "3"}]
   [:line {:x1 "20", :y1 "21", :x2 "20", :y2 "16"}]
   [:line {:x1 "20", :y1 "12", :x2 "20", :y2 "3"}]
   [:line {:x1 "1", :y1 "14", :x2 "7", :y2 "14"}]
   [:line {:x1 "9", :y1 "8", :x2 "15", :y2 "8"}]
   [:line {:x1 "17", :y1 "16", :x2 "23", :y2 "16"}]])

(defn smartphone [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "5", :y "2", :width "14", :height "20", :rx "2", :ry "2"}]
   [:line {:x1 "12", :y1 "18", :x2 "12.01", :y2 "18"}]])

(defn smile [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:path {:d "M8 14s1.5 2 4 2 4-2 4-2"}]
   [:line {:x1 "9", :y1 "9", :x2 "9.01", :y2 "9"}]
   [:line {:x1 "15", :y1 "9", :x2 "15.01", :y2 "9"}]])

(defn speaker [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "4", :y "2", :width "16", :height "20", :rx "2", :ry "2"}]
   [:circle {:cx "12", :cy "14", :r "4"}]
   [:line {:x1 "12", :y1 "6", :x2 "12.01", :y2 "6"}]])

(defn square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]])

(defn star [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon
    {:points
     "12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"}]])

(defn stop-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:rect {:x "9", :y "9", :width "6", :height "6"}]])

(defn sun [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "5"}]
   [:line {:x1 "12", :y1 "1", :x2 "12", :y2 "3"}]
   [:line {:x1 "12", :y1 "21", :x2 "12", :y2 "23"}]
   [:line {:x1 "4.22", :y1 "4.22", :x2 "5.64", :y2 "5.64"}]
   [:line {:x1 "18.36", :y1 "18.36", :x2 "19.78", :y2 "19.78"}]
   [:line {:x1 "1", :y1 "12", :x2 "3", :y2 "12"}]
   [:line {:x1 "21", :y1 "12", :x2 "23", :y2 "12"}]
   [:line {:x1 "4.22", :y1 "19.78", :x2 "5.64", :y2 "18.36"}]
   [:line {:x1 "18.36", :y1 "5.64", :x2 "19.78", :y2 "4.22"}]])

(defn sunrise [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M17 18a5 5 0 0 0-10 0"}]
   [:line {:x1 "12", :y1 "2", :x2 "12", :y2 "9"}]
   [:line {:x1 "4.22", :y1 "10.22", :x2 "5.64", :y2 "11.64"}]
   [:line {:x1 "1", :y1 "18", :x2 "3", :y2 "18"}]
   [:line {:x1 "21", :y1 "18", :x2 "23", :y2 "18"}]
   [:line {:x1 "18.36", :y1 "11.64", :x2 "19.78", :y2 "10.22"}]
   [:line {:x1 "23", :y1 "22", :x2 "1", :y2 "22"}]
   [:polyline {:points "8 6 12 2 16 6"}]])

(defn sunset [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M17 18a5 5 0 0 0-10 0"}]
   [:line {:x1 "12", :y1 "9", :x2 "12", :y2 "2"}]
   [:line {:x1 "4.22", :y1 "10.22", :x2 "5.64", :y2 "11.64"}]
   [:line {:x1 "1", :y1 "18", :x2 "3", :y2 "18"}]
   [:line {:x1 "21", :y1 "18", :x2 "23", :y2 "18"}]
   [:line {:x1 "18.36", :y1 "11.64", :x2 "19.78", :y2 "10.22"}]
   [:line {:x1 "23", :y1 "22", :x2 "1", :y2 "22"}]
   [:polyline {:points "16 5 12 9 8 5"}]])

(defn tablet [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "4", :y "2", :width "16", :height "20", :rx "2", :ry "2"}]
   [:line {:x1 "12", :y1 "18", :x2 "12.01", :y2 "18"}]])

(defn tag [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"}]
   [:line {:x1 "7", :y1 "7", :x2 "7.01", :y2 "7"}]])

(defn target [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:circle {:cx "12", :cy "12", :r "6"}]
   [:circle {:cx "12", :cy "12", :r "2"}]])

(defn terminal [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "4 17 10 11 4 5"}]
   [:line {:x1 "12", :y1 "19", :x2 "20", :y2 "19"}]])

(defn thermometer [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d "M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"}]])

(defn thumbs-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17"}]])

(defn thumbs-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"}]])

(defn toggle-left [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "1", :y "5", :width "22", :height "14", :rx "7", :ry "7"}]
   [:circle {:cx "8", :cy "12", :r "3"}]])

(defn toggle-right [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "1", :y "5", :width "22", :height "14", :rx "7", :ry "7"}]
   [:circle {:cx "16", :cy "12", :r "3"}]])

(defn tool [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"}]])

(defn trash-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "3 6 5 6 21 6"}]
   [:path
    {:d
     "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"}]
   [:line {:x1 "10", :y1 "11", :x2 "10", :y2 "17"}]
   [:line {:x1 "14", :y1 "11", :x2 "14", :y2 "17"}]])

(defn trash [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "3 6 5 6 21 6"}]
   [:path
    {:d
     "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"}]])

(defn trello [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:rect {:x "7", :y "7", :width "3", :height "9"}]
   [:rect {:x "14", :y "7", :width "3", :height "5"}]])

(defn trending-down [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "23 18 13.5 8.5 8.5 13.5 1 6"}]
   [:polyline {:points "17 18 23 18 23 12"}]])

(defn trending-up [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "23 6 13.5 15.5 8.5 10.5 1 18"}]
   [:polyline {:points "17 6 23 6 23 12"}]])

(defn triangle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"}]])

(defn truck [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "1", :y "3", :width "15", :height "13"}]
   [:polygon {:points "16 8 20 8 23 11 23 16 16 16 16 8"}]
   [:circle {:cx "5.5", :cy "18.5", :r "2.5"}]
   [:circle {:cx "18.5", :cy "18.5", :r "2.5"}]])

(defn tv [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "2", :y "7", :width "20", :height "15", :rx "2", :ry "2"}]
   [:polyline {:points "17 2 12 7 7 2"}]])

(defn twitch [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 2H3v16h5v4l4-4h5l4-4V2zM11 11V7M16 11V7"}]])

(defn twitter [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"}]])

(defn type [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "4 7 4 4 20 4 20 7"}]
   [:line {:x1 "9", :y1 "20", :x2 "15", :y2 "20"}]
   [:line {:x1 "12", :y1 "4", :x2 "12", :y2 "20"}]])

(defn umbrella [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M23 12a11.05 11.05 0 0 0-22 0zm-5 7a3 3 0 0 1-6 0v-7"}]])

(defn underline [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3"}]
   [:line {:x1 "4", :y1 "21", :x2 "20", :y2 "21"}]])

(defn unlock [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "11", :width "18", :height "11", :rx "2", :ry "2"}]
   [:path {:d "M7 11V7a5 5 0 0 1 9.9-1"}]])

(defn upload-cloud [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "16 16 12 12 8 16"}]
   [:line {:x1 "12", :y1 "12", :x2 "12", :y2 "21"}]
   [:path {:d "M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"}]
   [:polyline {:points "16 16 12 12 8 16"}]])

(defn upload [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
   [:polyline {:points "17 8 12 3 7 8"}]
   [:line {:x1 "12", :y1 "3", :x2 "12", :y2 "15"}]])

(defn user-check [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "8.5", :cy "7", :r "4"}]
   [:polyline {:points "17 11 19 13 23 9"}]])

(defn user-minus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "8.5", :cy "7", :r "4"}]
   [:line {:x1 "23", :y1 "11", :x2 "17", :y2 "11"}]])

(defn user-plus [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "8.5", :cy "7", :r "4"}]
   [:line {:x1 "20", :y1 "8", :x2 "20", :y2 "14"}]
   [:line {:x1 "23", :y1 "11", :x2 "17", :y2 "11"}]])

(defn user-x [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "8.5", :cy "7", :r "4"}]
   [:line {:x1 "18", :y1 "8", :x2 "23", :y2 "13"}]
   [:line {:x1 "23", :y1 "8", :x2 "18", :y2 "13"}]])

(defn user [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "12", :cy "7", :r "4"}]])

(defn users [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "9", :cy "7", :r "4"}]
   [:path {:d "M23 21v-2a4 4 0 0 0-3-3.87"}]
   [:path {:d "M16 3.13a4 4 0 0 1 0 7.75"}]])

(defn video-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34l1 1L23 7v10"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn video [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "23 7 16 12 23 17 23 7"}]
   [:rect {:x "1", :y "5", :width "15", :height "14", :rx "2", :ry "2"}]])

(defn voicemail [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "5.5", :cy "11.5", :r "4.5"}]
   [:circle {:cx "18.5", :cy "11.5", :r "4.5"}]
   [:line {:x1 "5.5", :y1 "16", :x2 "18.5", :y2 "16"}]])

(defn volume-1 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "11 5 6 9 2 9 2 15 6 15 11 19 11 5"}]
   [:path {:d "M15.54 8.46a5 5 0 0 1 0 7.07"}]])

(defn volume-2 [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "11 5 6 9 2 9 2 15 6 15 11 19 11 5"}]
   [:path
    {:d "M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"}]])

(defn volume-x [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "11 5 6 9 2 9 2 15 6 15 11 19 11 5"}]
   [:line {:x1 "23", :y1 "9", :x2 "17", :y2 "15"}]
   [:line {:x1 "17", :y1 "9", :x2 "23", :y2 "15"}]])

(defn volume [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "11 5 6 9 2 9 2 15 6 15 11 19 11 5"}]])

(defn watch [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "7"}]
   [:polyline {:points "12 9 12 12 13.5 13.5"}]
   [:path
    {:d
     "M16.51 17.35l-.35 3.83a2 2 0 0 1-2 1.82H9.83a2 2 0 0 1-2-1.82l-.35-3.83m.01-10.7l.35-3.83A2 2 0 0 1 9.83 1h4.35a2 2 0 0 1 2 1.82l.35 3.83"}]])

(defn wifi-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]
   [:path {:d "M16.72 11.06A10.94 10.94 0 0 1 19 12.55"}]
   [:path {:d "M5 12.55a10.94 10.94 0 0 1 5.17-2.39"}]
   [:path {:d "M10.71 5.05A16 16 0 0 1 22.58 9"}]
   [:path {:d "M1.42 9a15.91 15.91 0 0 1 4.7-2.88"}]
   [:path {:d "M8.53 16.11a6 6 0 0 1 6.95 0"}]
   [:line {:x1 "12", :y1 "20", :x2 "12.01", :y2 "20"}]])

(defn wifi [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path {:d "M5 12.55a11 11 0 0 1 14.08 0"}]
   [:path {:d "M1.42 9a16 16 0 0 1 21.16 0"}]
   [:path {:d "M8.53 16.11a6 6 0 0 1 6.95 0"}]
   [:line {:x1 "12", :y1 "20", :x2 "12.01", :y2 "20"}]])

(defn wind [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2"}]])

(defn x-circle [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "12", :cy "12", :r "10"}]
   [:line {:x1 "15", :y1 "9", :x2 "9", :y2 "15"}]
   [:line {:x1 "9", :y1 "9", :x2 "15", :y2 "15"}]])

(defn x-octagon [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon
    {:points
     "7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"}]
   [:line {:x1 "15", :y1 "9", :x2 "9", :y2 "15"}]
   [:line {:x1 "9", :y1 "9", :x2 "15", :y2 "15"}]])

(defn x-square [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:rect {:x "3", :y "3", :width "18", :height "18", :rx "2", :ry "2"}]
   [:line {:x1 "9", :y1 "9", :x2 "15", :y2 "15"}]
   [:line {:x1 "15", :y1 "9", :x2 "9", :y2 "15"}]])

(defn x [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:line {:x1 "18", :y1 "6", :x2 "6", :y2 "18"}]
   [:line {:x1 "6", :y1 "6", :x2 "18", :y2 "18"}]])

(defn youtube [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:path
    {:d
     "M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-2 29 29 0 0 0 .46-5.25 29 29 0 0 0-.46-5.33z"}]
   [:polygon {:points "9.75 15.02 15.5 11.75 9.75 8.48 9.75 15.02"}]])

(defn zap-off [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polyline {:points "12.41 6.75 13 2 10.57 4.92"}]
   [:polyline {:points "18.57 12.91 21 10 15.66 10"}]
   [:polyline {:points "8 8 3 14 12 14 11 22 16 16"}]
   [:line {:x1 "1", :y1 "1", :x2 "23", :y2 "23"}]])

(defn zap [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:polygon {:points "13 2 3 14 12 14 11 22 21 10 12 10 13 2"}]])

(defn zoom-in [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "11", :cy "11", :r "8"}]
   [:line {:x1 "21", :y1 "21", :x2 "16.65", :y2 "16.65"}]
   [:line {:x1 "11", :y1 "8", :x2 "11", :y2 "14"}]
   [:line {:x1 "8", :y1 "11", :x2 "14", :y2 "11"}]])

(defn zoom-out [{:keys [size color style stroke-width] :or {size 24 color "currentColor" stroke-width 2}}]
  [:svg
   {:stroke color,
    :fill "none",
    :stroke-linejoin "round",
    :width size,
    :xmlns "http://www.w3.org/2000/svg",
    :view-box "0 0 24 24",
    :style style,
    :stroke-linecap "round",
    :stroke-width stroke-width,
    :pointer-events "none",
    :height size}
   [:circle {:cx "11", :cy "11", :r "8"}]
   [:line {:x1 "21", :y1 "21", :x2 "16.65", :y2 "16.65"}]
   [:line {:x1 "8", :y1 "11", :x2 "14", :y2 "11"}]])

(def all-icons
[{:icon-name "activity", :icon-fn activity}
 {:icon-name "airplay", :icon-fn airplay}
 {:icon-name "alert-circle", :icon-fn alert-circle}
 {:icon-name "alert-octagon", :icon-fn alert-octagon}
 {:icon-name "alert-triangle", :icon-fn alert-triangle}
 {:icon-name "align-center", :icon-fn align-center}
 {:icon-name "align-justify", :icon-fn align-justify}
 {:icon-name "align-left", :icon-fn align-left}
 {:icon-name "align-right", :icon-fn align-right}
 {:icon-name "anchor", :icon-fn anchor}
 {:icon-name "aperture", :icon-fn aperture}
 {:icon-name "archive", :icon-fn archive}
 {:icon-name "arrow-down-circle", :icon-fn arrow-down-circle}
 {:icon-name "arrow-down-left", :icon-fn arrow-down-left}
 {:icon-name "arrow-down-right", :icon-fn arrow-down-right}
 {:icon-name "arrow-down", :icon-fn arrow-down}
 {:icon-name "arrow-left-circle", :icon-fn arrow-left-circle}
 {:icon-name "arrow-left", :icon-fn arrow-left}
 {:icon-name "arrow-right-circle", :icon-fn arrow-right-circle}
 {:icon-name "arrow-right", :icon-fn arrow-right}
 {:icon-name "arrow-up-circle", :icon-fn arrow-up-circle}
 {:icon-name "arrow-up-left", :icon-fn arrow-up-left}
 {:icon-name "arrow-up-right", :icon-fn arrow-up-right}
 {:icon-name "arrow-up", :icon-fn arrow-up}
 {:icon-name "at-sign", :icon-fn at-sign}
 {:icon-name "award", :icon-fn award}
 {:icon-name "bar-chart-2", :icon-fn bar-chart-2}
 {:icon-name "bar-chart", :icon-fn bar-chart}
 {:icon-name "battery-charging", :icon-fn battery-charging}
 {:icon-name "battery", :icon-fn battery}
 {:icon-name "bell-off", :icon-fn bell-off}
 {:icon-name "bell", :icon-fn bell}
 {:icon-name "bluetooth", :icon-fn bluetooth}
 {:icon-name "bold", :icon-fn bold}
 {:icon-name "book-open", :icon-fn book-open}
 {:icon-name "book", :icon-fn book}
 {:icon-name "bookmark", :icon-fn bookmark}
 {:icon-name "box", :icon-fn box}
 {:icon-name "briefcase", :icon-fn briefcase}
 {:icon-name "calendar", :icon-fn calendar}
 {:icon-name "camera-off", :icon-fn camera-off}
 {:icon-name "camera", :icon-fn camera}
 {:icon-name "cast", :icon-fn cast}
 {:icon-name "check-circle", :icon-fn check-circle}
 {:icon-name "check-square", :icon-fn check-square}
 {:icon-name "check", :icon-fn check}
 {:icon-name "chevron-down", :icon-fn chevron-down}
 {:icon-name "chevron-left", :icon-fn chevron-left}
 {:icon-name "chevron-right", :icon-fn chevron-right}
 {:icon-name "chevron-up", :icon-fn chevron-up}
 {:icon-name "chevrons-down", :icon-fn chevrons-down}
 {:icon-name "chevrons-left", :icon-fn chevrons-left}
 {:icon-name "chevrons-right", :icon-fn chevrons-right}
 {:icon-name "chevrons-up", :icon-fn chevrons-up}
 {:icon-name "chrome", :icon-fn chrome}
 {:icon-name "circle", :icon-fn circle}
 {:icon-name "clipboard", :icon-fn clipboard}
 {:icon-name "clock", :icon-fn clock}
 {:icon-name "cloud-drizzle", :icon-fn cloud-drizzle}
 {:icon-name "cloud-lightning", :icon-fn cloud-lightning}
 {:icon-name "cloud-off", :icon-fn cloud-off}
 {:icon-name "cloud-rain", :icon-fn cloud-rain}
 {:icon-name "cloud-snow", :icon-fn cloud-snow}
 {:icon-name "cloud", :icon-fn cloud}
 {:icon-name "code", :icon-fn code}
 {:icon-name "codepen", :icon-fn codepen}
 {:icon-name "codesandbox", :icon-fn codesandbox}
 {:icon-name "coffee", :icon-fn coffee}
 {:icon-name "columns", :icon-fn columns}
 {:icon-name "command", :icon-fn command}
 {:icon-name "compass", :icon-fn compass}
 {:icon-name "copy", :icon-fn copy}
 {:icon-name "corner-down-left", :icon-fn corner-down-left}
 {:icon-name "corner-down-right", :icon-fn corner-down-right}
 {:icon-name "corner-left-down", :icon-fn corner-left-down}
 {:icon-name "corner-left-up", :icon-fn corner-left-up}
 {:icon-name "corner-right-down", :icon-fn corner-right-down}
 {:icon-name "corner-right-up", :icon-fn corner-right-up}
 {:icon-name "corner-up-left", :icon-fn corner-up-left}
 {:icon-name "corner-up-right", :icon-fn corner-up-right}
 {:icon-name "cpu", :icon-fn cpu}
 {:icon-name "credit-card", :icon-fn credit-card}
 {:icon-name "crop", :icon-fn crop}
 {:icon-name "crosshair", :icon-fn crosshair}
 {:icon-name "database", :icon-fn database}
 {:icon-name "delete", :icon-fn delete}
 {:icon-name "disc", :icon-fn disc}
 {:icon-name "divide-circle", :icon-fn divide-circle}
 {:icon-name "divide-square", :icon-fn divide-square}
 {:icon-name "divide", :icon-fn divide}
 {:icon-name "dollar-sign", :icon-fn dollar-sign}
 {:icon-name "download-cloud", :icon-fn download-cloud}
 {:icon-name "download", :icon-fn download}
 {:icon-name "dribbble", :icon-fn dribbble}
 {:icon-name "droplet", :icon-fn droplet}
 {:icon-name "edit-2", :icon-fn edit-2}
 {:icon-name "edit-3", :icon-fn edit-3}
 {:icon-name "edit", :icon-fn edit}
 {:icon-name "external-link", :icon-fn external-link}
 {:icon-name "eye-off", :icon-fn eye-off}
 {:icon-name "eye", :icon-fn eye}
 {:icon-name "facebook", :icon-fn facebook}
 {:icon-name "fast-forward", :icon-fn fast-forward}
 {:icon-name "feather", :icon-fn feather}
 {:icon-name "figma", :icon-fn figma}
 {:icon-name "file-minus", :icon-fn file-minus}
 {:icon-name "file-plus", :icon-fn file-plus}
 {:icon-name "file-text", :icon-fn file-text}
 {:icon-name "file", :icon-fn file}
 {:icon-name "film", :icon-fn film}
 {:icon-name "filter", :icon-fn filter}
 {:icon-name "flag", :icon-fn flag}
 {:icon-name "folder-minus", :icon-fn folder-minus}
 {:icon-name "folder-plus", :icon-fn folder-plus}
 {:icon-name "folder", :icon-fn folder}
 {:icon-name "framer", :icon-fn framer}
 {:icon-name "frown", :icon-fn frown}
 {:icon-name "gift", :icon-fn gift}
 {:icon-name "git-branch", :icon-fn git-branch}
 {:icon-name "git-commit", :icon-fn git-commit}
 {:icon-name "git-merge", :icon-fn git-merge}
 {:icon-name "git-pull-request", :icon-fn git-pull-request}
 {:icon-name "github", :icon-fn github}
 {:icon-name "gitlab", :icon-fn gitlab}
 {:icon-name "globe", :icon-fn globe}
 {:icon-name "grid", :icon-fn grid}
 {:icon-name "hard-drive", :icon-fn hard-drive}
 {:icon-name "hash", :icon-fn hash}
 {:icon-name "headphones", :icon-fn headphones}
 {:icon-name "heart", :icon-fn heart}
 {:icon-name "help-circle", :icon-fn help-circle}
 {:icon-name "hexagon", :icon-fn hexagon}
 {:icon-name "home", :icon-fn home}
 {:icon-name "image", :icon-fn image}
 {:icon-name "inbox", :icon-fn inbox}
 {:icon-name "info", :icon-fn info}
 {:icon-name "instagram", :icon-fn instagram}
 {:icon-name "italic", :icon-fn italic}
 {:icon-name "key", :icon-fn key}
 {:icon-name "layers", :icon-fn layers}
 {:icon-name "layout", :icon-fn layout}
 {:icon-name "life-buoy", :icon-fn life-buoy}
 {:icon-name "link-2", :icon-fn link-2}
 {:icon-name "link", :icon-fn link}
 {:icon-name "linkedin", :icon-fn linkedin}
 {:icon-name "list", :icon-fn list}
 {:icon-name "loader", :icon-fn loader}
 {:icon-name "lock", :icon-fn lock}
 {:icon-name "log-in", :icon-fn log-in}
 {:icon-name "log-out", :icon-fn log-out}
 {:icon-name "mail", :icon-fn mail}
 {:icon-name "map-pin", :icon-fn map-pin}
 {:icon-name "map", :icon-fn map}
 {:icon-name "maximize-2", :icon-fn maximize-2}
 {:icon-name "maximize", :icon-fn maximize}
 {:icon-name "meh", :icon-fn meh}
 {:icon-name "menu", :icon-fn menu}
 {:icon-name "message-circle", :icon-fn message-circle}
 {:icon-name "message-square", :icon-fn message-square}
 {:icon-name "mic-off", :icon-fn mic-off}
 {:icon-name "mic", :icon-fn mic}
 {:icon-name "minimize-2", :icon-fn minimize-2}
 {:icon-name "minimize", :icon-fn minimize}
 {:icon-name "minus-circle", :icon-fn minus-circle}
 {:icon-name "minus-square", :icon-fn minus-square}
 {:icon-name "minus", :icon-fn minus}
 {:icon-name "monitor", :icon-fn monitor}
 {:icon-name "moon", :icon-fn moon}
 {:icon-name "more-horizontal", :icon-fn more-horizontal}
 {:icon-name "more-vertical", :icon-fn more-vertical}
 {:icon-name "mouse-pointer", :icon-fn mouse-pointer}
 {:icon-name "move", :icon-fn move}
 {:icon-name "music", :icon-fn music}
 {:icon-name "navigation-2", :icon-fn navigation-2}
 {:icon-name "navigation", :icon-fn navigation}
 {:icon-name "octagon", :icon-fn octagon}
 {:icon-name "package", :icon-fn package}
 {:icon-name "paperclip", :icon-fn paperclip}
 {:icon-name "pause-circle", :icon-fn pause-circle}
 {:icon-name "pause", :icon-fn pause}
 {:icon-name "pen-tool", :icon-fn pen-tool}
 {:icon-name "percent", :icon-fn percent}
 {:icon-name "phone-call", :icon-fn phone-call}
 {:icon-name "phone-forwarded", :icon-fn phone-forwarded}
 {:icon-name "phone-incoming", :icon-fn phone-incoming}
 {:icon-name "phone-missed", :icon-fn phone-missed}
 {:icon-name "phone-off", :icon-fn phone-off}
 {:icon-name "phone-outgoing", :icon-fn phone-outgoing}
 {:icon-name "phone", :icon-fn phone}
 {:icon-name "pie-chart", :icon-fn pie-chart}
 {:icon-name "play-circle", :icon-fn play-circle}
 {:icon-name "play", :icon-fn play}
 {:icon-name "plus-circle", :icon-fn plus-circle}
 {:icon-name "plus-square", :icon-fn plus-square}
 {:icon-name "plus", :icon-fn plus}
 {:icon-name "pocket", :icon-fn pocket}
 {:icon-name "power", :icon-fn power}
 {:icon-name "printer", :icon-fn printer}
 {:icon-name "radio", :icon-fn radio}
 {:icon-name "refresh-ccw", :icon-fn refresh-ccw}
 {:icon-name "refresh-cw", :icon-fn refresh-cw}
 {:icon-name "repeat", :icon-fn repeat}
 {:icon-name "rewind", :icon-fn rewind}
 {:icon-name "rotate-ccw", :icon-fn rotate-ccw}
 {:icon-name "rotate-cw", :icon-fn rotate-cw}
 {:icon-name "rss", :icon-fn rss}
 {:icon-name "save", :icon-fn save}
 {:icon-name "scissors", :icon-fn scissors}
 {:icon-name "search", :icon-fn search}
 {:icon-name "send", :icon-fn send}
 {:icon-name "server", :icon-fn server}
 {:icon-name "settings", :icon-fn settings}
 {:icon-name "share-2", :icon-fn share-2}
 {:icon-name "share", :icon-fn share}
 {:icon-name "shield-off", :icon-fn shield-off}
 {:icon-name "shield", :icon-fn shield}
 {:icon-name "shopping-bag", :icon-fn shopping-bag}
 {:icon-name "shopping-cart", :icon-fn shopping-cart}
 {:icon-name "shuffle", :icon-fn shuffle}
 {:icon-name "sidebar", :icon-fn sidebar}
 {:icon-name "skip-back", :icon-fn skip-back}
 {:icon-name "skip-forward", :icon-fn skip-forward}
 {:icon-name "slack", :icon-fn slack}
 {:icon-name "slash", :icon-fn slash}
 {:icon-name "sliders", :icon-fn sliders}
 {:icon-name "smartphone", :icon-fn smartphone}
 {:icon-name "smile", :icon-fn smile}
 {:icon-name "speaker", :icon-fn speaker}
 {:icon-name "square", :icon-fn square}
 {:icon-name "star", :icon-fn star}
 {:icon-name "stop-circle", :icon-fn stop-circle}
 {:icon-name "sun", :icon-fn sun}
 {:icon-name "sunrise", :icon-fn sunrise}
 {:icon-name "sunset", :icon-fn sunset}
 {:icon-name "tablet", :icon-fn tablet}
 {:icon-name "tag", :icon-fn tag}
 {:icon-name "target", :icon-fn target}
 {:icon-name "terminal", :icon-fn terminal}
 {:icon-name "thermometer", :icon-fn thermometer}
 {:icon-name "thumbs-down", :icon-fn thumbs-down}
 {:icon-name "thumbs-up", :icon-fn thumbs-up}
 {:icon-name "toggle-left", :icon-fn toggle-left}
 {:icon-name "toggle-right", :icon-fn toggle-right}
 {:icon-name "tool", :icon-fn tool}
 {:icon-name "trash-2", :icon-fn trash-2}
 {:icon-name "trash", :icon-fn trash}
 {:icon-name "trello", :icon-fn trello}
 {:icon-name "trending-down", :icon-fn trending-down}
 {:icon-name "trending-up", :icon-fn trending-up}
 {:icon-name "triangle", :icon-fn triangle}
 {:icon-name "truck", :icon-fn truck}
 {:icon-name "tv", :icon-fn tv}
 {:icon-name "twitch", :icon-fn twitch}
 {:icon-name "twitter", :icon-fn twitter}
 {:icon-name "type", :icon-fn type}
 {:icon-name "umbrella", :icon-fn umbrella}
 {:icon-name "underline", :icon-fn underline}
 {:icon-name "unlock", :icon-fn unlock}
 {:icon-name "upload-cloud", :icon-fn upload-cloud}
 {:icon-name "upload", :icon-fn upload}
 {:icon-name "user-check", :icon-fn user-check}
 {:icon-name "user-minus", :icon-fn user-minus}
 {:icon-name "user-plus", :icon-fn user-plus}
 {:icon-name "user-x", :icon-fn user-x}
 {:icon-name "user", :icon-fn user}
 {:icon-name "users", :icon-fn users}
 {:icon-name "video-off", :icon-fn video-off}
 {:icon-name "video", :icon-fn video}
 {:icon-name "voicemail", :icon-fn voicemail}
 {:icon-name "volume-1", :icon-fn volume-1}
 {:icon-name "volume-2", :icon-fn volume-2}
 {:icon-name "volume-x", :icon-fn volume-x}
 {:icon-name "volume", :icon-fn volume}
 {:icon-name "watch", :icon-fn watch}
 {:icon-name "wifi-off", :icon-fn wifi-off}
 {:icon-name "wifi", :icon-fn wifi}
 {:icon-name "wind", :icon-fn wind}
 {:icon-name "x-circle", :icon-fn x-circle}
 {:icon-name "x-octagon", :icon-fn x-octagon}
 {:icon-name "x-square", :icon-fn x-square}
 {:icon-name "x", :icon-fn x}
 {:icon-name "youtube", :icon-fn youtube}
 {:icon-name "zap-off", :icon-fn zap-off}
 {:icon-name "zap", :icon-fn zap}
 {:icon-name "zoom-in", :icon-fn zoom-in}
 {:icon-name "zoom-out", :icon-fn zoom-out}]
)

(def lookup
{"volume-x" volume-x,
 "trash" trash,
 "navigation-2" navigation-2,
 "percent" percent,
 "corner-up-right" corner-up-right,
 "slack" slack,
 "map" map,
 "youtube" youtube,
 "terminal" terminal,
 "file-text" file-text,
 "thermometer" thermometer,
 "battery-charging" battery-charging,
 "inbox" inbox,
 "minus-square" minus-square,
 "arrow-down" arrow-down,
 "battery" battery,
 "folder-plus" folder-plus,
 "volume-2" volume-2,
 "cloud" cloud,
 "printer" printer,
 "toggle-right" toggle-right,
 "save" save,
 "log-out" log-out,
 "folder" folder,
 "cloud-snow" cloud-snow,
 "book" book,
 "corner-down-left" corner-down-left,
 "mail" mail,
 "phone-incoming" phone-incoming,
 "play" play,
 "package" package,
 "plus-circle" plus-circle,
 "codepen" codepen,
 "shield-off" shield-off,
 "rewind" rewind,
 "list" list,
 "thumbs-down" thumbs-down,
 "toggle-left" toggle-left,
 "dribbble" dribbble,
 "disc" disc,
 "calendar" calendar,
 "map-pin" map-pin,
 "play-circle" play-circle,
 "user-x" user-x,
 "github" github,
 "cast" cast,
 "star" star,
 "clipboard" clipboard,
 "hard-drive" hard-drive,
 "rotate-ccw" rotate-ccw,
 "trending-up" trending-up,
 "chevron-down" chevron-down,
 "crop" crop,
 "more-horizontal" more-horizontal,
 "alert-octagon" alert-octagon,
 "shopping-cart" shopping-cart,
 "refresh-cw" refresh-cw,
 "server" server,
 "copy" copy,
 "command" command,
 "zap-off" zap-off,
 "x-square" x-square,
 "corner-right-up" corner-right-up,
 "menu" menu,
 "columns" columns,
 "hexagon" hexagon,
 "align-left" align-left,
 "book-open" book-open,
 "arrow-left" arrow-left,
 "cloud-off" cloud-off,
 "grid" grid,
 "camera-off" camera-off,
 "delete" delete,
 "git-pull-request" git-pull-request,
 "refresh-ccw" refresh-ccw,
 "figma" figma,
 "settings" settings,
 "corner-up-left" corner-up-left,
 "chevron-left" chevron-left,
 "wifi" wifi,
 "image" image,
 "bar-chart-2" bar-chart-2,
 "send" send,
 "user-plus" user-plus,
 "activity" activity,
 "unlock" unlock,
 "plus" plus,
 "download" download,
 "git-branch" git-branch,
 "hash" hash,
 "octagon" octagon,
 "upload" upload,
 "chevrons-left" chevrons-left,
 "minimize" minimize,
 "key" key,
 "repeat" repeat,
 "gift" gift,
 "x" x,
 "user" user,
 "chevrons-right" chevrons-right,
 "user-minus" user-minus,
 "cpu" cpu,
 "info" info,
 "compass" compass,
 "arrow-right-circle" arrow-right-circle,
 "briefcase" briefcase,
 "sliders" sliders,
 "tablet" tablet,
 "sidebar" sidebar,
 "smile" smile,
 "video-off" video-off,
 "cloud-rain" cloud-rain,
 "video" video,
 "arrow-right" arrow-right,
 "phone-outgoing" phone-outgoing,
 "x-octagon" x-octagon,
 "shuffle" shuffle,
 "camera" camera,
 "stop-circle" stop-circle,
 "corner-left-down" corner-left-down,
 "box" box,
 "minus" minus,
 "arrow-left-circle" arrow-left-circle,
 "twitter" twitter,
 "linkedin" linkedin,
 "at-sign" at-sign,
 "cloud-lightning" cloud-lightning,
 "chevrons-up" chevrons-up,
 "upload-cloud" upload-cloud,
 "coffee" coffee,
 "watch" watch,
 "umbrella" umbrella,
 "shopping-bag" shopping-bag,
 "anchor" anchor,
 "loader" loader,
 "maximize-2" maximize-2,
 "crosshair" crosshair,
 "mic-off" mic-off,
 "voicemail" voicemail,
 "instagram" instagram,
 "align-justify" align-justify,
 "cloud-drizzle" cloud-drizzle,
 "maximize" maximize,
 "power" power,
 "message-circle" message-circle,
 "align-center" align-center,
 "divide-square" divide-square,
 "tag" tag,
 "alert-triangle" alert-triangle,
 "fast-forward" fast-forward,
 "bookmark" bookmark,
 "check" check,
 "file-plus" file-plus,
 "scissors" scissors,
 "bell-off" bell-off,
 "edit" edit,
 "arrow-up-right" arrow-up-right,
 "credit-card" credit-card,
 "mic" mic,
 "archive" archive,
 "radio" radio,
 "users" users,
 "paperclip" paperclip,
 "framer" framer,
 "wifi-off" wifi-off,
 "tv" tv,
 "trello" trello,
 "folder-minus" folder-minus,
 "moon" moon,
 "music" music,
 "gitlab" gitlab,
 "eye" eye,
 "pause" pause,
 "database" database,
 "layout" layout,
 "phone-missed" phone-missed,
 "truck" truck,
 "rotate-cw" rotate-cw,
 "triangle" triangle,
 "bell" bell,
 "facebook" facebook,
 "filter" filter,
 "pocket" pocket,
 "eye-off" eye-off,
 "sunrise" sunrise,
 "headphones" headphones,
 "feather" feather,
 "download-cloud" download-cloud,
 "twitch" twitch,
 "speaker" speaker,
 "clock" clock,
 "codesandbox" codesandbox,
 "smartphone" smartphone,
 "heart" heart,
 "message-square" message-square,
 "trending-down" trending-down,
 "slash" slash,
 "sun" sun,
 "arrow-down-right" arrow-down-right,
 "move" move,
 "link" link,
 "square" square,
 "award" award,
 "sunset" sunset,
 "x-circle" x-circle,
 "type" type,
 "chrome" chrome,
 "home" home,
 "phone" phone,
 "chevron-right" chevron-right,
 "more-vertical" more-vertical,
 "git-commit" git-commit,
 "trash-2" trash-2,
 "bar-chart" bar-chart,
 "arrow-down-left" arrow-down-left,
 "layers" layers,
 "italic" italic,
 "git-merge" git-merge,
 "alert-circle" alert-circle,
 "life-buoy" life-buoy,
 "target" target,
 "arrow-up" arrow-up,
 "volume" volume,
 "navigation" navigation,
 "divide" divide,
 "pen-tool" pen-tool,
 "phone-off" phone-off,
 "shield" shield,
 "lock" lock,
 "corner-left-up" corner-left-up,
 "tool" tool,
 "arrow-down-circle" arrow-down-circle,
 "file" file,
 "log-in" log-in,
 "arrow-up-circle" arrow-up-circle,
 "minus-circle" minus-circle,
 "meh" meh,
 "chevron-up" chevron-up,
 "link-2" link-2,
 "thumbs-up" thumbs-up,
 "corner-down-right" corner-down-right,
 "zoom-out" zoom-out,
 "zoom-in" zoom-in,
 "mouse-pointer" mouse-pointer,
 "align-right" align-right,
 "wind" wind,
 "external-link" external-link,
 "skip-back" skip-back,
 "chevrons-down" chevrons-down,
 "circle" circle,
 "pause-circle" pause-circle,
 "plus-square" plus-square,
 "dollar-sign" dollar-sign,
 "pie-chart" pie-chart,
 "search" search,
 "bold" bold,
 "droplet" droplet,
 "edit-2" edit-2,
 "check-square" check-square,
 "monitor" monitor,
 "phone-forwarded" phone-forwarded,
 "underline" underline,
 "film" film,
 "code" code,
 "phone-call" phone-call,
 "zap" zap,
 "rss" rss,
 "help-circle" help-circle,
 "flag" flag,
 "check-circle" check-circle,
 "frown" frown,
 "arrow-up-left" arrow-up-left,
 "aperture" aperture,
 "globe" globe,
 "edit-3" edit-3,
 "minimize-2" minimize-2,
 "share" share,
 "share-2" share-2,
 "skip-forward" skip-forward,
 "file-minus" file-minus,
 "corner-right-down" corner-right-down,
 "volume-1" volume-1,
 "airplay" airplay,
 "divide-circle" divide-circle,
 "user-check" user-check,
 "bluetooth" bluetooth}
)
