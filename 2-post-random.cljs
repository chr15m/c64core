(ns post
  (:require
    ["keyv" :as Keyv]
    ["path" :as path]
    ["node-fetch" :as fetch]
    ["twitter-api-v2/dist" :refer [TwitterApi]]))

(defn env [k & [default]]
  (or (aget js/process.env k) default))

(def database-url (env "DATABASE" "sqlite://./database.sqlite"))

(def tw-keys {:appKey (env "TWITTER_API_APP_KEY")
              :appSecret (env "TWITTER_API_APP_SECRET")
              :accessToken (env "TWITTER_API_ACCESS_TOKEN")
              :accessSecret (env "TWITTER_API_TOKEN_SECRET")})

(defn kv [kv-ns]
  (Keyv. database-url (clj->js {:namespace kv-ns})))

(defn client []
  (->
    (Keyv. database-url)
    (aget "opts" "store")))

(defmacro plet
  [bindings & body]
  (let [binding-pairs (reverse (partition 2 bindings))
        body (cons 'do body)]
    (reduce (fn [body [sym expr]]
              (let [expr (list '.resolve 'js/Promise expr)]
                (list '.then expr (list 'clojure.core/fn (vector sym)
                                        body))))
            body
            binding-pairs)))

(defn get-pin-image [pin]
  (try
    (let [embed (aget pin "embed")]
      (if embed
        (aget embed "src")
        (-> (aget pin "images" "564x" "url") (.replace "564x" "originals"))))
    (catch :default e)))

(defn main! []
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
         media-id (.uploadMedia api buffer (clj->js {:type ext}))
         tweet-text (str "src: " pin-link)
         tweet (.tweet api tweet-text (clj->js {:media_ids media-id}))
         update-posted (.set posted pin-hash (clj->js {:tweet tweet :pin pin-data :img pin-image :link pin-link}))
         update-pins (.delete pins pin-hash)]
        (print "Posted")
        (print "Hash:" pin-hash)
        ;(print pin-data)
        (print pin-image)
        (print pin-link)
        (print (aget res "ok"))
        (print "Tweet:" tweet)
        (print)))

(main!)

(js/setTimeout
  #(print "Done.")
  (* (js/Math.random) 16 60 60 1000))
