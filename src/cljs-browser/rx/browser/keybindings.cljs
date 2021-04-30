(ns rx.browser.keybindings
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]))

(defn create-state []
  {:!binding-ids-stack (atom '())
   :!id->bindings (atom {})
   :!keydown-listener (atom nil)
   :!debug? (atom false)})

(defn add-bindings [{:keys [!binding-ids-stack
                            !id->bindings
                            !debug?]}
                    id bindings]
  
  (swap! !binding-ids-stack
    (fn [ids]
      (distinct
        (conj ids id))))
  (swap! !id->bindings assoc id bindings)
  (when @!debug?
    (ks/pp @!binding-ids-stack)))

(defn remove-bindings [{:keys [!binding-ids-stack
                               !id->bindings
                               !debug?]}
                       id]
  (when @!debug?
    (println "Removing binding" id))
  (swap! !binding-ids-stack
    (fn [ids]
      (->> ids
           (remove #(= % id)))))
  (swap! !id->bindings dissoc id))

(defn event->clj [e]
  {:shiftKey (.-shiftKey e)
   :altKey (.-altKey e)
   :metaKey (.-metaKey e)
   :ctrlKey (.-ctrlKey e)
   :key (.-key e)
   :keyCode (.-keyCode e)})

(defn binding-matches? [{:keys [altKey
                                shiftKey
                                metaKey
                                ctrlKey
                                key
                                keyCode]}
                        {:keys [key-name key-code
                                shift? alt?
                                meta? ctrl?]}]
  (and
    (if key-name
      (= key-name key)
      true)
    (if key-code
      (= key-code keyCode)
      true)

    (if (not shift?) (not shiftKey) true)
    (if (not alt?) (not altKey) true)
    (if (not meta?) (not metaKey) true)
    (if (not ctrl?) (not ctrlKey) true)
    
    (if shift? shiftKey true)
    (if alt? altKey true)
    (if meta? metaKey true)
    (if ctrl? ctrlKey true)))

(defn collapse-bindings [{:keys [!id->bindings
                                 !binding-ids-stack]}]
  (->> @!binding-ids-stack
       (map (fn [id]
              (->> (get @!id->bindings id)
                   (map #(merge
                           {::frame-id id}
                           %)))))
       (reduce concat)))

(defn create-keydown-listener [{:keys [!id->bindings
                                       !debug?]
                                :as state}]
  (fn [e]
    (let [event (event->clj e)
          bindings (collapse-bindings state)
          _ (when @!debug?
              (println "KB - Checking against" event))
          binding (->> bindings
                       (some (fn [binding]
                               (let [match? (binding-matches? event binding)]
                                 (when @!debug?
                                   (println match? (pr-str binding)))
                                 (when match?
                                   binding)))))
          handler (:handler binding)]
      (when handler
        (when @!debug?
          (println "Matched binding" (str "[" (::frame-id binding) "]")
            (pr-str
              (dissoc binding ::frame-id :handler))))
        (handler e)
        true))))

(defn disable-stack [{:keys [!keydown-listener]}]
  (browser/unlisten! js/window :keydown @!keydown-listener)
  (reset! !keydown-listener nil))

(defn enable-stack [{:keys [!keydown-listener] :as stack}]
  (when @!keydown-listener
    (disable-stack stack))
  (let [keydown-listener (create-keydown-listener stack)]
    (browser/listen! js/window :keydown keydown-listener)
    (reset! !keydown-listener keydown-listener))
  stack)

(defn inspect [state]
  (println "KB state")
  (ks/pp state))

(defonce DEFAULT (create-state))
(enable-stack DEFAULT)

(defn add-bindings! [id bindings]
  (add-bindings DEFAULT id bindings))

(defn remove-bindings! [id]
  (remove-bindings DEFAULT id))

(defn set-debug! [debug?]
  (reset! (:!debug? DEFAULT) debug?))

(defn inspect! []
  (inspect DEFAULT))

(comment
  
  (add-bindings!
    ::test-bindings
    [{:key-name "b"
      :desc "Runs b"
      :handler prn}
     {:key-name "a"
      :desc "AAA"
      :handler prn}])

  (add-bindings!
    ::another-one
    [{:key-name "b"
      :desc "Alt b"
      :handler prn}])

  (remove-bindings! ::another-one)

  (set-debug! false)

  (ks/spy DEFAULT)

  (inspect!)

  )
