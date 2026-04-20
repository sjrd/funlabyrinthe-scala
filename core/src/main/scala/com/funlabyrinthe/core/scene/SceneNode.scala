package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.scene

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

object Graphic {
  def apply(material: Material, crop: Rectangle): Graphic =
    Graphic(material, crop, Point.zero, crop.size.centerPoint)
}

/** Used to group elements to allow them to be manipulated as a collection.
  */
final case class Group(
  children: Batch[SceneNode],
  position: Point,
  ref: Point,
) extends SceneNode {
  def withRef(newRef: Point): Group =
    this.copy(ref = newRef)
  def withRef(x: Int, y: Int): Group =
    withRef(Point(x, y))

  def moveTo(pt: Point): Group =
    this.copy(position = pt)
  def moveTo(x: Int, y: Int): Group =
    moveTo(Point(x, y))
  def withPosition(newPosition: Point): Group =
    moveTo(newPosition)

  def moveBy(pt: Point): Group =
    moveTo(position + pt)
  def moveBy(x: Int, y: Int): Group =
    moveBy(Point(x, y))

  def addChild(child: SceneNode): Group =
    this.copy(children = children ++ Batch(child))

  def addChildren(additionalChildren: Batch[SceneNode]): Group =
    this.copy(children = children ++ additionalChildren)
}

object Group {
  def apply(children: SceneNode*): Group =
    Group(
      Batch.from(children),
      Point.zero,
      Point.zero,
    )

  def apply(children: Batch[SceneNode]): Group =
    Group(
      children,
      Point.zero,
      Point.zero,
    )

  val empty: Group =
    apply(Batch.empty)
}

object Shape {
  final case class Box(
    dimensions: Rectangle,
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode {
    def position: Point = dimensions.topLeft
  }

  object Box {
    def apply(dimensions: Rectangle, fill: Fill, stroke: Stroke): Box =
      Box(dimensions, fill, stroke, Point.zero)
  }

  final case class Circle(
    circle: scene.Circle,
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode {
    lazy val position: Point =
      circle.center - circle.radius - (stroke.width / 2)

    lazy val size: Size =
      Size(circle.radius * 2) + stroke.width
  }

  object Circle {
    def apply(circle: scene.Circle, fill: Fill): Circle =
      Circle(circle, fill, Stroke.None, Point.zero)
  }

  /** Draws a straight line.
    */
  final case class Line(
    start: Point,
    end: Point,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode {
    lazy val position: Point =
      Point(Math.min(start.x, end.x), Math.min(start.y, end.y)) - (stroke.width / 2)

    lazy val size: Size =
      Size(Math.abs(start.x - end.x), Math.abs(start.y - end.y)) + stroke.width
  }

  /** Draws an arbitrary polygon with up to 16 vertices.
    */
  final case class Polygon(
    vertices: Batch[Point],
    fill: Fill,
    stroke: Stroke,
    ref: Point,
  ) extends SceneNode {
    private lazy val verticesBounds: Rectangle =
      Rectangle.fromPointCloud(vertices).expand(stroke.width / 2)

    lazy val position: Point =
      verticesBounds.topLeft

    lazy val size: Size =
      verticesBounds.size
  }

  object Polygon {
    def apply(vertices: Batch[Point], fill: Fill): Polygon =
      Polygon(vertices, fill, Stroke.None, Point.zero)
  }
}

final case class Text(
  position: Point,
  text: String,
  font: FontKey,
  textColor: RGBA,
  ref: Point,
) extends SceneNode
