(ns integrant-duct-example.hierarchy
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::message-store [_ {:keys [message]}]
  (atom message))

(defmethod ig/init-key ::printer [_ {:keys [store]}]
  (prn (format "%s says: %s" ::printer @store)))

(derive ::message-store ::store)

(ig/init {::message-store {:message "love yourself, homie"}
          ::printer       {:store   (ig/ref ::store)}})
