(ns cerberus.ws)

(defn host []
  (let [location (.-location js/window)
        proto (.-protocol js/location)
        ws (clojure.string/replace proto #"^http" "ws")
        host (.-hostname location)
        port (.-port location)]
    (str ws "//" host ":" port))
  "ws://10.1.1.180")
