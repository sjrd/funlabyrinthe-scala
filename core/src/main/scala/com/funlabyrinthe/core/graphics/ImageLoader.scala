package com.funlabyrinthe.core.graphics

import java.net.URL

import scala.collection.JavaConversions._
import scala.collection.concurrent.{ Map => ConcurrentMap }

class ImageLoader(val resourceLoader: ClassLoader)(
    implicit val graphicsSystem: GraphicsSystem) {

  import ImageLoader._

  private val imageCache: ConcurrentMap[String, Option[ImageDescriptor]] =
    new java.util.concurrent.ConcurrentHashMap[String, Option[ImageDescriptor]]

  private def getDescriptor(name: String): Option[ImageDescriptor] = {
    if (name.isEmpty()) None
    else {
      val absoluteName =
        if (name.charAt(0) == '/') name.substring(1)
        else ImageNamePrefix + name

      imageCache.getOrElseUpdate(absoluteName, {
        getImageURLInternal(absoluteName) map (new ImageDescriptor(_))
      })
    }
  }

  private def getImageURLInternal(name: String): Option[URL] = {
    for (extension <- ImageLoader.ExtensionsWithEmpty) {
      val url = resourceLoader.getResource(name+extension)
      if (url ne null)
        return Some(url)
    }
    return None
  }

  def getImageURL(name: String): Option[URL] =
    getDescriptor(name) map (_.url)

  def apply(name: String): Option[Image] =
    getDescriptor(name) map (_.image)
}

object ImageLoader {
  val Extensions = List(".png", ".gif")
  val ExtensionsWithEmpty = "" :: Extensions

  val ImageNamePrefix = "Images/"

  private class ImageDescriptor(val url: URL)(implicit gs: GraphicsSystem) {
    lazy val image = gs.loadImage(url.toString())
  }
}
