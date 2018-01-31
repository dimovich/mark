(ns mark.util
  (:require ;;[taoensso.timbre :refer [info]]
            [dommy.core :as d]
            [bardo.ease :refer [ease]]
            [cljsjs.waypoints]
            [cljsjs.smooth-scroll]))


(def STICKY_ACTION   150)
(def STICKY_RELEASE  500)
(def STICKY_DURATION 300)



;; execute f after a delay for a number of times, and run a function
;; at the end (plus another delayed f). used for triggering
;; page redraw
(defn looper [f count delay ender]
  (if (pos? count)
    (js/setTimeout
     #(do (f) (looper f (dec count) delay ender))
     delay)
    
    (do (ender)
        (js/setTimeout f delay))))



(defn trigger-redraw
  ([times]
   (trigger-redraw times 200 identity))
  ([times delay]
   (trigger-redraw times delay identity))
  ([times delay ender]
   (let [ev (js/Event. "resize")
         f #(js/dispatchEvent ev)]
     (looper f times delay ender))))




(defn click-language [state lang]
  ;; show selected language
  (doseq [el (d/sel lang)]
    (d/remove-class! el :hidden))

  ;; hide other languages
  (doseq [lang (->> (:languages @state)
                    (remove #{lang}))]
    (doseq [el (d/sel lang)]
      (d/add-class! el :hidden))))



(defn init-languages [state]
  (swap!
   state assoc :languages
   (->> (some-> (d/sel1 :.languages)
                (d/sel :a))
        (map
         (fn [el]
           (let [lang (->> (d/attr el :href)
                           (re-find #"([.][\w-_]*)$")
                           second)]
             (d/listen! el :click
                        #(do (click-language state lang)
                             (trigger-redraw 1)
                             (.preventDefault %)))
             lang)))
        
        doall)))



(defn init-scroll [{:keys [lastscroll]}]
  (let [body (d/sel1 :body)]
    (.init js/smoothScroll
           (clj->js {:speed STICKY_DURATION
                     :custom-easing (ease :back-in-out)}))

    ;; keep track of how we scrolled
    (d/listen! body
               "mousewheel"     #(lastscroll :mousewheel)
               "DOMMouseScroll" #(lastscroll :mousewheel)
               "scroll"         #(lastscroll :dontcare)
               "mousemove"      #(lastscroll :dontcare))))



(defn jump-to [el & [ender]]
  (let [fun #(.preventDefault %)
        body (d/sel1 :body)]

    ;; prevent user from scrolling with mousewheel during sticky-time
    (d/listen! body
               "mousewheel" fun
               "DOMMouseScroll" fun)

    ;; schedule the sticky action
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
     STICKY_ACTION)

    ;; schedule the post sticky action
    (js/setTimeout
     #(do (when ender (ender))
          (d/unlisten! body
                       "mousewheel" fun
                       "DOMMouseScroll" fun))
     STICKY_RELEASE)))


(defn in-view [el opts]
  (js/Waypoint.
   (clj->js (-> {:element el}
                (merge opts)))))


(defn keep-centered [state el offset]
  (in-view
   el
   {:offset offset
    :handler (fn [direction]
               (when (and (= (:lastscroll @state) :mousewheel)
                          (not= (:lastscroll-el @state) el))
                 (swap! state dissoc :lastscroll-el)
                 (when (= direction "down")
                   (jump-to el #(swap! state assoc
                                       :lastscroll :dontcare
                                       :lastscroll-el el)))))}))



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



