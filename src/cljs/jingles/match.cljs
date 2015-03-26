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
S =  (Rule ' '+)* Rule
Rule = Val | Field
Val = Sym | Str | Number
Sym = #'[a-zA-Z][a-zA-Z0-9.-]*'
Number = #'[0-9]+'
Str = '\"' #'([^\"]|\\.)+' '\"'
Cmp = '>' | '<' | '=' | '~' | '>=' | '<='
Field = Sym ':' Cmp? Val
")


(defn simplify-query-element [q]
  (match
   q
   [:Rule [:Val [:Sym filter-str]]] (partial all (re-match filter-str))
   [:Rule [:Val [:Str "\"" filter-str "\""]]] (partial all  (re-match filter-str))
   ;; Sym Comp
   [:Rule [:Field [:Sym field-name] ":" [:Val [:Sym field-match]]]] (partial field field-name (re-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "~"] [:Val [:Sym field-match]]]] (partial field field-name (re-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "="] [:Val [:Sym field-match]]]] (partial field field-name (eq-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">"] [:Val [:Sym field-match]]]] (partial field field-name (gt-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<"] [:Val [:Sym field-match]]]] (partial field field-name (lt-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">="] [:Val [:Sym field-match]]]] (partial field field-name (gte-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<="] [:Val [:Sym field-match]]]] (partial field field-name (lte-match field-match))

   ;; Str Comp
   [:Rule [:Field [:Sym field-name] ":" [:Val [:Str field-match]]]] (partial field field-name (re-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "~"] [:Val [:Str field-match]]]] (partial field field-name (re-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "="] [:Val [:Str field-match]]]] (partial field field-name (eq-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">"] [:Val [:Str field-match]]]] (partial field field-name (gt-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<"] [:Val [:Str field-match]]]] (partial field field-name (lt-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">="] [:Val [:Str field-match]]]] (partial field field-name (gte-match field-match))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<="] [:Val [:Str field-match]]]] (partial field field-name (lte-match field-match))
   ;; Integer comparison
   [:Rule [:Field [:Sym field-name] ":" [:Val [:Number field-match]]]] (partial field field-name (eq-match (str->int field-match)))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "="] [:Val [:Number field-match]]]] (partial field field-name (eq-match (str->int field-match)))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<"] [:Val [:Number field-match]]]] (partial field field-name (lt-match (str->int field-match)))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">"] [:Val [:Number field-match]]]] (partial field field-name (gt-match (str->int field-match)))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp "<="] [:Val [:Number field-match]]]] (partial field field-name (lte-match (str->int field-match)))
   [:Rule [:Field [:Sym field-name] ":" [:Cmp ">="] [:Val [:Number field-match]]]] (partial field field-name (gte-match (str->int field-match)))
   :else (pr q))
  ;;[:Rule [:Field [:Sym field] ":" & field-rule]] (simplify-field-query field-rule)
  ;;[]
  )

(defn simplify-query [q]
  ()
  (filter boolean (map simplify-query-element q)))


(def parser (insta/parser syntax))

(defn parse [config filter-str]
  (let [q (simplify-query  (parser filter-str))]
    (pr q)
    (map #(% config) q)))

(defn run [filters element]
  (every? #(% element) filters))
