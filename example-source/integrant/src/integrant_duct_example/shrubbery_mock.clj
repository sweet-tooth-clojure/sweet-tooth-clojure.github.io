(ns integrant-duct-example.shrubbery-mock
  (:require [integrant.core :as ig]
            [sweet-tooth.endpoint.system :as es]
            [shrubbery.core :as shrub])
  (:refer-clojure :exclude [take]))

(defprotocol Queue
  (add [_ queue-name v])
  (take [_ queue-name]))

(defrecord QueueClient []
  Queue
  (add [_ queue-name v]
    ;; AWS interaction goes here
    :added)
  (take [_ queue-name]
    ;; AWS interaction goes here
    :taked))

(defmethod ig/init-key ::queue [_ _]
  (QueueClient.))

(defmethod es/config ::dev [_]
  {::queue {}})

(def real-component (::queue (es/system ::dev)))
(add real-component :foo :bar)

(take real-component :foo)


(defmethod es/config ::test [_]
  {::queue {::es/init-key-alternative ::es/shrubbery-mock
            ::es/shrubbery-mock       {}}})

(def mocked-component (::queue (es/system ::test)))
(add mocked-component :msgs "hi")

(shrub/calls mocked-component)

(shrub/received? mocked-component add [:msgs "hi"])

(defmethod es/config ::test-2 [_]
  {::queue {::es/init-key-alternative ::es/shrubbery-mock
            ::es/shrubbery-mock       {:add :mock-added}}})

(def mocked-component-2
  (::queue (es/system ::test-2)))

(add mocked-component-2 :msgs "hi")

(def mocked-component-3
  (::queue (es/system ::test {::queue {::es/shrubbery-mock {:add :mock-added}}})))

{::es/init-key-alternative ::es/shrubbery-mock
 ::es/shrubbery-mock       {:add :mock-added}}

(es/shrubbery-mock {:add :mock-added})
