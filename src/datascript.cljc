(ns datascript
  (:refer-clojure :exclude [filter])
  (:require
   [datascript.core :as dc #?@(:cljs [:refer [FilteredDB]])]
   [datascript.pull-api :as dp]
   [datascript.query :as dq]
   [datascript.impl.entity :as de]
   [datascript.btset :as btset])
  #?(:clj (:import [datascript.core FilteredDB])))

;; SUMMING UP

(def  q dq/q)
(def  entity de/entity)
(defn entity-db [entity] (.-db entity))

(def  datom dc/datom)

(def  pull dp/pull)
(def  pull-many dp/pull-many)
(def  touch de/touch)

(def  empty-db dc/empty-db)
(def  init-db dc/init-db)

(def  datom? dc/datom?)
(def  db? dc/db?)

(def ^:const tx0 dc/tx0)

(defn is-filtered [x]
  (instance? FilteredDB x))

(defn filter [db pred]
  (if (is-filtered db)
    (let [u (.-unfiltered-db db)]
      (FilteredDB. u #(and (pred u %) ((.-pred db) %)) #?(:clj (atom nil))))
    (FilteredDB. db #(pred db %) #?(:clj (atom nil)))))

(defn with [db tx-data & [tx-meta]]
  (if (is-filtered db)
    (throw (ex-info "Filtered DB cannot be modified" {:error :transaction/filtered}))
    (dc/transact-tx-data (dc/map->TxReport
                           { :db-before db
                             :db-after  db
                             :tx-data   []
                             :tempids   {}
                             :tx-meta   tx-meta}) tx-data)))

(defn db-with [db tx-data]
  (:db-after (with db tx-data)))

(defn datoms [db index & cs]
  (dc/-datoms db index cs))

(defn seek-datoms [db index & cs]
  (dc/-seek-datoms db index cs))

(def index-range dc/-index-range)

(def entid dc/entid)

;; Conn

(defn create-conn [& [schema]]
  (atom (empty-db schema)
        :meta { :listeners  (atom {}) }))

(defn -transact! [conn tx-data tx-meta]
  (let [report (atom nil)]
    (swap! conn (fn [db]
                  (let [r (with db tx-data tx-meta)]
                    (reset! report r)
                    (:db-after r))))
    @report))

(defn transact! [conn tx-data & [tx-meta]]
  (let [report (-transact! conn tx-data tx-meta)]
    (doseq [[_ callback] @(:listeners (meta conn))]
      (callback report))
    report))
           
(defn listen!
  ([conn callback] (listen! conn (rand) callback))
  ([conn key callback]
     (swap! (:listeners (meta conn)) assoc key callback)
     key))

(defn unlisten! [conn key]
  (swap! (:listeners (meta conn)) dissoc key))


;; ----------------------------------------------------------------------------
;; define data-readers to be made available to EDN readers. in CLJS
;; they're magically available. in CLJ, data_readers.clj may or may
;; not work, but you can always simply do
;;
;;  (clojure.edn/read-string {:readers datascript/data-readers} "...")
;;

(def data-readers {'datascript/Datom dc/datom-from-reader
                   'datascript/DB    dc/db-from-reader})

#?(:cljs
   (doseq [[tag cb] data-readers] (cljs.reader/register-tag-parser! tag cb)))


;; Datomic compatibility layer

(def last-tempid (atom -1000000))

(defn tempid
  ([part]
    (if (= part :db.part/tx)
      :db/current-tx
      (swap! last-tempid dec)))
  ([part x]
    (if (= part :db.part/tx)
      :db/current-tx
      x)))

(defn resolve-tempid [_db tempids tempid]
  (get tempids tempid))

(def db deref)

(defn transact [conn tx-data & [tx-meta]]
  (let [res (transact! conn tx-data tx-meta)]
    #?(:cljs
       (reify
         IDeref
         (-deref [_] res)
         IDerefWithTimeout
         (-deref-with-timeout [_ _ _] res)
         IPending
         (-realized? [_] true))
       :clj
       (reify
         clojure.lang.IDeref
         (deref [_] res)
         clojure.lang.IBlockingDeref
         (deref [_ _ _] res)
         clojure.lang.IPending
         (isRealized [_] true)))))

;; ersatz future without proper blocking
#?(:cljs
   (defn- future-call [f]
     (let [res      (atom nil)
           realized (atom false)]
       (js/setTimeout #(do (reset! res (f)) (reset! realized true)) 0)
       (reify
         IDeref
         (-deref [_] @res)
         IDerefWithTimeout
         (-deref-with-timeout [_ _ timeout-val] (if @realized @res timeout-val))
         IPending
         (-realized? [_] @realized)))))

(defn transact-async [conn tx-data & [tx-meta]]
  (future-call #(transact! conn tx-data tx-meta)))

(defn- rand-bits [pow]
  (rand-int (bit-shift-left 1 pow)))

(defn- to-hex-string [n l]
  (let [s (.toString n 16)
        c (count s)]
    (cond
      (> c l) (subs s 0 l)
      (< c l) (str (apply str (repeat (- l c) "0")) s)
      :else   s)))

(defn squuid []
  #?(:clj
      (let [uuid     (java.util.UUID/randomUUID)
            time     (int (/ (System/currentTimeMillis) 1000))
            high     (.getMostSignificantBits uuid)
            low      (.getLeastSignificantBits uuid)
            new-high (bit-or (bit-and high 0x00000000FFFFFFFF)
                             (bit-shift-left time 32)) ]
        (java.util.UUID. new-high low))
     :cljs
       (UUID.
         (str
               (-> (js/Date.) (.getTime) (/ 1000) (Math/round) (to-hex-string 8))
           "-" (-> (rand-bits 16) (to-hex-string 4))
           "-" (-> (rand-bits 16) (bit-and 0x0FFF) (bit-or 0x4000) (to-hex-string 4))
           "-" (-> (rand-bits 16) (bit-and 0x3FFF) (bit-or 0x8000) (to-hex-string 4))
           "-" (-> (rand-bits 16) (to-hex-string 4))
               (-> (rand-bits 16) (to-hex-string 4))
               (-> (rand-bits 16) (to-hex-string 4))))))

(defn squuid-time-millis [uuid]
  (-> (subs (:uuid uuid) 0 8)
      #?(:clj  (Integer/parseInt 16)
         :cljs (js/parseInt 16))
      (* 1000)))
