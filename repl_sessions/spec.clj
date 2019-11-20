(ns spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::uuid uuid?)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::date-time (partial instance? java.time.LocalDateTime))
(s/def ::duration (partial instance? java.time.Duration))
(s/def ::capacity (partial instance? java.lang.Long))
(s/def ::creator uuid?)
(s/def ::participants (s/coll-of ::uuid :kind set? :into #{}))

(s/valid? ::participants #{#uuid "3a875c6e-985b-4330-816c-6cca2dc4d812" #uuid "e813399b-eb22-4b0d-8563-5088853eb2d0"})
