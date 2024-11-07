package com.funlabyrinthe.editor.common.model

import scala.math.Ordering

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

final class ProjectFileContent(
  val isLibrary: Boolean,
  val dependencies: List[Dependency],
  val modules: List[String],
):
  override def toString(): String =
    s"""
      |ProjectFileContent(
      |  isLibrary = $isLibrary,
      |  modules = $modules,
      |)
    """.stripMargin.trim()
  end toString
end ProjectFileContent

object ProjectFileContent:
  def parseProject(input: String): ProjectFileContent =
    val structure = js.JSON.parse(input).asInstanceOf[Structure]
    ProjectFileContent(
      isLibrary = structure.isLibrary.getOrElse(false),
      dependencies = structure.dependencies.fold(Nil)(_.toList.map(parseDependency(_))),
      modules = structure.modules.fold(Nil)(_.toList),
    )
  end parseProject

  private def parseDependency(dep: String): Dependency =
    val s"funlaby:$ownerID/$simpleID/$versionStr" = dep: @unchecked
    val projectID = ProjectID(s"$ownerID/$simpleID")
    val version = versionStr match
      case "local-current"  => DependencyVersion.LocalCurrent
      case s"$major.$minor" => DependencyVersion.Versioned(Version(major.toInt, minor.toInt))
    Dependency(projectID, version)
  end parseDependency

  def stringifyProject(project: ProjectFileContent): String =
    def toJSArrayOrUndefined[A](list: List[A]): js.UndefOr[js.Array[A]] =
      if (list.isEmpty) js.undefined
      else list.toJSArray

    val structure = new Structure {
      if project.isLibrary then
        isLibrary = true
      if project.dependencies.nonEmpty then
        dependencies = project.dependencies.map(stringifyDependency(_)).toJSArray
      modules = toJSArrayOrUndefined(project.modules)
    }
    js.JSON.stringify(structure, space = 2)
  end stringifyProject

  private def stringifyDependency(dep: Dependency): String =
    val projectID = dep.projectID.id
    val version = dep.version match
      case DependencyVersion.LocalCurrent => "local-current"
      case DependencyVersion.Versioned(v) => v.toString()
    s"funlaby:$projectID/$version" // note: this is a valid URI
  end stringifyDependency

  private trait Structure extends js.Object:
    var isLibrary: js.UndefOr[Boolean] = js.undefined
    var dependencies: js.UndefOr[js.Array[String]] = js.undefined
    var modules: js.UndefOr[js.Array[String]] = js.undefined
  end Structure
end ProjectFileContent
