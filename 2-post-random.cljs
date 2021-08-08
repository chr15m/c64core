(ns post
  (:require
    ["path" :as path]
    ["node-fetch" :as fetch]
    ["twitter-api-v2/dist" :refer [TwitterApi]]
    [common :refer [plet env client kv get-pin-image]]))

(def tw-keys {:appKey (env "TWITTER_API_APP_KEY")
              :appSecret (env "TWITTER_API_APP_SECRET")
              :accessToken (env "TWITTER_API_ACCESS_TOKEN")
              :accessSecret (env "TWITTER_API_TOKEN_SECRET")})

(defn main! []
  (print "main!")
  (plet [db (client)
         pins (kv "pins")
         posted (kv "posted")
         rows (.query db "select * from keyv where key like 'pins:%' order by random() limit 1")
         pin (first rows)
         pin-hash (second (.split (aget pin "key") ":"))
         pin-data (-> (aget pin "value") (js/JSON.parse) (aget "value") (aget "pin"))
         pin-image (get-pin-image pin-data)
         pin-link (aget pin-data "link")
         res (fetch pin-image)
         pin-image (if (aget res "ok")
                     pin-image
                     (.replace pin-image "originals" "564x"))
         res (fetch pin-image)
         buffer (.buffer res)
         tw (TwitterApi. (clj->js tw-keys))
         api (aget tw "v1")
         ext (-> (path/extname pin-image) (.replace "." ""))
         tweet-text (str "src: " pin-link)

         media-id (.uploadMedia api buffer (clj->js {:type ext}))
         tweet (.tweet api tweet-text (clj->js {:media_ids media-id}))
         update-posted (.set posted pin-hash (clj->js {:tweet tweet :pin pin-data :img pin-image :link pin-link}))
         update-pins (.delete pins pin-hash)]
        
        (print "Posted")
        (print "Hash:" pin-hash)
        (print pin-image)
        (print pin-link)
        (print (aget res "ok"))
        (print)
        
        ;(print "Tweet:" tweet)
        ;(print pin-data)
        ))

(main!)

(let [wait-ms (* (js/Math.random) 16 60 60 1000)
      wait-hrs (/ wait-ms (* 1000 60 60))]
  (print "Wil wait" (js/Math.round wait-hrs) "hours to re-run.")
  (js/setTimeout
    #(print "Done.")
    wait-ms))
