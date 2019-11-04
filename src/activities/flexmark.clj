(ns activities.flexmark
  (:import (com.vladsch.flexmark.parser Parser)
           (com.vladsch.flexmark.html HtmlRenderer)))

(set! *warn-on-reflection* true)

(defn md->html [^String md]
  (let [parser          (.build (Parser/builder))
        parsed-document (.parse parser md)
        renderer        (.build (HtmlRenderer/builder))] 
    (.render renderer parsed-document)))
