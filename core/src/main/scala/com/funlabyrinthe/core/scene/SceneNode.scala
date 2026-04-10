package com.funlabyrinthe.core.scene

sealed abstract class SceneNode {
  def position: Point
  def ref: Point
}

final case class Graphic(
  material: Material,
  crop: Rectangle,
  position: Point,
  ref: Point,
)

object Graphic {
  def apply(material: Material, crop: Rectangle): Graphic =
    Graphic(material, crop, Point.zero, crop.size.centerPoint)
}
