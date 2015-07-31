(ns cerberus.validate
  (:require
   [clojure.string :refer [blank?]]
   [om.core :as om :include-macros true]
   [cerberus.utils :refer [val-by-id]]))

(defn match [event match validation-key value-key owner]
  (let [newValue (val-by-id  (.. event -target -id))]
    (if (and (= newValue match)
             (not (blank? match))
             (not (blank? newValue)))
      (om/set-state! owner validation-key true)
      (om/set-state! owner validation-key false))
    (om/set-state! owner value-key newValue)))

(defn nonempty [event validation-key value-key owner]
  (let [newValue (val-by-id  (.. event -target -id))]
    (if (not (blank? newValue))
      (om/set-state! owner validation-key true)
      (om/set-state! owner validation-key false))
    (om/set-state! owner value-key newValue)))
