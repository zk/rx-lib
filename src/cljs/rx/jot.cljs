(ns rx.jot
  (:require [rx.kitchen-sink :as ks]
            [clojure.string :as str]
            [clojure.set :as set]
            [editscript.core :as es]
            [editscript.edit :as ee]))

(def INDEX_CHUNK_SIZE 1000)

;; Block Operations

(defn text-block? [block]
  (get #{::paragraph
         ::heading-one
         ::heading-two
         ::heading-three
         ::unordered-list-item
         ::ordered-list-item
         ::blockquote}
    (::type block)))

(defn heading-block? [block]
  (get #{::heading-one
         ::heading-two
         ::heading-three}
    (::type block)))

(defn block-text [block]
  (::content-text block))

(defn block-empty? [block]
  (if (text-block? block)
    (empty? (::content-text block))
    false))

(defn block-offset->decos-in-range
  "Get map of offset -> deco within offsets of `[start, end)`."
  [{:keys [::offset->deco]} start end]
  (->> (range start end)
       (map (fn [offset]
              (let [deco (get offset->deco offset)]
                (when deco
                  [offset deco]))))
       (remove nil?)
       (into {})))

(def test-embed-block
  {:rx.jot/block-id "6e1f85f1fe474560bc67691b6417eb5a",
   :rx.jot/type :rx.jot/paragraph,
   :rx.jot/content-text "The Last Question by Isaac Asimov © 1956",
   :rx.jot/offset->deco
   {10 {:rx.jot/embed-id "my-embed"},
    11 {:rx.jot/embed-id "my-embed"},
    12 {:rx.jot/embed-id "my-embed"},
    13 {:rx.jot/embed-id "my-embed"},
    14 {:rx.jot/embed-id "my-embed"},
    15 {:rx.jot/embed-id "my-embed"},
    16 {:rx.jot/embed-id "my-embed"},
    17 {:rx.jot/embed-id "my-embed"},
    18 {:rx.jot/embed-id "my-embed"},
    19 {:rx.jot/embed-id "my-embed"},
    20 {:rx.jot/embed-id "my-embed"},
    21 {:rx.jot/embed-id "my-embed"},
    22 {:rx.jot/embed-id "my-embed"},
    23 {:rx.jot/embed-id "my-embed"},
    24 {:rx.jot/embed-id "my-embed"},
    25 {:rx.jot/embed-id "my-embed"},
    26 {:rx.jot/embed-id "my-embed"},
    27 {:rx.jot/embed-id "my-embed"},
    28 {:rx.jot/embed-id "my-embed"},
    29 {:rx.jot/embed-id "my-embed"},
    30 {:rx.jot/embed-id "my-embed"},
    31 {:rx.jot/embed-id "my-embed"},
    32 {:rx.jot/embed-id "my-embed"},
    33 {:rx.jot/embed-id "my-embed"},
    34 {:rx.jot/embed-id "my-embed"},
    35 {:rx.jot/embed-id "my-embed"},
    36 {:rx.jot/embed-id "my-embed"},
    37 {:rx.jot/embed-id "my-embed"},
    38 {:rx.jot/embed-id "my-embed"},
    39 {:rx.jot/embed-id "my-embed"}},
   :rx.jot/embed-id->embed-data
   {"my-embed"
    {:rx.jot/embed-type :highlight, :rx.jot/embed-id "my-embed"}}})

(comment

  (ks/spy
    (block-decos-in-range
      test-embed-block
      10
      11))

  )

(defn block-delete-decos-range [block start end]
  (-> block
      (update
        ::offset->deco
        (fn [offset->deco]
          (try
            (merge
              (sorted-map)
              (block-offset->decos-in-range block 0 start)
              (->> (block-offset->decos-in-range block
                     end
                     (inc (or (apply max (keys offset->deco)) 0)))
                   (map (fn [[offset decos]]
                          [(- offset (- end start))
                           decos]))
                   (into {})))
            (catch js/Error e
              (sorted-map)))))))

(comment

  (ks/pp
    (block-delete-decos-range
      test-embed-block
      0 12))

  )

