(ns integrant-duct-example.duct-modules
 (:require [duct.core :as duct]
           [integrant.core :as ig]))

(defmethod ig/init-key ::add-foo-component [_ _]
  (fn [config]
    (assoc config ::foo {})))

(duct/prep-config {:duct.profile/base  {::some-component {}}
                   ::add-foo-component {}})
