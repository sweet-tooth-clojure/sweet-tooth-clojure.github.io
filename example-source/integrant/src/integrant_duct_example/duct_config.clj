(ns integrant-duct-example.duct-config
  (:require [duct.core :as duct]
            [integrant.core :as ig]))

(defmethod ig/init-key ::message-store [_ {:keys [message]}]
  (atom message))

(defmethod ig/init-key ::printer [_ {:keys [store]}]
  (prn (format "%s says: %s" ::printer @store)))

(derive ::message-store ::store)

(duct/load-hierarchy)
(def system-config
  (duct/prep-config {:duct.profile/base {::message-store {:message "love yourself, homie"}
                                         ::printer       {:store   (ig/ref ::store)}}}))

(ig/init system-config)

{::message-store {:message "love yourself, homie"}
 ::printer       {:store   (ig/ref ::store)}}

(duct/prep-config
 {:duct.profile/base {::message-store {:message "love yourself, homie"}
                      ::printer       {:store   (ig/ref ::store)}}
  :duct.profile/prod {::message-store {:message "take care of yourself, homie"}}}
 [:duct.profile/prod])
{::message-store        {:message "take care of yourself, homie"}
 ::printer              {:store {:key ::store}}
 :duct.core/environment :production}
