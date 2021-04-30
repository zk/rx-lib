(ns rx.browser.forms-styleguide
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.css :as css]
            [rx.browser.styleguide :as sg
             :refer-macros [example]]
            [rx.browser.forms :as forms]
            [rx.browser.components :as cmp]
            [rx.browser.buttons :as btn]
            [rx.browser.ui :as ui]
            [rx.validators :as vld]
            [rx.theme :as th]
            [clojure.string :as str]
            [reagent.core :as r]
            [clojure.core.async :as async
             :refer [go <! timeout]]))

(def headings
  [[::forms :h1 "Forms"]
   [::example :h2 "Example"]
   [::form-data :h2 "Form Data"]
   [::components :h2 "Components"]
   [::more-examples :h2 "More Examples"]])

(defn range-example []
  (let [!val (r/atom 0)]
    (r/create-class
      {:reagent-render
       (fn []
         (sg/example
           {:form [ui/group
                   {:gap 20}
                   [ui/group
                    {:horizontal? true
                     :gap 12}
                    [forms/range
                     {:!val !val
                      :full-width? false
                      :min 30
                      :max 70}]
                    [:div {:style {:width 50}}
                     [ui/text @!val]]]
                   [forms/form
                    [forms/group
                     {:gap 8}
                     [forms/label "Range Input"]
                     [forms/range
                      {:label "Range"
                       :!val !val
                       :full-width? true
                       :help-text "Range help text"
                       :error "Range error"
                       :min 0
                       :max 100}]
                     [forms/error-text {:text "Error message here"}]]]]}))})))

(defn search-bar-section []
  (let [!val (r/atom 0)]
    (r/create-class
      {:reagent-render
       (fn []
         [:div
          [:h3 {:id "forms-search-bar"} "Search Bar"]
          [sg/checkerboard
           [ui/group
            {:horizontal? true
             :gap 12}
            [forms/search-bar
             {:!val !val
              :full-width? false}]]]])})))

(defn login-form []
  [sg/section
   {:gap 8}
   [:h3 "Login Form"]
   (sg/example
     {:form
      [forms/form
       {:on-submit (fn [m form]
                     (go
                       (<! (timeout 500))
                       (forms/set-error form
                         :incorrect-login
                         "Whoops, incorrect login")))
        :gap 20
        :prop-validations
        {:email
         (fn [v]
           (when-not (vld/email? v)
             "Please enter a valid email address"))
         :password
         (fn [v] (when (empty? v)
                   "Required"))}}
       [forms/group
        {:gap 12}
        [:h3 "Log in"]
        [forms/text
         {:data-key :email
          :validate-on-blur? true
          :placeholder "Email Address"}]
        [forms/password
         {:data-key :password
          :placeholder "Password"
          :validate-on-blur? true}]
        [forms/error-text
         {:data-key :incorrect-login}]]
       [forms/submit-button
        {:label "Log In"}]]})])

