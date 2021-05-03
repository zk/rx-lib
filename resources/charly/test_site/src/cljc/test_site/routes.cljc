(ns test-site.routes)

(defn routes [opts]
  [["/" :index]
   ["/foo/:bar" :foo-bar]])
