(ns sfsim25.vector3-test
  (:refer-clojure :exclude [+ -])
  (:require [clojure.test :refer :all]
            [sfsim25.vector3 :refer :all]))

(deftest display-test
  (testing "Display 3D vector"
    (is (= "(vector3 2.0 3.0 5.0)" (str (vector3 2 3 5))))))

(deftest component-test
  (testing "Get components of 3D vector"
    (is (= 2.0 (x (vector3 2 3 5))))
    (is (= 3.0 (y (vector3 2 3 5))))
    (is (= 5.0 (z (vector3 2 3 5))))))

(deftest norm-test
  (testing "Norm of 3D vector"
    (is (= 1.0 (norm (vector3 0.36 0.48 0.8))))))

(deftest add-test
  (testing "Add two vectors"
    (is (= (vector3 5 8 12) (+ (vector3 2 3 5) (vector3 3 5 7))))))

(deftest subtract-test
  (testing "Subtract two vectors"
    (is (= (vector3 2 3 5) (- (vector3 5 8 12) (vector3 3 5 7))))))

(deftest cross-product-test
  (testing "Cross product of two vectors"
    (is (= (vector3 0 0 1) (cross-product (vector3 1 0 0) (vector3 0 1 0))))))
