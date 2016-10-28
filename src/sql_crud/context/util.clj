(ns sql-crud.context.util)

(defn keyword->db-name
  "Will munge a keyword into a name that should be used in the database"
  [kw]
  (-> kw
      (clojure.core/name)
      (clojure.string/replace "-" "")
      (.toUpperCase)))

(defn cursor->hash-map
  "Using the table data-definition to determine which columns to get and the 
  JDBC `ResultSet` as a cursor, create a hash-map representing a single row
  from the database"
  [data-definition cursor]
  (reduce (fn [acc {:keys [name type]}]
            (let [col-name (-> name
                               (clojure.core/name)
                               (clojure.string/replace "-" "")
                               (.toUpperCase))
                  value
                  (cond (= type :string)
                        (.getString cursor col-name)

                        (= type :keyword)
                        (keyword (apply str (drop 1 (.getString cursor col-name))))

                        (= type :int)
                        (.getInt cursor col-name)

                        (= type :float)
                        (.getFloat cursor col-name)

                        (= type :boolean)
                        (.getBoolean cursor col-name))]
              (assoc acc name value)))
          {}
          (:columns data-definition)))
