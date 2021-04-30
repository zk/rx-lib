(ns rx.browser.frame
  (:require [rx.kitchen-sink :as ks]
            [rx.browser :as browser]
            [rx.browser.ui :as ui]
            [rx.browser.styleguide :as sg]
            [rx.browser.buttons :as btn]
            [clojure.string :as str]
            [reagent.core :as r]))

(defn create-frame-state []
  {:!left-transition (r/atom :open)
   :!left-ref (atom nil)})

(defn frame-render-header [{:keys [header render-header debug?] :as opts}]
  (when (or header render-header)
    [:div
     {:style (merge
               (when debug?
                 {:background-color "red"}))}
     (cond
       header header
       render-header [render-header opts])]))

(defn frame-render-footer [{:keys [footer render-footer debug?] :as opts}]
  (when (or footer render-footer)
    [:div
     {:style (merge
               (when debug?
                 {:background-color "green"}))}
     (cond
       footer footer
       render-footer [render-footer opts])]))

(defn frame-render-left [{:keys [left render-left debug?] :as opts}]
  (when (or left render-left)
    [:div
     {:style (merge
               (when debug?
                 {:background-color "blue"})
               {:overflow-y 'scroll})}
     (cond
       left left
       render-left [render-left opts])]))

(defn frame-render-right [{:keys [right render-right debug?] :as opts}]
  (when (or right render-right)
    [:div
     {:style (merge
               (when debug?
                 {:background-color "magenta"}))}
     (cond
       right right
       render-right [render-right opts])]))

(defn frame-render-content [{:keys [content render-content debug?] :as opts}]
  (when (or content render-content)
    [:div
     {:style (merge
               {:flex 1
                :overflow-y 'scroll}
               (when debug?
                 {:background-color "orange"}))}
     (cond
       content content
       render-content render-content)]))

(defn frame []
  (let [{:keys [!left-transition
                !left-ref]} (create-frame-state)
        on-left-ref (fn [ref] (reset! !left-ref ref))]
    (r/create-class
      (-> {:on-window-resize
           (fn [{:keys [::browser/width
                        ::browser/height]}])
           :reagent-render
           (fn [{:keys [max-width] :as opts}]
             [ui/group
              {:style (merge
                        {:flex 1
                         :overflow 'hidden}
                        (:style opts))}
              (frame-render-header opts)
              [ui/group
               {:horizontal? true
                :flex 1
                :style {:overflow 'hidden}}
               (frame-render-left opts)
               [:div {:style {:flex 1
                              :display 'flex}}
                (frame-render-content opts)]
               (frame-render-right opts)]
              (frame-render-footer opts)])}
          browser/bind-window-resize))))

(def headings
  [[::frame :h1 "Frame"]
   [::example :h2 "Example"]
   [::responsive :h2 "Responsive"]
   [::content-overflow :h2 "Content Overflow"]])

(defn sections []
  [ui/group
   {:gap 40}
   [sg/section-intro
    {:image {:url "https://www.oldbookillustrations.com/wp-content/high-res/1875/timber-framed-bartizan-768.jpg"
             :width 200}}
    [sg/heading headings ::frame]
    [:p "Provides a top level container for your app that supports collapsable edge compoennts (header, sidebar, etc). Mobile aware."]
    [:p "Import " [:code "rx.browser.frame"]]
    [:p "Frame is intendted to be used close to the root of the dom, but can be embedded in other components as well. Wrapping component for frame must be " [:code "display: flex"]]]
   [sg/section
    [sg/heading headings ::example]
    (sg/example
      {:form
       [frame
        {:debug? debug?
         :style {:width "100%"
                 :height 500}
         :header [ui/group {:pad 8} "Header"]
         :content [ui/group {:pad 8} "Content"]
         :left [ui/group {:pad 8} "Left"]
         :right [ui/group {:pad 8} "Right"]
         :footer [ui/group {:pad 8} "Footer"]}]
       :options
       [[:debug? :boolean]]
       :initial {:debug? true}})]
   #_[sg/section
      [sg/heading headings ::responsive]
      [:p "Frame can be configured as responsive, where excess space is distributed to the outer edges of the left and right sidebars keeping content centered in the window."]
      (sg/example
        {:form
         [frame
          {:debug? debug?
           :style {:width "100%"
                   :height 500}
           :responsive? true
           :max-width 300
           :header [ui/group {:pad 8} "Header"]
           :content [ui/group {:pad 8} "Content"]
           :left [ui/group {:pad 8} "Left"]
           :right [ui/group {:pad 8} "Right"]
           :footer [ui/group {:pad 8} "Footer"]}]
         :options
         [[:debug? :boolean]]
         :initial {:debug? true}})
      [:p "Another common use case is where you'd like a full-width header, and extra space to be contained within the left and right sidebars."]]
   [sg/section
    [sg/heading headings ::content-overflow]
    [:p "Left, center, and right content has " [:code "overflow-y: scroll"] " set."]
    (sg/example
      {:form
       [frame
        {:debug? debug?
         :style {:width "100%"
                 :height 500}
         :responsive? true
         :header [ui/group {:pad 8} "Header"]
         :content [ui/group {:pad 12
                             :gap 12}
                   (->> (str/split
                          "progris riport 1 martch 3

Dr Strauss says I shoud rite down what I think and remembir and evrey thing that happins to me from now on. I dont no why but he says its importint so they will see if they can use me. I hope they use me becaus Miss Kinnian says mabye they can make me smart. I want to be smart. My name is Charlie Gordon I werk in Donners bakery where Mr Donner gives me 11 dollers a week and bred or cake if I want. I am 32 yeres old and next munth is my brithday. I tolld dr Strauss and perfesser Nemur I cant rite good but he says it dont matter he says I shud rite just like I talk and like I rite compushishens in Miss Kinnians class at the beekmin collidge center for retarted adults where I go to lern 3 times a week on my time off. Dr. Strauss says to rite a lot evrything I think and evrything that happins to me but I cant think anymor because I have nothing to rite so I will close for today...yrs truly Charlie Gordon.

progris riport 2-martch 4

I had a test today. I think I faled it and I think mabye now they wont use me. What happind is I went to Prof Nemurs office on my lunch time like they said and his secertery took me to a place that said psych dept on the door with a long hall and alot of littel rooms with onley a desk and chares. And a nice man was in one of the rooms and he had some wite cards with ink spilld all over them. He sed sit down Charlie and make yourself cunfortible and rilax. He had a wite coat like a docter but I dont think he was no docter because he dint tell me to opin my mouth and say ah. All he had was those wite cards. His name is Burt. I fergot his last name because I dont remembir so good.

I dint know what he was gonna do and I was holding on tite to the chair like sometimes when I go to a dentist onley Burt aint no dentist neither but he kept telling me to rilax and that gets me skared because it always means its gonna hert.

So Burt sed Charlie what do you see on this card. I saw the spilld ink and I was very skared even tho I got my rabits foot in my pockit because when I was a kid I always faled tests in school and I spilld ink to.

I tolld Burt I saw ink spilld on a wite card. Burt said yes and he smild and that maid me feel good. He kept terning all the cards and I tolld him somebody spilld ink on all of them red and black. I thot that was a easy test but when I got up to go Burt stoppd me and said now sit down Charlie we are not thru yet. Theres more we got to do with these cards. I dint understand about it but I remembir Dr Strauss said do anything the testor telld me even if it dont make no sense because thats testing.

I dont remembir so good what Burt said but I remembir he wantid me to say what was in the ink. I dint see nothing in the ink but Burt sed there was picturs there. I coudnt see no picturs. I reely tryed to see. I holded the card up close and then far away. Then I said if I had my eye glassis I coud probaly see better I usully only ware my eyeglassis in the movies or to watch TV but I sed maybe they will help me see the picturs in the ink. I put them on and I said now let me see the card agan I bet I find it now.

I tryed hard but I still coudnt find the picturs I only saw the ink. I tolld Burt mabey I need new glassis. He rote somthing down on a paper and I got skared of faling the test. So I tolld him it was a very nice pictur of ink with pritty points all around the eges but he shaked his head so that wasnt it neither. I asked him if other pepul saw things in the ink and he sed yes they imagen picturs in the inkblot. He tolld me the ink on the card was calld inkblot.

Burt is very nice and he talks slow like Miss Kinnian dose in her class where I go to lern reeding for slow adults. He explaned me it was a raw shok test. He sed pepul see things in the ink. I said show me where. He dint show me he just kept saying think imagen theres something on the card. I tolld him I imaggen a inkblot. He shaked his head so that wasnt rite eather. He said what does it remind you of pretend its something. I closd my eyes for a long time to pretend and then I said I pretend a bottel of ink spilld all over a wite card. And thats when the point on his pencel broke and then we got up and went out.

I dont think I passd the raw shok test."
                          #"\n+")
                        (map (fn [s]
                               [:p s])))]
         :left [ui/group {:pad 8
                          :style {:width 100}}
                (->> (range 100)
                     (map (fn [i]
                            [btn/bare
                             {:label (str "Item " i)
                              :style {:margin-bottom 10}}])))]
         :right [ui/group {:pad 8} "Right"]
         :footer [ui/group {:pad 8} "Footer"]}]
       :options
       [[:debug? :boolean]]
       :initial {:debug? true}})]])

(defn test-frame []
  [frame
   {:debug? true
    :header
    [:div "Header"]

    :left
    [:div "Left"]

    :right
    [:div "Right"]
    
    :content
    [:div "Center"]
    
    :footer
    [:div "Footer"]}])


(comment

  (browser/<show-component!
    [test-frame])

  (browser/<set-root!
    [sg/standalone {:component sections
                    :headings headings}])

  )
