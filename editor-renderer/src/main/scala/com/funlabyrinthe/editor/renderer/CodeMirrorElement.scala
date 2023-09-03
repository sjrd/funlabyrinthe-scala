package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import typings.codemirror.mod.*
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.{EditorView, *}
import typings.codemirrorLangJavascript.mod.javascript

class CodeMirrorElement:
  lazy val topElement: Element =
    div(
      cls := "source-editor-codemirror-parent",
      onMountCallback { ctx =>
        setupCodeMirror(ctx.thisNode.ref)
      },
    )
  end topElement

  private def setupCodeMirror(parent0: dom.HTMLElement): Unit =
    val extensions0: js.Array[Any] = js.Array(
      basicSetup,
      lineNumbers(),
      EditorState.tabSize.of(2),
      javascript(),
    )

    val editor = new EditorView(new {
      this.extensions = extensions0
      this.parent = parent0
    })
  end setupCodeMirror
end CodeMirrorElement
