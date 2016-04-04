(ns cerberus.del
  (:require
   [om.core :as om :include-macros true]
   [om-bootstrap.button :as b]
   [om-bootstrap.modal :as md]
   [om-tools.dom :as d :include-macros true]
   [cerberus.state :refer [set-state!]]))



(defn show [uuid]
  (set-state! [:delete] uuid))

(defn state-show [owner uuid]
  (om/set-state! owner [:delete :id] uuid))

(defn menue-item [uuid]
  ["Delete" #(show uuid)])



(defn state-modal [state owner name-fn delete-fn]
  (let [id (get-in state [:delete :id])]
    (pr state (name-fn state))
    (d/div
     {:style {:display (if id "block" "none")} }
     (md/modal
      {:header (d/h4
                "Delete"
                (d/button {:type         "button"
                           :class        "close"
                           :aria-hidden  true
                           :on-click #(om/set-state! owner :delete nil)}
                          "×"))
       :close-button? false
       :visible? true
       :animate? false
       :style {:display "block"}
       :footer (d/div
                (b/button {:bs-style "danger"
                           :disabled? false
                           :on-click #(do
                                        (delete-fn id)
                                        (om/set-state! owner :delete nil))}
                          "delete"))}
      "Are you sure that you want to delete '" (d/strong (name-fn state)) "' (" id ")?" ))))

(defn modal [data root name-fn delete-fn]
  (let [id (:delete data)
        element (get-in data [root :elements id])]
    (d/div
     {:style {:display (if id "block" "none")} }
     (md/modal
      {:header (d/h4
                "Delete"
                (d/button {:type         "button"
                           :class        "close"
                           :aria-hidden  true
                           :on-click #(set-state! [:delete] nil)}
                          "×"))
       :close-button? false
       :visible? true
       :animate? false
       :style {:display "block"}
       :footer (d/div
                (b/button {:bs-style "danger"
                           :disabled? false
                           :on-click #(do
                                        (delete-fn data id)
                                        (set-state! [:delete] nil))}
                          "delete"))}
      "Are you sure that you want to delete '" (d/strong (name-fn element)) "' (" id ")?" ))))



(defn with-delete [data root name-fn delete-fn content]
  (d/div {}
         (modal data root name-fn delete-fn)
         content))
