(ns mail
  (:require
    [common :refer [env]]
    ["nodemailer" :as nm]))

(defn transport []
  (let [smtp-url (env "SMTP_SERVER" nil)]
    (if smtp-url
      (js/Promise. (fn [res err] (res (.createTransport nm smtp-url))))
      (-> (.createTestAccount nm)
          (.then (fn [account]
                   (.createTransport
                     nm
                     #js {:host "smtp.ethereal.email"
                          :port 587
                          :secure false
                          :auth #js {:user (aget account "user")
                                     :pass (aget account "pass")}})))))))

(defn send-mail [mail-transport to from subject html text]
  ; main().catch(console.error);
  (->
    (.sendMail
      mail-transport
      (clj->js {:from from
                :to to
                :subject subject
                :text text
                :html html}))
    (.catch (fn [err] #js {:error err}))
    (.then (fn [info]
             (aset info "url" (.getTestMessageUrl nm info))
             info))))