(defn block-decos-in-range [block start-offset end-offset]
  (->> (range start-offset end-offset)
       (mapv (fn [offset]
               (get (::offset->deco block) offset)))
       #_(remove nil?)))

(defn set-decos [block start-offset decos]
  (update
    block
    ::offset->deco
    (fn [offset->deco]
      (->> (merge
             offset->deco
             (->> decos
                  (map-indexed
                    (fn [i deco]
                      [(+ i start-offset) deco]))
                  (into {})))
           (remove #(nil? (second %)))
           (into (sorted-map))))))

(defn update-decos
  [block start-offset end-offset f & args]
  (update-in
    block
    [::offset->deco]
    (fn [offset->deco]
      (let [updates (->> (range start-offset end-offset)
                         (map
                           (fn [offset]
                             (let [deco (get offset->deco offset)]
                               [offset (apply f deco args)])))
                         (into {}))

            to-remove-offsets
            (->> updates
                 (filter #(empty? (second %)))
                 (map first))]
        (into
          (try
            (into
              (sorted-map)
              (apply dissoc offset->deco to-remove-offsets))
            (catch js/Error e
              (sorted-map)))
          (->> updates
               (remove #(empty? (second %)))))))))

(defn set-embed-data [doc embed-data]
  (let [embed-data (merge
                     {::embed-id (ks/uuid)}
                     embed-data)]
    (update-in
      doc
      [::embed-id->embed-data (::embed-id embed-data)]
      assoc
      embed-data)))

(defn block-update-embed-ids [block]
  (let [embed-ids (->> block
                       ::offset->deco
                       vals
                       (map ::embed-id)
                       distinct
                       (remove nil?)
                       vec)]
    (assoc
      block
      ::embed-ids
      embed-ids)))

(defn set-block-embed [block start-offset end-offset embed-id embed-data]
  (-> block
      (set-decos
        start-offset
        (->> (range (- end-offset start-offset))
             (map (fn [i]
                    {::embed-id embed-id}))))
      block-update-embed-ids
      #_(set-embed-data
          embed-id
          embed-data)))

(defn block-delete-range [block start end]
  (cond
    (text-block? block)
    (-> block
        (update
          ::content-text
          #(str
             (subs % 0 start)
             (subs % (or end (count %)))))
        (block-delete-decos-range start end)
        block-update-embed-ids)

    ;; Turn block into empty para
    :else (merge
            block
            {::content-text ""
             ::type ::paragraph})))

(comment

  (ks/pp
    (block-delete-range
      test-embed-block
      0 10))

  )



(defn para [& [{:keys [id text]}]]
  {::block-id (or id (ks/uuid))
   ::type ::paragraph
   ::content-text (or text "")})

(defn heading-one [& [{:keys [id text]}]]
  {::block-id (or id (ks/uuid))
   ::type ::heading-one
   ::content-text (or text "")})

(defn heading-two [& [{:keys [id text]}]]
  {::block-id (or id (ks/uuid))
   ::type ::heading-two
   ::content-text (or text "")})

(defn create-block [& [override]]
  (merge
    {::block-id (ks/uuid)
     ::type ::paragraph}
    override))

(defn convert-block-to-paragraph [block]
  (-> block
      (assoc ::type ::paragraph)
      (dissoc ::indent-level)))

(defn split-block [block offset]
  (if (::embed? block)
    [(para)
     block]
    (let [original-text (::content-text block)
          upd-block (-> block
                        (merge
                          {::content-text
                           (subs original-text 0 offset)
                           ::type (if (and (= 0 offset)
                                           (get #{::heading-one
                                                  ::heading-two
                                                  ::heading-three}
                                             (::type block)))
                                    ::paragraph
                                    (::type block))})
                        ((fn [block]
                           (if (= ::paragraph (::type block))
                             (dissoc block ::indent-level)
                             block)))
                        (set-decos
                          offset
                          (repeat (- (count original-text) offset) nil)))

          new-block-decos (block-decos-in-range
                            block
                            offset
                            (count original-text))

          new-block-id (ks/uuid)
          
          new-block (-> (merge
                          {::block-id new-block-id
                           ::content-text
                           (subs original-text offset)
                           ::type (if (and (not= 0 offset)
                                           (get #{::heading-one
                                                  ::heading-two
                                                  ::heading-three}
                                             (::type block)))
                                    ::paragraph
                                    (::type block))}
                          (select-keys
                            block
                            [::indent-level]))
                        ((fn [block]
                           (if (= ::paragraph (::type block))
                             (dissoc block ::indent-level)
                             block)))
                        (set-decos
                          0
                          new-block-decos)
                        block-update-embed-ids)]

      [upd-block
       new-block])))

(defn merge-blocks [blocks doc]
  (when (->> blocks
             (map ::type)
             (map text-block?)
             (reduce #(and %1 %2)))
    (ks/throw-str "Can't merge non-text blocks " (->> blocks (map ::type))))

  (when-not (empty? blocks)
    (let [new-content-text (->> blocks
                                (map ::content-text)
                                (apply str))

          [new-content-text
           new-offset->deco]
          (->> blocks
               (reduce
                 (fn [[accu-text accu-offset->deco] block]
                   (let [text (::content-text block)
                         offset->deco (::offset->deco block)]
                     [(str accu-text text)
                      (merge
                        accu-offset->deco
                        (->> offset->deco
                             (map (fn [[offset deco]]
                                    [(+ offset (count accu-text))
                                     deco]))
                             (into {})))]))
                 []))
          
          resulting-block (merge
                            (first blocks)
                            {::content-text new-content-text})

          type (if (block-empty? (first blocks))
                 (::type (last blocks))
                 (::type (first blocks)))

          resulting-block (-> (merge
                                resulting-block
                                {::type type}
                                {::offset->deco new-offset->deco})
                              block-update-embed-ids)]
      resulting-block)))

(defn block-end-offset [block]
  (when block
    (cond 
      (text-block? block) (count (::content-text block))
      :else 1)))

(defn para? [block]
  (= ::paragraph (::type block)))

(defn append-block [doc block]
  (let [last-index (first (last (::block-order doc)))
        new-index (+ last-index INDEX_CHUNK_SIZE)
        {:keys [::block-id]} block

        selection {::start [block-id 0]
                   ::end [block-id 0]
                   ::anchor [block-id 0]
                   ::focus [block-id 0]}]

    (-> doc
        (update
          ::block-id->block
          assoc
          block-id block)
        (update
          ::block-order
          assoc
          new-index block-id))))

(defn prepend-block [doc block]
  (let [first-index (ffirst (::block-order doc))
        new-index (- first-index INDEX_CHUNK_SIZE)
        {:keys [::block-id]} block

        selection {::start [block-id 0]
                   ::end [block-id 0]
                   ::anchor [block-id 0]
                   ::focus [block-id 0]}]
    
    (-> doc
        (update
          ::block-id->block
          assoc
          block-id block)
        (update
          ::block-order
          assoc
          new-index block-id)
        (assoc
          ::selection
          selection))))

(def undo-state-keys
  [::block-order
   ::block-id->block
   ::selection])

(defn push-undo [doc-after doc-before]
  (if (not= (select-keys
              (peek (::undo-stack doc-after))
              undo-state-keys)
            (select-keys
              doc-after
              undo-state-keys))
    (let [diff (es/diff
                 (select-keys doc-after undo-state-keys)
                 (select-keys doc-before undo-state-keys))]
      (if (> (ee/edit-distance diff) 0)
        (-> doc-after
            (update
              ::undo-stack
              (fn [stack]
                (->> (conj
                       (vec stack)
                       diff)
                     reverse
                     (take 50)
                     reverse
                     vec)))
            (dissoc ::redo-stack))
        doc-after))
    doc-after))

(defn undo [{:keys [::undo-stack] :as doc}]
  (if-let [undo-diff (peek undo-stack)]
    (let [undo-doc (dissoc
                     (es/patch doc undo-diff)
                     undo-state-keys)]
      (merge
        undo-doc
        (update
          undo-doc
          ::block-order
          (fn [sm]
            (into (sorted-map) sm)))
        {::undo-stack (pop undo-stack)
         ::redo-stack (conj
                        (vec (::redo-stack doc))
                        (es/diff
                          (select-keys undo-doc undo-state-keys)
                          (select-keys doc undo-state-keys)))}))
    doc))

(defn redo [{:keys [::redo-stack] :as doc}]
  (if-let [redo-diff (peek redo-stack)]
    (let [redo-doc (dissoc
                     (es/patch doc redo-diff)
                     undo-state-keys)]
      (merge
        redo-doc
        (update
          redo-doc
          ::block-order
          (fn [sm]
            (into (sorted-map) sm)))
        {::redo-stack (pop redo-stack)
         ::undo-stack (conj
                        (vec (::undo-stack doc))
                        (es/diff
                          (select-keys redo-doc undo-state-keys)
                          (select-keys doc undo-state-keys)))}))
    doc))

(defn blocks-by-ids [doc ids]
  (let [{:keys [::block-order]} doc
        id->index (set/map-invert block-order)]
    (->> ids
         (sort-by #(get id->index %))
         (mapv #(get (::block-id->block doc) %)))))

(defn block-by-id [doc id]
  (get (::block-id->block doc) id))

(defn valid-selection? [doc]
  (let [{:keys [::start ::end ::anchor ::focus]} (::selection doc)
        start-block (when start
                      (block-by-id doc (first start)))

        start-text (block-text start-block)

        end-block (when end
                    (block-by-id doc (first end)))

        end-text (block-text end-block)

        in-range? (and (<= 0 (second start) (count start-text))
                       (<= 0 (second end) (count end-text)))]
    
    (and start end anchor focus
         in-range?)))

(defn clamp-sel-part [doc [block-id offset]]
  (let [block (block-by-id doc block-id)
        min-offset 0
        max-offset (if (text-block? block)
                     (count (::content-text block))
                     0)]
    [block-id (ks/clamp offset min-offset max-offset)]))

(defn clamp-selection [doc]
  (let [{:keys [::selection]} doc
        {:keys [::start ::end ::anchor ::focus]} selection

        start (clamp-sel-part doc start)
        end (clamp-sel-part doc end)
        anchor (clamp-sel-part doc anchor)
        focus (clamp-sel-part doc focus)]
    (merge
      doc
      {::selection {::start start
                    ::end end
                    ::anchor anchor
                    ::focus focus}})))

(defn set-selection [doc {:keys [start end anchor focus]}]
  (let [{:keys [start end anchor focus]}
        (cond
          (and
            start end
            (not anchor)
            (not focus))
          {:start start
           :end end
           :anchor start
           :focus end}

          (and anchor focus (not= (first anchor) (first focus)))
          (let [id->offset {(first anchor) (second anchor)
                            (first focus) (second focus)}
                blocks (blocks-by-ids doc
                         [(first anchor)
                          (first focus)])

                [start end]
                (->> blocks
                     (map (fn [{:keys [::block-id]}]
                            [block-id (get id->offset block-id)])))]

            {:start start
             :end end
             :anchor anchor
             :focus focus})

          (and anchor focus)
          (let [offsets (->> [(second anchor)
                              (second focus)]
                             sort)
                start-offset (first offsets)
                end-offset (second offsets)
                id (first anchor)
                start [id start-offset]
                end [id end-offset]]
            {:start start
             :end end
             :anchor anchor
             :focus focus})
          
          :else {:start start
                 :end start
                 :anchor start
                 :focus start})

        doc (merge
              doc
              {::selection {::start start
                            ::end end
                            ::anchor anchor
                            ::focus focus}})

        doc (clamp-selection doc)]
    doc))

(defn blocks-count [doc]
  (count (::block-order doc)))

(defn blocks-by-selection [doc selection]
  (let [{:keys [::block-order
                ::block-id->block]} doc
        {:keys [::start ::end]} selection
        [start-id start-index] start
        [end-id end-index] end

        id->index (set/map-invert (::block-order doc))

        start-index (get id->index start-id)

        end-index (get id->index end-id)

        selected-index+ids
        (subseq
          block-order
          >= start-index
          <= end-index)]
    (->> selected-index+ids
         (mapv (fn [[index id]]
                (get block-id->block id))))))

(defn selected-blocks [doc]
  (blocks-by-selection doc (::selection doc)))

(defn block-selected? [doc block]
  (let [sel-block-ids-set
        (->> (selected-blocks doc)
             (map ::block-id)
             set)]
    (get sel-block-ids-set (::block-id block))))

(defn next-block-from-id [doc id]
  (when id
    (let [block (block-by-id doc id)
          id->index (set/map-invert (::block-order doc))
          index (get id->index id)
          [next-index next-id]
          (first
            (subseq
              (::block-order doc)
              >
              index))]
      (block-by-id doc next-id))))

(defn prev-block-from-id [doc id]
  (when id
    (let [block (block-by-id doc id)
          id->index (set/map-invert (::block-order doc))
          index (get id->index id)
          [prev-index prev-id]
          (last
            (subseq
              (::block-order doc)
              <
              index))]
      (block-by-id doc prev-id))))

(defn update-block [doc id f & args]
  (apply
    update-in
    doc
    [::block-id->block id]
    f
    args))

(defn sel-start-block [doc]
  (->> doc
       ::selection
       ::start
       first
       (block-by-id doc)))

(defn sel-end-block [doc]
  (->> doc
       ::selection
       ::end
       first
       (block-by-id doc)))

(defn sel-next-block [doc]
  (let [block (->> doc
                   ::selection
                   ::start
                   first
                   (block-by-id doc))]
    (next-block-from-id doc (::block-id block))))

(defn sel-prev-block [doc]
  (let [block (->> doc
                   ::selection
                   ::start
                   first
                   (block-by-id doc))]
    (prev-block-from-id doc (::block-id block))))

(defn update-current-block [doc f & args]
  (apply
    update-block
    doc
    (::block-id (sel-start-block doc))
    f
    args))

(defn set-block [doc id v]
  (let [embed-ids (->> v
                       ::embed-id->embed-data
                       keys)]
    (if id
      (-> doc
          (assoc-in
            [::block-id->block id]
            v)
          block-update-embed-ids)
      doc)))

(defn set-blocks [doc blocks]
  (->> blocks
       (reduce
         (fn [doc block]
           (set-block doc (::block-id block) block))
         doc)))

(defn ensure-last-block-non-embed [doc]
  (let [last-id (-> (::block-order doc)
                    last
                    last)
        block (block-by-id doc last-id)]
    (if (::embed? block)
      (-> doc
          (append-block (para)))
      doc)))

(defn ensure-selection [doc]
  (let [first-id (-> (::block-order doc)
                     first
                     second)]
    (if (valid-selection? doc)
      doc
      (-> doc
          (set-selection
            {:start [first-id 0]})))))

(defn remove-block-ids [doc ids]
  (let [id->index (set/map-invert (::block-order doc))
        blocks (->> (blocks-by-ids doc ids)
                    (sort-by #(get id->index %)))

        embed-ids (->> blocks
                       (map ::embed-id->embed-data)
                       (mapcat keys)
                       (remove nil?))]
    (-> doc
        (update
          ::block-id->block
          #(apply dissoc % ids))
        (update
          ::block-order
          (fn [block-order]
            (apply
              dissoc
              block-order
              (map
                #(get id->index %)
                ids))))
        #_(update
            ::embed-id->embed-data
            (fn [embed-id->embed-data]
              (merge
                embed-id->embed-data
                (->> embed-ids
                     (map (fn [id]
                            (let [embed-data (get embed-id->embed-data id)]
                              [id
                               (update
                                 embed-data
                                 ::block-id->layout
                                 (fn [m]
                                   (apply dissoc m embed-ids)))])))
                     (into {}))))))))

(defn embed? [block]
  (::embed? block))

(declare tf-earmuffs-expansion)

(defn delete-range-from-block [doc id start end]
  (let [block (block-by-id doc id)]
    (if (and start (not (embed? block)))
      (let [doc (-> doc
                    (update-block
                      id
                      block-delete-range start end))

            doc (tf-earmuffs-expansion
                  doc
                  (block-by-id doc id))]
        doc)
      doc)))

(defn add-embed-data-by-blocks [doc blocks]
  (update
    doc
    ::embed-id->embed-data
    (fn [embed-id->embed-data]
      (reduce
        (fn [accu {:keys [::embed-ids
                          ::block-id] :as block}]
          (reduce
            (fn [accu embed-id]
              (-> accu
                  (update-in
                    [embed-id ::block-id->layout block-id]
                    merge {})))
            accu
            embed-ids))
        embed-id->embed-data
        blocks))))

(defn remove-embed-data-by-blocks [doc blocks]
  (update
    doc
    ::embed-id->embed-data
    (fn [embed-id->embed-data]
      (reduce
        (fn [accu {:keys [::embed-ids
                          ::block-id] :as block}]
          (reduce
            (fn [accu embed-id]
              (-> accu
                  (update-in
                    [embed-id ::block-id->layout]
                    dissoc block-id)))
            accu
            embed-ids))
        embed-id->embed-data
        blocks))))

(defn merge-block-ids [doc ids]
  (if (= 1 (count ids))
    doc
    (let [blocks (->> (blocks-by-ids doc ids)
                      (remove embed?))
          to-remove-ids (->> blocks
                             rest
                             (map ::block-id)
                             (remove #(= (::block-id (first blocks)) %)))

          resulting-block (merge-blocks blocks doc)]
      (-> doc
          (remove-block-ids to-remove-ids)
          (remove-embed-data-by-blocks blocks)
          (add-embed-data-by-blocks [resulting-block])
          (set-block
            (::block-id resulting-block)
            resulting-block)))))

(defn collapse-selection-to-start [doc]
  (let [{:keys [::selection]} doc]
    (update
      doc
      ::selection
      (fn [{:keys [::start] :as sel}]
        (merge
          sel
          {::end start
           ::anchor start
           ::focus start})))))

(defn sel-collapsed? [doc]
  (let [sel (::selection doc)]
    (= (::start sel)
       (::end sel))))

(defn delete-selected [doc]
  (let [blocks (->> (selected-blocks doc)
                    (remove ::embed?))
        middle-blocks (->> blocks
                           (drop 1)
                           butlast)
        first-block (first blocks)
        last-block (last blocks)

        [start-id start-offset] (->> doc
                                     ::selection
                                     ::start)

        [end-id end-offset] (->> doc
                                 ::selection
                                 ::end)]
    (if (sel-collapsed? doc)
      doc
      (-> doc
          (remove-block-ids
            (->> middle-blocks
                 (map ::block-id)))
          (delete-range-from-block
            start-id
            start-offset
            (when (= end-id start-id)
              end-offset))
          (delete-range-from-block
            end-id
            (when (not= end-id start-id) 0)
            (when (not= end-id start-id) end-offset))
          (merge-block-ids
            (distinct
              [(-> first-block
                   ::block-id)
               (-> last-block
                   ::block-id)]))
          collapse-selection-to-start
          ensure-last-block-non-embed))))

(defn first-block? [doc id]
  (= id
     (->> doc
          ::block-order
          first
          second)))

(defn last-block? [doc id]
  (= id
     (->> doc
          ::block-order
          last
          second)))

(defn get-first-block [doc]
  (let [id (->> doc
                ::block-order
                first
                second)]
    (when id
      (block-by-id doc id))))

(defn insert-block-after [doc id new-block]
  (let [current-block (block-by-id doc id)

        next-block (next-block-from-id doc id)

        id->index (set/map-invert (::block-order doc))
        current-index (or (get id->index id) 0)

        next-index (or (get id->index (::block-id next-block))
                       (+ current-index INDEX_CHUNK_SIZE))

        new-index (/
                    (+ current-index
                       next-index)
                    2)

        new-block-id (::block-id new-block)]
    (when (get (::block-order doc) new-index)
      (prn
        "INDEX ALREADY EXISTS"
        new-index
        (get (::block-order doc) new-index)))
    (-> doc
        (update
          ::block-id->block
          assoc
          new-block-id new-block)
        (update
          ::block-order
          assoc
          new-index new-block-id)
        #_(#(do (ks/pp (dissoc % ::undo-stack ::redo-stack))
                %)))))

(defn split-on-empty-block? [block]
  (get #{::unordered-list-item
         ::ordered-list-item
         ::blockquote}
    (::type block)))

(defn get-sel [doc]
  (::selection doc))

(defn get-sel-start [doc]
  (::start (get-sel doc)))

(defn get-sel-end [doc]
  (::end (get-sel doc)))

(defn get-sel-anchor [doc]
  (::anchor (get-sel doc)))

(defn get-sel-focus [doc]
  (::focus (get-sel doc)))

(defn update-styles
  [block start-offset end-offset f & args]
  (update-decos
    block
    start-offset
    end-offset
    (fn [deco]
      (let [upd-styles (apply f (::styles deco) args)]
        (merge
          deco
          (when upd-styles
            {::styles upd-styles}))))))

(defn clear-styles
  [block start-offset end-offset]
  (update-decos
    block
    start-offset
    end-offset
    (fn [deco]
      (dissoc deco ::styles))))

(defn embed-datas [doc]
  (-> doc
      (get ::embed-id->embed-data)
      vals))

(defn embed-data-by-id [doc embed-id]
  (get-in
    doc
    [::embed-id->embed-data embed-id]))

(defn ensure-embed-id [embed]
  (merge
    embed
    {::embed-id (or (::embed-id embed)
                    (ks/uuid))}))

(defn set-inline-embed
  [block start-offset end-offset embed-data]
  (if embed-data
    (let [ie-id (or (::embed-id embed-data) (ks/uuid))
          embed-data
          (merge
            embed-data
            {::embed-id ie-id})]
      (-> block
          (update-decos
            start-offset
            end-offset
            (fn [deco]
              (assoc deco ::embed-id ie-id)))
          block-update-embed-ids))
    block))

(defn set-sel-inline-embed [doc embed-data]
  (if embed-data
    (let [embed-data (ensure-embed-id embed-data)
          start-block (sel-start-block doc)
          end-block (sel-end-block doc)
          [start-id start-offset] (get-sel-start doc)
          [end-id end-offset] (get-sel-end doc)
          blocks (selected-blocks doc)
          embed-blocks
          (->> blocks
               (map (fn [block]
                      (cond
                        (= start-block end-block)
                        (set-inline-embed
                          block
                          start-offset
                          end-offset
                          embed-data)
                        
                        (= start-block block)
                        (set-inline-embed
                          block
                          start-offset
                          (block-end-offset block)
                          embed-data)

                        (= end-block block)
                        (set-inline-embed
                          block
                          0
                          end-offset
                          embed-data)

                        :else
                        (set-inline-embed
                          block
                          0
                          (block-end-offset block)
                          embed-data)))))

          embed-data (merge
                       embed-data
                       {::block-id->layout
                        (->> blocks
                             (map (fn [b]
                                    [(::block-id b)
                                     {}]))
                             (into {}))})]
      (-> doc
          (set-blocks embed-blocks)
          (update
            ::embed-id->block-ids
            merge
            {(::embed-id embed-data)
             (->> embed-blocks
                  (mapv ::block-id))})
          (update
            ::embed-id->embed-data
            merge
            {(::embed-id embed-data)
             embed-data})))
    doc))

(defn remove-inline-embed [doc embed-id]
  (try
    (let [block-ids (-> doc
                        ::embed-id->embed-data
                        (get embed-id)
                        ::block-id->layout
                        keys)
          blocks (->> block-ids
                      (map #(block-by-id doc %))
                      (map (fn [block]
                             (-> block
                                 (update
                                   ::offset->deco
                                   (fn [offset->deco]
                                     (->> offset->deco
                                          (map (fn [[i embed-data]]
                                                 [i
                                                  (if (= (::embed-id embed-data)
                                                         embed-id)
                                                    {}
                                                    embed-data)]))
                                          (into {}))))))))]
      
      (-> doc
          (set-blocks blocks)
          (update
            ::embed-id->block-ids
            dissoc
            embed-id)))
    (catch js/Error e
      (prn e)
      doc)))

(defn tf-cancel-on-split-empty [doc]
  (let [current-block (sel-start-block doc)
        prev-block (prev-block-from-id
                     doc
                     (::block-id current-block))
        prev-prev-block (prev-block-from-id
                          doc
                          (::block-id prev-block))]

    (cond
      (and (split-on-empty-block? current-block)
           (split-on-empty-block? prev-block)
           (block-empty? prev-block)
           (block-empty? current-block))
      (-> doc
          (remove-block-ids
            [(::block-id prev-block)])
          (update-block
            (::block-id current-block)
            (fn [block]
              (merge
                (dissoc block ::indent-level)
                {::type ::paragraph}))))

      (and (split-on-empty-block? current-block)
           (split-on-empty-block? prev-block)
           (split-on-empty-block? prev-prev-block)
           (block-empty? prev-block)
           (block-empty? prev-prev-block)
           (not (block-empty? current-block)))
      (-> doc
          (update-block
            (::block-id prev-prev-block)
            (fn [block]
              (merge
                (dissoc block ::indent-level)
                {::type ::paragraph}))))

      

      :else doc)))

(defn post-split-transform [doc]
  (-> doc
      tf-cancel-on-split-empty))

(defn split-at-selection [doc]
  (let [doc (delete-selected doc)
        {:keys [::selection ::block-id->block]} doc
        {:keys [::start]} selection
        [start-id start-offset] start

        
        block (block-by-id doc start-id)

        prev-block (prev-block-from-id doc start-id)

        [first-block second-block]
        (split-block block start-offset)]
    
    (-> doc
        (remove-block-ids [(::block-id block)])
        (insert-block-after (::block-id prev-block) first-block)
        
        (insert-block-after (::block-id first-block) second-block)
        (set-selection
          {:start [(::block-id second-block) 0]})
        (add-embed-data-by-blocks [first-block second-block])
        post-split-transform
        ensure-last-block-non-embed
        (push-undo doc))))

(defn tf-reverse-expansion [doc block]
  (let [{:keys [::reverse-expansion-text
                ::reverse-expansion-type]} block]
    (if (and reverse-expansion-text
             (second (get-sel-start doc))
             (block-empty? block)
             (not (> (::indent-level block) 0)))
      (let [upd-block (merge
                        (dissoc block
                          ::reverse-expansion-text
                          ::reverse-expansion-type)
                        {::content-text reverse-expansion-text
                         ::type reverse-expansion-type})]
        (-> doc
            (set-block
              (::block-id upd-block)
              upd-block)
            (set-selection
              {:start [(::block-id upd-block)
                       (block-end-offset upd-block)]})))
      doc)))

(defn pre-backwards-delete-transform [block doc]
  (-> doc
      (tf-reverse-expansion block)))

(defn increase-indent [doc]
  (-> doc
      (update-current-block
        (fn [block]
          (if (get #{::unordered-list-item
                     ::ordered-list-item}
                (::type block))
            (merge
              block
              {::indent-level (min (inc (get block ::indent-level 0)) 8)})
            block)))))

(defn decrease-indent [doc]
  (-> doc
      (update-current-block
        (fn [block]
          (if (get #{::unordered-list-item
                     ::ordered-list-item}
                (::type block))
            (merge
              block
              {::indent-level (max (dec (get block ::indent-level 0)) 0)})
            block)))))

(defn sel-in-atomic-embed? [doc]
  (let [start (get-sel-start doc)
        block (sel-start-block doc)
        prev-deco (first
                    (block-decos-in-range block (dec (second start)) (second start)))

        embed (get-in block [::embed-id->embed-data (::embed-id prev-deco)])]
    (::atomic? embed)))

(defn current-inline-embed-range [doc]
  (let [start (get-sel-start doc)
        block (sel-start-block doc)
        prev-deco (first
                    (block-decos-in-range block (dec (second start)) (second start)))

        embed-id (::embed-id prev-deco)

        matching-offset+deco (->> block
                                  ::offset->deco
                                  (filter
                                    (fn [[offset deco]]
                                      (= embed-id (::embed-id deco)))))]
    [(first (first matching-offset+deco))
     (first (last matching-offset+deco))]))

(def ^:private re-surrogate-pair
  (js/RegExp. "([\\uD800-\\uDBFF])([\\uDC00-\\uDFFF])" "g"))

(defn surrogate-pair? [s]
  (and s
       (.match s re-surrogate-pair)))

(defn prev-char-offset [text offset]
  (let [subs-start (max (dec (dec offset)) 0)
        subs-end offset
        prev-two-chars-str
        (subs
          text
          subs-start
          subs-end)

        pair? (surrogate-pair? prev-two-chars-str)

        prev-char-offset (if pair?
                           (dec (dec offset))
                           (dec offset))]
    #_(prn
        ":text" text
        ":offset" offset
        ":subs-start" subs-start
        ":subs-end" subs-end
        ":prev-two" prev-two-chars-str
        ":offset" offset
        ":sur-pair?" pair?
        ":prev-char-offset" prev-char-offset)
    prev-char-offset))

(defn remove-previous-character-from-start [doc]
  (let [start (get-sel-start doc)
        [start-id start-offset] start

        current-block (block-by-id doc start-id)

        doc (pre-backwards-delete-transform current-block doc)
        start (get-sel-start doc)
        [start-id start-offset] start

        prev-block (prev-block-from-id doc start-id)
        prev-block-last-offset (block-end-offset prev-block)]

    (cond

      ;; adjusting indent level
      (and (= 0 start-offset)
           (> (::indent-level current-block) 0))
      (decrease-indent doc)

      ;; remove indent level when first block
      (and (= 0 start-offset)
           (first-block? doc start-id))
      (update-current-block
        doc
        (fn [block]
          (merge
            (dissoc
              block
              ::indent-level)
            {::type ::paragraph})))

      ;; When between two block embeds, move to beginning of previous
      ;; block embed
      (and (= 0 start-offset)
           (embed? current-block)
           (embed? prev-block))
      (-> doc
          (set-selection
            {:start [(::block-id prev-block) 0]}))

      ;; When on embed block, remove previous non embed block
      (and (= 0 start-offset)
           (embed? current-block))
      (-> doc
          (remove-block-ids [(::block-id prev-block)]))

      ;; When on non embed block, remove currrent block and move to
      ;; beginning of embed block
      (and (= 0 start-offset)
           (embed? prev-block))
      (-> doc
          (remove-block-ids [(::block-id current-block)])
          (set-selection
            {:start [(::block-id prev-block) 0]}))

      ;; When at beginning of block merge current and previous blocks
      (= 0 start-offset)
      (-> doc
          (merge-block-ids [(::block-id prev-block) start-id])
          (set-selection
            {:start [(::block-id prev-block) prev-block-last-offset]}))

      (and (sel-collapsed? doc)
           (sel-in-atomic-embed? doc))
      (let [block (sel-start-block doc)
            [start-offset end-offset] (current-inline-embed-range doc)]
        (-> doc
            (set-selection
              {:start [(::block-id block) start-offset]
               :end [(::block-id block) (inc end-offset)]})
            delete-selected))
      
      :else
      (-> doc
          (set-selection
            {:start [start-id (prev-char-offset
                                (block-text current-block)
                                start-offset)]
             :end [start-id start-offset]})
          delete-selected))))

(defn ensure-one-block [doc]
  (if (= 0 (blocks-count doc))
    (let [block (para)]
      (-> doc
          (append-block block)
          (set-selection
            {:start [(::block-id block) 0]})))
    doc))

(defn backspace-character [doc]
  (-> (if (sel-collapsed? doc)
        (-> doc
            remove-previous-character-from-start)
        (-> doc
            delete-selected))
      ensure-one-block
      ensure-last-block-non-embed
      (push-undo doc)))

(defn backwards-coord [doc [start-id start-offset :as coord]]
  (let [prev-block (prev-block-from-id doc start-id)
        prev-offset (block-end-offset prev-block)
        prev-coord [(::block-id prev-block) prev-offset]]
    (cond
      (and prev-block (= 0 start-offset)) prev-coord
      :else [start-id (max 0 (dec start-offset))])))

(defn forwards-coord [doc [start-id start-offset :as coord]]
  (let [current-block (block-by-id doc start-id)
        next-block (next-block-from-id doc start-id)
        next-offset 0
        next-coord [(::block-id next-block) next-offset]]
    (cond
      (embed? current-block) next-coord
      (and next-block (= (block-end-offset current-block) start-offset)) next-coord
      :else [start-id (min (block-end-offset current-block) (inc start-offset))])))

(defn upwards-coord [doc [start-id start-offset target-offset :as coord]]
  (let [prev-block (prev-block-from-id doc start-id)
        prev-offset (block-end-offset prev-block)
        prev-coord [(::block-id prev-block)
                    (min prev-offset
                      (max
                        start-offset
                        (or target-offset start-offset)))
                    (or target-offset start-offset)]]
    (cond
      prev-block prev-coord
      :else coord)))

(defn downwards-coord [doc [start-id start-offset target-offset :as coord]]
  (let [next-block (next-block-from-id doc start-id)
        next-offset (block-end-offset next-block)
        next-coord [(::block-id next-block)
                    (min next-offset
                      (max
                        start-offset
                        (or target-offset start-offset)))
                    (or target-offset start-offset)]]
    (cond
      next-block next-coord
      :else coord)))

(defn move-backwards [doc]
  (set-selection
    doc
    {:start (backwards-coord doc (get-sel-start doc))}))

(defn move-forwards [doc]
  (set-selection
    doc
    {:start (forwards-coord doc (get-sel-start doc))}))

(defn move-upwards [doc]
  (set-selection
    doc
    {:start (upwards-coord doc (get-sel-start doc))}))

(defn move-downwards [doc]
  (set-selection
    doc
    {:start (downwards-coord doc (get-sel-start doc))}))

(defn block-insert-string [block offset text]
  (let [start-offset offset]
    (-> block
        (update
          ::content-text
          #(str
             (when %
               (subs % 0 start-offset))
             text
             (when %
               (subs % start-offset))))
        (update
          ::offset->deco
          (fn [offset->deco]
            (merge
              (sorted-map)
              (->> (range start-offset)
                   (map (fn [offset]
                          (let [deco (get offset->deco offset)]
                            (when deco
                              [offset deco]))))
                   (remove nil?)
                   (into {}))
              (->> (range start-offset (+ start-offset (count text)))
                   (map (fn [offset]
                          (let [deco (get offset->deco
                                       start-offset
                                       #_(dec start-offset))
                                embed (get-in block [::embed-id->embed-data (::embed-id deco)])
                                deco (if (::atomic? embed)
                                       (dissoc deco ::embed-id)
                                       deco)]
                            
                            (when deco
                              [offset deco]))))
                   (remove nil?)
                   (into {}))
              (->> (range
                     start-offset
                     (inc (or (apply max (keys offset->deco)) 0)))
                   (map (fn [offset]
                          (let [deco (get offset->deco offset)]
                            (when deco
                              [(+ offset (count text)) deco]))))
                   (remove nil?)
                   (into {}))))))))

(defn tf-apply-heading [doc block]
  (let [text (::content-text block)]
    (if (and (para? block)
             #(re-matches #"^\#{1,3}\s" text))
      (let [hs (count (re-seq #"\#" text))
            type (condp = hs
                   1 ::heading-one
                   2 ::heading-two
                   3 ::heading-three)
            upd-block (merge
                        block
                        {::type hs})]
        (-> doc
             (set-block
               (::block-id upd-block)
               upd-block)))
      doc)))

(defn tf-header-expansion [doc block]
  (let [re #"(\#{1,3})\s(.*)"]
    (cond
      (and (para? block)
           (re-matches re (::content-text block)))
      (let [[_ hashes text] (re-matches re (::content-text block))
            hs (count hashes)
            upd-block (merge
                        block
                        {::type (condp = hs
                                  1 ::heading-one
                                  2 ::heading-two
                                  3 ::heading-three)
                         ::content-text text
                         ::reverse-expansion-text (str hashes " ")
                         ::reverse-expansion-type (::type block)})]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block) (block-end-offset upd-block)]})))
      :else doc)))

(defn tf-unordered-list-item-expansion [doc block]
  (let [re #"\*{1}\s(.*)"]
    (cond
      (and (para? block)
           (re-matches re (::content-text block)))
      (let [[_ text] (re-matches re (::content-text block))
            upd-block (merge
                        block
                        {::type ::unordered-list-item
                         ::content-text text
                         ::reverse-expansion-text (::content-text block)
                         ::reverse-expansion-type (::type block)})]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block) (block-end-offset upd-block)]})))
      :else doc)))

(defn tf-ordered-list-item-expansion [doc block]
  (let [re #"1\.\s(.*)"]
    (cond
      (and (para? block)
           (re-matches re (::content-text block)))
      (let [[_ text] (re-matches re (::content-text block))
            upd-block (merge
                        block
                        {::type ::ordered-list-item
                         ::content-text text
                         ::reverse-expansion-text (::content-text block)
                         ::reverse-expansion-type (::type block)})]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block) (block-end-offset upd-block)]})))
      :else doc)))

(defn tf-blockquote-expansion [doc block]
  (let [re #">\s(.*)"]
    (cond
      (and (para? block)
           (re-matches re (::content-text block)))
      (let [[_ text] (re-matches re (::content-text block))
            upd-block (merge
                        block
                        {::type ::blockquote
                         ::content-text text
                         ::reverse-expansion-text (::content-text block)
                         ::reverse-expansion-type (::type block)})]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block) (block-end-offset upd-block)]})))
      :else doc)))

