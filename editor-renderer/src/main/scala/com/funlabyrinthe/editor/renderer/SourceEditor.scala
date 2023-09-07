package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
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
  private val currentDoc: Var[Text] = Var(Text.of(initialContent.split("\n").toJSArray))

  lazy val topElement: Element =
    div(
      CodeMirrorElement(initialContent, currentDoc.writer.contramap(_.state.doc))
    )
  end topElement

  def saveContent(): Future[Unit] =
    sourceFile.writeString(currentDoc.now().toString())
end SourceEditor
