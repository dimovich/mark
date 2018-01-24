(ns mark.core
  (:require [dommy.core :as d]
            [cljsjs.sticky]
            ;;[taoensso.timbre :refer [info]]
            ))


(def state (atom {}))

(defn create-context []
  (if js/AudioContext
    (js/AudioContext.)
    (js/webkitAudioContext.)))


;; take an url and load the audio using an ajax request and set a
;; global state atom with the play, pause and close functions.
;;
(defn load-mp3 [url]
  (let [ctx (create-context)
        source (.createBufferSource ctx)
        req (js/XMLHttpRequest.)]

    (set! (.-responseType req) "arraybuffer")
    (set! (.-onload req)
          (fn []
            (let [data (.-response req)]
              ;; got the response from the server, try to decode the
              ;; audio
              (.decodeAudioData
               ctx data
               (fn [buffer]
                 (set! (.-buffer source) buffer)
                 (set! (.-loop source) true)
                 (.suspend ctx)
                 (.connect source (.-destination ctx))
                 (.start source)
                 ;; add play/stop/close-audio functions to the state
                 (swap! state assoc
                        :play #(.resume ctx)
                        :stop #(.suspend ctx)
                        :close-audio #(.close ctx)))
               #_(info "Error decoding audio data" (.-err %))))))
    
    (.open req "GET" url true)
    (.send req)))



;; take the vinyl-wrapper element and the play control element and
;; toggle play/pause
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
    

    ;; normal vinyl-wraps
    (doseq [el els1]
      (let [wrapper (d/create-element :div)
            control (d/create-element :div)]
        (when-let [img (d/sel1 el :img)]
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



;; takes a vinyl-wrapper and adds click events listeners to the play
;; control
(defn add-wrapper-listen! [base]
  (let [el (d/sel1 base :.vinyl-control)]
    (d/listen! el :click #(toggle-play base))))



;; execute f after a delay for a number of times, and run a function
;; at the end (plus another delayed f). used for triggering
;; page redraw
(defn looper [f delay count end-fn]
  (if (pos? count)
    (js/setTimeout #(do (f)
                        (looper f delay (dec count) end-fn))
                   delay)
    (do (end-fn)
        (js/setTimeout f delay))))



(defn get-viewports []
  (d/sel :.flex-viewport))


(defn spind? []
  (-> (d/sel :.spind) empty? not))


(defn add-spind []
  (d/add-class! (d/sel1 :body) :spind))


(defn checker [t]
  (let [views (get-viewports)]
    (if (empty? views)
      ;; retry
      (when (pos? t)
        (js/setTimeout #(checker (dec t)) 500))

      ;; check first if we didn't already add the wrappers
      (when-not (spind?)
        (load-mp3 "http://www.markforge.com/wp-content/uploads/vinyl.mp3")

        #_(-> (get-viewports)
              (map add-vinyl-wrapper)
              (map #(d/add-class! % :vinyl-spacer))
              (map #(map (fn [w] (add-wrapper-listen! w))
                         (d/sel % :.vinyl-wrapper))))
        
        
        ;; wrap the gallery items inside our div
        (doall (map wrap-divs (get-viewports)))
                    
        ;; add spacer margins to gallery viewport and add listen to
        ;; play button
        (doseq [view (get-viewports)]
          (d/add-class! view :vinyl-spacer)
          (doseq [wrapper (d/sel view :.vinyl-wrapper)]
            (add-wrapper-listen! wrapper)))
        


        #_(->> (d/sel :.wpb_gallery)
               (map (comp add-nav-ctrls-listen
                          pass-on-bg)))

        
        ;; process the gallery containers
        (doseq [base (d/sel :.wpb_gallery)]
          (pass-on-bg! base)
          (add-nav-controls-listen! base))
        
                    
        ;; trigger some delayed page resizes so the gallery redraws
        (let [ev (js/Event. "resize")
              f #(js/dispatchEvent ev)
              ;; make gallery visible
              ender (fn []
                      (doall
                       (map #(d/add-class! % :visible)
                            (d/sel :.wpb_gallery))))]
          
          (looper f 200 2 ender))

        ;; add a flag so we don't run again
        (add-spind)))))



(defn init []
  (checker 20))


;; yay
(defn ^:export main []
  (init))
