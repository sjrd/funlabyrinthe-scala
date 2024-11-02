package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait FileService extends js.Object:
  import FileService.*

  def showOpenImageDialog(): js.Promise[js.UndefOr[String]]

  def readFileToString(path: String): js.Promise[String]
  def writeStringToFile(path: String, content: String): js.Promise[Unit]

  def createDirectories(path: String): js.Promise[Unit]

  def listAvailableProjects(): js.Promise[js.Array[ProjectDef]]
  def createNewProject(projectID: String): js.Promise[ProjectDef]
end FileService

object FileService:
  trait ProjectDef extends js.Object:
    val id: String
    val baseURI: String
    val projectFileContent: String
  end ProjectDef
end FileService
