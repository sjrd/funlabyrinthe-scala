package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

object ProjectFileContent:
  def parseProject(input: String): Project =
    js.JSON.parse(input).asInstanceOf[Project]

  def stringifyProject(project: Project): String =
    js.JSON.stringify(project, space = 2)

  trait Project extends js.Object:
    var modules: js.UndefOr[js.Array[String]] = js.undefined
    var sources: js.UndefOr[js.Array[String]] = js.undefined
  end Project
end ProjectFileContent
