(ns post
  (:require
    ["keyv" :as Keyv]
    ["node-fetch" :as fetch]))

(defn env [k & [default]]
  (or (aget js/process.env k) default))

(def database-url (env "DATABASE" "sqlite://./database.sqlite"))

(defn kv [kv-ns]
  (Keyv. database-url #js {:namespace kv-ns}))

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
         rows (.query db "select * from keyv where key like 'pins:f5a84%' order by random() limit 1")
         pin (first rows)
         pin-hash (second (.split (aget pin "key") ":"))
         pin-data (-> (aget pin "value") (js/JSON.parse) (aget "value") (aget "pin"))
         pin-image (get-pin-image pin-data)
         pin-link (aget pin-data "link")
         res (fetch pin-image)
         pin-image (if (aget res "ok")
                     pin-image
                     (.replace pin-image "originals" "564x"))]
        (print "Hash:" pin-hash)
        ;(print pin-data)
        (print pin-image)
        (print pin-link)
        (print (aget res "ok"))
        (print (aget res "status"))))

(main!)
