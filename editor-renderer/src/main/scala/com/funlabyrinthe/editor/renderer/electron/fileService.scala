package com.funlabyrinthe.editor.renderer.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSGlobal
object fileService extends js.Object:
  def showOpenProjectDialog(): js.Promise[js.UndefOr[String]] = js.native
  def showSaveNewProjectDialog(): js.Promise[js.UndefOr[String]] = js.native

  def readFileToString(path: String): js.Promise[String] = js.native
  def writeStringToFile(path: String, content: String): js.Promise[Unit] = js.native
end fileService
