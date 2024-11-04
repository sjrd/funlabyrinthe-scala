package com.funlabyrinthe.editor.renderer.model

import scala.concurrent.{ExecutionContext, Future}

import com.funlabyrinthe.editor.common.FileService
import com.funlabyrinthe.editor.renderer.ProjectFileContent
import com.funlabyrinthe.editor.renderer.electron.fileService

case class ProjectDef(
  id: ProjectID,
  projectFileContent: ProjectFileContent
):
  def projectName: String = id.id
end ProjectDef

object ProjectDef:
  def fromFileServiceProjectDef(proj: FileService.ProjectDef): ProjectDef =
    ProjectDef(
      ProjectID(proj.id),
      ProjectFileContent.parseProject(proj.projectFileContent)
    )
  end fromFileServiceProjectDef

  def listAvailableProjects()(using ExecutionContext): Future[List[ProjectDef]] =
    for projects <- fileService.listAvailableProjects().toFuture yield
      projects.toList.map(fromFileServiceProjectDef(_))
  end listAvailableProjects
end ProjectDef
