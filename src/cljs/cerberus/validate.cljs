(ns cerberus.validate
  (:require
   [clojure.string :refer [blank?]]
   [om.core :as om :include-macros true]
   [cerberus.utils :refer [val-by-id]]))

(defn match [match validation-key value-key owner event]
  (let [new-value (val-by-id  (.. event -target -id))]
    (if (and (= new-value match)
             (not (blank? match))
             (not (blank? new-value)))
      (om/set-state! owner validation-key true)
      (om/set-state! owner validation-key false))
    (om/set-state! owner value-key new-value)))

(defn nonempty [validation-key value-key owner event]
  (let [new-value (val-by-id  (.. event -target -id))]
    (if (not (blank? new-value))
      (om/set-state! owner validation-key true)
      (om/set-state! owner validation-key false))
    (om/set-state! owner value-key new-value)))
