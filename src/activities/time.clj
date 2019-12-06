(ns activities.time
  (:require [java-time :as jt]))

(defmethod print-method java.time.Instant [inst writer]
  (.write
   writer
   (str "#java.time/instant \""
        (jt/format (jt/with-zone (jt/formatter "yyyy-MM-dd'T'HH:mm:ssZZ") "UTC") inst)
        "\"")))

(defmethod print-dup java.time.Instant [inst writer]
  (.write
   writer
   (str "#java.time/instant \""
        (jt/format (jt/with-zone (jt/formatter "yyyy-MM-dd'T'HH:mm:ssZZ") "UTC") inst)
        "\"")))

(defn parse-instant [s]
  (jt/instant (jt/with-zone (jt/formatter "yyyy-MM-dd'T'HH:mm:ssZZ") "UTC") s))