(defn earmuffs [s]
  (when s
    (let [[full
           _
           with-stars
           leading-stars
           text
           trailing-stars]
          (re-matches #".*([^\*]|^)((\*+)([^\*]+)(\*+)).*" s)]
      (when (and (= leading-stars trailing-stars)
                 (not= " " (first text))
                 (not= " " (last text)))
        {:stars-count (count leading-stars)
         :with-stars-text with-stars
         :without-stars-text text}))))

(defn tf-earmuffs-expansion [doc block]
  (let [{:keys [stars-count
                with-stars-text
                without-stars-text]}
        (earmuffs (::content-text block))]
    (cond
      (and (text-block? block)
           without-stars-text)
      (let [text (::content-text block)
            start-offset (str/index-of text with-stars-text)
            end-offset (+ start-offset (count with-stars-text))

            decos (block-decos-in-range block
                    (+ start-offset 1)
                    (- end-offset 1))
            
            upd-block (-> block
                          (block-delete-range start-offset end-offset)
                          (block-insert-string start-offset without-stars-text)
                          (set-decos start-offset decos)
                          (update-styles
                            start-offset
                            (+ start-offset (count without-stars-text))
                            (fn [styles]
                              (set
                                (conj
                                  styles
                                  (if (= 2 stars-count)
                                    ::bold
                                    ::italic))))))]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block)
                       (+ start-offset
                          (count without-stars-text))]})))
      :else doc)))

