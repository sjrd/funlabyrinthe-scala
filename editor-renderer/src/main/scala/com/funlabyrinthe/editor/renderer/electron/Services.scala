package com.funlabyrinthe.editor.renderer.electron

import scala.concurrent.{ExecutionContext, Future}

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.funlabyrinthe.editor.common.*
import com.funlabyrinthe.editor.common.model.ProjectDef

@js.native
@JSGlobal
val compilerService: CompilerService = js.native

@js.native
@JSGlobal
val fileService: FileService = js.native

object Services:
  def listAvailableProjects()(using ExecutionContext): Future[List[ProjectDef]] =
    for projects <- fileService.listAvailableProjects().toFuture yield
      projects.toList.map(ProjectDef.fromFileServiceProjectDef(_))
  end listAvailableProjects
end Services
