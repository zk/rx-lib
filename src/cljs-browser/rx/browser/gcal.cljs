(ns rx.browser.gcal
  (:require [rx.kitchen-sink :as ks]
            [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [clojure.core.async :as async
             :refer [go <! chan close! put!]]))

(defn <init-gcal-api [{:keys [api-key
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

(<defn <update-event [opts]
  (let [resp (<? (ks/<promise
                   (.update
                     (.. js/gapi -client -calendar -events)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error calling delete event"
           ::resp resp}))))

(<defn <patch-event [opts]
  (let [resp (<? (ks/<promise
                   (.patch
                     (.. js/gapi -client -calendar -events)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error calling patch event"
           ::resp resp}))))

(<defn <insert-event [opts]
  (let [resp (<? (ks/<promise
                   (.insert
                     (.. js/gapi -client -calendar -events)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error calling insert event"
           ::resp resp}))))


(<defn <get-calendar [opts]
  (let [resp (<? (ks/<promise
                   (.get
                     (.. js/gapi -client -calendar -calendars)
                     (clj->js opts))))]
    (if-let [res (.-result resp)]
      (js->clj res)
      (anom/anom
          {:desc "Error calling insert event"
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
                                                   
(defn tz-offset-now [tz-str]
  (let [date (js/Date.)
        utc-date (js/Date. (.toLocaleString date "en-US" (clj->js {:timeZone "UTC"})))
        tz-date (js/Date. (.toLocaleString date "en-US" (clj->js {:timeZone tz-str})))]
    (- (/
         (- (.getTime utc-date) (.getTime tz-date))
         (* 1000 60 60)))))

(defn tz-offset-str-now [tz-str]
  (let [offset (tz-offset-now tz-str)
        pad? (< (ks/abs offset) 10)
        pos? (>= offset 0)
        offset-str (str
                     (if pos?
                       "+"
                       "-")
                     (if pad? "0")
                     (* (ks/abs offset) 100))
        offset-str (str (apply str (take 3 offset-str))
                        ":"
                        (apply str (drop 3 offset-str)))]
    offset-str))

(defn date-to-iso8601 [date-str tz-str]
  (str date-str "T00:00:00" (tz-offset-str-now tz-str)))

(defn <events-for-calendar-day [{:keys [calendarId
                                        tz-str
                                        date-str]}]
  (go
    (let [res (<! (<list-events
                    (ks/spy
                      {:calendarId calendarId
                       :showDeleted false
                       :singleEvents true
                       :timeMin (str date-str "T00:00:00" (tz-offset-str-now tz-str))
                       :timeMax (str date-str "T23:59:59" (tz-offset-str-now tz-str))})))]
      (if (anom/? res)
        res
        (get res "items")))))

(defn <events-for-calendar-days [{:keys [calendarId
                                         tz-str
                                         days-count
                                         date-str]}]
  (go
    (let [max-date-str (let [d (js/Date. date-str)]
                         (.setDate d (+ (.getDate d) days-count))
                         (ks/date-format
                           (.getTime d)
                           "yyyy-MM-dd"))
          res (<! (<list-events
                    (ks/spy
                      {:calendarId calendarId
                       :showDeleted false
                       :singleEvents true
                       :maxResults 2000
                       :timeMin (str date-str "T00:00:00" (tz-offset-str-now tz-str))
                       :timeMax (str max-date-str "T23:59:59" (tz-offset-str-now tz-str))})))]
      (if (anom/? res)
        res
        (get res "items")))))

(comment

  (ks/<pp (<get-calendar
            {:calendarId "primary"}))

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

  (go
    (ks/pp (<! (<next (<! (<list-events
                            {:calendarId "primary"
                             :timeMin (.toISOString (js/Date.))
                             :maxResults 1}))))))

  (go
    (ks/pp (get
             (<! (<list-events
                      {:calendarId "primary"
                       :timeMin (.toISOString (js/Date.))
                       }))
             "items")))

  (go
    (ks/pp (count (<! (<events-for-calendar-days
                        {:calendarId "primary"
                         :days-count 7
                         :date-str "2021-04-08"
                         :tz-str "Pacific/Honolulu"})))))

  (->> (get events "items")
       (map (fn [item]
              (keys item))))

  (go
    (def event (<! (<insert-event
                     {:calendarId "primary"
                      :sendUpdates "none"
                      :resource {:start {:dateTime (js/Date. (+ (ks/now) (* 1000 60 60 10)))}
                                 :end {:dateTime (js/Date. (+ (ks/now) (* 1000 60 60 10)))}
                                 :attendees [{:email "zachary.kim@gmail.com"}]
                                 :summary "Freeday Test Event"}}))))

  


  (ks/<pp (<patch-event
            {:calendarId "primary"
             :eventId (get event "id")
             :resource
             {:description "Testing html desc:<br/>\n\nThis event cancelled by Freeday. <a href=\"https://freeday.zk.dev\">Get your free day too, for free</a>."}}))

  (ks/<pp (<delete-event
            {:calendarId "primary"
             :eventId (get event "id")
             :sendUpdates "all"}))

  (prn "e" event)

  (prn events)

  (ks/<pp (<delete-event
            {:calendarId "primary"
             :eventId "mb8sldcblvik3r9k0j4loedk1o"}))

  (<do-sign-in)

  )

#_(is-signed-in-listen
  (fn [& args]
    (prn args)
    (.log js/console (first args))))

(comment

  (test-stuff)

  (go
    (time
      (ks/pp
        )))


  (ks/<pp (<init-gcal-api
            {:api-key ""
             :client-id ""
             :discovery-docs ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"]
             :scopes "https://www.googleapis.com/auth/calendar.events"}))

  )
