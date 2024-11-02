package com.funlabyrinthe.editor.renderer.model

import com.funlabyrinthe.editor.renderer.ProjectFileContent

case class ProjectDef(
  id: ProjectID,
  projectFileContent: ProjectFileContent
):
  def projectName: String = id.id
end ProjectDef
