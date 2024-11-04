package com.funlabyrinthe.editor.renderer.model

final case class Version(major: Int, minor: Int):
  require(major >= 0 && minor >= 0, s"$major.$minor")

  override def toString(): String = s"$major.$minor"
end Version

object Version:
  given VersionOrdering: Ordering[Version] with
    def compare(x: Version, y: Version): Int =
      if x.major != y.major then Integer.compare(x.major, x.minor)
      else Integer.compare(x.minor, y.minor)
  end VersionOrdering
end Version
