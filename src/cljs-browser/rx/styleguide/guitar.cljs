(ns rx.styleguide.guitar)

(defn $view []
  [:div.container
   #_[:div.row
      [:div.col-sm-12
       [:br]
       [:br]
       [:div
        {:dangerouslySetInnerHTML
         {:__html
          (md/render-html
            (nu/slurp-cljs
              "src/md/guitar/intro.md"))}}]]]])