(defn wrapped-inline [doc block wrap-char deco-type]
  (let [[_ full-match inside-match]
        (re-matches
          (re-pattern (str ".*(" wrap-char "([^" wrap-char "]+)" wrap-char ").*"))
          (::content-text block))]
    (cond
      (and (text-block? block)
           inside-match)
      (let [text (::content-text block)
            start-offset (str/index-of text full-match)
            end-offset (+ start-offset (count full-match))

            decos (block-decos-in-range block
                    (+ start-offset 1)
                    (- end-offset 1))
            
            upd-block (-> block
                          (block-delete-range start-offset end-offset)
                          (block-insert-string start-offset inside-match)
                          (set-decos start-offset decos)
                          (update-styles
                            start-offset
                            (+ start-offset (count inside-match))
                            (fn [styles]
                              (set
                                (conj
                                  styles
                                  deco-type)))))]
        (-> doc
            (set-block (::block-id upd-block) upd-block)
            (set-selection
              {:start [(::block-id upd-block)
                       (+ start-offset
                          (count inside-match))]})))
      :else doc)))

(comment

  (ks/spy
    (let [block (para {:text "`foo bar`"})]
      (wrapped-inline
        (-> (empty-doc)
            (append-block block))
        block
        "`"
        ::code)))

  )

