(ns jingles.match
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core.match]
   [instaparse.core :as insta]
   [jingles.utils :refer [value-by-key str->int]]))


(defn re-match [filter-str]
  (let [re  (re-pattern filter-str)]
    #(re-find re (if (string? %) % (str %)))))

(defn eq-match [filter-str]
  #(= filter-str %))

(defn gt-match [filter-str]
  #(> % filter-str))

(defn lt-match [filter-str]
  #(< % filter-str))

(defn gte-match [filter-str]
  #(>= % filter-str))

(defn lte-match [filter-str]
  #(<= % filter-str))

(defn all [match-fn]
  (fn [element]
    (some (fn [field]
            (or (= (:filter field) false) (match-fn (:filter-text field)))) element)))

(defn field [field-name match-fn]
  (let [field-key (keyword field)]
    (fn [element]
      (if-let [[field] (filter #(= field-key (:id %)) element)]
        (match-fn (:filter-text field))
        false))))

(def syntax
  "
<s> =  (rule ' '+)* rule
<rule> = val | field
<val> = str | num | size
field = sym <':'> cmp? val
size = b | kb | mb | gb | tb | pb
b = #'[0-9]+' <('B' | 'b')>
kb = #'[0-9]+' <('KB' | 'kb')>
mb = #'[0-9]+' <('MB' | 'mb')>
gb = #'[0-9]+' <('GB' | 'gb')>
tb = #'[0-9]+' <('TB' | 'tb')>
pb = #'[0-9]+' <('PB' | 'pb')>
num = #'[0-9]+'
str = <'\"'> #'([^\"]|\\.)+' <'\"'> | sym
<sym> = #'[a-zA-Z][a-zA-Z0-9._-]*'
<cmp> = '>' | '<' | '=' | '~' | '>=' | '<='
")

(def cmp-fn
  {"~" re-match
   "=" eq-match
   ">" gt-match
   "<" lt-match
   ">=" gte-match
   "<=" lte-match})

(defn val-fn [v]
  (match
   v
   [:str field-match] field-match
   [:num field-match] (str->int field-match)))

(defn simplify-query-element [q]
  (match
   q
   [:str filter-str] (all (re-match filter-str))
   [:num filter-str] (all (re-match filter-str))
   ;; Sym Comp
   [:field field-name [:str field-match]]     (field field-name (re-match field-match))
   [:field field-name [:num field-match]]     (field field-name (eq-match (str->int field-match)))
   [:field field-name "~" [:num field-match]] (field field-name (re-match field-match))
   [:field field-name cmp val]                (field field-name ((cmp-fn cmp) (val-fn val)))
   :else (pr "unknown:" q)))

(defn simplify-query [q]
  (filter boolean (map simplify-query-element q)))

(def parser (insta/parser syntax))

(defn parse [filter-str]
  (let [s (parser filter-str)]
    (simplify-query s)))

(defn run [filters element]
  (every? #(% element) filters))
