(ns rx.browser.gdrive
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

(<defn <perm-create [opts]
  (let [resp (<? (ks/<promise
                   (.create
                     (.. js/gapi -client -drive -permissions)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
        {:desc "Error"
         ::resp resp}))))

(<defn <perm-list [opts]
  (let [resp (<? (ks/<promise
                   (.list
                     (.. js/gapi -client -drive -permissions)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
        {:desc "Error"
         ::resp resp}))))

(<defn <file-get [opts]
  (let [resp (<? (ks/<promise
                   (.get
                     (.. js/gapi -client -drive -files)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
        {:desc "Error"
         ::resp resp}))))

#_(<defn <next [resp]
  (if-let [page-token (get resp "page-token")]
    (let [{:keys [::call-opts ::call-type]} resp
          f (condp = call-type
              ::events-list
              <list-events)]
      (<? (f (merge call-opts
               {:nextPageToken (get resp "nextPageToken")}))))
    (anom/anom
      {:desc "Page token not present"})))


(comment

  (ks/<pp (<get-calendar
            {:calendarId "primary"}))

  (ks/<pp
    (<perm-list
      {:fileId ""
       :supportsAllDrives true}))

  (ks/<pp
    (<perm-create
      {:fileId ""
       :supportsAllDrives true
       :resource {:role "writer"
                  :type "user"
                  :emailAddress "welcomecapital@investor-crm.iam.gserviceaccount.com"}}))

  (ks/<pp
    (<file-get
      {:fileId ""
       :supportsAllDrives true}))

  )

(defn test-create-dummy-event [id]
  )


(comment

  (<do-sign-in)

  

  (test-stuff)
  
  (ks/<pp
    (<init-api
      {:api-key ""
       :client-id ""
       :discovery-docs ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"]
       :scopes "https://www.googleapis.com/auth/drive.file"}))

  )
