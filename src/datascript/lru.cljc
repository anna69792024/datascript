(ns ^:no-doc datascript.lru
    #?(:cljr (:use [datascript.impl.core])))

(declare assoc-lru cleanup-lru)

#?(:cljr
   (extend-protocol ILookup
       clojure.lang.PersistentArrayMap
       (-lookup [key-value k] (.valAt key-value k))
       (-lookup [key-value k nf] (.valAt key-value k nf))

       clojure.lang.PersistentHashMap
       (-lookup [key-value k] (.valAt key-value k))
       (-lookup [key-value k nf] (.valAt key-value k nf)) 
       ))
    
#?(:cljs
    (deftype LRU [key-value gen-key key-gen gen limit]
      IAssociative
      (-assoc [this k v] (assoc-lru this k v))
      (-contains-key? [_ k] (-contains-key? key-value k))
      ILookup
      (-lookup [_ k]    (-lookup key-value k nil))
      (-lookup [_ k nf] (-lookup key-value k nf))
      IPrintWithWriter
      (-pr-writer [_ writer opts]
                  (-pr-writer key-value writer opts)))
   :clj
    (deftype LRU [^clojure.lang.Associative key-value gen-key key-gen gen limit]
      clojure.lang.ILookup
      (valAt [_ k]           (.valAt key-value k))
      (valAt [_ k not-found] (.valAt key-value k not-found))
      clojure.lang.Associative
      (containsKey [_ k] (.containsKey key-value k))
      (entryAt [_ k]     (.entryAt key-value k))
      (assoc [this k v]  (assoc-lru this k v)))
   :cljr
    (deftype LRU [key-value gen-key key-gen gen limit]
        IAssociative
        (-assoc [this k v] (assoc-lru this k v))
        (-contains-key? [_ k] (-contains-key? key-value k))
        ILookup
        (-lookup [_ k]    (-lookup key-value k nil))
        (-lookup [_ k nf] (-lookup key-value k nf))
        clojure.lang.Associative
        (containsKey [this k] (-contains-key? this k))
        (assoc [this k v]  (-assoc this k v))
        clojure.lang.ILookup
        (valAt [this k]           (-lookup this k))
        (valAt [this k not-found] (-lookup this k not-found))))

(defn assoc-lru [^LRU lru k v]
  (let [key-value (.-key-value lru)
        gen-key   (.-gen-key lru)
        key-gen   (.-key-gen lru)
        gen       (.-gen lru)
        limit     (.-limit lru)]
    (if-let [g (key-gen k nil)]
      (->LRU key-value
             (-> gen-key
                 (dissoc g)
                 (assoc gen k))
             (assoc key-gen k gen)
             (inc gen)
             limit)
      (cleanup-lru
        (->LRU (assoc key-value k v)
               (assoc gen-key gen k)
               (assoc key-gen k gen)
               (inc gen)
               limit)))))

(defn cleanup-lru [^LRU lru]
  (if (> (count (.-key-value lru)) (.-limit lru))
    (let [key-value (.-key-value lru)
          gen-key   (.-gen-key lru)
          key-gen   (.-key-gen lru)
          gen       (.-gen lru)
          limit     (.-limit lru)
          [g k]     (first gen-key)]
      (->LRU (dissoc key-value k)
             (dissoc gen-key g)
             (dissoc key-gen k)
             gen
             limit))
    lru))

(defn lru [limit]
  (->LRU {} (sorted-map) {} 0 limit))

