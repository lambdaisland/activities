(ns polymorphism)

(defprotocol Beverage
  (make-beverage [this])
  (content [this]))

(defrecord CoffeeBeans [desc]
  Beverage
  (make-beverage [this]
    {:type :coffee
     :desc desc})
  (content [this]
    "20 cl"))

(defrecord Fruit [type size])

(extend-protocol Beverage
  Fruit
  (make-beverage [this]
    (str "a " (:size this) " " (:type this) " juice"))
  (contents [this]
    (:size this))

  java.util.Date
  (make-beverage [this]
    (str "this beverage expires on " this))
  )

(make-beverage #inst "2019-09-19")

(->CoffeeBeans "Arabica")
(->Fruit "apple" "large")
(make-beverage (->Fruit "apple" "large"))

(make-beverage (->CoffeeBeans "Arabica"))
