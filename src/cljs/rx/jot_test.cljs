(ns rx.jot-test
  (:require [rx.kitchen-sink :as ks]
            [rx.test :as test]
            [rx.jot :as c]))

(test/reg ::append-block
  (fn [_]
    (let [doc (c/base-doc)
          doc (c/append-block doc (c/para))]

      [[(= 1 (c/blocks-count doc))]])))

(test/reg ::prepend-block
  (fn [_]
    (let [doc (c/base-doc)
          doc (c/prepend-block doc (c/para))]

      [[(= 1 (c/blocks-count doc))]])))

(test/reg ::selected-blocks
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para
                      {:id "foo"
                       :text "Hello World"}))
                  (c/append-block
                    (c/para
                      {:id "bar"
                       :text "The quick"})))

          doc (assoc
                doc
                ::c/selection
                {::c/start ["foo" 0]
                 ::c/end ["foo" 0]
                 ::c/anchor ["foo" 0]
                 ::c/focus ["foo" 0]})
          
          foo-block (first (c/selected-blocks doc))

          doc (assoc
                doc
                ::c/selection
                {::c/start ["bar" 0]
                 ::c/end ["bar" 0]
                 ::c/anchor ["bar" 0]
                 ::c/focus ["bar" 0]})

          bar-block (first (c/selected-blocks doc))]

      [[(= "foo" (::c/block-id foo-block))]
       [(= "bar" (::c/block-id bar-block))]])))

(test/reg ::blocks-by-id
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para
                      {:id "foo"
                       :text "Hello World"}))
                  (c/append-block
                    (c/para
                      {:id "bar"
                       :text "The quick"}))
                  (c/append-block
                    (c/para
                      {:id "baz"
                       :text "Asdf"})))

          blocks (c/blocks-by-ids doc ["foo" "bar"])]

      [[(= 2 (count blocks)) "Blocks count should be 2"]])))

(test/reg ::delete-selected-single-block
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para
                      {:id "foo"
                       :text "Hello World"}))
                  (c/set-selection
                    {:start ["foo" 0]
                     :end ["foo" 5]
                     :anchor ["foo" 0]
                     :focus ["foo" 5]}))
          
          doc (c/delete-selected doc)

          block (c/block-by-id doc "foo")]

      [[(= " World" (::c/content-text block))
        (::c/content-text block)]])))

(test/reg ::delete-selected-delete-block
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para {:id "foo" :text "Hello World"}))
                  (c/append-block
                    (c/para {:id "bar" :text "The quick"}))
                  (c/set-selection
                    {:start ["foo" 0]
                     :end ["bar" 0]})
                  c/delete-selected)]
      [[(= 1 (c/blocks-count doc))]
       [(= "The quick"
           (::c/content-text
            (c/block-by-id doc "foo")))]])))

(test/reg ::delete-selected-in-non-para
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para {:id "foo" :text "Hello World"}))
                  (c/append-block
                    (c/create-block
                      {::c/block-id "bar"
                       ::c/type ::c/media}))
                  (c/append-block
                    (c/para {:id "baz" :text "The quick"}))
                  (c/set-selection
                    {:start ["bar" 0]
                     :end ["bar" 1]})
                  c/delete-selected)

          block (c/block-by-id doc "bar")]
      
      [[(= 3 (c/blocks-count doc))]
       [(= "" (::c/content-text block))]
       [(= ::c/paragraph (::c/type block))]])))

(test/reg ::delete-selected-across-non-para
  (fn [_]
    (let [doc (-> (c/base-doc)
                  (c/append-block
                    (c/para {:id "foo" :text "Hello World"}))
                  (c/append-block
                    (c/create-block
                      {::c/block-id "bar"
                       ::c/type ::c/media}))
                  (c/append-block
                    (c/para {:id "baz" :text "The quick"}))
                  (c/set-selection
                    {:start ["foo" 5]
                     :end ["bar" 1]})
                  c/delete-selected)
          
          block (c/block-by-id doc "bar")]
      
      [[(= 2 (c/blocks-count doc))]])))

(comment

  (test/<run-key-report-repl! ::selected-blocks)
  
  (test/<run-all-report-repl!
    {:namespaces [:rx.jot-test]})

  (test/remove-all-tests!)
  
  )
