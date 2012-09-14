(ns mosaic.tile
  (import java.io.File)
  (import javax.imageio.ImageIO)
  (import java.awt.image.BufferedImage)
  (import com.mortennobel.imagescaling.ResampleOp)
  (import java.lang.Math))

(defn test-image []
  (ImageIO/read
   (File. "test/resources/image.jpg")))

(defn rescale [x y ^BufferedImage b]
  (. (ResampleOp. x y) (filter b nil)))

(defn grid
  "Divide region 0,0,x,y into grid squares of size n.
   Partial grids squares are omitted. Optional parameter
   s sets the step size to allow for overlapping squares."
  ([n x y] (grid n x y n))
  ([n x y s]
     (for
	 [a (range 0 (- x n -1) s)
	  b (range 0 (- y n -1) s)]
       [a,b,(+ a n),(+ b n)])))

(defn gen-tiles [n ^BufferedImage b]
  "Generate tiles of size n from image b.
   Returns a list of BufferedImages."
  (map #(.getSubimage b (first %) (second %) n n)
       (grid 100 (.getWidth b) (.getHeight b))))

(defn save-image [^BufferedImage b ^String f]
  "Save a BufferedImage to filename f."
  (ImageIO/write b "jpg" (File. f)))

(defn tile-names [^String base]
  "Infinite sequence of tile names."
  (map #(str base % ".jpg") (range)))

(defn save-tiles [tiles ^String d]
  "Save tiles (list of BufferedImages) to directory d."
  (map #(save-image %1 %2) tiles (tile-names d)))

(defn get-samples [^BufferedImage b]
  "Get a flat list of all samples in an image."
  (let [d (.getData b)]
    (for [x (range (.getWidth d))
	  y (range (.getHeight d))
	  z (range (.getNumBands d))]
      (.getSample d x y z))))

(defn delta [^BufferedImage a ^BufferedImage b]
  "Calculates the difference between images a and b."
  (reduce + (map #(Math/abs %)
		 (map - (get-samples a) (get-samples b)))))

(defn sample-tiles [n tiles]
  "Resample tiles to size n by n.
   Output format: {:tile :sample}"
  (for [t tiles] {:tile t :sample (rescale n n t)}))

(defn best-match [n samples ^BufferedImage b]
  "Find the best matching tile to image b."
  (let [s (rescale n n b)]
    (reduce #(if (< (delta s (:sample %1))
		    (delta s (:sample %2)))
	       %1 %2)
	    (first samples)
	    (rest samples))))


(defn match-tiles [coll tx ty n ^BufferedImage b]
  )


(defn floor [n x]
  "Round x down to the nearest multiple of n."
  (int (* n (Math/floor (/ x n)))))
  
(defn image-floor [n ^BufferedImage b]
  "Crop image b down the a width and height which
   is a multiple of n."
  (let [x (.getWidth b)
	y (.getHeight b)]
    (.getSubimage b 0 0 (floor n x) (floor n y))))
    


(defn mosaic [^BufferedImage source
	      ^BufferedImage target
	      tile-size tiles-wide]
  (let []))


; input: <tile_source> <target> <tile_size> <tile_width>
; 1. resize the source image to tile_size * tile_width
; 2. grid out the new source image
; 3. replace the image with a sub image, cropped to height
; 4. for each grid square replace it with the best matching tile

