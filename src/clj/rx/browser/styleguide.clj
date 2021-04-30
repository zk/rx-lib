(ns rx.browser.styleguide
  (:require [rx.kitchen-sink :as ks]
            [cljfmt.core :as cljfmt]))

(defmacro example [opts]
  (let [{:keys [form]} opts
        #_(cljfmt/reformat-string (pr-str (:form opts)))
        form-str (ks/pp-str form)
        opts (merge
               (dissoc opts :form)
               {:form-str form-str})
        opt-keywords (->> opts :options (map first) (map name) (map keyword) vec)]
    `[rx.browser.styleguide/example-container
      (merge
        ~opts
        {:form-str ~form-str
         :render (fn [{:keys ~opt-keywords}]
                   ~form)})]))


#_(ks/spy
    (macroexpand-1
      '(example
         {:form
          [frame
           {:debug? debug?
            :style {:width "100%"
                    :height 500}
            :header [ui/group {:pad 8} "Header"]
            :content [ui/group {:pad 8} "Content"]
            :left [ui/group {:pad 8} "Left"]
            :right [ui/group {:pad 8} "Right"]
            :footer [ui/group {:pad 8} "Footer"]}]
          :options
          [[:debug? :boolean "Debug"]]})))



