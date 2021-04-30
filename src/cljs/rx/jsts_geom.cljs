(ns rx.jsts-geom
  (:require [rx.kitchen-sink :as ks]))

(def jsts nil #_(js/require "jsts"))

(defn c
  "create a coordinate object."
  ([x y] (jsts.geom.Coordinate.
           (double x) (double y)))
  ([x y z] (jsts.geom.Coordinate.
             (double x) (double y) (double z))))

(def GF #_(jsts.geom.GeometryFactory.))

(defn linear-ring [coords]
  (.createLinearRing
    GF
    (into-array coords)))

(defn polygon [ring rings]
  (.createPolygon
    GF
    ring
    (into-array rings)))

(defn multi-point [ps]
  (.createMultiPoint GF (into-array ps)))

(defn rect->linear-ring [{:keys [x y w h]}]
  (->> [[x y]
        [(+ x w) y]
        [(+ x w) (+ y h)]
        [x (+ y h)]
        [x y]]

       (map (fn [[x y]]
              (c x y)))
       linear-ring))

(defn points->linear-ring [points]
  (->> points
       (map (fn [{:keys [x y]}]
              (c x y)))
       linear-ring))

(defn rect->poly [rect]
  (polygon
    (rect->linear-ring rect)
    nil))

(defn points->poly [points]
  (polygon
    (points->linear-ring points)
    nil))

(defn circle->closed-circle-points [{:keys [x y r n]}]
  (let [thetas (->> (range n)
                    (map #(* % (/ 360 n))))
        points (->> thetas
                    (map #(* % (/ ks/PI 180)))
                    (map (fn [rad]
                           {:x (+ x
                                  (* r (ks/cos rad)))
                            :y (+ y
                                  (* r (ks/sin rad)))})))]
    (vec
      (concat
        points
        [(first points)]))))

(defn union [this that]
  (.union this that))

(defn calc-circle-outlines [circles]
  (->> circles
       (map circle->closed-circle-points)
       (map points->poly)
       (reduce union)
       #_cg/boundary
       #_mls->coordinate-groups
       #_vec
       #_cpoly->points
       #_calc-poly-layers))

(defn cpoly->points [cpoly]
  (when cpoly
    (->> (.getCoordinates cpoly)
         (map (fn [c]
                {:x (.-x c) :y (.-y c)}))
         vec)))

(defn mls->coordinate-groups [mls]
  (->> (range (.getNumGeometries mls))
       (map (fn [i]
              (.getGeometryN mls i)))
       (map cpoly->points)))

(defn boundary [o]
  (.getBoundary o))

(defn poly-union [p1 p2]
  (cpoly->points
    (boundary
      (union
        (polygon
          (rect->linear-ring p1)
          nil)

        (polygon
          (rect->linear-ring p2)
          nil)))))

(defn poly-union-boundary [p1 p2]
  (polygon
    (boundary
      (union
        p1 p2))
    nil))

(defn intersects? [this that]
  (.intersects this that))

(defn add-to-polys [polys p]
  (let [ext-inter (->> polys
                       (map
                         (fn [parent-poly]
                           (if (intersects? parent-poly p)
                             (poly-union-boundary parent-poly p)
                             parent-poly))))
        ext-inter (concat
                    ext-inter
                    [p])]
    (reduce
      (fn [polys p]
        (if (and (last polys)
                 (intersects?
                   (last polys)
                   p))

          (vec
            (concat
              (butlast polys)
              [(poly-union-boundary (last polys) p)]))

          (vec (concat polys [p]))))
      []
      ext-inter)))

(defn calc-poly-layers [polys]
  (->> (range (count polys))
       (map (fn [i]
              (take (inc i) polys)))
       (map (fn [poly-perm]
              (->> poly-perm
                   (reduce
                     add-to-polys
                     [])
                   (map cpoly->points))))))

(defn calc-outlines [images]
  (->> images
       (map rect->poly)
       calc-poly-layers))

(defn coords->poly [coords]
  (->> coords
       points->linear-ring))

(defn multipoly->polylines [multi]
  (->> multi
       boundary
       mls->coordinate-groups
       vec
       cpoly->points
       #_calc-poly-layers))

(defn doug-simplify [geom]
  (.getResultGeometry
    (jsts.simplify.DouglasPeuckerSimplifier. geom)))

(defn topo-simplify [geom]
  (jsts.simplify.TopologyPreservingSimplifier.simplify
    geom
    0.0003))

(defn add-circle-to-polylines [circle polylines]
  (->> polylines
       (map points->linear-ring)
       (map #(polygon % nil))
       ((fn [polygons]
          (if (not (empty? polygons))
            (reduce union polygons)
            (polygon nil nil))))
       ((fn [existing-poly]
          (union
            existing-poly
            (polygon
              (points->linear-ring
                (circle->closed-circle-points circle))
              nil))))
       #_topo-simplify
       doug-simplify


       boundary
       mls->coordinate-groups
       vec))

(defn poly-clj->poly-obj [{:keys [shell-coords
                                  holes-coords]}]
  (polygon
    (->> shell-coords
         (map (fn [{:keys [x y]}]
                (c x y)))
         linear-ring)
    (->> holes-coords
         (map (fn [coords]
                (map
                  (fn [{:keys [x y]}]
                    (c x y))
                  coords)))
         (map linear-ring))))

(defn poly-obj->poly-clj [poly-obj]
  (let [exterior-ring (cpoly->points
                        (.getExteriorRing poly-obj))
        interior-rings (->> (range (.getNumInteriorRing poly-obj))
                            (map #(.getInteriorRingN poly-obj %))
                            (mapv cpoly->points))]
    (merge
      (when exterior-ring
        {:shell-coords exterior-ring})
      (when (and interior-rings (not (empty? interior-rings)))
        {:holes-coords interior-rings}))))

(defn multipoly-clj->multipoly-obj [{:keys [polys]}]
  (.createMultiPolygon
    GF
    (into-array (map poly-clj->poly-obj polys))))

(defn multipoly-obj->multipoly-clj [mp-obj]
  {:polys (->> (range (.getNumGeometries mp-obj))
               (map (fn [i]
                      (let [poly-obj (.getGeometryN mp-obj i)]
                        (poly-obj->poly-clj poly-obj))))
               vec)
   :envelope-coords (->> mp-obj
                         #_(#(.getBoundary %))
                         (#(.getEnvelope %))
                         cpoly->points)})

(comment

  (ks/pp (multipoly-clj->multipoly-obj nil))

  )

(defn add-circle-to-multipoly [circle-clj multipoly-clj]
  (let [mp-obj (multipoly-clj->multipoly-obj multipoly-clj)]
    (if circle-clj
      (-> mp-obj
          (union
            (polygon
              (points->linear-ring
                (circle->closed-circle-points circle-clj))
              nil))
          topo-simplify
          multipoly-obj->multipoly-clj)
      (multipoly-obj->multipoly-clj mp-obj))))

(comment

  (try
    (ks/pp
      (add-circle-to-multipoly
        {:x 37.33193509
         :y -122.0376846
         :r 0.001
         :n 8}
        {:polys
         [{:shell-coords
           [{:x -122.03880937999999, :y 37.33298434}
            {:x -122.0391022732188, :y 37.33227723321882}
            {:x -122.03980938, :y 37.331984340000005}
            {:x -122.04051648678119, :y 37.33227723321882}
            {:x -122.04073989254765, :y 37.332816582450114}
            {:x -122.0408145832188, :y 37.33263626321882}
            {:x -122.04152169, :y 37.332343370000004}
            {:x -122.04222879678119, :y 37.33263626321882}
            {:x -122.04252169, :y 37.33334337}
            {:x -122.04222879678119, :y 37.33405047678119}
            {:x -122.04152169, :y 37.33434337}
            {:x -122.0408145832188, :y 37.33405047678119}
            {:x -122.04059117745234, :y 37.33351112754989}
            {:x -122.04051648678119, :y 37.33369144678119}
            {:x -122.03980938, :y 37.33398434}
            {:x -122.0391022732188, :y 37.33369144678119}
            {:x -122.03880937999999, :y 37.33298434}]}
          {:shell-coords
           [{:x -122.02966241, :y 37.33767947}
            {:x -122.02995530321881, :y 37.33697236321881}
            {:x -122.03066241, :y 37.33667947}
            {:x -122.0313695167812, :y 37.33697236321881}
            {:x -122.03160086758152, :y 37.33753089345862}
            {:x -122.03182577321881, :y 37.336987923218814}
            {:x -122.03253288, :y 37.33669503}
            {:x -122.0332399867812, :y 37.336987923218814}
            {:x -122.03353288000001, :y 37.33769503}
            {:x -122.0332399867812, :y 37.338402136781184}
            {:x -122.03253288, :y 37.33869503}
            {:x -122.03182577321881, :y 37.338402136781184}
            {:x -122.0315944224185, :y 37.33784360654138}
            {:x -122.0313695167812, :y 37.33838657678118}
            {:x -122.03066241, :y 37.338679469999995}
            {:x -122.02995530321881, :y 37.33838657678118}
            {:x -122.02966241, :y 37.33767947}]}
          {:shell-coords
           [{:x -122.03618938999999, :y 37.33760128}
            {:x -122.0364822832188, :y 37.336894173218816}
            {:x -122.03718939, :y 37.336601280000004}
            {:x -122.03789649678119, :y 37.336894173218816}
            {:x -122.03818939, :y 37.33760128}
            {:x -122.03789649678119, :y 37.338308386781186}
            {:x -122.03718939, :y 37.33860128}
            {:x -122.0364822832188, :y 37.338308386781186}
            {:x -122.03618938999999, :y 37.33760128}]}
          {:shell-coords
           [{:x -122.02222490999999, :y 37.33086434}
            {:x -122.0225178032188, :y 37.33015723321881}
            {:x -122.02274098387352, :y 37.33006478876477}
            {:x -122.0229629632188, :y 37.32952888321881}
            {:x -122.02367007, :y 37.32923599}
            {:x -122.02437717678119, :y 37.32952888321881}
            {:x -122.02462785686006, :y 37.33013407846507}
            {:x -122.02489661321881, :y 37.32948524321881}
            {:x -122.02560372, :y 37.32919235}
            {:x -122.0263108267812, :y 37.32948524321881}
            {:x -122.02660372000001, :y 37.33019235}
            {:x -122.0263108267812, :y 37.33089945678118}
            {:x -122.02560372, :y 37.331192349999995}
            {:x -122.02489661321881, :y 37.33089945678118}
            {:x -122.02464593313994, :y 37.330294261534924}
            {:x -122.02437717678119, :y 37.33094309678118}
            {:x -122.02415399612647, :y 37.331035541235224}
            {:x -122.02405576590944, :y 37.331272689957416}
            {:x -122.02420789, :y 37.33163995}
            {:x -122.02394861531434, :y 37.332265894462516}
            {:x -122.0243009667812, :y 37.332411843218814}
            {:x -122.02459386000001, :y 37.33311895}
            {:x -122.024440884882, :y 37.333488264604625}
            {:x -122.02459082, :y 37.33385024}
            {:x -122.02442363097042, :y 37.33425387002271}
            {:x -122.02457759, :y 37.33462556}
            {:x -122.0244041231428, :y 37.33504434603928}
            {:x -122.02452918, :y 37.33534626}
            {:x -122.02423628678119, :y 37.33605336678119}
            {:x -122.02394523672015, :y 37.33617392366379}
            {:x -122.02422852000001, :y 37.33685783}
            {:x -122.02406034167169, :y 37.33726384840113}
            {:x -122.02415707102733, :y 37.3374973737234}
            {:x -122.0243679432188, :y 37.336988283218815}
            {:x -122.02507505, :y 37.33669539}
            {:x -122.02578215678119, :y 37.336988283218815}
            {:x -122.02598827094408, :y 37.33748588682627}
            {:x -122.02619456321881, :y 37.336987853218815}
            {:x -122.02690167, :y 37.33669496}
            {:x -122.0276087767812, :y 37.336987853218815}
            {:x -122.02790167, :y 37.33769496}
            {:x -122.0276087767812, :y 37.338402066781185}
            {:x -122.02690167, :y 37.33869496}
            {:x -122.02619456321881, :y 37.338402066781185}
            {:x -122.02598844905592, :y 37.33790446317373}
            {:x -122.02578215678119, :y 37.338402496781185}
            {:x -122.02507505, :y 37.33869539}
            {:x -122.0243679432188, :y 37.338402496781185}
            {:x -122.02411801897267, :y 37.3377991262766}
            {:x -122.0239071467812, :y 37.338308216781186}
            {:x -122.02320004, :y 37.33860111}
            {:x -122.02249293321881, :y 37.338308216781186}
            {:x -122.02220004, :y 37.33760111}
            {:x -122.02236821832832, :y 37.33719509159887}
            {:x -122.02222852, :y 37.33685783}
            {:x -122.02252141321881, :y 37.336150723218815}
            {:x -122.02281246327985, :y 37.33603016633621}
            {:x -122.02252917999999, :y 37.33534626}
            {:x -122.0227026468572, :y 37.33492747396072}
            {:x -122.02257759, :y 37.33462556}
            {:x -122.02274477902958, :y 37.33422192997729}
            {:x -122.02259081999999, :y 37.33385024}
            {:x -122.022743795118, :y 37.33348092539537}
            {:x -122.02259386, :y 37.33311895}
            {:x -122.02285313468566, :y 37.332493005537486}
            {:x -122.0225007832188, :y 37.33234705678119}
            {:x -122.02220788999999, :y 37.33163995}
            {:x -122.02237703409055, :y 37.331231600042585}
            {:x -122.02222490999999, :y 37.33086434}]}
          {:shell-coords
           [{:x -122.02972924999999, :y 37.33148411}
            {:x -122.02983168234381, :y 37.3312368164463}
            {:x -122.02937447, :y 37.331426199999996}
            {:x -122.0286673632188, :y 37.33113330678118}
            {:x -122.02837446999999, :y 37.3304262}
            {:x -122.0286673632188, :y 37.32971909321881}
            {:x -122.02937447, :y 37.3294262}
            {:x -122.03008157678119, :y 37.32971909321881}
            {:x -122.03026865530032, :y 37.33017074071692}
            {:x -122.0305346332188, :y 37.32952861321881}
            {:x -122.03124174, :y 37.32923572}
            {:x -122.03162455835, :y 37.32939428855249}
            {:x -122.03221244, :y 37.32915078}
            {:x -122.03291954678119, :y 37.32944367321881}
            {:x -122.03321244, :y 37.33015078}
            {:x -122.03291954678119, :y 37.33085788678118}
            {:x -122.03221244, :y 37.331150779999994}
            {:x -122.03182962164999, :y 37.3309922114475}
            {:x -122.03157003668676, :y 37.33109973505986}
            {:x -122.03172925, :y 37.33148411}
            {:x -122.03143635678119, :y 37.33219121678118}
            {:x -122.03072925, :y 37.332484109999996}
            {:x -122.0300221432188, :y 37.33219121678118}
            {:x -122.02972924999999, :y 37.33148411}],
           :holes-coords
           [[{:x -122.03034755469967, :y 37.33049117928307}
             {:x -122.03027203765618, :y 37.3306734935537}
             {:x -122.03040095331323, :y 37.33062009494014}
             {:x -122.03034755469967, :y 37.33049117928307}]]}
          {:shell-coords
           [{:x -122.03536171993544, :y 37.3314743747264}
            {:x -122.03586087, :y 37.331267620000006}
            {:x -122.03656797678119, :y 37.33156051321882}
            {:x -122.03686087, :y 37.33226762}
            {:x -122.03656797678119, :y 37.33297472678119}
            {:x -122.03586087, :y 37.33326762}
            {:x -122.03540998006456, :y 37.33308085527361}
            {:x -122.03491083, :y 37.33328761}
            {:x -122.03420372321881, :y 37.33299471678119}
            {:x -122.03391083, :y 37.33228761}
            {:x -122.03420372321881, :y 37.33158050321882}
            {:x -122.03491083, :y 37.331287610000004}
            {:x -122.03536171993544, :y 37.3314743747264}]}
          {:shell-coords
           [{:x -122.03987029, :y 37.3375652}
            {:x -122.04016318321881, :y 37.336858093218815}
            {:x -122.04087029, :y 37.3365652}
            {:x -122.0415773967812, :y 37.336858093218815}
            {:x -122.04187029, :y 37.3375652}
            {:x -122.0415773967812, :y 37.338272306781185}
            {:x -122.04087029, :y 37.3385652}
            {:x -122.04016318321881, :y 37.338272306781185}
            {:x -122.03987029, :y 37.3375652}]}]}))
    (catch js/Error e
      (prn "ERR")))

  (multipoly-clj->multipoly-obj
    {:polys
     [{:shell-coords
       [{:x 55, :y 50}
        {:x 54.33012701892219, :y 52.5}
        {:x 52.5, :y 54.33012701892219}
        {:x 50, :y 55}
        {:x 47.5, :y 54.33012701892219}
        {:x 45.66987298107781, :y 52.5}
        {:x 45, :y 50}
        {:x 45.66987298107781, :y 47.5}
        {:x 47.5, :y 45.66987298107781}
        {:x 50, :y 45}
        {:x 52.5, :y 45.66987298107781}
        {:x 54.33012701892219, :y 47.5}
        {:x 55, :y 50}]}]})

  (ks/pp
    (add-circle-to-multipolys
      {:x 55
       :y 55
       :r 5
       :n 12}
      {:polys
       [{:shell-coords
         [{:x 55, :y 50}
          {:x 54.33012701892219, :y 52.5}
          {:x 52.5, :y 54.33012701892219}
          {:x 50, :y 55}
          {:x 47.5, :y 54.33012701892219}
          {:x 45.66987298107781, :y 52.5}
          {:x 45, :y 50}
          {:x 45.66987298107781, :y 47.5}
          {:x 47.5, :y 45.66987298107781}
          {:x 50, :y 45}
          {:x 52.5, :y 45.66987298107781}
          {:x 54.33012701892219, :y 47.5}
          {:x 55, :y 50}]}]}))

  (ks/pp
    (add-circle-to-multipolys
      {:x 50
       :y 50
       :r 5
       :n 12}
      nil))

  (polygon
    (points->linear-ring
      (circle->closed-circle-points circle))
    nil)


  )

(defn multipolygon->clj [o]
  (.getCoordinates (.getBoundary o)))

(comment

  (rect->linear-ring
    {:x 0 :y 0 :w 100 :h 100})

  (.createPoint GF (c 0 0))

  (prn GF)

  (.createLinearRing GF #js [(c 0 0)
                             (c 0 1)
                             (c 1 1)
                             (c 1 0)
                             (c 0 0)])

  (linear-ring
    [(c 0 0)
     (c 0 1)
     (c 1 1)
     (c 0 0)])

  (->> {:x 50
        :y 50
        :r 5
        :n 12}
       circle->closed-circle-points
       points->poly
       (reduce poly-union-boundary))





  (ks/pp
    (multipolygon->clj
      (calc-circle-outlines
        [{:x 50
          :y 50
          :r 10
          :n 12}
         {:x 60
          :y 60
          :r 10
          :n 12}

         {:x 80
          :y 80
          :r 10
          :n 12}])))









  #_(nu/pp
      (add-to-polys
        [(rect->poly (first IMAGES))
         (rect->poly (second IMAGES))]
        (rect->poly (nth IMAGES 2))))







  #_(cg/polygon nil nil)

  (ks/pp
    (add-circle-to-polylines
      {:x 10 :y 100 :r 50 :n 16}
      [[{:x 60.0, :y 50.0}
        {:x 58.66025403784438, :y 44.99999999999999}
        {:x 55.0, :y 41.33974596215562}
        {:x 50.0, :y 40.0}
        {:x 44.99999999999999, :y 41.33974596215562}
        {:x 41.33974596215562, :y 45.0}
        {:x 40.0, :y 50.0}
        {:x 41.33974596215561, :y 55.0}
        {:x 45.0, :y 58.66025403784439}
        {:x 50.0, :y 60.0}
        {:x 51.33974596215561, :y 65.0}
        {:x 55.0, :y 68.66025403784438}
        {:x 60.0, :y 70.0}
        {:x 65.0, :y 68.66025403784438}
        {:x 68.66025403784438, :y 65.0}
        {:x 70.0, :y 60.0}
        {:x 68.66025403784438, :y 54.99999999999999}
        {:x 65.0, :y 51.33974596215562}
        {:x 60.0, :y 50.0}]
       [{:x 90.0, :y 80.0}
        {:x 88.66025403784438, :y 75.0}
        {:x 85.0, :y 71.33974596215562}
        {:x 80.0, :y 70.0}
        {:x 75.0, :y 71.33974596215562}
        {:x 71.33974596215562, :y 75.0}
        {:x 70.0, :y 80.0}
        {:x 71.33974596215562, :y 85.0}
        {:x 75.0, :y 88.66025403784438}
        {:x 80.0, :y 90.0}
        {:x 85.0, :y 88.66025403784438}
        {:x 88.66025403784438, :y 85.0}
        {:x 90.0, :y 80.0}]]))

  (ks/pp
    (add-circle-to-polylines
      {:x 10 :y 100 :r 50 :n 16}
      []))

  (ks/pp
    (add-circle-to-polylines
      {:x 10 :y 100 :r 50 :n 16}
      [[{:x 60, :y 100}
        {:x 56.19397662556434, :y 119.13417161825448}
        {:x 45.35533905932738, :y 135.35533905932738}
        {:x 29.13417161825449, :y 146.19397662556435}
        {:x 10.000000000000004, :y 150}
        {:x -9.134171618254488, :y 146.19397662556435}
        {:x -25.35533905932737, :y 135.35533905932738}
        {:x -36.19397662556434, :y 119.1341716182545}
        {:x -40, :y 100}
        {:x -36.19397662556434, :y 80.86582838174552}
        {:x -25.355339059327385, :y 64.64466094067262}
        {:x -9.134171618254474, :y 53.80602337443566}
        {:x 9.999999999999991, :y 50}
        {:x 29.1341716182545, :y 53.80602337443567}
        {:x 45.35533905932737, :y 64.64466094067262}
        {:x 56.19397662556434, :y 80.86582838174553}
        {:x 60, :y 100}]]))

  #_(points->linear-ring
      (circle->closed-circle-points
        {:x 10 :y 10 :r 50 :n 20}))

  )


(comment

  (c 0 0)

  (prn jsts.geom.Coordinate)

  (prn (jsts.geom.Coordinate.
         (double 0)
         (double 0)))





  )
