(ns sql-crud.database)

(defprotocol Table
  "A data and hash-map centric interface to CRUD operations on a database"
  (create! [this]
    "Create a table in the database based on the context")
  (insert! [this m]
    "Insert a record into the table, based on a hash-map into the database")
  (update! [this set-map where-map]
    "Update matching records in the table, setting new values by key in set-map. Isolates target records using where-map")
  (delete! [this where-map]
    "Delete records from the table, choosing which based on matching values to keys in where-map")
  (drop! [this]
    "Drop the table from the database")
  (select [this where-map]
    "Select records from the table, choosing which based on matching values to keys in where-map"))
