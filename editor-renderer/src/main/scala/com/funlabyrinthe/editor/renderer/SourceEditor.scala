package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.IconName

class SourceEditor(val universeFile: UniverseFile, val sourceName: String)(using ErrorHandler):
  private val sourceFile = universeFile.sourcesDirectory / sourceName

  lazy val topElement: Element =
    div(
      editor.topElement
    )
  end topElement

  lazy val editor: CodeMirrorElement = new CodeMirrorElement

  ErrorHandler.handleErrors {
    for content <- sourceFile.readAsString() yield
      editor.loadContent(content)
  }

  def saveContent(): Future[Unit] =
    editor.getContent().flatMap(sourceFile.writeString(_))
end SourceEditor
