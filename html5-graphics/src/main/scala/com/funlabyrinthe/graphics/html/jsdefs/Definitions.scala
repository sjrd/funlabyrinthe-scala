package com.funlabyrinthe.graphics.html.jsdefs

import scala.scalajs.js

object Window extends js.GlobalScope {
  val document: DOMDocument = ???
}

trait DOMDocument extends js.Object {
  def getElementById(id: js.String): HTMLElement
  def createElement(tag: js.String): HTMLElement
}

trait Element extends js.Object {
}

trait HTMLElement extends Element {
}

class Image extends HTMLElement {
  var width: js.Number = _
  var height: js.Number = _

  var src: js.String = _
}

trait HTMLCanvasElement extends HTMLElement {
  var width: js.Number = _
  var height: js.Number = _

  def getContext(kind: js.String): RenderingContext
}

trait RenderingContext extends js.Object

trait CanvasRenderingContext2D extends RenderingContext {
  val canvas: HTMLCanvasElement

  // State saving

  def save(): Unit
  def restore(): Unit

  // Transformations

  def translate(x: js.Number, y: js.Number): Unit
  def scale(x: js.Number, y: js.Number): Unit
  def rotate(degrees: js.Number): Unit

  def transform(
      mxx: js.Number, mxy: js.Number,
      myx: js.Number, myy: js.Number,
      mxt: js.Number, myt: js.Number): Unit

  def setTransform(
      mxx: js.Number, mxy: js.Number,
      myx: js.Number, myy: js.Number,
      mxt: js.Number, myt: js.Number): Unit

  // State, aka configuration

  var globalAlpha: js.Number
  var fillStyle: js.String
  var strokeStyle: js.String
  var lineWidth: js.Number
  var font: js.String
  var textAlign: js.String
  var textBaseline: js.String

  // Drawing text

  def fillText(text: js.String, x: js.Number, y: js.Number): Unit
  def strokeText(text: js.String, x: js.Number, y: js.Number): Unit
  def measureText(text: js.String): TextMeasure

  // Paths

  def beginPath(): Unit
  def moveTo(x0: js.Number, y0: js.Number): Unit
  def lineTo(x1: js.Number, y1: js.Number): Unit
  def quadraticCurveTo(xc: js.Number, yc: js.Number, x1: js.Number, y1: js.Number): Unit
  def bezierCurveTo(xc1: js.Number, yc1: js.Number, xc2: js.Number, yc2: js.Number,
      x1: js.Number, y1: js.Number): Unit
  def arcTo(x1: js.Number, y1: js.Number, x2: js.Number, y2: js.Number,
      radius: js.Number): Unit
  def rect(x: js.Number, y: js.Number, w: js.Number, h: js.Number): Unit
  def closePath(): Unit

  // Drawing of the path

  def fill(): Unit
  def stroke(): Unit

  // Convenience drawing of common paths

  def fillRect(x: js.Number, y: js.Number, w: js.Number, h: js.Number): Unit
  def strokeRect(x: js.Number, y: js.Number, w: js.Number, h: js.Number): Unit

  // Drawing images

  def drawImage(img: Image,
      x: js.Number, y: js.Number): Unit
  def drawImage(img: Image,
      x: js.Number, y: js.Number, w: js.Number, h: js.Number): Unit
  def drawImage(img: Image,
      sx: js.Number, sy: js.Number, sw: js.Number, sh: js.Number,
      dx: js.Number, dy: js.Number, dw: js.Number, dh: js.Number)

  // Clipping

  def clip(): Unit

  // Miscellaneous

  def isPointInPath(x: js.Number, y: js.Number): js.Boolean

  def clearRect(x: js.Number, y: js.Number, w: js.Number, h: js.Number): Unit
}

trait TextMeasure extends js.Object {
  val width: js.Number = ???
}

trait KeyboardEvent extends js.Object {
  val keyCode: js.Number = ???
  val shiftKey: js.Boolean = ???
  val ctrlKey: js.Boolean = ???
  val altKey: js.Boolean = ???
  val metaKey: js.Boolean = ???
}

trait MouseEvent extends js.Object {
  val button: js.Number = ???
  val clientX: js.Number = ???
  val clientY: js.Number = ???
}
