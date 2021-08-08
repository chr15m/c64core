(ns robot.core
  (:require
    ["fs" :as fs]
    ["crypto" :as crypto]
    [common :refer [log kv plet get-pin-image]]))

(def n "1-store-pins.cljs:")

(defn hash-str [s]
  (-> (crypto/createHash "sha256") (.update s) (.digest "hex")))

(defn read-pins-files [dir f]
  (try
    (-> (str "data/" f)
        (fs/readFileSync)
        (js/JSON.parse)
        (aget "data")
        (aget "pins"))
    (catch :default e #js [])))

(defn fetch-fresh-json-data [])

(defn <p-add-pin-to-database [db pin]
  (when-let [url (get-pin-image pin)]
    (let [pin-hash (hash-str url)]
      (->
        (.get db pin-hash)
        (.then (fn [data]
                 (when (nil? data)
                   (log n "adding pin:")
                   (log n "\tsrc: " (aget pin "link"))
                   (log n "\tembed: " url)
                   (log n "\thash: " pin-hash)
                   (.set db pin-hash #js {:pin pin :added (js/Date.)}))))))))

(defn update-db-from-data-dir []
  (let [db (kv "pins")
        files (fs/readdirSync "data")
        files (filter #(.endsWith % ".json") files)]
    (.all js/Promise
          (to-array
            (map
              (fn [f]
                (log n "file =" f)
                (let [pins (read-pins-files "data" f)
                      promises (map #(<p-add-pin-to-database db %) pins)]
                  (log n "Pin count =" (aget pins "length"))
                  (.all js/Promise (to-array promises))))
              files)))))

(defn main! []
  (log n "Updating db from json files.")
  (plet [result (update-db-from-data-dir)]
        (log n "Done.")))

(main!)
