(ns rx.browser.location)

(defn create-manager [opts]
  (let [on-popstate (fn [e]
                      (.preventDefault e)
                      (prn "POP"))]
    (.addEventListener js/window "popstate" on-popstate)
    
    {:on-popstate on-popstate}))

(defn destroy-manager [{:keys [on-popstate]}]
  (.removeEventListener js/window "popstate" on-popstate))

(defonce !manager (atom nil))

(defn start-manager! [opts]
  (when @!manager
    (destroy-manager @!manager))
  (reset! !manager (create-manager opts)))


(comment

  

  (start-manager! {})


  )


