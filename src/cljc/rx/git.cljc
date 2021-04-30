(ns rx.git
  #?(:clj
     (:import [org.eclipse.jgit.storage.file
               FileRepositoryBuilder]
              [org.eclipse.jgit.lib ObjectId])))

#?(:clj
   (defn get-head-hash [path]
     (let [r (-> (FileRepositoryBuilder.)
                 (.setGitDir (java.io.File. path))
                 (.readEnvironment)
                 (.findGitDir)
                 (.build))
           head (.resolve r "HEAD")]
       (when head
         (ObjectId/toString head)))))

#?(:clj
   (defmacro head-hash [path]
     (try
       (let [hash# (get-head-hash path)]
         `~hash#)
       (catch Exception e nil))))




