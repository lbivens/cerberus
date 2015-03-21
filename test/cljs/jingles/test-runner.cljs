(ns jingles.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [jingles.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'jingles.core-test))
    0
    1))
