(ns ddbatom
  (:require [promesa.core :as p]
            [cljs.reader :as reader]
            [cljs.core :refer [IPrintWithWriter IDeref IReset ISwap]]
            ["aws-sdk" :as AWS])
  )

(defn serialize [v]
  (binding [*print-dup*      true
            *print-readably* true]
    (pr-str v)))

(def deserialize
  (memoize reader/read-string))

(def state-item "de.px0.state-item")
(def state-attribute "de.px0.state-attribute")

(deftype DynamoDBTable [ddb table-name state-item state-attribute]
  IPrintWithWriter
  (-pr-writer [o writer _]
    (-write writer (str "#<DynamoDBTable: " table-name ">"))))

(extend-type DynamoDBTable
  IDeref
  (-deref [o]
    (let [params (clj->js {:TableName            table-name
                           :Key                  {:KEY_NAME {:S state-item}}
                           :ProjectionExpression "ATTRIBUTE_NAME"})])
    (p/promise (fn [resolve reject]
                 (.getItem params (fn [err data]
                                    (if err
                                      (reject err)
                                      (resolve
                                       (-> data
                                           (get "Item")
                                           (get state-attribute)
                                           (get "S")
                                           (deserialize)))))))))

  IReset
  (-reset! [o new-value]
    (let [params (clj->js {:TableName table-name
                           :Item      { state-attribute {:S (serialize new-value)}}})]
      (p/promise (fn [resolve reject]
                   (.putItem ddb (fn [err data]
                                   (if err
                                     (reject err)
                                     (resolve (deref o)))))))))

  ISwap
  (-swap!
    ([o f]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval)]
             (reset! o newval)))
    ([o f a]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval a)]
             (reset! o newval)))
    ([o f a b]
     (p/alet [oldval (p/await (deref o))
              newval (f oldval a b)]
             (reset! o newval)))
    ([o f a b xs]
     (p/alet [oldval (p/await (deref o))
              newval (apply f oldval a b xs)]
             (reset! o newval)))))

(defn ddb-atom
  ([aws-config table-name]
   (ddb-atom aws-config table-name "de.px0.state-item" "de.px0.state-attribute"))
  ([aws-config table-name state-item state-attribute]
   (let [aws (.config.update AWS (clj->js aws-config))
         ddb (new .DynamoDB AWS #js {:apiVersion "2012-10-08"})])
   (DynamoDBTable. ddb table-name state-item state-attribute)))



(defn main! []
  (println "hi"))
