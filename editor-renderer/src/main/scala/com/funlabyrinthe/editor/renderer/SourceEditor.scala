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

import com.funlabyrinthe.editor.renderer.codemirror.*
import com.funlabyrinthe.editor.renderer.electron.fileService

class SourceEditor(
  val universeFile: UniverseFile,
  val sourceName: String,
  initialContent: String,
  highlightingInitialized: ScalaSyntaxHighlightingInit.Initialized,
  problems: Signal[List[Problem]],
)(using ErrorHandler):
  private val currentDoc: Var[(Text, Boolean)] = Var((Text.of(initialContent.split("\n").toJSArray), false))

  val isModified: Signal[Boolean] = currentDoc.signal.map(_._2)

  lazy val topElement: Element =
    div(
      CodeMirrorElement(
        highlightingInitialized,
        initialContent,
        Observer { viewUpdate =>
          if viewUpdate.docChanged then
            currentDoc.set((viewUpdate.state.doc, true))
        },
        problems,
      )
    )
  end topElement

  def saveContent()(using ExecutionContext): Future[Unit] =
    val projectID = universeFile.projectID.id
    val content = currentDoc.now()._1.toString()

    for _ <- fileService.saveSourceFile(projectID, sourceName, content).toFuture yield
      currentDoc.update((doc, prevModified) => (doc, false))
  end saveContent
end SourceEditor
