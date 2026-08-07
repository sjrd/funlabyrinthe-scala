package com.funlabyrinthe.core.scene

import scala.Conversion.into

import com.funlabyrinthe.core.Component
import com.funlabyrinthe.core.pickling.*

final class Painter(
  val items: List[Painter.PainterItem],
) {
  import Painter._

  @transient
  private var presentCache: Batch[SceneNode] | Null = null

  override def toString(): String =
    items.mkString(";")

  override def equals(that: Any) = that match {
    case that: Painter => this.items == that.items
    case _ => false
  }

  override def hashCode(): Int = items.##

  def presentTiled(posX: Int, posY: Int, cellSize: Size): Batch[SceneNode] = {
    Batch.from(items.map {
      case PainterItem.ImageDescription(name, width, height) =>
        val srcX = Math.floorMod(posX, width / cellSize.width) * cellSize.width
        val srcY = Math.floorMod(posY, height / cellSize.height) * cellSize.height
        Graphic(Material(name), Rectangle.ltwh(srcX, srcY, cellSize.width, cellSize.height))
    })
  }

  def present(): Batch[SceneNode] = {
    val cached = presentCache
    if cached != null then
      cached
    else
      val computed = computePresent()
      presentCache = computed
      computed
  }

  private def computePresent(): Batch[SceneNode] = {
    Batch.from(items.map {
      case PainterItem.ImageDescription(name, width, height) =>
        Graphic(Material(name), Rectangle.ltwh(0, 0, width, height))
    })
  }

  def empty = new Painter(Nil)

  def +(item: into[PainterItem]): Painter =
    new Painter(items :+ item)

  def ++(items1: IterableOnce[PainterItem]): Painter =
    new Painter(items ++ items1)
}

object Painter {
  val Empty = new Painter(Nil)

  def apply(items: List[PainterItem]): Painter =
    new Painter(items)

  given PainterPickleable: Pickleable[Painter] with
    def pickle(value: Painter)(using PicklingContext): Pickle =
      ListPickle(value.items.map(Pickleable.pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Painter] =
      for items <- Pickleable.unpickle[List[PainterItem]](pickle) yield
        Painter(items)
    end unpickle

    def removeReferences(value: Painter, reference: Component)(
        using PicklingContext): Pickleable.RemoveRefResult[Painter] =
      for items <- summon[Pickleable[List[PainterItem]]].removeReferences(value.items, reference) yield
        value.empty ++ items
  end PainterPickleable

  into enum PainterItem derives Pickleable:
    case ImageDescription(name: String, width: Int, height: Int)

    override def toString(): String = this match
      case ImageDescription(name, width, height) => s"$name ${width}x${height}"
  end PainterItem

  object PainterItem:
    given StringToPainterItem: Conversion[String, PainterItem] with
      def apply(name: String): PainterItem = ImageDescription(name, 30, 30)
  end PainterItem
}
