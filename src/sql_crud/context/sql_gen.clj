(ns sql-crud.context.sql-gen
  (:require [sql-crud.database :as db]
            [sql-crud.jdbc :as jdbc]
            [sql-crud.context.util :refer [keyword->db-name
                                           type->db-type
                                           type->format
                                           to-sql-fn
                                           cursor->hash-map]]))

(defn get-format
  "Looks for a :format key, and in it's absense uses the system defined one for :type"
  [m]
  (get m :format (type->format m)))

(defn- generate-column-def
  "From a column data definition, create a column definition which can be used 
  to create a table"
  [{:keys [name type format constraints nullable] :as col}]
  (let [col-name (keyword->db-name name)
        type' (type->db-type col)
        null-text (if (true? nullable) "" "NOT NULL")]
    (clojure.string/join " " (filter (complement empty?)
                                     (flatten [col-name type' constraints null-text])))))

(defn- generate-create-sql
  "From a table data definition, create a CREATE query that can be used to create
  the table"
  [{:keys [table-name columns] :as data-definition}]
  (let [table-name' (keyword->db-name table-name)
        columns' (map generate-column-def columns)]
    (str "CREATE TABLE IF NOT EXISTS " table-name' " "
         "(" (clojure.string/join ", " columns') ")")))

(defn- get-non-generated-columns
  [columns]
  (filter #(not (true? (:db-generated %))) columns))

(defn- generate-insert-format-sql
  "From a table data-definition, create an INSERT format string that can be used
  to insert into a table"
  [{:keys [table-name columns] :as data-definition}]
  (let [table-name' (keyword->db-name table-name)
        columns  (get-non-generated-columns columns)
        column-names (clojure.string/join ", " (map #(keyword->db-name (:name %))
                                                   columns))
        value-format-str (clojure.string/join ", "
                                              (map get-format
                                                   columns))]
    (clojure.string/join " " ["INSERT INTO" table-name' "(" column-names ")"
                              "VALUES" "(" value-format-str ")"])))

(declare to-sql)

(defn- get-clauses
  "Used for generating SET and WHERE clauses for queries"
  [m columns data-definition]
  (let [present-columns (filter #(not (nil? (get m (:name %)))) columns)
        to-sql-map (to-sql data-definition m)
        clauses (map #(let [kw (:name %)
                            k (keyword->db-name kw)
                            v (get to-sql-map kw)
                            pieces ["%s"
                                    "="
                                    (get-format %)]
                            format-str (clojure.string/join " " pieces)]
                        (format format-str k v))
                     present-columns)]
    clauses))

(defn- get-set-clause
  [m columns data-definition]
  (let [clauses (get-clauses m columns data-definition)]
    (if (empty? clauses)
      nil
      (clojure.string/join " , " clauses))))

(defn- get-where-clause
  [m columns data-definition]
  (let [clauses (get-clauses m columns data-definition)]
    (if (empty? clauses)
      nil
      (clojure.string/join " AND " clauses))))

(defn- generate-update-base-sql
  [{:keys [table-name columns] :as data-definition}]
  (fn [set-hash-map where-hash-map]
    (let [table-name' (keyword->db-name table-name)
          set-clause (get-set-clause set-hash-map columns data-definition)
          where-clause (get-where-clause where-hash-map columns data-definition)
          where (if (nil? where-clause) 
                  ""
                  (str "WHERE " where-clause))]
      (clojure.string/join " " (filter (complement empty?)
                                       ["UPDATE OR ROLLBACK"
                                        table-name'
                                        "SET"
                                        set-clause
                                        where])))))

(defn- generate-select-base-sql
  "From a table data-definition, create a SELECT query that gets all of the
  columns. Note, a where clause can be appended on this to limit search."
  [{:keys [table-name columns] :as data-definition}]
  (let [table-name' (keyword->db-name table-name)]
    (clojure.string/join " " ["SELECT" "*" "FROM" table-name'])))

(defn- generate-delete-base-sql
  "From a table data-definition, create a DELETE query that will delete allow
  records in the table. Note, a where clause can be appended on this to limit
  it's scope."
  [{:keys [table-name columns] :as data-definition}]
  (let [table-name' (keyword->db-name table-name)]
    (clojure.string/join " " ["DELETE" "FROM" table-name'])))

(defn- generate-drop-sql
  "From a table data-definition, create a DROP query that will drop the table
  from the database."
  [{:keys [table-name] :as data-definition}]
  (let [table-name' (keyword->db-name table-name)]
    (str "DROP TABLE IF EXISTS " table-name')))

(defn- to-sql
  "Use data-definition to translate the values from map m to a new map that
  can be used to save in sql"
  [{:keys [columns] :as data-definition} m]
  (let [valid-keys (set (map :name columns))
        dd-to-sql-map (reduce (fn [acc {:keys [name type to-sql] :as cur}]
                                (let [acc (assoc acc name (to-sql-fn cur))
                                      acc (if (not (nil? to-sql))
                                            (assoc acc name to-sql)
                                            acc)]
                                  acc))
                       {}
                       columns)
        valid-map-entries (filter (fn [[k v]] (valid-keys k)) (seq m))]
    (reduce (fn [acc [k v]]
              (let [value
                    (if-let [to-sql-fn (get dd-to-sql-map k)]
                      (to-sql-fn v)
                      v)]
                (assoc acc k value)))
            {}
            valid-map-entries)))

(defn- from-sql
  [data-definition cursor]
  (let [result (cursor->hash-map data-definition cursor)]
    result))

(defn data-definition->db-context
  "Using the table data-definition, create a db-context that allows easy CREATE,
  INSERT, UPDATE, DELETE, and DROP operations to be performed through Clojure.

  NOTE: Some helpers are provided to allow you to view the queries that would
  be generated, given arguments, without actually running the queries. This is
  helpful for debugging and design."
  [{:keys [columns] :as data-definition} {:keys [connection-string driver-string]
                                          :or {connection-string "jdbc:sqlite:test.db"
                                               driver-string "org.sqlite.JDBC"}}]
  (let [create-sql (generate-create-sql data-definition)
        insert-format-sql (generate-insert-format-sql data-definition)
        insert-query (fn [m]
                       (let [to-sql-map (to-sql data-definition m)
                             args (reduce (fn [acc {:keys [name] :as col}]
                                            (conj acc (to-sql-map name)))
                                          []
                                          (get-non-generated-columns columns))
                             format-args (cons insert-format-sql args) ]
                         (apply format  format-args)))
        
        update-query (generate-update-base-sql data-definition)
        
        select-base-query (generate-select-base-sql data-definition)
        select-query (fn [m]
                       (let [where-clause (get-where-clause m columns data-definition)]
                         (if (>= (count where-clause) 1)
                           (clojure.string/join " "
                                                [select-base-query
                                                 "WHERE"
                                                 where-clause])
                           select-base-query)))
        delete-base-query (generate-delete-base-sql data-definition)
        delete-query (fn [m]
                       (let [where-clause (get-where-clause m columns data-definition)]
                         (if (> (count where-clause) 1)
                           (clojure.string/join " "
                                                [delete-base-query
                                                 "WHERE"
                                                 where-clause])
                           delete-base-query)))
        result-set-row-parser (partial from-sql data-definition)
        drop-query (generate-drop-sql data-definition)
        db-execute (fn [m]
                     (jdbc/db-execute (merge {:connection-string connection-string
                                              :driver-string driver-string}
                                             m)))]
    {:create-sql create-sql
     :create (fn []
               (db-execute {:sql-query-maps [{:type :create
                                              :sql create-sql}]}))
     :insert-format-sql insert-format-sql
     :insert-query insert-query
     :insert (fn [m]
               (let [iq (insert-query m)]
                 (db-execute {:sql-query-maps [{:type :insert
                                                :sql iq}]})))
     :result-set-row-parser result-set-row-parser
     :update-query update-query
     :update (fn [set-m where-m]
               (let [uq (update-query set-m where-m)]
                 (db-execute {:sql-query-maps [{:type :update
                                                :sql uq}]})))
     :select-base-query select-base-query
     :select-query select-query
     :select (fn [m]
               (let [sq (select-query m)]
                 (db-execute {:sql-query-maps [{:type :select
                                                :sql sq
                                                :cursor-to-result result-set-row-parser}]})))
     :delete-base-query delete-base-query
     :delete-query delete-query
     :delete (fn [m]
               (let [dq (delete-query m)]
                 (db-execute {:sql-query-maps [{:type :delete
                                                :sql dq}]})))
     :drop-query drop-query
     :drop (fn []
             (db-execute {:sql-query-maps [{:type :drop
                                            :sql drop-query}]}))}))

(defprotocol IUnderlyingContext
  (get-underlying-context [this]
    "Get the underlying context associated with an abstraction"))

(defn context
  "Creates a database table context that provides basic CRUD operations"
  ([data-definition]
   (context
       data-definition
       {:connection-string "jdbc:sqlite:test.db"
        :driver-string "org.sqlite.JDBC"}))
  ([data-definition {:keys [connection-string driver-string] :as config}]
   (let [ctx (data-definition->db-context data-definition config)]
     (reify
       IUnderlyingContext
       (get-underlying-context [this] ctx)
       db/Table
       (create! [this]
         ((:create ctx)))
       (insert! [this m]
         ((:insert ctx) m))
       (update! [this sm wm]
         ((:update ctx) sm wm))
       (delete! [this m]
         ((:delete ctx) m))
       (drop! [this]
         ((:drop ctx)))
       (select [this m]
         (-> ((:select ctx) m)
             first
             :result))))))
