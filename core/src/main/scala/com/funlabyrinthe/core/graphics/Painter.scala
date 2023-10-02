package com.funlabyrinthe.core.graphics

import scala.language.implicitConversions

import com.funlabyrinthe.core.ResourceLoader
import com.funlabyrinthe.core.pickling.*

import scala.collection.GenTraversableOnce

final class Painter(val resourceLoader: ResourceLoader,
    val items: List[Painter.PainterItem] = Nil) {
  import Painter._

  @transient
  private lazy val image: Image = buildImage()

  override def toString(): String =
    items.mkString(";")

  override def equals(that: Any) = that match {
    case that: Painter => this.items == that.items
    case _ => false
  }

  override def hashCode(): Int = items.##

  def drawTo(context: DrawContext): Unit = {
    if (image ne null)
      context.gc.drawImage(image,
          context.minX, context.minY, context.width, context.height)
  }

  def empty = new Painter(resourceLoader, Nil)

  def +(item: PainterItem): Painter =
    new Painter(resourceLoader, items :+ item)

  def ++(items1: GenTraversableOnce[PainterItem]): Painter =
    new Painter(resourceLoader, items ++ items1)

  private def buildImage(): Image = {
    items match {
      case Nil =>
        null

      case PainterItem.ImageDescription(name) :: Nil =>
        resourceLoader.loadImage(name) getOrElse null

      case _ =>
        // TODO
        ???
    }
  }
}

object Painter {
  given PainterPickleable: Pickleable[Painter] with
    def pickle(value: Painter)(using PicklingContext): Pickle =
      ListPickle(value.items.map(Pickleable.pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Painter] =
      pickle match
        case ListPickle(itemPickles) =>
          val optItems = itemPickles.map(Pickleable.unpickle[PainterItem](_))
          val items = optItems.flatten // ignores invalid items
          val resourceLoader = summon[PicklingContext].universe.resourceLoader
          Some(Painter(resourceLoader, items))
        case _ =>
          None
    end unpickle
  end PainterPickleable

  enum PainterItem derives Pickleable:
    case ImageDescription(name: String)

    override def toString(): String = this match
      case ImageDescription(name) => name
  end PainterItem

  object PainterItem:
    implicit def fromName(name: String): ImageDescription =
      ImageDescription(name)
  end PainterItem
}
