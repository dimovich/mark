(ns mark.core
  (:require ;;[taoensso.timbre :refer [info]]
            [dommy.core :as d]))


(defonce state (atom {}))


(defn create-context []
  (if js/AudioContext
    (js/AudioContext.)
    (js/webkitAudioContext.)))



(defn load-mp3 [url]
  (let [ctx (create-context)
        source (.createBufferSource ctx)
        req (js/XMLHttpRequest.)]
    (.open req "GET" url true)
    (set! (.-responseType req) "arraybuffer")
    (set! (.-onload req)
          (fn []
            (let [data (.-response req)]
              (.decodeAudioData
               ctx data
               (fn [buffer]
                 (set! (.-buffer source) buffer)
                 (set! (.-loop source) true)
                 (.connect source (.-destination ctx))
                 (.suspend ctx)
                 (.start source)
                 (swap! state assoc
                        :play #(.resume ctx)
                        :stop #(.suspend ctx)
                        :close-audio #(.close ctx)))
               ;;#(info "Error decoding audio data" (.-err %))
               ))))
    (.send req)))





(defn toggle-play [base el]
  (let [img (d/sel1 base :img)]
    (if (d/has-class? img :spin)
      (d/toggle-class! img :spin-pause)
      (d/add-class! img :spin))

    (if (d/has-class? el :play)
      (when-let [play (:play @state)]
        (do
          (swap! state update :play-count inc)
          (play)))
      (when-let [stop (:stop @state)]
        (do
          (when (pos? (:play-count @state))
            (when (zero? (-> (swap! state update :play-count dec)
                             :play-count))
              (stop))))))
    
    (d/toggle-class! el :play)
    (d/toggle-class! el :pause)))



(defn pass-on-bg [base]
  (let [els (d/sel base :.vinyl-wrapper)
        cls (map second (re-seq #"(\bbg[-_][\w]*)" (d/class base)))]
    (doseq [cl cls]
      (d/remove-class! base cl)
      (doseq [el els]
        (d/add-class! el cl)))))


(defn wrap-divs [base]
  (let [els (d/sel base :li)
        spacers (d/sel base :.vinyl-wrapper)]
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


(defn add-listen [base]
  (let [el (d/sel1 base :.vinyl-control)]
    (d/listen! el :click #(toggle-play base el))))


(defn ^:export init []
  (letfn [(resizer [fun delay count end-fn]
            (if (pos? count)
              (js/setTimeout #(do (fun)
                                  (resizer fun delay (dec count) end-fn))
                             delay)
              (do
                (end-fn)
                (js/setTimeout fun delay))))
          (checker []
            (let [views (d/sel :.flex-viewport)]
              (if (empty? views)
                (do
                  (js/setTimeout checker 500))
                (do
                  ;;(doall (map #(d/add-class! % :hidden) (d/sel :.wpb_gallery)))
                  
                  (doall (map wrap-divs (d/sel :.flex-viewport)))
                  (doall (map #(do (d/add-class! % :vinyl-spacer)
                                   (doseq [div (d/sel % :.vinyl-wrapper)]
                                     (add-listen div)))
                              (d/sel :.flex-viewport)))
                  (doall (map pass-on-bg (d/sel :.wpb_gallery)))

                  (let [ev (js/Event. "resize")
                        fun #(js/dispatchEvent ev)
                        ender (fn [] (doall (map #(d/set-style! % :opacity 1) (d/sel :.wpb_gallery))))]
                    (resizer fun 300 3 ender))))))]
    (load-mp3 "http://www.markforge.com/wp-content/uploads/vinyl.mp3")
    (checker)))


(defn ^:export main []
  (init))
