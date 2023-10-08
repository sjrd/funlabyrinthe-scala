package com.funlabyrinthe.editor.renderer

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.IconName
import typings.codemirrorState.mod.Text

class SourceEditor(val universeFile: UniverseFile, val sourceName: String, initialContent: String)(using ErrorHandler):
  private val sourceFile = universeFile.sourcesDirectory / sourceName
  private val currentDoc: Var[(Text, Boolean)] = Var((Text.of(initialContent.split("\n").toJSArray), false))

  val isModified: Signal[Boolean] = currentDoc.signal.map(_._2)

  lazy val topElement: Element =
    div(
      CodeMirrorElement(
        initialContent,
        Observer { viewUpdate =>
          if viewUpdate.docChanged then
            currentDoc.set((viewUpdate.state.doc, true))
        },
      )
    )
  end topElement

  def saveContent()(using ExecutionContext): Future[Unit] =
    for _ <- sourceFile.writeString(currentDoc.now()._1.toString()) yield
      currentDoc.update((doc, prevModified) => (doc, false))
end SourceEditor
