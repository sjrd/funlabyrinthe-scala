package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

final class ProjectFileContent(
  val modules: List[String],
  val sources: List[String],
):
  override def toString(): String =
    s"""
      |ProjectFileContent(
      |  modules = $modules,
      |  sources = $sources,
      |)
    """.stripMargin.trim()
  end toString
end ProjectFileContent

object ProjectFileContent:
  def parseProject(input: String): ProjectFileContent =
    val structure = js.JSON.parse(input).asInstanceOf[Structure]
    ProjectFileContent(
      modules = structure.modules.fold(Nil)(_.toList),
      sources = structure.sources.fold(Nil)(_.toList),
    )
  end parseProject

  def stringifyProject(project: ProjectFileContent): String =
    def toJSArrayOrUndefined[A](list: List[A]): js.UndefOr[js.Array[A]] =
      if (list.isEmpty) js.undefined
      else list.toJSArray

    val structure = new Structure {
      modules = toJSArrayOrUndefined(project.modules)
      sources = toJSArrayOrUndefined(project.sources)
    }
    js.JSON.stringify(structure, space = 2)
  end stringifyProject

  private trait Structure extends js.Object:
    var modules: js.UndefOr[js.Array[String]] = js.undefined
    var sources: js.UndefOr[js.Array[String]] = js.undefined
  end Structure
end ProjectFileContent
