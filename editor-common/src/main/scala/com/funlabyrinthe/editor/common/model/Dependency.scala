package com.funlabyrinthe.editor.common.model

final case class Dependency(projectID: ProjectID, version: DependencyVersion)

object Dependency:
  given DependencyOrdering: Ordering[Dependency] =
    Ordering.by(dep => (dep.projectID, dep.version))
