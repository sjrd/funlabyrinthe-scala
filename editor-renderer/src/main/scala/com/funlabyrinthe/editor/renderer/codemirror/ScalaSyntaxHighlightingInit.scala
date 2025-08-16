package com.funlabyrinthe.editor.renderer.codemirror

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom

import typings.webTreeSitter.mod

import com.funlabyrinthe.editor.renderer.JSPI

object ScalaSyntaxHighlightingInit:
  final class Initialized private[ScalaSyntaxHighlightingInit] (
    private[ScalaSyntaxHighlightingInit] val module: TreeSitterModule,
    val scalaLanguage: mod.Language,
    val scalaQuery: mod.Query,
  ):
    def newParser(): mod.Parser = new module.Parser()
  end Initialized

  private lazy val initializedPromise: js.Promise[Initialized] = JSPI.async {
    val initOptions = new js.Object {
      def locateFile(scriptName: String, scriptDirectory: String): String =
        s"./target/tree-sitter-scala/$scriptName"
    }

    // start loading the query file in parallel
    val highlightQueryPromise = JSPI.async {
      val response = JSPI.await(dom.fetch(s"./target/tree-sitter-scala/highlights.scm"))
      JSPI.await(response.text())
    }

    val module = JSPI.await(js.`import`[TreeSitterModule]("web-tree-sitter"))
    JSPI.await(module.Parser.init(initOptions))
    val language = JSPI.await(module.Parser.Language.load("./target/tree-sitter-scala/tree-sitter-scala.wasm"))
    val highlightQuery = JSPI.await(highlightQueryPromise)
    val query = language.query(highlightQuery)
    Initialized(module, language, query)
  }

  def initialize(): Initialized = JSPI.await(initializedPromise)

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
