(ns rx.browser.aws-admin.entry
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom :refer-macros [gol <? <defn]]
            [rx.box2.reagent :as bre]
            [rx.theme :as th]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.modal :as modal]
            [rx.browser.forms :as fm]
            [rx.aws :as aws]
            [rx.view2 :as view]
            [reagent.dom :as rdom]))

(defn menu-items []
  [{:title "Cognito"}])

(defn service-key [{:keys [::aws/service-name]}]
  (condp = service-name
    "CognitoISP" :cognitoisp
    nil))

(<defn <nav [{:keys [::state]} route]
  (let [{:keys [!main-view]} state]
    (reset! !main-view (<? (view/<realize-route route)))))

(<defn <open-servint [opts servint])

(defn <handle-create-servint [{:keys [:box/conn]
                               :as opts}
                              servint-form-data]
  (gol
    (let [servint (merge
                    {::aws/servint-id (ks/uuid)}
                    servint-form-data
                    {::aws/service-key (service-key servint-form-data)})]
      (<? (bre/<transact
            conn
            [servint]))
      (<? (modal/<hide!))
      (<open-servint opts servint))))

(<defn create-service-modal []
  {:render
   (fn [opts]
     (let [{:keys [fg-color]}
           (th/resolve opts
             [[:fg-color :color/fg-1]])]
       [ui/panel
        {:layer 0
         :style {:width 300
                 :height 480
                 :overflow-y 'scroll
                 :border-radius 4}}
        [ui/panel
         {:layer 1
          :vpad 8
          :hpad 12
          :style {:font-weight 'bold
                  :font-size 12
                  :color fg-color}}
         "Create Service Interface"]
        [fm/form
         {:pad 12
          :gap 20
          :initial-data {::aws/service-key :cognitoisp
                         ::aws/access-id "accessid"
                         ::aws/secret-key "secretkey"
                         ::aws/region-code "us-west-2"}
          :on-submit (partial <handle-create-servint opts)
          :prop-validations
          {::aws/service-name (fn [v]
                                (cond
                                  (empty? v)
                                  "Required"
                                  
                                  (not (service-key {::aws/service-name v}))
                                  "Couldn't find service key"))
           ::aws/access-id (fn [v]
                             (when (empty? v)
                               "Required"))
           ::aws/secret-key (fn [v]
                              (when (empty? v)
                                "Required"))
           ::aws/region-code (fn [v]
                               (when (empty? v)
                                 "Required"))}}
         [fm/group
          {:gap 8
           :data-key ::aws/service-key}
          [fm/label "AWS Service"]
          [fm/select
           {:options
            [{:label "Choose service..."
              :disabled? true}
             {:label "Cognito ISP"
              :value :cognitoisp}]}]
          [fm/error-text]]
         [fm/group
          {:gap 8
           :data-key ::aws/access-id}
          [fm/label "Access ID"]
          [fm/text]
          [fm/error-text]]
         [fm/group
          {:gap 8
           :data-key ::aws/secret-key}
          [fm/label "Secret Key"]
          [fm/text]
          [fm/error-text]]
         [fm/group
          {:gap 8
           :data-key ::aws/region-code}
          [fm/label "Region"]
          [fm/text]
          [fm/error-text]]
         [fm/group
          {:horizontal? true
           :justify-content 'flex-end}
          [fm/submit-button {:label "Create Service"}]]]]))})

(defn <show-create-service [opts]
  (gol
    (<? (modal/<show!
          [create-service-modal opts]))))

(defn root [{:keys [:box/conn]
             :as opts}]
  [ui/g
   {:horizontal? true
    :style {:flex 1}}
   [ui/panel
    {:layer 1
     :style {:width 250
             :padding-top 30}}
    [ui/g
     {:flex 1}
     [ui/g {:overflow-y 'scroll
            :flex 1}
      [ui/list-container
       {:layer 1}
       (->> (menu-items)
            (map (fn [{:keys [title]}]
                   [ui/clickable
                    {:on-click
                     (fn []
                       (prn "HI"))
                     :hpad 4
                     :vpad 8}
                    [:div title]])))]]
     [ui/separator
      {:layer 1}]
     [ui/clickable
      {:on-click (partial <show-create-service opts)
       :pad 4}
      "New Service"]]]
   [ui/g
    {:flex 1}
    [:pre (ks/pp-str conn)]]])

(defn init [& [opts]]
  (browser/<show-component!
    [root opts]))

(comment

  (init)

  (gol
    (<! (modal/<hide!))
    (<show-create-service {}))

  )
