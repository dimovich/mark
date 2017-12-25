(ns mark.core
  (:require [dommy.core :as d]



(defonce state (atom {}))


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
    (.open req "GET" url true)
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
                 (.connect source (.-destination ctx))
                 (.suspend ctx)
                 (.start source)
                 ;; add play/stop/close-audio functions to the state
                 (swap! state assoc
                        :play #(.resume ctx)
                        :stop #(.suspend ctx)
                        :close-audio #(.close ctx)))
               #_(info "Error decoding audio data" (.-err %))))))
    (.send req)))




;; take the vinyl-wrapper element and the play control element and
;; toggle play/pause
;;
(defn toggle-play [base el]
  (let [img (d/sel1 base :img)]
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
    (d/toggle-class! el :pause)))



;; takes a parent element and passes down to vinyl-wrappers the
;; classes that start with "bg-"
;;
(defn pass-on-bg [base]
  (let [els (d/sel base :.vinyl-wrapper)
        cls (map second (re-seq #"(\bbg[-_][\w]*)" (d/class base)))]
    (doseq [cl cls]
      (d/remove-class! base cl)
      (doseq [el els]
        (d/add-class! el cl)))))




;; takes a parent element and wraps the contents of <li> items inside
;; divs with play control.
(defn wrap-divs [base]
  (let [els (d/sel base :li)
        spacers (d/sel base :.vinyl-wrapper)]
    
    ;; check first if we didn't already add the wrappers
    (when (empty? spacers)
      (doseq [el els]
        (let [wrapper (d/create-element :div)
              control (d/create-element :div)
              img (d/sel1 el :img)]
          (d/add-class! wrapper "vinyl-wrapper")
          (d/add-class! control "vinyl-control" "play")
          (d/append! wrapper img)
          (d/append! wrapper control)
          (d/append! el wrapper))))))


;; takes a vinyl-wrapper and adds click events listeners to the play
;; control
(defn add-listen [base]
  (let [el (d/sel1 base :.vinyl-control)]
    (d/listen! el :click #(toggle-play base el))))



(defn ^:export init []
  (letfn [ ;; execute f after a delay for a number of times, and run a
          ;; function at the end (plus another delayed f). used for
          ;; triggering page redraw
          (looper [f delay count end-fn]
            (if (pos? count)
              (js/setTimeout #(do (f) (looper f delay (dec count) end-fn))
                             delay)
              (do (end-fn)
                  (js/setTimeout f delay))))

          ;; check for a few times if page loaded then modify it
          (checker [t]
            (let [views (d/sel :.flex-viewport)]
              (if (empty? views)
                (when (pos? t)
                  (js/setTimeout #(checker (dec t)) 500))
                (do
                  ;; wrap the gallery items inside our div
                  (doall (map wrap-divs (d/sel :.flex-viewport)))
                  
                  ;; add spacer margins to gallery and add listen to play button
                  (doall (map #(do (d/add-class! % :vinyl-spacer)
                                   (doseq [div (d/sel % :.vinyl-wrapper)]
                                     (add-listen div)))
                              (d/sel :.flex-viewport)))

                  ;; pass "bg-" classes down to our vinyl-wrappers
                  (doall (map pass-on-bg (d/sel :.wpb_gallery)))

                  ;; trigger some delayed page resizes so the gallery redraws
                  (let [ev (js/Event. "resize")
                        f #(js/dispatchEvent ev)
                        ;; make gallery visible
                        ender (fn []
                                (doall (map #(d/add-class! % :visible)
                                            (d/sel :.wpb_gallery))))]
                    
                    (looper f 300 3 ender))))))]

    
    (load-mp3 "http://www.markforge.com/wp-content/uploads/vinyl.mp3")
    (checker 10)))


(defn ^:export main []
  (init))
