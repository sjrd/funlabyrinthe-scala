package com.funlabyrinthe.graphics.html

import scala.language.implicitConversions

import com.funlabyrinthe.core.graphics._

import Conversions._

class GraphicsContextWrapper(
    val delegate: jsdefs.CanvasRenderingContext2D) extends GraphicsContext {

  // State saving

  def save(): Unit = delegate.save()
  def restore(): Unit = delegate.restore()

  // Transformations

  def translate(x: Double, y: Double): Unit =
    delegate.translate(x, y)

  def scale(x: Double, y: Double): Unit =
    delegate.scale(x, y)

  def rotate(degrees: Double): Unit =
    delegate.rotate(degrees)

  def transform(
      mxx: Double, myx: Double,
      mxy: Double, myy: Double,
      mxt: Double, myt: Double): Unit = {
    // ! Order of parameters is different
    delegate.transform(mxx, mxy, myx, myy, mxt, myt)
  }

  def setTransform(
      mxx: Double, myx: Double,
      mxy: Double, myy: Double,
      mxt: Double, myt: Double): Unit = {
    // ! Order of parameters is different
    delegate.setTransform(mxx, mxy, myx, myy, mxt, myt)
  }

  // State, aka configuration

  def globalAlpha: Double = delegate.globalAlpha
  def globalAlpha_=(value: Double): Unit = delegate.globalAlpha = value

  def fill: Paint = htmlPaint2core(delegate.fillStyle)
  def fill_=(value: Paint): Unit =
    delegate.fillStyle = corePaint2html(value)

  def stroke: Paint = htmlPaint2core(delegate.strokeStyle)
  def stroke_=(value: Paint): Unit =
    delegate.strokeStyle = corePaint2html(value)

  def lineWidth: Double = delegate.lineWidth
  def lineWidth_=(value: Double): Unit = delegate.lineWidth = value

  def font: Font = htmlFont2core(delegate.font)
  def font_=(value: Font): Unit =
    delegate.font = coreFont2html(value)

  def textAlign: TextAlignment = htmlTextAlign2core(delegate.textAlign)
  def textAlign_=(value: TextAlignment): Unit =
    delegate.textAlign = coreTextAlign2html(value)

  def textBaseline: TextBaseline = htmlTextBaseline2core(delegate.textBaseline)
  def textBaseline_=(value: TextBaseline): Unit =
    delegate.textBaseline = coreTextBaseline2html(value)

  // Drawing text

  def fillText(text: String, x: Double, y: Double): Unit =
    delegate.fillText(text, x, y)

  def strokeText(text: String, x: Double, y: Double): Unit =
    delegate.strokeText(text, x, y)

  // Paths

  def beginPath(): Unit =
    delegate.beginPath()

  def moveTo(x0: Double, y0: Double): Unit =
    delegate.moveTo(x0, y0)

  def lineTo(x1: Double, y1: Double): Unit =
    delegate.lineTo(x1, y1)

  def quadraticCurveTo(xc: Double, yc: Double, x1: Double, y1: Double): Unit =
    delegate.quadraticCurveTo(xc, yc, x1, y1)

  def bezierCurveTo(xc1: Double, yc1: Double, xc2: Double, yc2: Double,
      x1: Double, y1: Double): Unit =
    delegate.bezierCurveTo(xc1, yc1, xc2, yc2, x1, y1)

  def arcTo(x1: Double, y1: Double, x2: Double, y2: Double,
      radius: Double): Unit =
    delegate.arcTo(x1, y1, x2, y2, radius)

  def rect(x: Double, y: Double, w: Double, h: Double): Unit =
    delegate.rect(x, y, w, h)

  def closePath(): Unit =
    delegate.closePath()

  // Drawing of the path

  def fillPath(): Unit =
    delegate.fill()

  def strokePath(): Unit =
    delegate.stroke()

  // Convenience drawing of common paths

  def fillRect(x: Double, y: Double, w: Double, h: Double): Unit =
    delegate.fillRect(x, y, w, h)

  def strokeRect(x: Double, y: Double, w: Double, h: Double): Unit =
    delegate.strokeRect(x, y, w, h)

  // Drawing images

  def drawImage(img: Image, x: Double, y: Double): Unit =
    delegate.drawImage(img, x, y)

  def drawImage(img: Image, x: Double, y: Double, w: Double, h: Double): Unit =
    delegate.drawImage(img, x, y, w, h)

  def drawImage(img: Image,
      sx: Double, sy: Double, sw: Double, sh: Double,
      dx: Double, dy: Double, dw: Double, dh: Double) = {
    delegate.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh)
  }

  // Clipping

  def clip(): Unit =
    delegate.clip()

  // Miscellaneous

  def isPointInPath(x: Double, y: Double): Boolean =
    delegate.isPointInPath(x, y)

  def clearRect(x: Double, y: Double, w: Double, h: Double): Unit =
    delegate.clearRect(x, y, w, h)

  // Private conversions

  implicit def coreImage2html(image: Image): jsdefs.Image =
    image.asInstanceOf[ImageWrapper].delegate
}
