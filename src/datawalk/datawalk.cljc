(ns datawalk.datawalk
  "Transforms data"
  (:require
            ;; Temporary for dev:
            [datawalk.print :as pr]
            ))

;;;;;;; State:

;; We track current data,
(def data (atom nil))

;; the paths from root to each visited node (we save them all so we can
;; know the correct path when we time travel),
(def paths (atom {}))

;; a map of values to return on exit,
(def saved (atom {}))

;; the past (visited nodes),
(def the-past (atom []))

;; and the future.
(def the-future (atom []))

;; Throughout: d = data

;;;;;;; Low-level helpers

(defn- not-set [x]
  (if (set? x) (vec x) x))

;; The past and the future are both stacks; to move backward or forward, we pop
;; something off one stack, push current data onto the other, and return the
;; popped value as the new present
;; from & to are past & future in some order
(defn- time-travel [from-time present to-time]
  (if (seq to-time)
    [(conj from-time present) (peek to-time) (pop to-time)]
    (do (println "You have reached the end of time. You shall go no further.\n")
        [from-time present to-time])))

;;;;;;; High-level helpers

(defn save [item]
  (let [next-index (count @saved)]
    (swap! saved (assoc next-index item))))

(defn reset-data! [d]
  (reset! data d))

(defn- move-forward [past present future]
  (time-travel past present future))

(defn- move-backward [past present future]
  (reverse ; we reverse because time-travel always returns [from-time present to-time]
   (time-travel future present past)))

;;;;;;; User API

;; All API fns take data as their final argument, and return
;; updated data. Changes to the paths, saved, the-past, and the-future atoms are
;; made inline as side effects.

(defn no-op [data]
  data)

(defn drill
  "Given a number n, drill down to that numbered item"
  [n data]
  ;; (println "drilling into" data)
  (cond (sequential? data)
        , (let [next-data (nth data n)
                ;; _ (println "conjing (in seq) onto" (type @paths))
                next-path (conj (@paths data) n)]
            (swap! paths assoc (not-set next-data) next-path)
            next-data)
        (map? data)
        , (let [;_ (println "nonsequential data is a" (type data))
                ks (keys data)
                k (nth ks n)
                ;; _ (println "conjing (in map) onto" (type (@paths data)))
                next-data (get data k)
                next-path (conj (@paths data) k)]
            ;; (println "k:" k)
            (swap! paths assoc (not-set next-data) next-path)
            next-data)
        :else ; not drillable; no-op
        , (do (println "Can't drill into a" (type data))
              data)))

(defn exit [data]
  ;; Returns saved data
  @saved)

(defn exit-with-current [data]
  data)

(defn save-current [data])

(defn save-path [data])

(defn backward [data])

(defn forward [data])

(defn root [data])

;; TODO maybe?
(defn up [data])

(defn print-help [data])

(defn print-path [data])

(defn function [data])


;; Dev helpers
(defn init []
  (do (reset-data! {:a 1 :b {:c 2 :d 3}})
      (reset! paths {})
      (reset! saved {})))