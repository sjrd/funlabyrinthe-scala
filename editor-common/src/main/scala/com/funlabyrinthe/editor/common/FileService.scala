package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait FileService extends js.Object:
  def funlabyCoreLibs(): js.Promise[js.Array[String]]

  def showOpenImageDialog(): js.Promise[js.UndefOr[String]]

  def readFileToString(path: String): js.Promise[String]
  def writeStringToFile(path: String, content: String): js.Promise[Unit]

  def createDirectories(path: String): js.Promise[Unit]

  def listAvailableProjects(): js.Promise[js.Array[String]]
  def createNewProject(projectName: String): js.Promise[String]
end FileService
