(ns sql-crud.context.util
  "Utility functions needed to make working with the specifics of sqlite easier")

(defn keyword->db-name
  "Will munge a keyword into a name that should be used in the database"
  [kw]
  (-> kw
      (clojure.core/name)
      (clojure.string/replace "-" "")
      (.toUpperCase)))

(defmulti type->format
  "Use type to determine how to represent the sql literal for creating queries"
  #(:type %))

(defmethod type->format
  :boolean
  [_] "%s")

(defmethod type->format
  :double
  [_] "%s")

(defmethod type->format
  :int
  [_] "%s")

(defmethod type->format
  :float
  [_] "%s")

(defmethod type->format
  :keyword
  [_] "'%s'")

(defmethod type->format
  :string
  [_] "'%s'")

(defmethod type->format
  :default
  [_] "'%s'")

(defmulti  type->db-type
  "Maps the data-definition type to a type that the database will understand"
  #(:type %))

(defmethod type->db-type
  :boolean
  [_] "INT")

(defmethod type->db-type
  :double
  [_] "REAL")

(defmethod type->db-type
  :float
  [_] "REAL")

(defmethod type->db-type
  :int
  [_] "INTEGER")

(defmethod type->db-type
  :keyword
  [_] "TEXT")

(defmethod type->db-type
  :string
  [_] "TEXT")

(defmethod type->db-type
  :default
  [_] "TEXT")


(defmulti to-sql-fn
  "Used by sql-crud.context.sql-gen/to-sql to determine how to handle data-definition
  values for serialization."
  #(:type %))

(defmethod to-sql-fn
  :boolean
  [_]
  (fn [v] (if v 1 0)))

(defmethod to-sql-fn
  :keyword
  [_]
  (fn [v] (str v)))

(defmethod to-sql-fn
  :default
  [_]
  (fn [v] v))


(defmulti from-sql-fn
  "A function that given a cursor (JDBC `ResultSet`) and col-name, tells how to retrieve a value."
  #(:type %))

(defmethod from-sql-fn
  :boolean
  [_] (fn [cursor col-name]
        (.getBoolean cursor col-name)))

(defmethod from-sql-fn
  :double
  [_] (fn [cursor col-name]
        (.getDouble cursor col-name)))

(defmethod from-sql-fn
  :float
  [_] (fn [cursor col-name]
        (.getFloat cursor col-name)))

(defmethod from-sql-fn
  :int
  [_] (fn [cursor col-name]
        (.getInt cursor col-name)))

(defmethod from-sql-fn
  :keyword
  [_] (fn [cursor col-name]
        (keyword (apply str (drop 1 (.getString cursor col-name))))))

(defmethod from-sql-fn
  :string
  [_] (fn [cursor col-name]
        (.getString cursor col-name)))

(defmethod from-sql-fn
  :default
  [_] (fn [cursor col-name]
        (.getString cursor col-name)))

(defn cursor->hash-map
  "Using the table data-definition to determine which columns to get and the 
  JDBC `ResultSet` as a cursor, create a hash-map representing a single row
  from the database"
  [data-definition cursor]
  (reduce (fn [acc {:keys [name type] :as col}]
            (let [col-name (-> name
                               (clojure.core/name)
                               (clojure.string/replace "-" "")
                               (.toUpperCase))
                  value ((from-sql-fn col) cursor col-name)]
              (assoc acc name value)))
          {}
          (:columns data-definition)))
