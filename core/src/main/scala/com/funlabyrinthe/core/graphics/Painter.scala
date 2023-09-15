package com.funlabyrinthe.core.graphics

import scala.language.implicitConversions

import com.funlabyrinthe.core.ResourceLoader

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

      case ImageDescription(name) :: Nil =>
        resourceLoader.loadImage(name) getOrElse null

      case _ =>
        // TODO
        ???
    }
  }
}

object Painter {
  abstract class PainterItem {
    def apply(painter: Painter, context: DrawContext): Unit
  }

  object PainterItem {
    implicit def fromName(name: String): ImageDescription =
      ImageDescription(name)
  }

  case class ImageDescription(name: String) extends PainterItem {
    def apply(painter: Painter, context: DrawContext) = {
      painter.resourceLoader.loadImage(name) foreach { image =>
        context.gc.drawImage(image,
            context.minX, context.minY, context.width, context.height)
      }
    }

    override def toString(): String = name
  }

  class ImageRectDescription(val name: String,
      val rect: Rectangle2D) extends PainterItem {
    def apply(painter: Painter, context: DrawContext) = {
      painter.resourceLoader.loadImage(name) foreach { image =>
        context.gc.drawImage(image,
            rect.minX, rect.minY, rect.width, rect.height,
            context.minX, context.minY, context.width, context.height)
      }
    }
  }
}
