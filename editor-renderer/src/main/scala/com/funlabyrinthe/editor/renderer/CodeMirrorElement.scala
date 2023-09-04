package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
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
  private val updatesBus = new EventBus[ViewUpdate]

  private lazy val baseExtensions: js.Array[Any] = js.Array(
    basicSetup,
    lineNumbers(),
    EditorState.tabSize.of(2),
    javascript(),
    EditorView.updateListener.of(updatesBus.emit(_)),
  )

  lazy val topElement: Element =
    div(
      cls := "source-editor-codemirror-parent",
      onMountUnmountCallbackWithState(
        mount = { ctx =>
          setupCodeMirror(ctx.thisNode.ref)(using ctx.owner)
        },
        unmount = { (ctx, editorViewOpt) =>
          editorViewOpt.foreach(_.destroy())
        }
      ),
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

  def getContent(): Future[String] =
    ???

  private def setupCodeMirror(parent0: dom.HTMLElement)(using Owner): EditorView =
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

    editor
  end setupCodeMirror
end CodeMirrorElement
