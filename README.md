# sql-crud

A Clojure library designed to provide simple CRUD access to a SQL database

## Disclaimer

I made this library to get some intuition into how the other
popular SQL tools in the Clojure space actually do their work. I wanted to 
understand how to interface with `JDBC`, which would help me know what 
questions to ask and features to look for when working with other tools. 
I wanted this to be a very focused library, geared to my own needs and learning.

There are a lot of times where manipulating native Clojure data structures in
memory is nice, and I wanted to easily be able to persist collections of
records (vectors of hash-maps) to disk. This library provides a simple 
interface for this need. It does not attempt to solve problems through
a DSL or through integrations with hand-written queries.

## Usage

The main goal was to allow simple persistence of Clojure hash-maps to a 
relational database.

To start with data, the concept of a `data-definition` is the first thing
to cover. A `data-definition` is just a hash-map that describes the data
that makes up each table. It will serve us to look at an example from the
unit tests.

```clojure
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
```

Here, `:table-name` is given a name through a keyword, which will be *munged*
and used in the database to store the data. The `:columns` key is given an array of hash-maps
that describe the columns in the table. `:name` maps to a keyword that will
be *munged* to name the column. `:type` defines the type of the column.
The current supported types are `:int`, `:string`, `:keyword`, and `:boolean`.
Obviously more are needed, but this library is meant as a proof of concept and
will slowly incorporate more as time goes on. `:db-generated` will allow insert
to work without specifying some columns (these columns will be returned when returned by select). 
A `:to-sql` key allows a custom way to serialize a value to be specified.

The `sql-crud.database` namespace contains a `Table` protocol with operations 
to support basic CRUD operations. 

With the `data-definition` used to describe a table in a relational database,
and the `Table` protocol to define how to access table data, the `sql-crud.context.sql-gen` 
namespace has a `context` function to create a type that allows simple table 
operations to be performed. This context type is used with the `Table` protocol
functions in order to do work with a table.

`(def context (sql-gen/context data-definition))`

The `create!` function takes a context an will create the table in the database.

`(create! context)`

The `insert!` function takes a hash-map representing a record to insert into the
table. The data-definition is used to coerce values.

`(insert! context {:id 1 :name "Lucile"})`

The `update!` function allows you to change records based on a set-map and
where-map argument. set-map is the first argument, used to provide the desired
values. where-map is the second argument, used to select the records to change.

`(update! context {:name "Loose Seal"} {:id 1})`

The `select` function allows you to find records based on a where-map. Each key
in the where-map is AND'ed together, using equality for each value to filter
records. An empty where-map will return all records.

`(select context {:name "Loose Seal"})`

The `delete!` function allows you to remove records based on a where-map. The
where-map behaves the same as in `select`, but matches will be removed from
the database table. An empty where-map will delete all records.

`(delete context {:id 1})`

The `drop!` function allows you to drop a table from the database.

`(drop! context)`

### Running the tests

```clojure
lein test
```

### What It Doesn't Do

Currently, only direct equality is supported in `WHERE` clauses. This 
functionality could be added, but is probably better suited with another
library.

Each supported datatype has to be added in order to properly serialize/deserialize.
There is probably an abstraction (multi-method) that can help extend these
functions arbitrarily, but that is not in place yet.

### Still TODO

The current context implementation works through generating SQL queries. In
doing more research, I realized that `PreparedStatements` are used more
frequently and would ease some of the type serialization/deserialization.
I wanted to provide an alternative context that would use prepared statements.

I also wanted to collect the api into one capable of more complex transactions.
This would also be easier to do by leaning on the `JDBC` code with prepared 
statements. That work may come in the future, given if this proof of concept
becomes more useful in my other work.

## License

Copyright © 2016 Tom Kidd

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
