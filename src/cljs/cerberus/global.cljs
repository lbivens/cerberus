(ns cerberus.global
    (:refer-clojure :exclude [get print]))

(defn get
  ([key dflt]
   (get-in (js->clj js/Config) [key] dflt))
  ([key]
   (get-in (js->clj js/Config) [key]))
  ([]
   (js->clj js/Config)))


(defn print []
  (pr (get)))
