package com.funlabyrinthe.core.graphics

import scala.collection.mutable
import scala.Conversion.into

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

  def drawStretchedTo(context: DrawContext): Unit = {
    for image <- getImage() do
      context.gc.drawImage(image, context.tickCount,
          context.minX, context.minY, context.width, context.height)
  }

  def drawTiledTo(context: DrawContext, posX: Int, posY: Int): Unit = {
    // TODO If our size is not a multiple of the context size, things won't get properly tiled. Fix it?
    for image <- getImage() if image.width > 0 && image.height > 0 do
      val width = Math.rint(context.width).toInt
      val height = Math.rint(context.height).toInt
      val srcX = Math.floorMod(posX, image.width / width) * width
      val srcY = Math.floorMod(posY, image.height / height) * height
      context.gc.drawImage(image, context.tickCount,
          srcX, srcY, width, height,
          context.minX, context.minY, context.width, context.height)
  }

  def empty = new Painter(graphicsSystem, resourceLoader, Nil)

  def +(item: into[PainterItem]): Painter =
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
        val validImages = allImages.filter(img => img.isComplete && img.width > 0 && img.height > 0)
        val cacheValid = validImages.sizeCompare(allImages) == 0

        if validImages.isEmpty then
          (None, cacheValid)
        else
          val width = lcm(validImages.map(_.width))
          val height = lcm(validImages.map(_.height))

          val builtImage =
            if validImages.exists(_.isAnimated) then
              makeAnimated(width, height, validImages)
            else
              makeStatic(width, height, validImages)

          (Some(builtImage), cacheValid)
    }
  end buildImage

  private def makeStatic(width: Int, height: Int, images: List[Image]): Image =
    val canvas = graphicsSystem.createCanvas(width, height)
    for image <- images do
      drawRepeat(canvas, image, tickCount = 0L)
    canvas
  end makeStatic

  private def makeAnimated(width: Int, height: Int, images: List[Image]): Image =
    // Compute total time - least common multiple of all total times
    val totalTime = lcm(images.filter(_.isAnimated).map(_.time))

    // Build set of transition points
    val transitionPointsSet = mutable.SortedSet.empty[Int]
    for image <- images if image.isAnimated do
      val frames = image.frames
      Iterator.from(0)
        .map(i => image.frames(i % frames.length).time) // infinite cycle of frame times
        .scanLeft(0)((totalSoFar, frameTime) => totalSoFar + frameTime) // infinite cumulated times
        .takeWhile(_ < totalTime) // cut off once we have enough
        .foreach(transitionPointsSet += _) // add them to the transition points
    end for

    // Add totalTime itself, so we have both 0 and totalTime
    transitionPointsSet += totalTime

    val transitionPoints = transitionPointsSet.toList

    // Now create one frame for each transition point
    val frames: List[Canvas] =
      for (startTime, endTime) <- transitionPoints.zip(transitionPoints.tail) yield
        val tickCount = Integer.toUnsignedLong(startTime)
        val canvas = graphicsSystem.createFrameCanvas(width, height, time = endTime - startTime)
        for image <- images do
          drawRepeat(canvas, image, tickCount)
        canvas

    graphicsSystem.createAnimated(frames)
  end makeAnimated

  private def drawRepeat(canvas: Canvas, image: Image, tickCount: Long): Unit =
    val width = canvas.width.toInt
    val height = canvas.height.toInt
    val w = image.width.toInt
    val h = image.height.toInt
    for
      x <- 0 until width by w
      y <- 0 until height by h
    do
      canvas.getGraphicsContext2D().drawImage(image, tickCount, x, y)
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

  // TODO Add `into` when we upgrade from 3.7.3 to 3.8.0
  enum PainterItem derives Pickleable:
    case ImageDescription(name: String)

    override def toString(): String = this match
      case ImageDescription(name) => name
  end PainterItem

  object PainterItem:
    given StringToPainterItem: Conversion[String, PainterItem] with
      def apply(name: String): PainterItem = ImageDescription(name)
  end PainterItem
}
