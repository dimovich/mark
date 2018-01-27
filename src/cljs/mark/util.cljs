(ns mark.util
  (:require [dommy.core :as d]
            [cljsjs.waypoints]
            [cljsjs.smooth-scroll]))



(defn init-scroll [{:keys [onscroll]}]
  (let [body (d/sel1 :body)]
    (.init js/smoothScroll
           (clj->js {:speed 500 :easing "easeInCubic"}))
    (d/listen! body
               "mousewheel" #(onscroll :mousewheel)
               "DOMMouseScroll" #(onscroll :mousewheel)
               "scroll"     #(onscroll :scrollbar))))



(defn jump-to [el]
  (let [fun #(.preventDefault %)
        body (d/sel1 :body)]
    (d/listen! body "mousewheel" fun)
    (js/setTimeout #(d/unlisten! body "mousewheel" fun) 1500)
    (js/setTimeout
     #(let [brect (d/bounding-client-rect body)
            erect (d/bounding-client-rect el)]
        (-> js/smoothScroll
            (.animateScroll
             (js/Math.abs
              (- (:top brect) (:top erect)
                 (/ (- (.-innerHeight js/window)
                       (:height erect))
                    -2))))))
     500)))



(defn in-view [el opts]
  (js/Waypoint.Inview.
   (clj->js (-> {:element el}
                (merge opts)))))



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






(defn create-audio-context []
  (if js/AudioContext
    (js/AudioContext.)
    (js/webkitAudioContext.)))


;; take an url and load the audio using an ajax request and set a
;; global state atom with the play, pause and close functions.
;;
(defn load-mp3 [state url]
  (let [ctx (create-audio-context)
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



