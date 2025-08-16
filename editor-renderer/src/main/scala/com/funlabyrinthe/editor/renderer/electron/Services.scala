package com.funlabyrinthe.editor.renderer.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.funlabyrinthe.editor.common.*
import com.funlabyrinthe.editor.common.model.ProjectDef

import com.funlabyrinthe.editor.renderer.JSPI

@js.native
@JSGlobal
val compilerService: CompilerService = js.native

@js.native
@JSGlobal
val fileService: FileService = js.native

object Services:
  def listAvailableProjects(): List[ProjectDef] =
    val projects = JSPI.await(fileService.listAvailableProjects())
    projects.toList.map(ProjectDef.fromFileServiceProjectDef(_))
  end listAvailableProjects
end Services
