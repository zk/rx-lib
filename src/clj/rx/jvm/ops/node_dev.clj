(ns rx.jvm.ops.node-dev
  (:require [rx.jvm.ops.dev :as dev]))

(def config
  {:id "rx-node"
   :node-proc {:max-old-space-size 4096}
   :target-path "target/rx-node/dev"
   :main-namespace "rx.dev.node"
   :watch-dirs ["src/cljs-node"
                "src/cljs"
                "src/cljc"]
   :compiler
   {:recompile-dependents true
    :target :nodejs
    :infer-externs true}})


(defn start! []
  (dev/start-dev! config))

(comment

  (start!)

  )


