(ns rx.browser.styleguide.css
  (:require [rx.theme :as th]))

(defn rules []
  [[(str "." (th/kw->class-name :rx.browser.styleguide/section))
    [:dd {:margin-bottom "10px"}]
    #_[:p {:margin-bottom "20px"}]]])




