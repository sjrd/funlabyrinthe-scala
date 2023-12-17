package com.funlabyrinthe.htmlenv

import java.io.IOException

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core.{ ResourceLoader => CoreResourceLoader, _ }
import graphics._

import com.funlabyrinthe.graphics.html._
import scala.util.Success
import scala.util.Failure

class ResourceLoader(val baseURL: String) extends CoreResourceLoader {

  import ResourceLoader._

  private val imageCache = mutable.Map.empty[String, Option[Image]]

  def loadImage(name: String): Option[Image] =
    imageCache.getOrElseUpdate(name, doLoadImage(name))

  private def doLoadImage(name: String): Option[Image] = {
    if (name.isEmpty()) {
      None
    } else {
      val delayedImage = new DelayedImage

      val underlying: Future[Image] = fetchAlternatives(name).flatMap { (ext, response) =>
        ext match
          case ".gif" =>
            response.arrayBuffer().toFuture.map(new GIFImage(_))
          case _ =>
            response.blob().toFuture.flatMap { blob =>
              val image = new dom.Image()
              image.src = dom.URL.createObjectURL(blob)
              new ImageWrapper(image).future
            }
      }

      underlying.onComplete {
        case Success(underlying) =>
          delayedImage.complete(underlying)

        case Failure(exception) =>
          delayedImage.completeAsError()
          exception.printStackTrace() // TODO Log this somewhere better
      }

      Some(delayedImage)
    }
  }

  private def fetchAlternatives(name: String): Future[(String, dom.Response)] =
    val relPath =
      if name.charAt(0) == '/' then name.substring(1)
      else ImageNamePrefix + name

    def loop(extensions: List[String]): Future[(String, dom.Response)] =
      extensions match
        case Nil =>
          Future.failed(new IOException(s"Cannot find any image resource named '$name'"))

        case ext :: restExts =>
          dom.fetch(baseURL + relPath + ext).toFuture.map(ext -> _).recoverWith {
            case th: js.JavaScriptException =>
              loop(restExts)
          }
    end loop

    loop(Extensions)
  end fetchAlternatives
}

object ResourceLoader {
  val Extensions = List(".png", ".gif")
  val ExtensionsWithEmpty = "" :: Extensions

  val ImageNamePrefix = "Images/"
}