(defn apply-block-transforms [{:keys [::block-transforms] :as doc}
                              {:keys [::block-id]}]
  (let [block (block-by-id doc block-id)]
    (loop [tfs block-transforms
           doc doc]
      (if (empty? tfs)
        doc
        (recur
          (rest tfs)
          (let [upd-block ((first block-transforms) block)]
            (set-block doc block-id upd-block)))))))

(defn post-insert-string-transform [doc block opts]
  (-> doc
      (tf-header-expansion block)
      (tf-unordered-list-item-expansion block)
      (tf-ordered-list-item-expansion block)
      (tf-blockquote-expansion block)
      (tf-earmuffs-expansion block)
      (wrapped-inline
        block
        "`"
        ::code)
      (apply-block-transforms block)
      clamp-selection))

(defn insert-line [doc line opts]
  (let [{:keys [::selection]} doc
        {:keys [::start]} selection
        [start-id start-offset] start
        block (block-by-id doc start-id)]
    (if (embed? block)
      doc
      (let [upd-block (block-insert-string block start-offset line)

            new-offset (min
                         (+ start-offset (count line))
                         (block-end-offset upd-block))]
        (-> doc

            (set-block start-id upd-block)
            (assoc
              ::selection
              {::start [start-id new-offset]
               ::end [start-id new-offset]
               ::anchor [start-id new-offset]
               ::focus [start-id new-offset]})
            
            (post-insert-string-transform upd-block opts))))))

