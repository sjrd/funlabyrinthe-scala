package com.funlabyrinthe.htmlenv

import scala.scalajs.js.Dynamic
import org.scalajs.dom

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
      val ext = if name == "Fields/Water" then ".gif" else ".png"
      val relPath =
        if name.charAt(0) == '/' then name.substring(1)
        else ImageNamePrefix + name
      val absoluteName = baseURL + relPath + ext

      if ext == ".gif" then
        Some(new GIFImage(absoluteName))
      else
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
