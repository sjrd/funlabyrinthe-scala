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
