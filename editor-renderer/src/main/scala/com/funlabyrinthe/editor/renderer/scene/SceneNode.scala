package com.funlabyrinthe.editor.renderer.scene

sealed abstract class SceneNode {
  def position: Point
  def ref: Point
}

final case class Graphic(
  material: Material,
  crop: Rectangle,
  position: Point,
  ref: Point,
) extends SceneNode

/** Used to group elements to allow them to be manipulated as a collection.
  */
final case class Group(
  children: Batch[SceneNode],
  position: Point,
  ref: Point,
) extends SceneNode

object Shape {
  final case class Box(
    dimensions: Rectangle,
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode {
    def position: Point = dimensions.topLeft
  }
}
