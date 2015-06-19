(ns grafter.rdf.preview
  "Tool support for rendering previews of grafter.tabular graph-fn templates
  with values from datasets."
  (:require [clojure.walk]
            [clojure.edn :as edn]))

(defn- symbolize-keys [m]
  (zipmap (map (comp symbol name) (keys m)) (vals m)))

(defn- substitution-map [bindings row-data]
  (symbolize-keys (merge (select-keys row-data (map (comp keyword name) (:keys bindings)))
                         (select-keys row-data (map name (:strs bindings))))))

(defmulti templatable? class)

(defmethod templatable? nil [v] true)

(defmethod templatable? Object [v] false)

(defmethod templatable? Number [v] true)

(defmethod templatable? String [v] true)

(defmethod templatable? clojure.lang.Keyword [v] true)

(defn- bind-constants [ns form]
  (comment
    (z/root (walk/prewalk form (fn [n]
                                 (let [s (z/sexpr n)]
                                   (if (and (symbol? s)
                                            (ns-resolve ns s))
                                     (when-let [varr (get (ns-map ns) s)]
                                       (when (templatable? @varr)
                                         (z/replace n @varr)))
                                     n))))))

  ;; TODO replace above with this
  (clojure.walk/postwalk
   (fn [f]
     (if (and (symbol? f)
              (ns-resolve ns f))
       (if-let [varr (get (ns-map ns) f)]
         (if (templatable? @varr)
           @varr
           f)
         f)
       f)
     ) form))

(defrecord UnreadableForm [form-string form-class])

(defn- readable-form?
  "Returns false if the form can't be read, or returns the form (truthy) if it
  can."
  [form]
  (try
    (binding [*print-dup* true] (pr-str form))
    (catch IllegalArgumentException ex
      false)))

(defn- ->printable-row [row-data]
  (letfn [(make-readable [val]
            (if (readable-form? val)
              val
              (->UnreadableForm (str val)  (str (.getName (class val))))))]
    (zipmap (map make-readable (keys row-data))
            (map make-readable (vals row-data)))))

(defn preview-graph
  "Takes a dataset a function built via grafter.tabular/graph-fn and a row
  number, and returns an EDN datastructure representing the template (body) of
  the graph-fn function with column variables substituted for data.

  Takes an optional final argument which can be :render-constants, if the user
  would also like to substitute symbols from within the graph-fn body with
  renderable constants found in referenced vars."
  ([dataset graphf row]
   (preview-graph dataset graphf row false))

  ([dataset graphf row render-constants?]
   (let [form (:grafter.tabular/template (meta graphf))
         bindings (first (second form))
         body-forms (drop 2 form)
         printable-row-data (->printable-row (nth (:rows dataset) row nil))
         subs (substitution-map bindings printable-row-data)]

     {:bindings bindings :row printable-row-data :template
      (->> body-forms
           (map (fn [body-form]
                  (let [replaced-vals (clojure.walk/postwalk-replace subs body-form)]
                    (if (= :render-constants render-constants?)
                      (bind-constants (:grafter.tabular/defined-in-ns (meta graphf)) replaced-vals)
                      replaced-vals)))))})))

(comment

  (do (require '(grafter [tabular :refer [graph-fn make-dataset]]))
      (require '(grafter [rdf :refer [s]]))
      (require '(grafter.rdf [templater :refer [graph]]))

      (def ds (make-dataset [[(Object.) "bar" (s "baz")]]))

      (def foaf:friendOf "http://foaf/friendOf")

      (def my-template (graph-fn [{:strs [a b c] :keys [c]}]
                                 (graph "http://foo.com/"
                                        [(first b)
                                         [b c]]
                                        [a
                                         [foaf:friendOf c]
                                         [b c]
                                         [b c]])
                                 (graph a [a [b c]]))))

  (preview-graph ds my-template 0)

  )
