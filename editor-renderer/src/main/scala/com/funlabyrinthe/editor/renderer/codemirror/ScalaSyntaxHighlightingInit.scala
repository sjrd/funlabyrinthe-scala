package com.funlabyrinthe.editor.renderer.codemirror

import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom

import typings.webTreeSitter.mod

object ScalaSyntaxHighlightingInit:
  final class Initialized private[ScalaSyntaxHighlightingInit] (
    private[ScalaSyntaxHighlightingInit] val module: TreeSitterModule,
    val scalaLanguage: mod.Language,
    val scalaQuery: mod.Query,
  ):
    def newParser(): mod.Parser = new module.Parser()
  end Initialized

  private lazy val initializedFuture: Future[Initialized] =
    val initOptions = new js.Object {
      def locateFile(scriptName: String, scriptDirectory: String): String =
        s"./target/tree-sitter-scala/$scriptName"
    }

    // start loading the query file in parallel
    val highlightQueryFuture =
      for
        response <- dom.fetch(s"./target/tree-sitter-scala/highlights.scm").toFuture
        text <- response.text().toFuture
      yield
        text

    for
      module <- js.`import`[TreeSitterModule]("web-tree-sitter").toFuture
      _ <- module.Parser.init(initOptions).toFuture
      language <- module.Parser.Language.load("./target/tree-sitter-scala/tree-sitter-scala.wasm").toFuture
      highlightQuery <- highlightQueryFuture
    yield
      val query = language.query(highlightQuery)
      Initialized(module, language, query)
  end initializedFuture

  def initialize(): Future[Initialized] = initializedFuture

  @js.native
  private trait TreeSitterModule extends js.Any:
    @js.native
    @JSName("default")
    class Parser() extends typings.webTreeSitter.mod.Parser

    @js.native
    @JSName("default")
    object Parser extends js.Object:
      def init(initOptions: js.Object): js.Promise[Unit] = js.native

      @js.native
      object Language extends js.Object:
        def load(input: String): js.Promise[typings.webTreeSitter.mod.Language] = js.native
      end Language
    end Parser
  end TreeSitterModule
end ScalaSyntaxHighlightingInit
