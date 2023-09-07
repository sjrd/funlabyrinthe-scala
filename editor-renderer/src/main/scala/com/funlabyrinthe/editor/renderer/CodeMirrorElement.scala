package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.api.L.{*, given}

import typings.codemirror.mod.*
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.{EditorView, *}
import typings.codemirrorLangJavascript.mod.javascript
import typings.codemirrorLanguage.mod.LanguageSupport

object CodeMirrorElement:
  val BaseExtensions: List[Extension | LanguageSupport] = List(
    basicSetup,
    lineNumbers(),
    EditorState.tabSize.of(2),
    javascript(),
  )

  def apply(
    initialDoc: String,
    viewUpdatesObserver: Observer[ViewUpdate],
  ): Element =
    apply(initialDoc, BaseExtensions, viewUpdatesObserver)

  def apply(
    initialDoc: String,
    extensions: List[Extension | LanguageSupport],
    viewUpdatesObserver: Observer[ViewUpdate],
  ): Element =
    val baseExtensions = (extensions: List[Any]).toJSArray
    new CodeMirrorElement(initialDoc, baseExtensions, viewUpdatesObserver).topElement
  end apply
end CodeMirrorElement

private class CodeMirrorElement(
  initialDoc: String,
  baseExtensions0: js.Array[Any],
  viewUpdatesObserver: Observer[ViewUpdate]
):
  private val transactionsBus = new EventBus[EditorState => Transaction]
  private val updatesBus = new EventBus[ViewUpdate]

  private val baseExtensions = baseExtensions0 :+ EditorView.updateListener.of(updatesBus.emit(_))

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
      updatesBus.events --> viewUpdatesObserver,
    )
  end topElement

  private def enqueueTransaction(makeTransaction: EditorState => Transaction): Unit =
    transactionsBus.emit(makeTransaction)

  private def setupCodeMirror(parent0: dom.HTMLElement)(using Owner): EditorView =
    val editor = new EditorView(new {
      this.doc = initialDoc
      this.extensions = baseExtensions
      this.parent = parent0
    })

    transactionsBus.events.foreach { makeTransaction =>
      editor.dispatch(makeTransaction(editor.state))
    }

    editor
  end setupCodeMirror
end CodeMirrorElement
