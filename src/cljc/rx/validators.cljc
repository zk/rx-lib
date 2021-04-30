(ns rx.validators)

(defn email? [s]
  (when s
    (re-find #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$" s)))


