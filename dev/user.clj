(ns user
  (:require [sql-crud.context.sql-gen :refer [get-underlying-context] :as sql-gen]
            [sql-crud.database :refer [create! insert! update! delete! select]]))

(def data-type-tests-data-definition
  {:table-name :data-type-tests
   :columns [{:name :id
              :type :int
              :constraints ["PRIMARY KEY"
                           "AUTOINCREMENT"]
              :db-generated true
              }
             {:name :integer
              :type :int
              }
             {:name :keyword
              :type :keyword
              }
             {:name :string
              :type :string
              }
             {:name :boolean
              :type :boolean
              }]})

(def test-ds
  [{:integer 1 :keyword :kw :string "string" :boolean true}
   {:integer 2 :keyword :kw2 :string "string2" :boolean false}])

(def ctx
  (sql-gen/context data-type-tests-data-definition))
