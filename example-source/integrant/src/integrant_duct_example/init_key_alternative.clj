(ns integrant-duct-example.init-key-alternative
  (:require [integrant.core :as ig]
            [sweet-tooth.endpoint.system :as es]
            [shrubbery.core :as shrub]))

(defmethod ig/init-key ::printer [_ {:keys [message]}]
  (prn (format "message: %s" message))
  {:message message})

(es/init-key ::printer {:message                  "hi"
                        ::es/init-key-alternative ::es/replacement
                        ::es/replacement          "bye"})

(es/init {::printer {:message "hi"}})

(es/init {::printer {::es/init-key-alternative ::es/replacement
                     ::es/replacement          "replacement"
                     :message                  "hi"}})

(defprotocol Logger
  (print-msg [_ msg]))

(defmethod ig/init-key ::logger [_ _]
  (reify Logger
    (print-msg [_ msg] (prn msg))))

