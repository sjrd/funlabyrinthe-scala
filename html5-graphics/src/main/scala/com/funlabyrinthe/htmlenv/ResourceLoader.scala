package com.funlabyrinthe.htmlenv

import scala.scalajs.js.Dynamic
import org.scalajs.dom.{raw => dom}

import com.funlabyrinthe.core.{ ResourceLoader => CoreResourceLoader, _ }
import graphics._

import com.funlabyrinthe.graphics.html._

import scala.collection.mutable

class ResourceLoader(val baseURL: String) extends CoreResourceLoader {

  import ResourceLoader._

  private val imageCache = mutable.Map.empty[String, Option[Image]]

  def loadImage(name: String): Option[Image] =
    imageCache.getOrElseUpdate(name, doLoadImage(name))

  private def doLoadImage(name: String): Option[Image] = {
    if (name.isEmpty()) {
      None
    } else {
      val absoluteName = baseURL + (
        if (name.charAt(0) == '/') name.substring(1)
        else ImageNamePrefix + name) + ".png"

      val image = createImageElement(absoluteName)
      Some(new ImageWrapper(image))
    }
  }

  private def createImageElement(src: String): dom.HTMLImageElement = {
    val element = Dynamic.newInstance(
        Dynamic.global.Image)().asInstanceOf[dom.HTMLImageElement]
    element.src = src
    element
  }
}

object ResourceLoader {
  val Extensions = List(".png", ".gif")
  val ExtensionsWithEmpty = "" :: Extensions

  val ImageNamePrefix = "Images/"
}
