(ns post
  (:require
    [nbb.core :refer [*file*]]
    ["path" :as path]
    ["node-fetch" :as fetch]
    ["twitter-api-v2/dist" :refer [TwitterApi]]
    [common :refer [log bail plet env client kv get-pin-image]]))

(def n "2-post-random.cljs:")

(def tw-keys {:appKey (env "TWITTER_API_APP_KEY")
              :appSecret (env "TWITTER_API_APP_SECRET")
              :accessToken (env "TWITTER_API_ACCESS_TOKEN")
              :accessSecret (env "TWITTER_API_ACCESS_SECRET")})

(doseq [[k v] tw-keys]
  (when (nil? v)
    (let [n (-> k name (.replace ":" "") (.replace #"([A-Z])" "_$1") .toUpperCase)]
    (bail (str "TWITTER_API_" n " is not set")))))

; TODO: check when the last tweet was and wait min-hours

(defn main! []
  (log *file* "main!")
  (plet [db (client)
         pins (kv "pins")
         posted (kv "posted")
         _ (log *file* "Running query.")
         rows (.query db "select * from keyv where key like 'pins:%' order by random() limit 1")
         pin (first rows)
         pin-hash (second (.split (aget pin "key") ":"))
         pin-data (-> (aget pin "value") (js/JSON.parse) (aget "value") (aget "pin"))
         pin-image (get-pin-image pin-data)
         pin-link (aget pin-data "link")
         _ (log *file* "First fetch.")
         res (fetch pin-image)
         pin-image (if (aget res "ok")
                     pin-image
                     (.replace pin-image "originals" "564x"))
         _ (log *file* "Second fetch.")
         res (fetch pin-image)
         buffer (.buffer res)
         _ (log *file* "Twitter API.")
         tw (TwitterApi. (clj->js tw-keys))
         api (aget tw "v1")
         ext (-> (path/extname pin-image) (.replace "." ""))
         tweet-text (str "src: " pin-link)

         _ (log *file* "Twitter uploadMedia.")
         media-id (.uploadMedia api buffer (clj->js {:type ext}))
         _ (log *file* "Twitter tweet.")
         tweet (.tweet api tweet-text (clj->js {:media_ids media-id}))
         _ (log *file* "Update database.")
         update-posted (.set posted pin-hash (clj->js {:tweet tweet :pin pin-data :img pin-image :link pin-link}))
         update-pins (.delete pins pin-hash)]

        (log *file* "Posted")
        (log *file* "Hash:" pin-hash)
        (log *file* pin-image)
        (log *file* pin-link)
        (log *file* (aget res "ok"))
        ;(print "Tweet:" tweet)
        ;(print pin-data)
        (print)))

(main!)

(let [wait-ms (* (js/Math.random) 16 60 60 1000)
      wait-hrs (/ wait-ms (* 1000 60 60))]
  (log *file* "Will wait" (js/Math.round wait-hrs) "hours to re-run.")
  (js/setTimeout
    #(log *file* "Exiting.")
    wait-ms))
