package com.funlabyrinthe.editor.main.node

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object path:
  @js.native
  @JSImport("path")
  def join(parts: String*): String = js.native

  @js.native
  @JSGlobal
  val __dirname: String = js.native
end path
