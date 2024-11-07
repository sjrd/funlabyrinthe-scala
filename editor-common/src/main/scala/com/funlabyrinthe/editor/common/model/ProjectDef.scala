package com.funlabyrinthe.editor.common.model

import scala.concurrent.{ExecutionContext, Future}

import com.funlabyrinthe.editor.common.FileService

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
end ProjectDef
