package com.funlabyrinthe.editor.renderer.model

enum DependencyVersion:
  case LocalCurrent
  case Versioned(version: Version)

  def displayString: String = this match
    case LocalCurrent       => "current local version"
    case Versioned(version) => s"v$version"
end DependencyVersion

object DependencyVersion:
  given DependencyVersionOrdering: Ordering[DependencyVersion] with
    def compare(x: DependencyVersion, y: DependencyVersion): Int = (x, y) match
      case (LocalCurrent, LocalCurrent)   => 0
      case (LocalCurrent, _: Versioned)   => 1
      case (_: Versioned, LocalCurrent)   => -1
      case (Versioned(vx), Versioned(vy)) => summon[Ordering[Version]].compare(vx, vy)
    end compare
  end DependencyVersionOrdering
end DependencyVersion
