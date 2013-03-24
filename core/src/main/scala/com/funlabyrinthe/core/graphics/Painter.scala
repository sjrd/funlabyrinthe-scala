package com.funlabyrinthe.core.graphics

import scala.language.implicitConversions

import scala.collection.GenTraversableOnce

class Painter(val imageLoader: ImageLoader,
    val items: List[Painter.PainterItem] = Nil) {
  import Painter._

  private lazy val image: Image = buildImage()

  def drawTo(context: DrawContext) {
    if (image ne null)
      context.gc.drawImage(image,
          context.minX, context.minY, context.width, context.height)
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
    def apply(painter: Painter, context: DrawContext)
  }

  object PainterItem {
    implicit def fromName(name: String) = ImageDescription(name)
  }

  case class ImageDescription(name: String) extends PainterItem {
    def apply(painter: Painter, context: DrawContext) = {
      painter.imageLoader(name) foreach { image =>
        context.gc.drawImage(image,
            context.minX, context.minY, context.width, context.height)
      }
    }
  }

  class ImageRectDescription(val name: String,
      val rect: Rectangle2D) extends PainterItem {
    def apply(painter: Painter, context: DrawContext) = {
      painter.imageLoader(name) foreach { image =>
        context.gc.drawImage(image,
            rect.minX, rect.minY, rect.width, rect.height,
            context.minX, context.minY, context.width, context.height)
      }
    }
  }
}
