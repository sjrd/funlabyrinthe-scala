package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait FileService extends js.Object:
  def showOpenProjectDialog(): js.Promise[js.UndefOr[String]]
  def showSaveNewProjectDialog(): js.Promise[js.UndefOr[String]]

  def readFileToString(path: String): js.Promise[String]
  def writeStringToFile(path: String, content: String): js.Promise[Unit]
end FileService
