package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

abstract class Editor(
  val project: Project,
)(using ErrorHandler):
  val tabTitle: String
  val isModified: Signal[Boolean]

  def saveContent(): Unit

  lazy val topElement: Signal[Element]
end Editor
