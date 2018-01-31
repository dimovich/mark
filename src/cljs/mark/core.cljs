(ns mark.core
  (:require ;;[taoensso.timbre :refer [info]]
            [dommy.core :as d]
            [mark.util  :as u]))


(def state (atom {}))


;; take a vinyl wrapper and toggle play/pause
;;
(defn toggle-play [base]
  (when-let [el (d/sel1 base :.vinyl-control)]
    (when-let [img (d/sel1 base :img)]
      (if (d/has-class? img :spin)
        (d/toggle-class! img :spin-pause)
        (d/add-class! img :spin))

      ;; start the audio and increase the number of players
      (if (d/has-class? el :play)
        (when-let [play (:play @state)]
          (do
            (swap! state update :play-count inc)
            (play)))
      
        ;; decrease the number of players and stop the audio if none
        ;; left
        (when-let [stop (:stop @state)]
          (do
            (when (pos? (:play-count @state))
              (when (zero? (-> (swap! state update :play-count dec)
                               :play-count))
                (stop))))))

      ;; change control element class
      (d/toggle-class! el :play)
      (d/toggle-class! el :pause))))



(defn get-wrapper [base]
  (when base
    (d/sel1 base :.vinyl-wrapper)))


(defn playing? [base]
  (when base
    (-> (d/sel1 base :.vinyl-control)
        (d/has-class? :pause))))



;; when changing current image make sure we stop the current spinning
;; vinyl after some delay
(defn handle-nav-click [base]
  (when-let [wrapper (-> (d/sel1 base :.flex-active-slide)
                         get-wrapper)]
    (when-let [img (d/sel1 wrapper :img)]
      (js/setTimeout
       #(do
          (when (playing? wrapper) (toggle-play wrapper))
          (d/remove-class! img :spin :spin-pause))
       300))))



(defn get-nav-controls [base]
  (when base
    (-> (d/sel1 base :.flex-control-nav)
        (d/sel :li))))



(defn add-nav-controls-listen! [base]
  (let [xs (get-nav-controls base)]
    (doseq [el xs]
      (d/listen! el :click  #(handle-nav-click base)))))




;; takes a parent element and passes down to vinyl-wrappers the
;; classes that start with "bg-"
;;
(defn pass-on-bg! [base]
  (let [els (d/sel base :.vinyl-wrapper)
        last-els (d/sel base :.vinyl-wrapper-last)
        cls (map second (re-seq #"(\bbg[-_][\w-_]*)" (d/class base)))]
    (doseq [cl cls]
      (d/remove-class! base cl)
      (doseq [el els]
        (d/add-class! el cl))
      (doseq [el last-els]
        (d/add-class! el (str cl "-last"))))))




;; takes a parent element and wraps the contents of <li> items inside
;; divs with play control.
(defn wrap-divs [base]
  (let [els (->> (d/sel base :li))
        els-no-clones (remove #(d/has-class? % :clone) els)
        els-clones    (filter #(d/has-class? % :clone) els)
        num (count els-no-clones)
        [els1 els2] (if (>= num 2)
                      [(concat (butlast els-no-clones)
                               (rest els-clones))
                       (list (last els-no-clones)
                             (first els-clones))]
                      [els])]

    (d/add-class! base :vinyl-spacer)

    ;; normal vinyl-wraps
    (doseq [el els1]
      (let [wrapper (d/create-element :div)
            control (d/create-element :div)]
        (when-let [img (d/sel1 el :img)]
          (d/listen! control :click #(toggle-play wrapper))
          (d/add-class! wrapper :vinyl-wrapper)
          (d/add-class! control :vinyl-control :play)
          (d/append! wrapper img)
          (d/append! wrapper control)
          (d/append! el wrapper))))

    
    ;; vinyl-wraps for the last elements
    (doseq [el els2]
      (let [wrapper (d/create-element :div)]
        (when-let [img (d/sel1 el :img)]
          (d/add-class! wrapper :vinyl-wrapper-last)
          (d/append! wrapper img)
          (d/append! el wrapper))))))



(defn get-viewports []
  (d/sel :.flex-viewport))


(defn get-galleries []
  (d/sel :.wpb_gallery))


(defn spind? []
  (-> (d/sel :.spind) empty? not))


(defn add-spind []
  (d/add-class! (d/sel1 :body) :spind))


;; check if slider viewports have loaded and add extra functionality
;;
(defn checker [t]
  (let [views (get-viewports)]
    (if (empty? views)
      ;; retry
      (when (pos? t)
        (js/setTimeout #(checker (dec t)) 500))

      ;; check first if we didn't already add the wrappers
      (when-not (spind?)
        ;; load the vinyl noise mp3
        (u/load-mp3 state "http://www.markforge.com/wp-content/uploads/vinyl.mp3")

        ;; keep track how we scrolled (mousewheel or other ways)
        (u/init-scroll {:lastscroll #(swap! state assoc :lastscroll %)})

        ;; extract languages from page
        (u/init-languages state)

        ;; put gallery images inside vinyl wrappers
        (doall (map wrap-divs (get-viewports)))

        ;; process gallery containers
        (doseq [gallery (get-galleries)]
          ;; keep track when gallery is almost in view and center it
          (u/keep-centered state gallery "20%")
          ;; take bg-* class name and pass it down to vinyl wrappers
          (pass-on-bg! gallery)
          ;; modify nav menu
          (add-nav-controls-listen! gallery))

        ;; trigger some delayed page resizes so the gallery redraws
        ;; and make gallery visible
        (let [ender #(doseq [el (get-galleries)]
                       (d/add-class! el :visible))]
          (u/trigger-resize 2 200 ender))

        ;; add a flag so we don't run again
        (add-spind)))))



(defn init []
  (checker 25))


;; yay
(defn ^:export main []
  (init))
