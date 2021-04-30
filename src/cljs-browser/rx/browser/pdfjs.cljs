(ns rx.browser.pdfjs
  (:require [rx.anom :as anom
             :refer-macros [<defn <? gol]]
            [rx.browser :as browser]
            [rx.browser.pdfjs-core :as pc]
            [rx.kitchen-sink :as ks]
            [pdfjs]))

(pc/init-pdfjs
  js/pdfjsLib
  {:worker-src "https://unpkg.com/pdfjs-dist@2.6.347/es5/build/pdf.worker.js"})

(def document pc/document)
(def viewer pc/viewer)
(def <pdf-obj pc/<pdf-obj)

(comment

  (pc/init-pdfjs
    js/pdfjsLib
    {:worker-src "https://unpkg.com/pdfjs-dist@2.6.347/es5/build/pdf.worker.js"})

  (gol
    (ks/spy
      (<? (pc/<pdf-obj
            (or
              #_"http://cs.williams.edu/~freund/cs434/gal-trace.pdf"
              #_"https://github.com/papers-we-love/papers-we-love/raw/master/design/out-of-the-tar-pit.pdf"
              "https://arxiv.org/ftp/arxiv/papers/1809/1809.07858.pdf"))))

    )

  )
