(ns beats)

(def binaural-beat-freq 40)
(defonce ctx (or (js/window.AudioContext.) (js/window.webkitAudioContext.)))
(defonce started? (atom false))
(def slider (js/document.getElementById "frequencyRange"))
(def display (js/document.getElementById "frequencyDisplay"))

(defn ->panner [left?]
  (let [panner (. ctx createStereoPanner)]
    (set! (.. panner -pan -value) (if left? -1 1))
    (.connect panner ctx.destination)
    panner))

(def panners
  (delay {:left (->panner true) :right (->panner false)}))

(defn update-display! [value]
  (set! (.-innerHTML display)
        (str "Base frequency: " value " Hz")))

(def get-oscillator
  (memoize
   (fn [panner]
     (let [o (ctx.createOscillator)]
       (set! (.- o type) "sine")
       (. o start)
       (. o (connect panner))
       o))))

(defn oscillate [panner hz]
  (let [o (get-oscillator panner)
        _ (set! (.. o -frequency -value) hz)])
  hz)

(defn update-app [frequency-value]
  (let [frequency-value (/ frequency-value 1000.0)]
    (-> @panners :right (oscillate (+ frequency-value binaural-beat-freq)))
    (-> @panners :left (oscillate frequency-value))
    (update-display! frequency-value)))

(defn start-app []
  (.resume ctx)
  (update-app (* 1000 152.74))
  (set! (.-value slider) (* 1000 152.74))
  (reset! started? true))

(defn pause-app []
  (.suspend ctx))

(defn toggle-app []
  (if (= (.-state ctx) "running")
    (pause-app)
    (if @started?
      (.resume ctx)
      (start-app))))

(defn handle-keydown [e]
  (when (or (= (.-code e) "Space")
            (= (.-key e) " "))
    (.preventDefault e)
    (toggle-app)))

(.addEventListener js/document "keydown" handle-keydown)

(set! (.-update_app js/window) update-app)
(set! (.-start_app js/window) start-app)
(set! (.-pause_app js/window) pause-app)
