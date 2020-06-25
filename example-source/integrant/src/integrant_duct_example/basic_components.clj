(ns integrant-duct-example.basic-components
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::message-store [_ {:keys [message]}]
  (atom message))

(defmethod ig/init-key ::printer [_ {:keys [store]}]
  (prn (format "%s says: %s" ::printer @store)))

(ig/init {::message-store {:message "love yourself, homie"}
          ::printer       {:store   (ig/ref ::message-store)}})
