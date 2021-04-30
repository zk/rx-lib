(ns rx.browser.loaders)


(defn circle [{:keys [size color period disabled?]}]
  [:svg {:version "1.1"
         :xmlns "http://www.w3.org/2000/svg"
         "xmlns:xlink" "http://www.w3.org/1999/xlink"
         :width size
         :height size
         :view-box "0 0 50 50"
         :style {:enable-background "new 0 0 50 50"}
         "xml:space" "preserve"}
   (when-not disabled?
     [:path {:fill color
             :d "M43.935,25.145c0-10.318-8.364-18.683-18.683-18.683c-10.318,0-18.683,8.365-18.683,18.683h4.068c0-8.071,6.543-14.615,14.615-14.615c8.072,0,14.615,6.543,14.615,14.615H43.935z"}])
   (when-not disabled?
     [:animateTransform
      {:attributeType "xml"
       :attributeName "transform"
       :type "rotate"
       :from "0 0 0"
       :to "360 0 0"
       :dur period
       :repeatCount "indefinite"}])])
