(ns ^:no-doc datascript.impl.sorted-set.arrays
    (:refer-clojure :exclude
                    [make-array into-array array amap aget aset alength array? aclone])
    #?(:cljr (:import [System Array])))

#?(:cljr
   (defn make-array ^{:tag "System.Object[]"} [size]
       (clojure.core/make-array System.Object size)))

#?(:cljr
   (defn into-array ^{:tag "System.Object[]"} [aseq]
       (clojure.core/into-array System.Object aseq)))

#?(:cljr
   (defmacro aget [arr i]
       `(clojure.lang.RT/aget ~(vary-meta arr assoc :tag "System.Object") (int ~i))))

#?(:cljr
   (defmacro alength [arr]
       `(clojure.lang.RT/alength ~(vary-meta arr assoc :tag "System.Object"))))

#?(:cljr
   (defmacro aset [arr i v]
       `(clojure.lang.RT/aset ~(vary-meta arr assoc :tag "System.Object") (int ~i) ~v)))

#?(:cljr
   (defmacro array [& args]
       (let [len (count args)]
           (if (zero? len)
               'clojure.lang.RT/EMPTY_ARRAY
               `(let [arr# (clojure.core/make-array System.Object ~len)]
                 (doto ^{:tag "System.Object"} arr#
                       ~@(map #(list 'aset % (nth args %)) (range len))))))))

#?(:cljr
   (defmacro acopy [from from-start from-end to to-start]
       `(let [l# (- ~from-end ~from-start)]
         (when (pos? l#)
             (Array/Copy ~from ~from-start ~to ~to-start l#)))))

#?(:cljr
   (defn aclone [from]
       (let [length (alength from)
             to     (make-array length)]
           (acopy from 0 length to 0)
           to)))

#?(:cljr
   (defn aconcat [a b]
       (let [al  (alength a)
             bl  (alength b)
             res (make-array (+ al bl))]
           (.CopyTo a res 0)
           (.CopyTo b res al)
           res)))

#?(:cljr
   (defn amap
       ([f arr]
        (amap f System.Object arr))
       ([f type arr]
        (let [res (clojure.core/make-array type (alength arr))]
            (dotimes [i (alength arr)]
                (aset res i (f (aget arr i))))
            res))))

#?(:cljr
   (defn asort [arr cmp]
       (doto arr (Array/Sort cmp))))

#?(:cljr
   (defn array? [^Object x]
       (instance? System.Array x)))

#?(:cljr
   (defmacro alast [arr]
       `(let [arr# ~arr]
         (aget arr# (dec (alength arr#))))))

#?(:cljr
   (defmacro half [x]
       `(unsigned-bit-shift-right ~x 1)))

#?(:cljr
   (def array-type
       (memoize
        (fn [type]
            (clojure.core/type (clojure.core/make-array type 0))))))
