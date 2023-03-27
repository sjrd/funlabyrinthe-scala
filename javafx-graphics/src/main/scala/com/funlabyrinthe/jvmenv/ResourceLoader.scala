package com.funlabyrinthe.jvmenv

import com.funlabyrinthe.core.{ ResourceLoader => CoreResourceLoader, _ }
import graphics._

import com.funlabyrinthe.graphics.jfx._

import scala.collection.mutable

class ResourceLoader(val loader: ClassLoader)
extends com.funlabyrinthe.core.ResourceLoader {

  import ResourceLoader._

  private val imageCache = mutable.Map.empty[String, Option[Image]]

  def loadImage(name: String): Option[Image] = {
    imageCache.getOrElseUpdate(name, doLoadImage(name))
  }

  private def doLoadImage(name: String): Option[Image] = {
    if (name.isEmpty()) {
      None
    } else {
      val absoluteName =
        if (name.charAt(0) == '/') name.substring(1)
        else ImageNamePrefix + name

      lookupImageNameToURL(absoluteName) map (loadImageFromURL(_))
    }
  }

  private def lookupImageNameToURL(name: String): Option[String] = {
    ResourceLoader.ExtensionsWithEmpty
      .iterator
      .map(extension => loader.getResource(name + extension))
      .collectFirst {
        case url if url != null => url.toString()
      }
  }

  private def loadImageFromURL(url: String): Image =
    new ImageWrapper(new javafx.scene.image.Image(url))
}

object ResourceLoader {
  val Extensions = List(".png", ".gif")
  val ExtensionsWithEmpty = "" :: Extensions

  val ImageNamePrefix = "Images/"
}
