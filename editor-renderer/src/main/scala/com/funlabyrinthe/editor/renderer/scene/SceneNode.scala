package com.funlabyrinthe.editor.renderer.scene

import com.funlabyrinthe.editor.renderer.scene

sealed abstract class SceneNode

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
  ) extends SceneNode

  final case class Circle(
    circle: scene.Circle,
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode

  /** Draws a straight line.
    */
  final case class Line(
    start: Point,
    end: Point,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode

  /** Draws an arbitrary polygon with up to 16 vertices.
    */
  final case class Polygon(
    vertices: Batch[Point],
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode
}

final case class Text(
  position: Point,
  text: String,
  font: FontKey,
  textColor: RGBA,
  ref: Point,
) extends SceneNode
