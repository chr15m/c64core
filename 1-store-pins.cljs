(ns robot.core
  (:require
    ["fs" :as fs]
    ["crypto" :as crypto]
    [common :refer [log kv plet get-pin-image]]))

(def n "1-store-pins.cljs:")

; https://api.pinterest.com/v3/pidgets/users/chrismgamedraw/pins/
; https://api.pinterest.com/v3/pidgets/boards/chrismgamedraw/retro-computing/pins/
; https://api.pinterest.com/v3/pidgets/sections/chrismgamedraw/retro-computing/retro-watches/pins/

; #js {:description Approaching Lacaille 8760-2 &#128165; video gif animation future ux ui explore exploration design interface space, :link https://dribbble.com/shots/4128271-Approaching-Lacaille-8760-2, :aggregated_pin_data #js {:aggregated_stats #js {:saves 9835, :done 0}}, :domain dribbble.com, :native_creator nil, :id 727190671070208399, :is_video false, :embed #js {:src https://i.pinimg.com/originals/51/9a/10/519a10f30f71d4053f13d44146a746a1.gif, :height 300, :width 400, :type gif}, :repin_count 0, :pinner #js {:follower_count 20, :about , :pin_count 2118, :full_name Chrism+gamedraw, :id 727190808490541556, :location , :profile_url https://www.pinterest.com/chrismgamedraw/, :image_small_url https://s.pinimg.com/images/user/default_60.png}, :attribution nil, :story_pin_data nil, :dominant_color #1d1d1f, :images #js {237x #js {:width 237, :height 177, :url https://i.pinimg.com/237x/51/9a/10/519a10f30f71d4053f13d44146a746a1.jpg}, 564x #js {:width 400, :height 300, :url https://i.pinimg.com/564x/51/9a/10/519a10f30f71d4053f13d44146a746a1.jpg}}}

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
