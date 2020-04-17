(ns datascript.impl.sorted-set
    (:require [#?(:cljr clojure.core) :as c])
    (:import [clojure.lang PersistentTreeSet]))


(def sorted-set-by c/sorted-set-by)
(def sorted-set c/sorted-set)
(def conj c/conj)
(def disj c/disj)

(defn from-sorted-array
    [array])

(defprotocol IPersistentSortedSet
    "有序集合接口。
    slice   对有序集合中大小在 [from to] 之间的元素顺序切片。
    rslice  对有序集合中大小在 [to from] 之间的元素逆序切片。"
    (slice [this from to] [this from to cmp])
    (rslice [this from to] [this from to cmp]))

(extend-type
    PersistentTreeSet
    IPersistentSortedSet
    (slice
     ([this from to] (slice this from to 
                            (fn[x y] 
                                (let [cmp (.comparator ^PersistentTreeSet this)]
                                    (.Compare ^IComparer cmp  x y)))))
     ([this from to cmp]
         (let [items (filter
                      (fn [v]
                          (and (>= 0 (cmp from v))
                               (>= 0 (cmp v to))))
                      this)] 
             (when-not (empty? items) 
                 (seq (apply sorted-set-by cmp items))))))
    (rslice
     ([this from to] (rslice this from to c/compare))
     ([this from to cmp] nil)))
