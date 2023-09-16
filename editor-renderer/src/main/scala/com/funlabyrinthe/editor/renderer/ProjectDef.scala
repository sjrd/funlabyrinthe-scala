package com.funlabyrinthe.editor.renderer

case class ProjectDef(
  projectDir: File
):
  def projectName: String = projectDir.name
end ProjectDef
