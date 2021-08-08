(ns dl
  (:require
    ["node-fetch" :as fetch]
    ["fs" :as fs]
    ["mkdirp" :as mkdirp]
    [common :refer [log env bail]]))

(def n "0-download-latest.cljs:")

(def board (or (env "PINTEREST_BOARD") (bail "PINTEREST_BOARD is not set.")))

(def fresh-pins-url (str "https://api.pinterest.com/v3/pidgets/boards/" board "/pins/"))

(mkdirp "data")

(log n "Starting download.")

(-> (fetch fresh-pins-url)
    (.then #(.json %))
    (.then (fn [json]
      (fs/writeFileSync "data/latest.json" (js/JSON.stringify json nil 2))
      (log n "Done."))))

