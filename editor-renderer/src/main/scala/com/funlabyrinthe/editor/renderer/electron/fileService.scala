package com.funlabyrinthe.editor.renderer.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSGlobal
object fileService extends js.Object:
  def showSaveNewProjectDialog(): js.Promise[js.UndefOr[String]] = js.native

  def writeStringToFile(path: String, content: String): js.Promise[Unit] = js.native
end fileService
