{:foreign-libs
 [{:file "resources/auth0/auth0-spa-js.production.js"
   :file-min "resources/auth0/auth0-spa-js.production.js"
   :provides ["auth0-spa-js"]}
  {:file "resources/auth0/auth0.min.js"
   :file-min "resources/auth0/auth0.min.js"
   :provides ["auth0"]}
  {:file "resources/amplitude/amplitude-8.1.0-min.gz.js"
   :file-min "resources/amplitude/amplitude-8.1.0-min.gz.js"
   :provides ["amplitude"]}
  {:file "resources/aws/aws-sdk-2.903.0.min.js"
   :file-min "resources/aws/aws-sdk-2.903.0.min.js"
   :provides ["aws"]}
  {:file "resources/sentry/bundle.min.js"
   :file-min "resources/sentry/bundle.min.js"
   :provides ["sentry"]}
  {:file "resources/sentry/bundle.tracing.min.js"
   :file-min "resources/sentry/bundle.tracing.min.js"
   :provides ["sentry-tracing"]}]
 :externs ["resources/auth0/auth0.min.js"
           "resources/amplitude/amplitude-8.1.0-min.gz.js"
           "resources/sentry/bundle.min.js"
           "resources/sentry/bundle.tracing.min.js"]}
