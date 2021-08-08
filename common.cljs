(ns common
  (:require ["keyv" :as Keyv]))

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

(defn now []
  (-> (js/Date.)
      (.toISOString)
      (.split ".")
      first
      (.replace "T" " ")))

(defn log [& args]
  (apply print (conj args (now))))
