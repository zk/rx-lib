(ns rx.view
  (:refer-clojure :exclude [def]))

(defmacro defview [sym & body]
  (let [ns# (or (namespace sym) (str (ns-name *ns*)))
        name# (name sym)
        kw# (keyword ns# name#)]
    `(do
       (defn ~sym ~@body)
       (rx.view/register-views {~kw# ~sym}))))

