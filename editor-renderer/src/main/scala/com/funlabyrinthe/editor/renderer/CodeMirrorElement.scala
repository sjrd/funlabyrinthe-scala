package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.api.L.{*, given}

import typings.codemirror.mod.*
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.{EditorView, *}
import typings.codemirrorLangJavascript.mod.javascript

class CodeMirrorElement:
  private val transactionsBus = new EventBus[EditorState => Transaction]
  private val setStateBus = new EventBus[EditorState]

  private lazy val baseExtensions: js.Array[Any] = js.Array(
    basicSetup,
    lineNumbers(),
    EditorState.tabSize.of(2),
    javascript(),
  )

  lazy val topElement: Element =
    div(
      cls := "source-editor-codemirror-parent",
      onMountCallback { ctx =>
        setupCodeMirror(ctx.thisNode.ref)(using ctx.owner)
      },
    )
  end topElement

  private def enqueueTransaction(makeTransaction: EditorState => Transaction): Unit =
    transactionsBus.emit(makeTransaction)

  def loadContent(content: String): Unit =
    setStateBus.emit(EditorState.create(new {
      this.doc = content
      this.extensions = baseExtensions
    }))
  end loadContent

  private def setupCodeMirror(parent0: dom.HTMLElement)(using Owner): Unit =
    val editor = new EditorView(new {
      this.extensions = baseExtensions
      this.parent = parent0
    })

    transactionsBus.events.foreach { makeTransaction =>
      editor.dispatch(makeTransaction(editor.state))
    }

    setStateBus.events.foreach { state =>
      editor.setState(state)
    }
  end setupCodeMirror
end CodeMirrorElement
