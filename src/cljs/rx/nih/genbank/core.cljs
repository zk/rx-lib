(ns rx.nih.genbank.core
  (:require [rx.kitchen-sink :as ks
                     :refer [slurp-cljs]]
            [clojure.string :as str]
            [clj-http.client :as hc]))

(defn download-report-url [id report-type]
  (str "https://www.ncbi.nlm.nih.gov/sviewer/viewer.cgi?tool=portal&save=file&log$=seqview&db=nuccore&report="
       report-type
       "&id="
       id
       "&conwithfeat=on&hide-cdd=on"))





(defn download-genome-data [id]
  (let [gb-full-resp
        (hc/get
          (download-report-url
            id
            "gbwithparts"))
        fasta-resp
        (hc/get
          (download-report-url
            id
            "fasta"
            #_"gbwithparts"))]
    {:fasta-text (:body fasta-resp)
     :gb-text (:body gb-full-resp)}))

(defn parse-raw-sections [genbank-text]
  (let [lines (str/split genbank-text #"\n")]
    (loop [rem-lines lines
           working-section nil
           sections []]
      (if (empty? rem-lines)
        sections
        (let [next-line (first rem-lines)
              new-section? (not (str/starts-with? next-line " "))
              next-working-section (if new-section?
                                     {:section-name (->> next-line
                                                         (take-while #(not= % \space))
                                                         (apply str))
                                      :section-lines [next-line]}
                                     (update
                                       working-section
                                       :section-lines
                                       conj
                                       next-line))
              next-sections (if (and new-section? working-section)
                              (conj sections working-section)
                              sections)]
          (recur
            (rest rem-lines)
            next-working-section
            next-sections))))))

(defn parse-locus-name [s]
  (->> s
       (drop-while #(not= % \space))
       (drop-while #(= % \space))
       (take-while #(not= % \space))
       (apply str)))

(defn parse-sequence-length [s]
  (->> s
       (drop-while #(not= % \space))
       (drop-while #(= % \space))
       (drop-while #(not= % \space))
       (drop-while #(= % \space))
       (apply str)
       (re-find #"^(\d+)")
       first
       ks/parse-long))

(defn parse-modification-date [s]
  (->> s
       (re-find #"(\d{2}-[A-Z]{3}-\d{4})")
       first))

(defn parse-genbank-division [s]
  (->> ["PRI" "ROD" "MAM" "VRT" "INV" "PLN"
        "BCT" "VRL" "PHG" "SYN" "UNA" "EST"
        "PAT" "STS" "GSS" "HTG" "HTC" "ENV"]
       (filter #(str/includes? s %))
       first))

(defn section->locus [lines]
  (let [s (->> (apply str lines)
               str/trim)]
    {:type :locus
     :locus-name (parse-locus-name s)
     :sequence-length-bp (parse-sequence-length s)
     :modification-date (parse-modification-date s)
     :genbank-division (parse-genbank-division s)
     :raw-lines lines}))

(defn section->text [name lines]
  {:type (-> name
             str/lower-case
             keyword)
   :raw-lines lines})

(defn section->source [lines]
  (let [has-organism? (->> lines
                           (map str/trim)
                           (some #(str/starts-with?
                                    (str/lower-case %)
                                    "organism")))
        info (->> lines
                  (map str/trim)
                  (take-while #(not (str/starts-with? (str/lower-case %) "organism")))
                  (apply str)
                  str/trim)]
    {:type :source
     :raw-lines lines}))

(defn collect-sections [raw-sections]
  (->> raw-sections
       (map (fn [{:keys [section-name section-lines]}]
              (condp = section-name
                "LOCUS" (section->locus section-lines)
                "DEFINITION" (section->text section-name section-lines)
                "ACCESSION" (section->text section-name section-lines)
                "VERSION" (section->text section-name section-lines)
                "KEYWORDS" (section->text section-name section-lines)
                "SOURCE" (section->source section-lines)
                "REFERENCE" (section->text section-name section-lines)
                "COMMENT" (section->text section-name section-lines)
                nil)))
       (remove nil?)))

(defn request-sars-cov-2-locuses []
  (hc/post
    "https://www.ncbi.nlm.nih.gov/genomes/VirusVariation/vvsearch2/"
    {:form-params
     {:wt "json"
      :indent "true"
      :q "*:*"
      :sort "CreateDate_dt asc"
      :facet "on"
      :facet.limit "500"
      :facet.sort "index"
      :fl "id,*_s,*_ss,*_sw,*_txt,*_i,*_csv,*_dt,*_dr"
      :sord "asc"
      :start "0"
      :rows "200"
      :fq ["{!tag=SeqType_s}SeqType_s:(\"Nucleotide\")"
           "VirusLineageId_ss:(2697049)"]}}))

(defn parse-locuses-response [resp]
  (->> resp
       :body
       ks/from-json
       :response))

(defn genbank-gi-id [gi-doc]
  (:gi_i gi-doc))

(defn parse-doc [doc]
  {:id (:id doc)
   :genbank-gi-id (genbank-gi-id doc)
   :doc doc})

(defn fetch-genbank-gi-id [id]
  (->> (hc/get (str "https://www.ncbi.nlm.nih.gov/nuccore/" id))
       :body
       (re-find #"<meta name=\"ncbi_uidlist\" content=\"(\d+)\" />")
       second))

(comment

  (->> (request-sars-cov-2-locuses)
       parse-locuses-response
       :docs
       (map parse-doc)
       (map #(merge
               %
               (when-not (:genbank-gi-id %)
                 {:genbank-gi-id (fetch-genbank-gi-id (:id %))})))
       (map #(merge
               %
               (when (:genbank-gi-id %)
                 (download-genome-data (:genbank-gi-id %)))))
       ks/to-transit
       (spit "resources/covid-19/genome_data.transit.json")
       time)

  )

(defn do-the-thing []
  (let [genbank-text (slurp-cljs "resources/covid-19/sequence.gb")
        raw-sections (parse-raw-sections genbank-text)
        sections (collect-sections raw-sections)]
    sections))


(comment

  (ks/pp (do-the-thing))

  (ks/pp
    (hc/get
      (str "https://www.ncbi.nlm.nih.gov/sviewer/viewer.cgi?tool=portal&save=file&log$=seqview&db=nuccore&report="
           #_"fasta"
           "gbwithparts"
           "&id=1806649153&conwithfeat=on&hide-cdd=on")))

  (ks/pp (download-genome-data "1798174254"))

  (spit
    "resources/covid-19/tmp.txt"
    (:body
     (hc/post
       "https://www.ncbi.nlm.nih.gov/genomes/VirusVariation/vvsearch2/"
       {:form-params
        {:wt "json"
         :indent "true"
         :q "*:*"
         :sort "CreateDate_dt asc"
         :facet "on"
         :facet.limit "500"
         :facet.sort "index"
         :fl "id,*_s,*_ss,*_sw,*_txt,*_i,*_csv,*_dt,*_dr"
         :sord "asc"
         :start "0"
         :rows "200"
         :fq ["{!tag=SeqType_s}SeqType_s:(\"Nucleotide\")"
              "VirusLineageId_ss:(2697049)"]}})))


  (->> (slurp "resources/covid-19/tmp.txt")
       ks/from-json
       :response
       :docs
       )

  
  )

