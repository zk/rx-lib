(ns rx.node.http)

(defn google-analytics-frag [ga-tracking-id]
  (let [src (str
              "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', '" ga-tracking-id "', 'auto');
  ga('send', 'pageview');")]
    {:head-js [[:script {:type "text/javascript"} src]]
     :csp [{:script-src ["https://www.google-analytics.com"]}]}))
