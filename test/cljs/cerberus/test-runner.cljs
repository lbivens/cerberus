(ns cerberus.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [cerberus.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'cerberus.core-test))
    0
    1))
