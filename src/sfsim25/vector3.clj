(ns sfsim25.vector3
  (:refer-clojure :exclude [+ -])
  (:require [clojure.core :as c]))

(set! *unchecked-math* true)

(deftype Vector3 [^double x ^double y ^double z]
  Object
  (equals [this other] (and (instance? Vector3 other) (= x (.x other)) (= y (.y other)) (= z (.z other))))
  (toString [this] (str "(vector3 " x \space y \space z ")")))

(set! *warn-on-reflection* true)

(defn vector3 ^Vector3 [^double x ^double y ^double z]
  "Construct a 3D vector"
  (Vector3. x y z))

(defn x ^double [^Vector3 v] (.x v))
(defn y ^double [^Vector3 v] (.y v))
(defn z ^double [^Vector3 v] (.z v))

(defn + ^Vector3 [^Vector3 a ^Vector3 b]
  "Add two 3D vectors"
  (Vector3. (c/+ (.x a) (.x b)) (c/+ (.y a) (.y b)) (c/+ (.z a) (.z b))))

(defn - ^Vector3 [^Vector3 a ^Vector3 b]
  "Add two 3D vectors"
  (Vector3. (c/- (.x a) (.x b)) (c/- (.y a) (.y b)) (c/- (.z a) (.z b))))

(defn norm2 ^double [^Vector3 v]
  "Squared norm of vector"
  (c/+ (* (.x v) (.x v)) (* (.y v) (.y v)) (* (.z v) (.z v))))

(defn norm ^double [^Vector3 v]
  "Norm of vector"
  (Math/sqrt (norm2 v)))

(defn cross-product ^Vector3 [^Vector3 a ^Vector3 b]
  "Cross-product of two vectors"
  (Vector3. (c/- (* (.y a) (.z b)) (* (.z a) (.y b)))
            (c/- (* (.z a) (.x b)) (* (.x a) (.z b)))
            (c/- (* (.x a) (.y b)) (* (.y a) (.x b)))))

(set! *warn-on-reflection* false)
(set! *unchecked-math* false)