(defn insert-lines [doc lines opts]
  (loop [doc doc
         lines lines]
    (if (empty? lines)
      doc
      (let [line (first lines)
            doc (if (empty? line)
                  (split-at-selection doc)
                  (insert-line doc line opts))]
        (recur doc (rest lines))))))

(defn insert-string [doc s opts]
  (if s
    (-> doc
        ensure-selection
        delete-selected
        (insert-lines (str/split s #"\n" -1) opts)
        (push-undo doc))
    doc))

(defn move-to-beginning [doc]
  (set-selection
    doc
    {:start [(first (get-sel-start doc)) 0]}))

(defn move-to-end [doc]
  (let [start-id (first (get-sel-start doc))
        start-block (block-by-id doc start-id)
        last-offset (or (block-end-offset start-block) 0)]
    (set-selection
      doc
      {:start [start-id last-offset]})))

(defn delete-to-end [doc]
  (let [[start-id start-offset] (get-sel-start doc)
        start-block (block-by-id doc start-id)
        last-offset (or (block-end-offset start-block) 0)]

    (-> doc
        (set-selection
          {:start [start-id start-offset]
           :end [start-id last-offset]})
        delete-selected)))

(defn kill-line [doc]
  (let [current-block (first (selected-blocks doc))
        prev-block (prev-block-from-id doc (::block-id current-block))
        next-block (next-block-from-id doc (::block-id current-block))]
    (-> (if (and (block-empty? current-block) next-block)
          (-> doc
              (remove-block-ids [(::block-id current-block)])
              (set-selection
                {:start (cond
                          next-block
                          [(::block-id next-block) 0]
                          #_prev-block
                          #_[(::block-id prev-block) (last-offset prev-block)]
                          :else
                          [(::block-id current-block) 0])})
              ensure-one-block)
          (-> doc
              delete-to-end))
        (push-undo doc))))

(defn blocks-in-order [doc]
  (->> doc
       ::block-order
       (mapv (fn [[index id]]
               (block-by-id doc id)))))

(defn select-all [doc]
  (let [blocks (blocks-in-order doc)
        first-block (first blocks)
        last-block (last blocks)]
    (set-selection
      doc
      {:start [(::block-id first-block) 0]
       :end [(::block-id last-block) (block-end-offset last-block)]})))

(defn add-block-type-behavior [doc type-key behavior-map]
  (update
    doc
    ::block-type->behavior-map
    merge
    {type-key behavior-map}))

(defn add-block-behaviors [doc block-type->behavior-map]
  (update
    doc
    ::block-type->behavior-map
    merge
    block-type->behavior-map))

(defn block-behavior [doc block]
  (get-in
    doc
    [::block-type->behavior-map (::type block)]))

(defn valid-doc? [{:keys [::block-id->block
                          ::block-order]}]
  (if block-id->block true false))

(defn base-doc []
  {::block-id->block {}
   ::block-order (sorted-map)
   ::selection {}
   ::undo-stack (list)})

(defn empty-doc []
  (let [doc (-> (base-doc)
                (append-block (para)))]
    (-> doc
        (set-selection
          {:start [(::block-id (get-first-block doc)) 0]}))))

(defn doc-from-blocks [blocks]
  {::block-id->block
   (->> blocks
        (map (fn [block]
               [(::block-id block) block]))
        (into {}))
   ::block-order (into
                   (sorted-map)
                   (->> blocks
                        (map-indexed
                          (fn [i block]
                            [(+ INDEX_CHUNK_SIZE (* i INDEX_CHUNK_SIZE)) (::block-id block)]))))})

(defn initialize-block-order [doc]
  (update
    doc
    ::block-order
    (fn [m]
      (->> m
           (sort-by first)
           (map-indexed
             (fn [i [_ block-id]]
               [(* i INDEX_CHUNK_SIZE) block-id]))
           (into (sorted-map))))))

(defn doc->edn [doc]
  (-> doc
      (update
        ::undo-stack
        (fn [stack]
          (->> stack
               (mapv
                 (fn [EditScript]
                   (try
                     (ee/get-edits EditScript)
                     (catch js/Error e
                       EditScript)))))))
      (update
        ::redo-stack
        (fn [stack]
          (->> stack
               (mapv
                 (fn [EditScript]
                   (try
                     (ee/get-edits EditScript)
                     (catch js/Error e
                       EditScript)))))))
      (dissoc ::block-transforms)))

(defn edn->doc [edn]
  (-> edn
      (update
        ::undo-stack
        (fn [stack]
          (->> stack
               (map (fn [edits]
                      (try
                        (ee/edits->script edits)
                        (catch js/Error e nil))))
               (remove nil?)
               vec)))
      (update
        ::redo-stack
        (fn [stack]
          (->> stack
               (map (fn [edits]
                      (try
                        (ee/edits->script edits)
                        (catch js/Error e nil))))
               (remove nil?)
               vec)))
      initialize-block-order))

(defn set-block-transforms [doc transform-fns]
  (assoc
    doc
    ::block-transforms transform-fns))

(defn toc-blocks [doc]
  (->> doc
       blocks-in-order
       (filter heading-block?)))

(comment

  (-> (update-decos
        {}
        0 10
        (fn [deco]
          {::styles #{::bold ::italic}}))
      (clear-styles 0 4)
      (set-inline-embed 0 4 {::inline-embed-id "foo"
                             :hello "world"})
      (remove-block-inline-embed 2 4)
      ks/pp)
  
  )
