(ns repl-sessions.flexmark
  (:import (com.vladsch.flexmark.parser Parser)
           (java.util ArrayList)
           (com.vladsch.flexmark.html HtmlRenderer)))

;; import com.vladsch.flexmark.util.ast.Node
;; import com.vladsch.flexmark.html.HtmlRenderer
;; import com.vladsch.flexmark.parser.Parser
;; import com.vladsch.flexmark.parser.ParserEmulationProfile
;; import com.vladsch.flexmark.util.data.MutableDataHolder
;; import com.vladsch.flexmark.util.data.MutableDataSet

;; Parser parser = Parser.builder().build();
;; Node document = parser.parse("This is *Sparta*");
;; HtmlRenderer renderer = HtmlRenderer.builder().build();
;; renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"

(def parser (.build (Parser/builder)))

(def parsed-document (.parse parser "test *string*"))

(def renderer (.build (HtmlRenderer/builder)))

(.render renderer parsed-document)
