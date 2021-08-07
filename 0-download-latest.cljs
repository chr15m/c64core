(ns dl
  (:require
    ["node-fetch" :as fetch]
    ["fs" :as fs]
    ["mkdirp" :as mkdirp]))

(def fresh-pins-url "https://api.pinterest.com/v3/pidgets/boards/chrismgamedraw/retro-computing/pins/")

(mkdirp "data")

(print "Starting download.")

(-> (fetch fresh-pins-url)
    (.then #(.json %))
    (.then (fn [json]
      (fs/writeFileSync "data/latest.json" (js/JSON.stringify json nil 2))
      (print "Done."))))

