(ns rx.browser.gsheets
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [clojure.core.async :as async
             :refer [go <! chan close! put!]]))

(defn <init-api [{:keys [api-key
                         client-id
                         discovery-docs
                         scopes]}]
  (let [ch (chan)]
    (gol
      (<! (browser/<ensure-script "https://apis.google.com/js/api.js"))
      (.load js/gapi "client:auth2"
        (fn []
          (gol
            (let [res (<! (ks/<promise
                            (.init (.-client js/gapi)
                              (clj->js
                                {:apiKey api-key
                                 :clientId client-id
                                 :discoveryDocs discovery-docs
                                 :scope scopes}))))]
              (if (anom/? res)
                (put! ch res))
              (close! ch))))))
    ch))

(defn test-stuff []
  (browser/<show-component!
    [:div "HI"]))

(defn auth-inst []
  (.getAuthInstance (.-auth2 js/gapi)))

(defn current-user-profile []
  (let [user (.get (.-currentUser (auth-inst)))]
    (when user
      (let [p (.getBasicProfile user)]
        {:id (.getId p)
         :name (.getName p)
         :given-name (.getGivenName p)
         :family-name (.getFamilyName p)
         :image-url (.getImageUrl p)
         :email (.getEmail p)}))))

(defn is-signed-in-listen [f]
  (.listen (.-isSignedIn (auth-inst)) f))

(defn is-signed-in []
  (.get (.-isSignedIn (auth-inst))))

(defn <do-sign-in []
  (ks/<promise (.signIn (auth-inst))))

(defn <list-events [opts]
  (gol
    (let [resp (<? (ks/<promise
                     (.list
                       (.. js/gapi -client -calendar -events)
                       (clj->js opts))))]
      (if-let [res (.-result resp)]
        (merge
          (js->clj res)
          {::call-opts opts
           ::call-type ::events-list})
        (anom/anom
          {:desc "Error listing events"
           ::resp resp})))))

(<defn <delete-event [opts]
  (let [resp (<? (ks/<promise
                   (.delete
                     (.. js/gapi -client -calendar -events)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error calling delete event"
           ::resp resp}))))

(<defn <values-get [opts]
  (let [resp (<? (ks/<promise
                   (.get
                     (.. js/gapi -client -sheets -spreadsheets -values)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error"
           ::resp resp}))))

(<defn <values-update [opts]
  (let [resp (<? (ks/<promise
                   (.update
                     (.. js/gapi -client -sheets -spreadsheets -values)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
        {:desc "Error"
         ::resp resp}))))

(<defn <values-append [opts]
  (let [resp (<? (ks/<promise
                   (.append
                     (.. js/gapi -client -sheets -spreadsheets -values)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
        {:desc "Error"
         ::resp resp}))))

(<defn <next [resp]
  (if-let [page-token (get resp "page-token")]
    (let [{:keys [::call-opts ::call-type]} resp
          f (condp = call-type
              ::events-list
              <list-events)]
      (<? (f (merge call-opts
               {:nextPageToken (get resp "nextPageToken")}))))
    (anom/anom
      {:desc "Page token not present"})))

(defn lead-value-vec [{:keys [full-name
                              typical-check-dollars
                              investor-type]}]
  [full-name
   "1 - Lead"
   "Emailing to schedule meeting"
   ""
   (str "$" typical-check-dollars)
   investor-type
   ""
   ""])

(defn valid-leads? [_] true)

(defn <append-investor-rows [{:keys [spreadsheet-id]}
                             investor-leads]
  (when-not (valid-leads? investor-leads)
    (anom/throw-anom
      {:desc "Invalid investor leads"
       :investor-leads investor-leads}))
  (<values-append
    {:spreadsheetId ""
     :range "Investors!A1:H1"
     :valueInputOption "USER_ENTERED"
     :majorDimension "ROWS"
     :values (map lead-value-vec investor-leads)}))
                                                   

(comment

  (ks/<pp (<get-calendar
            {:calendarId "primary"}))

  (ks/<pp
    (<values-get
      {:spreadsheetId ""
       :range "Dashboard!A3:A9"}))

  (ks/<pp
    (<append-investor-rows
      {:spreadsheet-id ""}
      [{:full-name "ZK"
        :typical-check-dollars 50000}]))

  (ks/<pp)

  (ks/<pp
    (<events-for-calendar-day
      {:calendarId "primary"
       :tz-str "Pacific/Honolulu"
       :date-str "2021-04-08"}))

  )

(defn test-create-dummy-event [id]
  )


(comment

  (<do-sign-in)

  

  (test-stuff)
  
  (ks/<pp
    (<init-api
      {:client-id ""
       :discovery-docs ["https://sheets.googleapis.com/$discovery/rest?version=v4"]
       :scopes "https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile"}))

  )
