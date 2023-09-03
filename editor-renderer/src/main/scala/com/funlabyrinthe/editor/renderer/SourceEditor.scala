package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.IconName

class SourceEditor(val universeFile: UniverseFile, val sourceName: String):
  lazy val topElement: Element =
    div(
      p(sourceName),
      editor.topElement
    )
  end topElement

  lazy val editor: CodeMirrorElement = new CodeMirrorElement
end SourceEditor
