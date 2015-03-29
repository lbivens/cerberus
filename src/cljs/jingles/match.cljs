(ns jingles.match
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core.match]
   [instaparse.core :as insta]
   [jingles.utils :refer [value-by-key str->int]]))

(defn get-filter-field [field element]
  (if-let [key (:filter-key field)]
    (value-by-key key element)
    (value-by-key (:key field) element)))

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

(defn all [match-fn config]
  (let [fields (filter #(not= (:filter %) false) (vals (:fields config)))
        fields (map #(partial get-filter-field %) fields)]
    (fn [e]
      (some match-fn (map #(% e) fields)))))

(defn field [field-name match-fn config]
  (let [field-key (keyword field-name)]
    (if-let [f (get-in config [:fields  field-key])]
      (fn [e]
        (if-let [v (get-filter-field f e)]
          (match-fn v)
          false))
      (do
        (pr field-key (config field-key))
        (constantly true)))))

(def syntax
  "
<s> =  (rule ' '+)* rule
<rule> = val | field
<val> = str | num | size
size = num | b | kb | mb | gb | tb | pb
b = #'[0-9]+' <('B' | 'b')>
kb = #'[0-9]+' <('KB' | 'kb')>
mb = #'[0-9]+' <('MB' | 'mb')>
gb = #'[0-9]+' <('GB' | 'gb')>
tb = #'[0-9]+' <('TB' | 'tb')>
pb = #'[0-9]+' <('PB' | 'pb')>
num = #'[0-9]+'
<sym> = #'[a-zA-Z][a-zA-Z0-9.-]*'
str = <'\"'> #'([^\"]|\\.)+' <'\"'> | sym
<cmp> = '>' | '<' | '=' | '~' | '>=' | '<='
field = sym <':'> cmp? val
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
   [:str filter-str] (partial all (re-match filter-str))
   [:num filter-str] (partial all (re-match filter-str))
   ;; Sym Comp
   [:field field-name [:str field-match]] (partial field field-name (re-match field-match))
   [:field field-name [:num field-match]] (partial field field-name (eq-match (str->int field-match)))
   [:field field-name "~" [:num field-match]] (partial field field-name (re-match field-match))
   [:field field-name cmp val] (partial field field-name ((cmp-fn cmp) (val-fn val)))
   :else (pr "unknown:" q)))

(defn simplify-query [q]
  ()
  (filter boolean (map simplify-query-element q)))

(def parser (insta/parser syntax))

(defn parse [config filter-str]
  (let [q (simplify-query  (parser filter-str))]
    (map #(% config) q)))

(defn run [filters element]
  (every? #(% element) filters))
