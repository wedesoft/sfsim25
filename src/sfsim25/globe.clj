(ns sfsim25.globe
  (:require [clojure.core.memoize :as m]
            [sfsim25.cubemap :refer (cube-map longitude latitude map-pixels-x map-pixels-y scale-point cube-coordinate
                                     offset-longitude offset-latitude elevation-pixel world-map-pixel)]
            [sfsim25.util :refer (tile-path slurp-image spit-image slurp-shorts spit-bytes spit-floats set-pixel!
                                  cube-dir cube-path ubyte->byte)]
            [sfsim25.rgb :as r]
            [sfsim25.vector3 :as v])
  (:import [java.io File]
           [sfsim25.vector3 Vector3])
  (:gen-class))

(set! *unchecked-math* true)

(defn interpolate
  "Interpolate elevation or RGB values"
  [in-level width point get-pixel p+ p*]
  (let [lon                     (longitude point)
        lat                     (latitude point)
        [dx0 dx1 xfrac0 xfrac1] (map-pixels-x lon width in-level)
        [dy0 dy1 yfrac0 yfrac1] (map-pixels-y lat width in-level)
        v0                      (get-pixel dy0 dx0 in-level width)
        v1                      (get-pixel dy0 dx1 in-level width)
        v2                      (get-pixel dy1 dx0 in-level width)
        v3                      (get-pixel dy1 dx1 in-level width)]
    (p+ (p* (* yfrac0 xfrac0) v0) (p* (* yfrac0 xfrac1) v1) (p* (* yfrac1 xfrac0) v2) (p* (* yfrac1 xfrac1) v3))))

(defn color-for-point
  "Compute interpolated RGB value for a point on the world"
  [^long in-level ^long width ^Vector3 point]
  (interpolate in-level width point world-map-pixel r/+ r/*))

(defn elevation-for-point
  "Compute interpolated elevation value for a point on the world"
  [^long in-level ^long width ^Vector3 point]
  (interpolate in-level width point elevation-pixel + *))

(defn elevated-point
  "Get elevated 3D point for a point on the world"
  [in-level width p radius1 radius2]
  (let [height (elevation-for-point in-level width p)]
    (scale-point p (+ radius1 (max 0 height)) (+ radius2 (max 0 height)))))

(defn surrounding-points
  "Compute local point cloud consisting of nine points"
  [p in-level out-level width tilesize radius1 radius2]
  (let [d1 (offset-longitude p out-level tilesize)
        d2 (offset-latitude p out-level tilesize radius1 radius2)]
    (for [dj (range -1 2) di (range -1 2)]
        (let [ps (v/+ p (v/* dj d2) (v/* di d1))]
          (elevated-point in-level width ps radius1 radius2)))))

(defn normal-for-point
  "Estimate normal vector for a point on the world"
  [p in-level out-level width tilesize radius1 radius2]
  (let [pc (surrounding-points p in-level out-level width tilesize radius1 radius2)
        sx [-0.25  0    0.25, -0.5 0 0.5, -0.25 0   0.25]
        sy [-0.25 -0.5 -0.25,  0   0 0  ,  0.25 0.5 0.25]
        n1 (apply v/+ (map v/* sx pc))
        n2 (apply v/+ (map v/* sy pc))]
    (v/normalize (v/cross-product n1 n2))))

(defn water-for-point
  "Decide whether point is on land or on water"
  [^long in-level ^long width ^Vector3 point]
  (let [height (elevation-for-point in-level width point)]
    (if (< height 0) (int (/ (* height 255) -500)) 0)))

(defn set-vertex-data!
  "Write 3D point, texture coordinates, and 3D normal to vertex data array"
  [vertex tilesize u v point u-scaled v-scaled normal]
  (let [offset (* 8 (+ (* tilesize v) u))]
    (aset-float vertex (+ offset 0) (.x ^Vector3 point))
    (aset-float vertex (+ offset 1) (.y ^Vector3 point))
    (aset-float vertex (+ offset 2) (.z ^Vector3 point))
    (aset-float vertex (+ offset 3) u-scaled)
    (aset-float vertex (+ offset 4) v-scaled)
    (aset-float vertex (+ offset 5) (.x ^Vector3 normal))
    (aset-float vertex (+ offset 6) (.y ^Vector3 normal))
    (aset-float vertex (+ offset 7) (.z ^Vector3 normal))))

(defn -main
  "Program to generate tiles for cube map"
  [& args]
  (when-not (= (count args) 2)
    (.println *err* "Syntax: lein run-globe [input level] [output level]")
    (System/exit 1))
  (let [in-level  (Integer/parseInt (nth args 0))
        out-level (Integer/parseInt (nth args 1))
        n         (bit-shift-left 1 out-level)
        width     675
        tilesize  33
        radius1   6378000.0
        radius2   6357000.0]
    (doseq [k (range 6) b (range n) a (range n)]
      (let [data   (byte-array (* 3 tilesize tilesize))
            water  (byte-array (* tilesize tilesize))
            vertex (float-array (* 8 tilesize tilesize))
            tile   [tilesize tilesize data]]
        (doseq [v (range tilesize)]
          (let [j (cube-coordinate out-level tilesize b v)]
            (doseq [u (range tilesize)]
              (let [i      (cube-coordinate out-level tilesize a u)
                    p      (v/normalize (cube-map k j i))
                    color  (color-for-point in-level width p)
                    point  (elevated-point in-level width p radius1 radius2)
                    normal (normal-for-point p in-level out-level width tilesize radius1 radius2)]
                (set-pixel! tile v u color)
                (aset-byte water (+ (* tilesize v) u) (ubyte->byte (water-for-point in-level width p)))
                (set-vertex-data! vertex tilesize u v point (float (/ u (dec tilesize))) (float (/ v (dec tilesize))) normal)))))
        (.mkdirs (File. (cube-dir "globe" k out-level a)))
        (spit-image (cube-path "globe" k out-level b a ".png") tilesize tilesize data)
        (spit-bytes (cube-path "globe" k out-level b a ".bin") water)
        (spit-floats (cube-path "globe" k out-level b a ".raw") vertex)
        (println (cube-path "globe" k out-level b a ".*"))))))

(set! *unchecked-math* false)
