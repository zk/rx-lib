(ns rx.err-test
  (:require
   [rx.kitchen-sink :as ks]
   [rx.err :refer-macros [<defn]]))

(<defn foobar [x]
  (throw (js/Error. "foo")))

(comment

  (ks/<pp (foobar 1))

  )
