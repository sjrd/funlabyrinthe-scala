package com.funlabyrinthe.core.graphics

import scala.language.implicitConversions

import com.funlabyrinthe.core.{Component, ResourceLoader}
import com.funlabyrinthe.core.pickling.*

final class Painter(
  graphicsSystem: GraphicsSystem,
  resourceLoader: ResourceLoader,
  val items: List[Painter.PainterItem],
) {
  import Painter._

  @transient
  private var knownComplete: Boolean = false

  @transient
  private var imageCache: Option[Option[Image]] = None

  override def toString(): String =
    items.mkString(";")

  override def equals(that: Any) = that match {
    case that: Painter => this.items == that.items
    case _ => false
  }

  override def hashCode(): Int = items.##

  def drawTo(context: DrawContext): Unit = {
    for image <- getImage() do
      context.gc.drawImage(image,
          context.minX, context.minY, context.width, context.height)
  }

  def empty = new Painter(graphicsSystem, resourceLoader, Nil)

  def +(item: PainterItem): Painter =
    new Painter(graphicsSystem, resourceLoader, items :+ item)

  def ++(items1: IterableOnce[PainterItem]): Painter =
    new Painter(graphicsSystem, resourceLoader, items ++ items1)

  def isComplete: Boolean =
    if knownComplete then
      true
    else
      val result = items.forall {
        case PainterItem.ImageDescription(name) => resourceLoader.loadImage(name).forall(_.isComplete)
      }
      if result then
        knownComplete = true
      result
  end isComplete

  private def getImage(): Option[Image] =
    imageCache.getOrElse {
      val (img, cacheValid) = buildImage()
      if cacheValid then
        imageCache = Some(img)
      img
    }

  private def buildImage(): (Option[Image], Boolean) =
    items match {
      case Nil =>
        (None, true)

      case PainterItem.ImageDescription(name) :: Nil =>
        (resourceLoader.loadImage(name), true)

      case _ =>
        val allImages = items.flatMap {
          case PainterItem.ImageDescription(name) => resourceLoader.loadImage(name)
        }
        val validImages = allImages.filter(img => img.isComplete && img.width >= 1.0 && img.height >= 1.0)
        val cacheValid = validImages.sizeCompare(allImages) == 0

        if validImages.isEmpty then
          (None, cacheValid)
        else
          val width = lcm(validImages.map(_.width.toInt))
          val height = lcm(validImages.map(_.height.toInt))

          val canvas = graphicsSystem.createCanvas(width, height)
          for image <- validImages do
            drawRepeat(canvas, image)
          (Some(canvas), cacheValid)
    }
  end buildImage

  private def drawRepeat(canvas: Canvas, image: Image): Unit =
    val width = canvas.width.toInt
    val height = canvas.height.toInt
    val w = image.width.toInt
    val h = image.height.toInt
    for
      x <- 0 until width by w
      y <- 0 until height by h
    do
      canvas.getGraphicsContext2D().drawImage(image, x, y)
  end drawRepeat

  private def lcm(as: List[Int]): Int =
    as.reduce(lcm(_, _))

  private def lcm(a: Int, b: Int): Int = a * b / gcd(a, b)

  private def gcd(a: Int, b: Int): Int =
    if b == 0 then a
    else if a < b then gcd(b, a)
    else gcd(b, a % b)
}

object Painter {
  given PainterPickleable: Pickleable[Painter] with
    def pickle(value: Painter)(using PicklingContext): Pickle =
      ListPickle(value.items.map(Pickleable.pickle(_)))

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Painter] =
      for items <- Pickleable.unpickle[List[PainterItem]](pickle) yield
        val universe = summon[PicklingContext].universe
        Painter(universe.graphicsSystem, universe.resourceLoader, items)
    end unpickle

    def removeReferences(value: Painter, reference: Component)(
        using PicklingContext): Pickleable.RemoveRefResult[Painter] =
      for items <- summon[Pickleable[List[PainterItem]]].removeReferences(value.items, reference) yield
        value.empty ++ items
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
