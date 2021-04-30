(ns rx.browser.fast-list-otto
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as b]
            [rx.browser.fast-list :as fl]
            [cljs.core.async :as async
             :refer [chan <! >! timeout close! put! sliding-buffer alts!]
             :refer-macros [go]]))

(defn verify-default []
  (go
    (let [ts (ks/now)
          control (fl/create-state
                    {:id-key :sort
                     :sort-key :sort})]
      (<! (b/<set-root!
            [(fn []
               {:render
                (fn []
                  [fl/view
                   {:control control
                    :initial-items
                    (->> (range 50 51)
                         (map (fn [i]
                                {:sort i})))
                    :render-item
                    (fn [opts item]
                      [:div
                       [:pre (pr-str opts)]
                       [:pre (pr-str item)]
                       [:div {:style {:height 20}}]])}])})]))
      (<! (timeout 100))
      (fl/append-items
        control
        (->> (range 0 100)
             (map (fn [i]
                    {:sort i})))))))

(defn verify-order []
  (go
    (let [ts (ks/now)
          control (fl/create-state
                    {:id-key :rx.log/event-id
                     :sort-key :rx.log/created-ts})]
      (<! (b/<set-root!
            [(fn []
               {:render
                (fn []
                  [fl/view
                   {:control control
                    :render-item
                    (fn [opts item]
                      [:div
                       [:pre (pr-str opts)]
                       [:pre (pr-str item)]
                       [:div {:style {:height 20}}]])}])})]))
      (doseq [items [#_'({:rx.log/app-name "Rx Test App",
                          :rx.log/event-id "6e94081e3773422a9680491317baf686",
                          :rx.log/ingress-ts 1579652184073,
                          :rx.log/group-name "rx.browser.logview",
                          :rx.log/created-ts 1579652183898,
                          :rx.log/event-name "log-stream-comp.call"})
                     #_'({:rx.log/app-name "Rx Test App",
                          :rx.log/event-id "05c8caaf27084abfbc535778828a570c",
                          :rx.log/ingress-ts 1579652184099,
                          :rx.log/group-name "rx.browser.logview",
                          :rx.log/created-ts 1579652183940,
                          :rx.log/event-name "start-filter-text-processor"})
                     '({:rx.log/foo "bar"
                        :rx.log/app-name "Rx Test App",
                        :rx.log/event-id "f632b0ac2635416188e44a3e7cb1ad17",
                        :rx.log/ingress-ts 1579652184163,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579652183955, 
                        :rx.log/event-name "view.init"})
                     #_'({:rx.log/app-name "Rx Test App",
                          :rx.log/event-id "de1bd11f0cae456890ddecbe7eead608",
                          :rx.log/ingress-ts 1579636007408,
                          :rx.log/group-name "rx.browser.fast-list",
                          :rx.log/created-ts 1579636007242,
                          :rx.log/event-name "view.init"}
                         {:rx.log/app-name "Rx Test App",
                          :rx.log/event-id "bea7650e47104d13829ab73b6aeacd67",
                          :rx.log/ingress-ts 1579636068572,
                          :rx.log/group-name "rx.browser.logview",
                          :rx.log/created-ts 1579636068399,
                          :rx.log/event-name "log-stream-comp.call"}
                         )
                     '({:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "de1bd11f0cae456890ddecbe7eead608",
                        :rx.log/ingress-ts 1579636007408,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579636007242,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "bea7650e47104d13829ab73b6aeacd67",
                        :rx.log/ingress-ts 1579636068572,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579636068399,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "e7151eed05d74f489dd7b4502a7ea943",
                        :rx.log/ingress-ts 1579636068578,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579636068442,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "110627696a3c4e4796dbf626f414e788",
                        :rx.log/ingress-ts 1579636068636,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579636068460,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "63c765103cd648979536af6b9234ac17",
                        :rx.log/ingress-ts 1579636115534,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579636115366,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "e32299d797d542ad8b42042fb5ce8b64",
                        :rx.log/ingress-ts 1579636115541,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579636115404,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "41b0df7589d74dca8c88b7144bd6fb02",
                        :rx.log/ingress-ts 1579636115599,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579636115423,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "d9c3623455ea48bba9497ad773f1ed3c",
                        :rx.log/ingress-ts 1579639223326,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639223268,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "687d65660bd7429d8b940e5d843a7faa",
                        :rx.log/ingress-ts 1579639223335,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639223301,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "b23fa0d8513945b59d04f46337b99361",
                        :rx.log/ingress-ts 1579639223361,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639223309,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "43d231150ff74d5da69c101e2ab0d830",
                        :rx.log/ingress-ts 1579639241798,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639241758,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "82c9fec8d799436a97ff12e9810a24f8",
                        :rx.log/ingress-ts 1579639241807,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639241778,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "ce90a7dec456435194e66ba36835acb7",
                        :rx.log/ingress-ts 1579639241837,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639241783,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "c3a07a3178414aa2825ad6fe89face18",
                        :rx.log/ingress-ts 1579639263955,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639263899,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "f30a63855532461284599191e0f1560a",
                        :rx.log/ingress-ts 1579639263959,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639263932,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "c66f1e59a86343d1a531a9b76e143f64",
                        :rx.log/ingress-ts 1579639263978,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639263942,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "c832ff262bf14620ae623066ce4a13ba",
                        :rx.log/ingress-ts 1579639277656,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639277587,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "daa7b79912a64d608677f067606b7e2d",
                        :rx.log/ingress-ts 1579639277684,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639277632,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "06402fc001c948d582d1fc50a086547e",
                        :rx.log/ingress-ts 1579639277720,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639277640,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "a5a93819b0994af9ae1c09f17cc25409",
                        :rx.log/ingress-ts 1579639284223,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639283887,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "afef7b4161da47f0904e8e58265ded40",
                        :rx.log/ingress-ts 1579639309535,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639309395,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "b973c155e77346a49d13d638af3cfde8",
                        :rx.log/ingress-ts 1579639309691,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579639309431,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "a3b544a8726f4236ba7f470bd07ed8e7",
                        :rx.log/ingress-ts 1579639310029,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579639309448,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "43a139497ca94ee78826d5e6befc5078",
                        :rx.log/ingress-ts 1579650377673,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579650377615,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "2700c6e96df549809f37c6735e1ccfe0",
                        :rx.log/ingress-ts 1579650377682,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579650377639,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "0c01f13fc63a4ad0a513b67675d514d6",
                        :rx.log/ingress-ts 1579650377714,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579650377649,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "b7aa719cb7a942c9b84128bd8c4b3cbe",
                        :rx.log/ingress-ts 1579652117919,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579652116988,
                        :rx.log/event-name "view.init"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "e2dfbed5d2f14c5cbe4596c82dfab493",
                        :rx.log/ingress-ts 1579652168508,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579652168459,
                        :rx.log/event-name "log-stream-comp.call"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "6990f02b739c4f71a5e68fcff123890c",
                        :rx.log/ingress-ts 1579652168520,
                        :rx.log/group-name "rx.browser.logview",
                        :rx.log/created-ts 1579652168481,
                        :rx.log/event-name "start-filter-text-processor"}
                       {:rx.log/app-name "Rx Test App",
                        :rx.log/event-id "9edefd4cc6d74dceb91213384d5fd87b",
                        :rx.log/ingress-ts 1579652168573,
                        :rx.log/group-name "rx.browser.fast-list",
                        :rx.log/created-ts 1579652168488, 
                        :rx.log/event-name "view.init"})]]
        
        (<! (timeout 100))
        (fl/append-items
          control
          items)))))




(comment

  (verify-default)

  (verify-order)

  )


