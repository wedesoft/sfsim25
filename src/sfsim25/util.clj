(ns sfsim25.util
  (:require [clojure.java.io :as io]
            [sfsim25.rgb :refer (rgb)])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.io ByteArrayOutputStream]
           [magick MagickImage ImageInfo ColorspaceType]
           [sfsim25.rgb RGB]))

(defn slurp-bytes
  "Read bytes from a file"
  [file-name]
  (with-open [in  (io/input-stream file-name)
              out (ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn slurp-shorts
  "Read short integers from a file"
  [file-name]
  (let [byte-data    (slurp-bytes file-name)
        n            (.count (seq byte-data))
        short-buffer (-> byte-data ByteBuffer/wrap (.order ByteOrder/LITTLE_ENDIAN) .asShortBuffer)
        result       (short-array (/ n 2))]
    (.get short-buffer result)
    result))

(defn slurp-floats
  "Read floating point numbers from a file"
  [file-name]
  (let [byte-data    (slurp-bytes file-name)
        n            (.count (seq byte-data))
        float-buffer (-> byte-data ByteBuffer/wrap (.order ByteOrder/LITTLE_ENDIAN) .asFloatBuffer)
        result       (float-array (/ n 4))]
    (.get float-buffer result)
    result))

(defn spit-bytes
  "Write bytes to a file"
  [file-name byte-data]
  (with-open [out (io/output-stream file-name)]
    (.write out byte-data)))

(defn spit-shorts
  "Write short integers to a file"
  [file-name short-data]
  (let [n           (count short-data)
        byte-buffer (.order (ByteBuffer/allocate (* n 2)) ByteOrder/LITTLE_ENDIAN)]
    (.put (.asShortBuffer byte-buffer) short-data)
    (spit-bytes file-name (.array byte-buffer))))

(defn spit-floats
  "Write floating point numbers to a file"
  [file-name float-data]
  (let [n           (count float-data)
        byte-buffer (.order (ByteBuffer/allocate (* n 4)) ByteOrder/LITTLE_ENDIAN)]
    (.put (.asFloatBuffer byte-buffer) float-data)
    (spit-bytes file-name (.array byte-buffer))))

(defn tile-path
  "Determine file path of map tile"
  [prefix level y x suffix]
  (str prefix \/ level \/ x \/ y suffix))

(defn tile-dir
  "Determine directory name of map tile"
  [prefix level x]
  (str prefix \/ level \/ x))

(defn cube-path
  "Determine file path of cube tile"
  [prefix face level y x suffix]
  (str prefix \/ face \/ level \/ x \/ y suffix))

(defn cube-dir
  "Determine directory name of cube tile"
  [prefix face level x]
  (str prefix \/ face \/ level \/ x))

(defn sinc
  "sin(x) / x function"
  ^double [^double x]
  (if (zero? x) 1.0 (/ (Math/sin x) x)))

(defn spit-image
  "Save an RGB image"
  [file-name width height data]
  (let [info  (ImageInfo.)
        image (MagickImage.)]
    (.constituteImage image width height "RGB" data)
    (.setSize info (str width \x height))
    (.setDepth info 8)
    (.setColorspace info ColorspaceType/RGBColorspace)
    (.setFileName image file-name)
    (.writeImage image info)
    image))

(defn slurp-image [file-name]
  "Load an RGB image"
  (let [info      (ImageInfo. file-name)
        image     (MagickImage. info)
        dimension (.getDimension image)]
    (.setDepth info 8)
    (.setColorspace info ColorspaceType/RGBColorspace)
    (.setMagick info "RGB")
    [(.width dimension) (.height dimension) (.imageToBlob image info)]))

(defn byte->ubyte [b]
  "Convert byte to unsigned byte"
  (if (>= b 0) b (+ b 256)))

(defn ubyte->byte ^long [^long u]
  "Convert unsigned byte to byte"
  (if (<= u 127) u (- u 256)))

(defn get-pixel ^RGB [[width height data] ^long y ^long x]
  (let [offset (* 3 (+ (* width y) x))]
    (rgb (byte->ubyte (aget data offset)) (byte->ubyte (aget data (inc offset))) (byte->ubyte (aget data (inc (inc offset)))))))

(defn set-pixel! [[width height data] ^long y ^long x ^RGB c]
  (let [offset (* 3 (+ (* width y) x))]
    (aset-byte data offset (ubyte->byte (.r c)))
    (aset-byte data (inc offset) (ubyte->byte (.g c)))
    (aset-byte data (inc (inc offset)) (ubyte->byte (.b c)))))
