(ns dl
  (:require
    ["node-fetch" :as fetch]
    ["fs" :as fs]
    ["mkdirp" :as mkdirp]
    [common :refer [log]]))

(def n "0-download-latest.cljs:")

(def fresh-pins-url "https://api.pinterest.com/v3/pidgets/boards/chrismgamedraw/retro-computing/pins/")

(mkdirp "data")

(log n "Starting download.")

(-> (fetch fresh-pins-url)
    (.then #(.json %))
    (.then (fn [json]
      (fs/writeFileSync "data/latest.json" (js/JSON.stringify json nil 2))
      (log n "Done."))))

