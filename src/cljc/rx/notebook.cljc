(ns rx.notebook
  (:require [rx.kitchen-sink :as ks]))

(def box-attrs
  {::doc-id {:box.attr/ident? true
             :box.attr/value-type :box.type/string}
   ::title {:box.attr/index? true
            :box.attr/value-type :box.type/string}
   ::last-updated-ts {:box.attr/index? true
                      :box.attr/value-type :box.type/long}
   ::doc-link-title-key {:box.attr/index? true
                         :box.attr/value-type :box.type/string}})

(defn box-schema [& [override]]
  (merge
    {:box/attrs box-attrs}
    override))
