(ns rx.annie
  (:require
   [rx.kitchen-sink :as ks]
   [clojure.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! take!
            alts! timeout pipe mult tap
            go go-loop]]))

(def error-margin 0.001)

(def ms-per-frame (/ 1000 60))

(defn <raf []
  (let [ch (chan)]
    (if (and (resolve 'js/window)
             (.-requestAnimationFrame js/window))
      (.requestAnimationFrame
        js/window
        (fn []
          (close! ch)))
      (js/setTimeout
        (fn []
          (close! ch))
        16.6666))
    ch))

(defonce !anims (atom {}))




(def !animations (atom {}))

;; https://github.com/chenglou/react-motion/blob/master/src/stepper.js

(defn step
  [framerate
   x
   v
   dest-x
   k
   b]
  (let [f-spring (* (- k) (- x dest-x))
        f-damper (* (- b) v)
        a (+ f-spring f-damper)
        new-v (+ v (* a framerate))
        new-x (+ x (* new-v framerate))]
    (if (and (< (ks/abs new-v) error-margin)
             (< (ks/abs (- new-x dest-x)) error-margin))
      [dest-x 0]
      [new-x new-v])))

(defn raf [f]
  (if (and (resolve 'js/window)
           (.-requestAnimationFrame js/window))
    (.requestAnimationFrame js/window f)
    (js/setTimeout f 16.6)))

(defn -run [continue? from to callback & [spring-params]]
  (let [k (or (:k spring-params) 118)
        b (or (:b spring-params) 18)]
    (letfn [(do-frame [x v]
              (if (and (continue?) (not= x to))
                (let [[next-x next-v] (step (/ ms-per-frame 1000) x v to k b)]
                  (callback next-x)
                  (raf (fn []
                         (do-frame next-x next-v))))))]
      (do-frame from 0))))

(defn run [!anims from to callback]
  (let [!run-anim? (atom true)
        keep-running? (fn [] @!run-anim?)
        stop-anim (fn []
                    (reset! !run-anim? false))]
    (swap! !anims assoc (gensym) stop-anim)
    (-run
      keep-running?
      from
      to
      callback)))

(defn spring [!anims from to callback & [spring-params]]
  (let [!run-anim? (atom true)
        keep-running? (fn [] @!run-anim?)
        stop-anim (fn []
                    (reset! !run-anim? false))]
    (swap! !anims assoc (gensym) stop-anim)
    (-run
      keep-running?
      from
      to
      callback
      spring-params)))


(defn -run-eieo [continue? start-ts from to duration callback]
  (letfn [(do-frame []
            (when (continue?)
              (let [pct (/
                          (- (ks/now) start-ts)
                          duration)
                    norm-pct (if (< pct 0.5)
                               (* 2 pct pct)
                               (+ -1
                                 (* pct (- 4 (* 2 pct)))))

                    next-x (* norm-pct
                             (+ (- to from) from))]
                (callback next-x)
                (if (>
                      (ks/abs (- to next-x))
                      error-margin)
                  (raf (fn []
                         (do-frame)))
                  (callback to)))))]
    (do-frame)))

(defn eieo [!anims from to duration callback]
  (let [!run-anim? (atom true)
        keep-running? (fn [] @!run-anim?)
        stop-anim (fn []
                    (reset! !run-anim? false))
        start-ts (ks/now)]
    (swap! !anims assoc (gensym) stop-anim)
    (-run-eieo
      keep-running?
      start-ts
      from
      to
      duration
      callback)))

(defn stop-all [!anims]
  (doseq [[k v] @!anims]
    (v))
  (reset! !anims {}))

