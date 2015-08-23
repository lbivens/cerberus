(ns cerberus.debug)

(defn error [& args]
  (apply pr "[error]" args))

(defn warning [& args]
  (apply pr "[warning]" args))

(defn info [& args]
  (apply pr "[info]" args))

(defn debug [& args]
  (apply pr "[debug]" args))
