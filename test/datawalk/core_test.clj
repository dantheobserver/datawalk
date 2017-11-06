(ns datawalk.core-test
  "Integration tests, really. Does a series of walks through data & verifies
  that things are as they should be."
  (:require [clojure.string :as s]
            [clojure.test :refer :all]
            [datawalk.core :refer :all]
            [datawalk.datawalk :as dw]))


;; We're going to use the underlying `datawalk` fn rather than add complexity by
;; abstracting `read-line` just for test purposes. Unlike `repl`, `w` always
;; returns nil, so we have to check the global data to ensure the current data
;; (or other globals) are as expected.

(defmacro gval
  "global-value. Return the value in one of the global-state atoms."
  [g-name#]
  `(deref (var-get (var ~g-name#))))

(defn dw-call
  "Walk through data with a series of steps, as though the steps
  were entered individually in the repl. Non-numeric steps should be
  quoted to prevent evaluation (eg steps could be [2 's 4])"
  [data steps]
  (reduce datawalk data (map str steps)))

(defn expect
  "Verifies that when `steps` have been performed on `data`, the
  resulting value is expected-result"
  [data steps expected-result]
  (look-at data) ; init
  (is (= expected-result (dw-call data steps))))

(defn expect-saved
  "Verifies that when `steps` have been performed on `data`, the
  data in the saved-data map (dw/saved) matches expected-saved"
  [data steps expected-saved]
  (look-at data) ; init
  (dw-call data steps)
  (is (= expected-saved (gval dw/saved))))

(defn- whitespace-normalized [string]
  (s/trim (s/replace string #"\s+" " ")))

(defn matches-ish
  "True if two strings differ by at most whitespace"
  [s1 s2]
  (= (whitespace-normalized s1) (whitespace-normalized s2)))

(defn expect-str
  "Verifies that when `steps` have been performed on `data`, the
  printed session matches expected-result"
  [data steps expected-result]
  (look-at data) ; init
  (let [result (with-out-str (dw-call data steps))]
    (println (str "**" result "**"))
    (println "***")
    (prn result)
    (prn "whit-nor")
    (prn (whitespace-normalized result))
    (println)
    (is (= (whitespace-normalized result)
           (whitespace-normalized expected-result))))

  #_(is (= expected-result (with-out-str (dw-call data steps)))))

(deftest vec-test
  (expect [1 2 [3 [[4] 5 6]]]
          [2 1 0 0]
          4))

(deftest set'-test
  (expect #{1 2 3 [4 #{5 6 7}]}
          [3 1 2]
          5))

(deftest list-test
  (expect '(1 2 (3 ((4) 5)))
          [2 1 0 0]
          4))

(deftest map-test
  (expect {1 2 3 [4 {5 6}]}
          [1 1 0]
          6))

(deftest saved-cur-test
  (expect-saved {1 2 3 [4 {5 6}]}
                [1 's 1 0 's]
                {1 [4 {5 6}], 2 6}))

(deftest saved-path-test
  (expect-saved {1 2 3 [4 {5 6}]}
                [1 'v 'p 1 0 'p 'v]
                {1 [3], 2 [3 1 5]}))

(deftest time-travel-test
  (expect {1 2 3 [4 {5 6}]}
          [1 1 'b 'f 'f 0 'b 'b 'b 'b 1 1]
          {5 6}))

(deftest up-test
  (expect-saved {1 2 3 [4 {5 6}]}
                [1 1 0 'p 'v 'u 'u 'u 's 'p 'v]
                {1 [3 1 5], 2 {1 2, 3 [4 {5 6}]}, 3 []}))

;; Examine string output. Brittle, but necessary for the
;; print-centric tangential commands.
(deftest map-str-test
  (expect-str {1 2 3 [4 {5 6}]}
              [1 1 'p]
              "(00. 4 01. {5 6} ) (00. 5: 6 ) PATH: [3 1] (00. 5: 6 )"))
