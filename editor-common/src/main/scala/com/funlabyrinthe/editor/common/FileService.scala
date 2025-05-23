package com.funlabyrinthe.editor.common

import scala.scalajs.js

trait FileService extends js.Object:
  import FileService.*

  def showOpenImageDialog(): js.Promise[js.UndefOr[String]]

  def listAvailableProjects(): js.Promise[js.Array[ProjectDef]]

  def createNewProject(
    projectID: String,
    createAsLibrary: Boolean,
  ): js.Promise[js.Tuple2[ProjectDef, ProjectLoadInfo]]

  def loadProject(projectID: String): js.Promise[ProjectLoadInfo]

  def saveProject(projectID: String, projectFileContent: String,
      universeFileContent: js.UndefOr[String]): js.Promise[Unit]

  def loadSourceFile(projectID: String, sourceFile: String): js.Promise[String]
  def saveSourceFile(projectID: String, sourceFile: String, content: String): js.Promise[Unit]
end FileService

object FileService:
  trait ProjectDef extends js.Object:
    val id: String
    val projectFileContent: String
  end ProjectDef

  trait ProjectLoadInfo extends js.Object:
    val runtimeURI: String
    val universeFileContent: js.UndefOr[String]
    val sourceFiles: js.Array[String]
  end ProjectLoadInfo
end FileService
