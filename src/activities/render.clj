(ns activities.render)

(defn flash-message [req msg]
  (assoc req :flash msg))
