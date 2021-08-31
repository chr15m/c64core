(ns post
  (:require
    ["node-fetch$default" :as fetch]
    ["path" :refer [extname]]
    ["twitter-api-v2/dist$default" :refer [TwitterApi]]
    [common :refer [log bail plet env client kv get-pin-image]]
    [nbb.core :refer [*file*]]))

(def tw-keys {:appKey (env "TWITTER_API_APP_KEY")
              :appSecret (env "TWITTER_API_APP_SECRET")
              :accessToken (env "TWITTER_API_ACCESS_TOKEN")
              :accessSecret (env "TWITTER_API_ACCESS_SECRET")})

(def min-tweet-gap-hours 5)
(def pause-random-hours 24)

(def live (env "LIVE"))

(def n *file*)

(doseq [[k v] tw-keys]
  (when (nil? v)
    (let [n (-> k name (.replace ":" "") (.replace #"([A-Z])" "_$1") .toUpperCase)]
    (bail (str "TWITTER_API_" n " is not set")))))

(defn main! []
  (log *file* "main!")
  (plet [_ (log n "Set up Twitter API.")
         tw (TwitterApi. (clj->js tw-keys))
         api (aget tw "v1")

         user (.currentUser tw)
         user-id (aget user "id_str")
         timeline (.get api "statuses/user_timeline.json" (clj->js {:user_id user-id}))
         latest-tweet (first timeline)
         latest-tweet-time (js/Date. (aget latest-tweet "created_at"))
         now (js/Date.)
         hours-since-last (/ (- now latest-tweet-time) 1000 60 60)
         _ (log n "Latest Tweet was" (int hours-since-last) "hours ago.")]

        (if (< hours-since-last min-tweet-gap-hours)
          (log n "Min of" min-tweet-gap-hours "hours have not passed. Skipping post.")
          (plet
            [db (client)
             pins (kv "pins")
             posted (kv "posted")
             _ (log n "Running query.")
             rows (.query db "select * from keyv where key like 'pins:%' order by random() limit 1")
             pin (first rows)
             pin-hash (second (.split (aget pin "key") ":"))
             pin-data (-> (aget pin "value") (js/JSON.parse) (aget "value") (aget "pin"))
             pin-image (get-pin-image pin-data)
             pin-link (aget pin-data "link")
             ext (-> (extname pin-image) (.replace "." ""))
             tweet-text (str (if (empty? pin-link) "" (str "src: " pin-link))
                             "\n#retrocomputing #aesthetic")

             _ (log n "First fetch.")
             res (fetch pin-image)
             pin-image (if (aget res "ok")
                         pin-image
                         (.replace pin-image "originals" "564x"))
             _ (log n "Second fetch.")
             res (fetch pin-image)
             buffer (.buffer res)

             _ (log n "Twitter uploadMedia.")
             media-id (when live (.uploadMedia api buffer (clj->js {:type ext})))
             _ (log n "Twitter tweet.")
             tweet (when live (.tweet api tweet-text (clj->js {:media_ids media-id})))
             _ (log n "Update database.")
             update-posted (.set posted pin-hash (clj->js {:tweet tweet :pin pin-data :img pin-image :link pin-link}))
             update-pins (.delete pins pin-hash)]

            (log n "Posted and updated.")
            (log n "Hash:" pin-hash)
            (log n "Image:" pin-image)
            (log n "Link:" pin-link)
            (log n "Done, now waiting.")))
        ;(print "Tweet:" tweet)
        ;(print pin-data)
        ))

(main!)

(let [wait-ms (* (js/Math.random) pause-random-hours 60 60 1000)
      wait-hrs (/ wait-ms (* 1000 60 60))]
  (log *file* "Will wait" (js/Math.round wait-hrs) "hours to re-run.")
  (js/setTimeout
    #(log *file* "Exiting.")
    wait-ms))
