(ns sql-crud.core-test
  (:require [clojure.test :refer :all]
            [sql-crud.core :refer :all]
            [sql-crud.database :refer [create! insert! update! delete! select drop!]]
            [sql-crud.context.sql-gen :as sql-gen]))

(def type-data-definition
  {:table-name :data-type-tests
   :columns [{:name :id
              :type :int
              :constraints ["PRIMARY KEY"
                           "AUTOINCREMENT"]
              :db-generated true}
             {:name :integer
              :type :int}
             {:name :keyword
              :type :keyword}
             {:name :string
              :type :string}
             {:name :boolean
              :type :boolean}]})

(deftest type-tests
  (testing "Test that selecting by types behaves as expected"
    (let [ctx (sql-gen/context type-data-definition)
          test-records [{:integer 1 :keyword :kw :string "string" :boolean true}
                        {:integer 2 :keyword :kw2 :string "string2" :boolean false}]
          _ (create! ctx)
          _ (doall (map #(insert! ctx %) test-records))
          r1 (first test-records)
          r2 (first (rest test-records))
          q1 (select ctx {:id 1})
          q2 (select ctx {:id 2})
          q3 (select ctx {:keyword :kw})
          q4 (select ctx {:string "string"})
          q5 (select ctx {:boolean true})
          q6 (select ctx {:boolean false})
          first-result #(dissoc (first %) :id)
          _ (delete! ctx {})
          _ (drop! ctx)]
      (is (= (first-result q1) r1))
      (is (= (first-result q2) r2))
      (is (= (first-result q3) r1))
      (is (= (first-result q4) r1))
      (is (= (first-result q5) r1))
      (is (= (first-result q6) r2)))))

(deftest update-tests
  (testing "Test that update behavior works as expected"
    (let [ctx (sql-gen/context type-data-definition)
          test-record {:integer 1 :keyword :kw :string "string" :boolean true}
          _ (create! ctx)
          _ (insert! ctx test-record)

          ; Update :keyword
          q1 (select ctx {:keyword :kw})
          _ (update! ctx {:keyword :kw2} {:keyword :kw})
          q2 (select ctx {:keyword :kw2})

          ; Update :boolean
          q3 (select ctx {:boolean true})
          _ (update! ctx {:boolean false} {:kw :kw2})
          q4 (select ctx {:boolean false})

          ; Update :string
          q5 (select ctx {:string "string"})
          _ (update! ctx {:string "new-string"} {:string "string"})
          q6 (select ctx {:string "new-string"})
          
          ; Expected results
          kw-r (assoc test-record :keyword :kw2)
          b-r (assoc kw-r :boolean false)
          s-r (assoc b-r :string "new-string")
          
          first-result #(dissoc (first %) :id)
          _ (delete! ctx {})
          _ (drop! ctx)]
      (is (= (first-result q1) test-record))
      (is (= (first-result q2) kw-r))
      (is (= (first-result q3) kw-r))
      (is (= (first-result q4) b-r))
      (is (= (first-result q5) b-r))
      (is (= (first-result q6) s-r)))))
