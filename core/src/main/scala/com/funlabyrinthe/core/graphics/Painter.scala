package com.funlabyrinthe.core.graphics

import scala.language.implicitConversions

import scala.collection.GenTraversableOnce

class Painter(val imageLoader: ImageLoader,
    val items: List[Painter.PainterItem] = Nil) {
  import Painter._

  private lazy val image: Image = buildImage()

  def drawTo(context: GraphicsContext, x: Double, y: Double) {
    if (image ne null)
      context.drawImage(image, x, y)
  }

  def +(item: PainterItem) =
    new Painter(imageLoader, items :+ item)

  def ++(items1: GenTraversableOnce[PainterItem]) =
    new Painter(imageLoader, items ++ items1)

  private def buildImage(): Image = {
    items match {
      case Nil =>
        null

      case ImageDescription(name) :: Nil =>
        imageLoader(name) getOrElse null

      case _ =>
        // TODO
        ???
    }
  }
}

object Painter {
  abstract class PainterItem {
    def apply(painter: Painter, canvas: Canvas, context: GraphicsContext)
  }

  object PainterItem {
    implicit def fromName(name: String) = ImageDescription(name)
  }

  case class ImageDescription(name: String) extends PainterItem {
    def apply(painter: Painter, canvas: Canvas, context: GraphicsContext) = {
      painter.imageLoader(name) foreach { image =>
        context.drawImage(image, 0, 0, canvas.width.value, canvas.height.value)
      }
    }
  }

  class ImageRectDescription(val name: String,
      val rect: Rectangle2D) extends PainterItem {
    def apply(painter: Painter, canvas: Canvas, context: GraphicsContext) = {
      painter.imageLoader(name) foreach { image =>
        context.drawImage(image,
            rect.minX, rect.minY, rect.width, rect.height,
            0, 0, canvas.width.value, canvas.height.value)
      }
    }
  }
}
