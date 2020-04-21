(ns datascript.test.impl.sorted-set.arrays
    (:require
        [clojure.test :as t
         :refer           [is are deftest testing]]
        [datascript.impl.sorted-set.arrays :as arrays]))

(deftest arrays-all-tests
    (testing "read only test"
             (is
              (= [nil nil nil]
                 (arrays/make-array 3)))

             (let [arr (arrays/into-array [1 2 3])]
                 (is (= [1 2 3] arr))
                 (is (= 3 (arrays/aget arr 2)))
                 (is (= 3 (arrays/alength arr)))
                 (is (= [] (arrays/array)))
                 (is (= [1 2 3] (arrays/array 1 2 3)))

                 (let [to       (arrays/make-array 3)
                       copy-ret (arrays/acopy arr 0 2 to 1)]
                     (is (= nil copy-ret))
                     (is (= [nil 1 2] to)))

                 (is (= [1 2 3] (arrays/aclone arr)))
                 (is (= [1 2 3 1 2 3] (arrays/aconcat arr arr)))
                 (is (= ["1" "2" "3"] (arrays/amap str System.String arr)))
                 (is (= true (arrays/array? arr)))
                 (is (= 3 (arrays/alast arr))))

             (is (= 2 (arrays/half 5))))

    (testing "modify test"

             ;; aset test
             (let [arr      (arrays/into-array [11 12 13])
                   aset-ret (arrays/aset arr 2 4)]
                 (is (= 4 aset-ret))
                 (is (= [11 12 4] arr)))

             ;; asort test
             (let [arr      (arrays/into-array [11 12 13])]
                 (is (= [13 12 11] (arrays/asort arr >))))))
