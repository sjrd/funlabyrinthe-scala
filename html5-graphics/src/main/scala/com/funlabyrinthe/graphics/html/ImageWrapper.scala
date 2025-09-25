package com.funlabyrinthe.graphics.html

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core.graphics._

class ImageWrapper(val delegate: dom.HTMLImageElement) extends Image {
  def isComplete: Boolean = delegate.complete

  def width: Int = delegate.width
  def height: Int = delegate.height

  def isAnimated: Boolean = false
  def time: Int = 0
  def frames: IArray[Image] = Constants.EmptyImageArray

  val future: Future[this.type] =
    val promise = delegate.asInstanceOf[js.Dynamic].decode().asInstanceOf[js.Promise[Unit]]
    promise.toFuture.map(_ => this)
}
