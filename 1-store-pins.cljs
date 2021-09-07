(ns robot.core
  (:require
    ["crypto$default" :as crypto]
    ["fs" :as fs]
    [common :refer [log kv client plet get-pin-image]]
    [nbb.core :refer [*file*]]))

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

(defn <p-add-pin-to-database [db db-posted pin]
  (when-let [url (get-pin-image pin)]
    (let [pin-hash (hash-str url)
          in-db (.get db pin-hash)
          in-posted (.get db-posted pin-hash)]
      (->
        (.all js/Promise (to-array [in-db in-posted]))
        (.then (fn [[in-db-result in-posted-result]]
                 (when (and (nil? in-db-result) (nil? in-posted-result))
                   (log *file* "adding pin:")
                   (log *file* "\tsrc: " (aget pin "link"))
                   (log *file* "\tembed: " url)
                   (log *file* "\thash: " pin-hash)
                   (.set db pin-hash #js {:pin pin :added (js/Date.)}))))))))

(defn update-db-from-data-dir []
  (let [db (kv "pins")
        db-posted (kv "posted")
        files (fs/readdirSync "data")
        files (filter #(.endsWith % ".json") files)]
    (.all js/Promise
          (to-array
            (map
              (fn [f]
                (log *file* "file =" f)
                (let [pins (read-pins-files "data" f)
                      promises (map #(<p-add-pin-to-database db db-posted %) pins)]
                  (log *file* "Pin count =" (aget pins "length"))
                  (.all js/Promise (to-array promises))))
              files)))))

(defn count-pins [db prefix]
  (plet [q (.query db (str "select count(*) as count from keyv where key like '" prefix ":%'"))
         c (-> q first (aget "count"))]
        c))

(defn main! []
  (log *file* "Updating db from json files.")
  (plet [result (update-db-from-data-dir)
         db (client)
         pins (count-pins db "pins")
         posted (count-pins db "posted")]
        (log *file* "Pins remaining:" pins)
        (log *file* "Pins posted:" posted)
        (log *file* "Done.")))

(main!)
