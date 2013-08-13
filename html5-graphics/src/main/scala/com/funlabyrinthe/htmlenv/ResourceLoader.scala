package com.funlabyrinthe.htmlenv

import com.funlabyrinthe.core.{ ResourceLoader => CoreResourceLoader, _ }
import graphics._

import com.funlabyrinthe.graphics.html._

import scala.collection.mutable

class ResourceLoader(val baseURL: String) extends CoreResourceLoader {

  import ResourceLoader._

  private val imageCache = js.Dictionary.empty

  def loadImage(name: String): Option[Image] = {
    val cachedImage = imageCache(name)
    if (!cachedImage) {
      val loadedImage = doLoadImage(name)
      imageCache(name) = loadedImage.asInstanceOf[js.Any]
      loadedImage
    } else {
      cachedImage.asInstanceOf[Option[Image]]
    }
  }

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

  private def createImageElement(src: String): jsdefs.Image = {
    val element = new jsdefs.Image
    element.src = src
    element
  }
}

object ResourceLoader {
  val Extensions = List(".png", ".gif")
  val ExtensionsWithEmpty = "" :: Extensions

  val ImageNamePrefix = "Images/"
}
