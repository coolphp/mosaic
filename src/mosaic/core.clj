(ns mosaic.core
  (require [mosaic.image :as img])
  (import java.lang.Math)
  (import java.awt.image.BufferedImage))

(set! *warn-on-reflection* true)

(defn- grid
  "Divide region 0,0,x,y into grid squares of size n.
   Partial grids squares are omitted. Optional parameter
   s sets the step size to allow for overlapping squares."
  ([n x y] (grid n x y n))
  ([n x y s]
     (for
	 [a (range 0 (- x n -1) s)
	  b (range 0 (- y n -1) s)]
       [a,b,n,n])))

(defn- gen-tiles 
  "Generate tiles of size n from image b.
   Returns a list of coordinate, image pairs
   rather than subimages for space efficiency."
  ([n ^BufferedImage b] (gen-tiles n b n))
  ([n ^BufferedImage b s]
     (map #(hash-map :coord %1 :image %2)
	  (grid n (.getWidth b) (.getHeight b) s)
	  (repeat b))))

(defn- gen-tiles-coll
  "Generate tiles of size n from images coll."
  ([n coll] (gen-tiles-coll n coll n))
  ([n coll s]
     (flatten (map gen-tiles (repeat n) coll))))

(defn- delta [seq-a seq-b]
  "Calculate the difference between sequences a and b.
   This is like a k-d manhattan distance."
  (reduce + (map #(Math/abs (int %)) (map - seq-a seq-b))))

(defn- sample-tiles [n tiles]
  "Get average RGB in regions n-by-n for tiles.
   Output format: {:tile :samples}"
  (for [t tiles]
    (let [i (img/sub-image (:coord t) (:image t))
	  s (img/get-samples (img/rescale n n i))]
      {:tile t :samples s} )))

(defn- best-match [n samples ^BufferedImage b]
  "Find the best matching tile to image b.
   Applies sub-image to the resulting tile
   to return a BufferedImage."
  (let [s (img/get-samples (img/rescale n n b))
	t (reduce #(if (< (delta s (:samples %1))
			  (delta s (:samples %2)))
		     %1 %2)
		  (first samples)
		  (rest samples))
	i (:tile t)]
    (img/sub-image (:coord i) (:image i))))
    
(defn- gen-canvas [n w ^BufferedImage b]
  "Rescale and crop image b to evenly fit tiles of
   size n, with w tiles across."
  (let [x (* n w)]
    (img/image-floor n (img/rescale-fixed-ratio x b))))

(defn mosaic [tiles ; collection of tile sources
	      ^BufferedImage target ; image to mosaic
	      n  ; tile size
	      ns ; tile step size
	      w  ; width in tiles
	      s] ; sample size (actual sample regions are s^2)
  (let [^BufferedImage canvas (gen-canvas n w target)
	tiles (sample-tiles s (gen-tiles-coll n tiles ns))]
    ; Replace each tile in canvas with the best match from tiles coll.
    (dorun (map #(img/insert!
		  (best-match n tiles (img/sub-image % canvas))
		  canvas (first %) (second %))
		(grid n (.getWidth canvas) (.getHeight canvas))))
    canvas))