(defn signup-form []
  [sg/section
   {:gap 8}
   [:h3 "Sign-Up Form"]
   (sg/example
     {:form
      [forms/form
       {:on-submit (fn [m form]
                     (go
                       (<! (timeout 500))
                       #_(js/alert (ks/pp-str m))))
        :gap 20
        :initial-data nil
        #_{:email "zk@heyzk.com"
                       :password "Foo123"}
        :prop-validations
        {:email
         (fn [v]
           (when-not (vld/email? v)
             "Please enter a valid email address"))
         :password
         (fn [v]
           (cond
             (empty? v) "Please enter a password"
             (or (not (>= (count v) 4))
                 (not (re-find #"[A-Z0-9]" v)))
             "Dosen't meet password requirements"))}}
       [forms/section
        {:title "Sign Up"
         :gap 20}
        
        [forms/with-form-state
         (fn [{:keys [form-state style]}]
           (let [submit-errors (forms/submit-errors form-state)]
             (when (not (empty? submit-errors))
               [:div
                {:style (merge
                          {:border "solid red 1px"
                           :padding 12}
                          style)}
                "Please fix "
                (count submit-errors)
                " "
                (ks/pluralize (count submit-errors) "error" "errors")])))]
        [forms/group
         {:data-key :email
          :gap 10}
         [forms/label {:text "Email"}]
         [forms/text
          {:validate-on-blur? true}]
         [forms/error-text]]
        [forms/group
         {:data-key :password
          :gap 10}
         [forms/label {:text "Password"}]
         [forms/password
          {:data-key :password
           :validate-on-blur? true}]
         [forms/error-text]
         [forms/with-form-state
          (fn [{:keys [form-state]}]
            (let [{:keys [password]} (forms/form-data form-state)]
              [forms/group
               {:gap 0}
               [:div "Password Requirements"]
               [:div
                "More than 4 characters"
                (when (>= (count password) 4)
                  " - ✔️")]
               [:div
                "1 upper character"
                (when (re-find #"[A-Z]" (or password ""))
                  " - ✔️")]
               [:div
                "1 number"
                (when (re-find #"[0-9]" (or password ""))
                  " - ✔️")]]))]]]
       [forms/submit-button
        {:label "Sign Up"}]]})])

(defn sections []
  [sg/section-container
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1875/printing-form-768.jpg"
             :width 300}}
    [sg/heading headings ::forms]
    [:p "Ugh, forms. Why are they such a PITA? There are a few parts to get right:"]
    [:ul
     {:style {:list-style-type 'disc
              :padding-left 20}}
     [:li "UI -- sections, input controls, etc"]
     [:li "Validations -- local and remote validations on inputs or collections of inputs and the form as a whole"]
     [:li "State -- tracking values provided by the user, and validation errors at the form, section, and input levels"]]
    [:p [:code "rx.browser.forms"] " is an attempt to provide a batteries-included way to support easily building forms."]
    [:p "Providing value to the user often requires getting input from that user. Forms help you accomplish this in your programs via their layout, their controls, and their expository content."]
    [:p "Good forms provide certainty about what the input wil be used for, and what will happen after the form is complete."]
    [:h3 "Namespace"]
    [:code "rx.browser.forms"]]
   [sg/section
    [sg/heading headings ::example]
    [:p "Here's a simple sign-up form that has a single input field -- email -- which is locally validated for correctness, and remotely validated for uniqueness."]
    (sg/example
      {:form
       [forms/form
        {:on-submit (fn [m form]
                      (go
                        (<! (timeout 500))
                        (forms/set-error form
                          :email-already-taken? true)))
         :gap 20
         :prop-validations
         {:email
          #_(fn [v]
              (go
                (<! (timeout 500))
                (when-not (vld/email? v)
                  "Please enter a valid email address")))
          (fn [v]
            (when-not (vld/email? v)
              "Please enter a valid email address"))}}
        [forms/group
         {:gap 16}
         [:h3 "Sign up here"]
         #_[ui/text {:scale "title"} "Sign up here"]
         [forms/group
          {:data-key :email
           :gap 8}
          [forms/label {:text "Email"}]
          [forms/text
           {:validate-on-blur? true}]
          [forms/error-text]]
         [forms/error-group
          {:data-key :email-already-taken?}
          "That email is already taken. Would you like to "
          [:a {:href "/login"} "log in"]
          " instead?"]]
        [forms/group
         {:horizontal? true
          :justify-content 'space-between}
         [forms/btn-bare
          {:label "Cancel"}]
         [forms/submit-button
          {:label "Submit"}]]]})]

   [sg/section
    [sg/heading headings ::form-data]
    [:p "Form data is the result of your users filling out forms. The form component will recursively look through it's children for form elements with a `:data-key` property set and collect any changes to those elements for you automatically."]
    (sg/example
      {:form
       [ui/group
        {:gap 20}
        [forms/form
         {:gap 8}
         [:div
          [forms/group
           {:gap 8
            :data-key :email
            :justify-content 'flex-start}
           [forms/label "Email"]
           [forms/text]]]
         [:div
          [forms/group
           {:gap 8
            :data-key :level-of-interest}
           [forms/label "Level of Interest"]
           [forms/range]]]
         [forms/group
          {:gap 8
           :data-key :newsletter?}
          [forms/checkbox {:label "Receive newsletter"}]]
         [forms/with-form-state
          (fn [{:keys [form-state]}]
            [:div
             [forms/label "Form Data"]
             [:pre (ks/pp-str (forms/form-data form-state))]])]]]
       #_[ui/group
          {:gap 20}
          [forms/form
           {:gap 8}
           [forms/group
            {:gap 8
             :data-key :email}
            [forms/label "Email"]
            [forms/text]]
           [forms/group
            {:gap 8
             :data-key :level-of-interest}
            [forms/label "Level of Interest"]
            [forms/range]]
           [forms/group
            {:gap 8
             :data-key :newsletter?}
            [forms/checkbox {:label "Receive newsletter"}]]
           [forms/with-form-state
            (fn [{:keys [form-state]}]
              [:div
               [forms/label "Form Data"]
               [:pre (ks/pp-str (forms/form-data form-state))]])]]]})]

   [sg/section
    {:gap 32}
    [sg/heading headings ::components]
    [sg/component-doc
     {:var #'forms/form}]
    [sg/component-doc
     {:var #'forms/group}
     (sg/example
       {:form [forms/group
               {:gap 8
                :data-key :email}
               [forms/label "Email"]
               [forms/text]]})]
    [sg/component-doc
     {:var #'forms/label}
     (sg/example
       {:form [forms/label "Hello Label"]})]

    [sg/component-doc
     {:var #'forms/textarea}
     (sg/example
       {:form [forms/textarea
               {:placeholder "Placeholder text"
                :on-change-text prn}]})]

    [sg/component-doc
     {:var #'forms/text}
     (sg/example
       {:form [forms/text
               {:placeholder "Placeholder text"
                :on-change-text prn}]})]

    [sg/component-doc
     {:var #'forms/password}
     (sg/example
       {:form [forms/password
               {:placeholder "Placeholder text"
                :on-change-text prn}]})]

    [sg/component-doc
     {:var #'forms/range}
     [range-example]]

    [sg/component-doc
     {:var #'forms/checkbox}
     (sg/example
       {:form [forms/checkbox
               {:label "Hello World"}]})]

    [sg/component-doc
     {:var #'forms/select}
     (sg/example
       {:form [forms/select
               {:options [{:label "Disabled"
                           :disabled? true}
                          {:value "string"
                           :label "string"}
                          {:value :keyword
                           :label "keyword"}
                          {:value {:baz "yes!"}
                           :label "map"}
                          {:value [1 2 3]
                           :label "vec"}
                          {:value true
                           :label "bool"}]}]})]]

   [sg/section
    [sg/heading headings ::more-examples]
    [login-form]
    [signup-form]]])

(comment

  (browser/<set-root!
    [sg/standalone
     {:component sections
      :headings headings}])
 
  (browser/<set-root!
    [sg/standalone
     {:component range-section}])

  (browser/<set-root!
    [sg/standalone
     {:component search-bar-section}])

  (browser/<set-root!
    [sg/standalone
     {:component basic-form}])

  (browser/<set-root!
    [sg/standalone
     {:component login-form}])

  (browser/<set-root!
    [sg/standalone
     {:component signup-form}])

  )

;; Layout and nesting, you're required to use the 

;; Validations are just functions, but there are a few helpers to make composition easier.
