(ns common
  (:require ["keyv$default" :as Keyv]
            ["path" :refer [basename]]))

(defn env [k & [default]]
  (or (aget js/process.env k) default))

(def database-url (env "DATABASE" "sqlite://./database.sqlite"))

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

(defn now []
  (-> (js/Date.)
      (.toISOString)
      (.split ".")
      first
      (.replace "T" " ")))

(defn log [file-path & args]
  (apply print (conj (conj args (str (basename file-path) ":")) (now))))

(defn bail [msg]
  (js/console.error msg)
  (js/process.exit 1))
