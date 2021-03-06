(ns specql.impl.update
  "Implementation of update!"
  (:require [specql.impl.util :refer :all]
            [specql.impl.registry :as registry]
            [specql.impl.where :as where]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]))

(defn update! [db table record where]
  (assert-table table)
  (let [table-info-registry @registry/table-info-registry
        {table-name :name columns :columns :as tbl} (table-info-registry table)
        record (transform-to-sql table-info-registry tbl record)
        alias-fn (gen-alias)
        alias (alias-fn table-name)

        [column-names value-names value-parameters :as cols]
        (columns-and-values-to-set table-info-registry table record)

        [where-clause where-parameters]
        (where/sql-where table-info-registry
                         #(when (= % [])
                            {:table table
                             :alias alias})
                         where)

        sql (str "UPDATE " (q table-name) " AS " alias
                 " SET " (str/join ","
                                   (map (fn [column-name value-name]
                                          (str (q column-name) "=" value-name))
                                        column-names value-names))
                 " WHERE " where-clause)
        sql-and-params (into [sql] (concat value-parameters
                                           where-parameters))]
    (first (jdbc/execute! db sql-and-params))))
