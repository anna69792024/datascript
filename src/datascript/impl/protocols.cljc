(ns datascript.impl.protocols)

(defprotocol ICloneableProtocol
    "Protocol for cloning a value."
    (^clj -clone [value]
        "Creates a clone of value."))

(defprotocol ILookup
    "Protocol for looking up a value in a data structure."
    (-lookup [o k] [o k not-found]
        "Use k to look up a value in o. If not-found is supplied and k is not
         a valid value that can be used for look up, not-found is returned."))

(defprotocol IPrintWithWriter
    "The old IPrintable protocol's implementation consisted of building a giant
     list of strings to concatenate.  This involved lots of concat calls,
     intermediate vectors, and lazy-seqs, and was very slow in some older JS
     engines.  IPrintWithWriter implements printing via the IWriter protocol, so it
     be implemented efficiently in terms of e.g. a StringBuffer append."
    (-pr-writer [o writer opts]))

(defprotocol IMeta
    "Protocol for accessing the metadata of an object."
    (;^clj-or-nil
        -meta [o]
        "Returns the metadata of object o."))

(defprotocol ICounted
    "Protocol for adding the ability to count a collection in constant time."
    (^number -count [coll]
        "Calculates the count of coll in constant time. Used by cljs.core/count."))

(defprotocol IReversible
    "Protocol for reversing a seq."
    (^clj -rseq [coll]
        "Returns a seq of the items in coll in reversed order."))

(defprotocol IHash
    "Protocol for adding hashing functionality to a type."
    (-hash [o]
        "Returns the hash code of o."))

(defprotocol IEquiv
    "Protocol for adding value comparison functionality to a type."
    (^boolean -equiv [o other]
        "Returns true if o and other are equal, false otherwise."))

(defprotocol IEditableCollection
    "Protocol for collections which can transformed to transients."
    (^clj -as-transient [coll]
        "Returns a new, transient version of the collection, in constant time."))

(defprotocol IEmptyableCollection
    "Protocol for creating an empty collection."
    (-empty [coll]
        "Returns an empty collection of the same category as coll. Used
         by cljs.core/empty."))

(defprotocol ISet
    "Protocol for adding set functionality to a collection."
    (^clj -disjoin [coll v]
        "Returns a new collection of coll that does not contain v."))

(defprotocol IReduce
    "Protocol for seq types that can reduce themselves.
    Called by cljs.core/reduce."
    (-reduce [coll f] [coll f start]
        "f should be a function of 2 arguments. If start is not supplied,
         returns the result of applying f to the first 2 items in coll, then
         applying f to that result and the 3rd item, etc."))

(defprotocol ITransientCollection
    "Protocol for adding basic functionality to transient collections."
    (^clj -conj! [tcoll val]
        "Adds value val to tcoll and returns tcoll.")
    (^clj -persistent! [tcoll]
        "Creates a persistent data structure from tcoll and returns it."))

(defprotocol ISeqable
    "Protocol for adding the ability to a type to be transformed into a sequence."
    (;^clj-or-nil
        -seq [o]
        "Returns a seq of o, or nil if o is empty."))

(defprotocol ITransientSet
    "Protocol for adding set functionality to a transient collection."
    (^clj -disjoin! [tcoll v]
        "Returns tcoll without v."))

(defprotocol IWithMeta
    "Protocol for adding metadata to an object."
    (^clj -with-meta [o meta]
        "Returns a new object with value of o and metadata meta added to it."))

(defprotocol ICollection
    "Protocol for adding to a collection."
    (^clj -conj [coll o]
        "Returns a new collection of coll with o added to it. The new item
         should be added to the most efficient place, e.g.
         (conj [1 2 3 4] 5) => [1 2 3 4 5]
         (conj '(2 3 4 5) 1) => '(1 2 3 4 5)"))

(defprotocol IFn
    "Protocol for adding the ability to invoke an object as a function.
    For example, a vector can also be used to look up a value:
    ([1 2 3 4] 1) => 2"
    (-invoke
        [this]
        [this a]
        [this a b]
        [this a b c]
        [this a b c d]
        [this a b c d e]
        [this a b c d e f]
        [this a b c d e f g]
        [this a b c d e f g h]
        [this a b c d e f g h i]
        [this a b c d e f g h i j]
        [this a b c d e f g h i j k]
        [this a b c d e f g h i j k l]
        [this a b c d e f g h i j k l m]
        [this a b c d e f g h i j k l m n]
        [this a b c d e f g h i j k l m n o]
        [this a b c d e f g h i j k l m n o p]
        [this a b c d e f g h i j k l m n o p q]
        [this a b c d e f g h i j k l m n o p q r]
        [this a b c d e f g h i j k l m n o p q r s]
        ;; clojure.lang.CljCompiler.Ast.ParseException: Can't specify more than 20 parameters
        #_[this a b c d e f g h i j k l m n o p q r s rest]
        #_[this a b c d e f g h i j k l m n o p q r s t rest]))

(defprotocol IChunk
    "Protocol for accessing the items of a chunk."
    (-drop-first [coll]
        "Return a new chunk of coll with the first item removed."))

(defprotocol IIndexed
    "Protocol for collections to provide indexed-based access to their items."
    (-nth [coll n] [coll n not-found]
        "Returns the value at the index n in the collection coll.
         Returns not-found if index n is out of bounds and not-found is supplied."))

(defprotocol INext
    "Protocol for accessing the next items of a collection."
    (;^clj-or-nil
        -next [coll]
        "Returns a new collection of coll without the first item. In contrast to
         rest, it should return nil if there are no more items, e.g.
         (next []) => nil
         (next nil) => nil"))

(defprotocol ISeq
    "Protocol for collections to provide access to their items as sequences."
    (-first [coll]
        "Returns the first item in the collection coll. Used by cljs.core/first.")
    (^clj -rest [coll]
        "Returns a new collection of coll without the first item. It should
         always return a seq, e.g.
         (rest []) => ()
         (rest nil) => ()"))

(defprotocol ISequential
    "Marker interface indicating a persistent collection of sequential items")

(defprotocol IChunkedSeq
    "Protocol for accessing a collection as sequential chunks."
    (-chunked-first [coll]
        "Returns the first chunk in coll.")
    (-chunked-rest [coll]
        "Return a new collection of coll with the first chunk removed."))

(defprotocol IChunkedNext
    "Protocol for accessing the chunks of a collection."
    (-chunked-next [coll]
        "Returns a new collection of coll without the first chunk."))

(defprotocol IWriter
    "Protocol for writing. Currently only implemented by StringBufferWriter."
    (-write [writer s]
        "Writes s with writer and returns the result.")
    (-flush [writer]
        "Flush writer."))

(defprotocol IAssociative
    "Protocol for adding associativity to collections."
    (^boolean -contains-key? [coll k]
        "Returns true if k is a key in coll.")
    #_(-entry-at [coll k])
    (^clj -assoc [coll k v]
        "Returns a new collection of coll with a mapping from key k to
         value v added to it."))
