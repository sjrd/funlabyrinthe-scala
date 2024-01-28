package com.funlabyrinthe.core.graphics

trait GraphicsContext {

  // State saving

  def save(): Unit
  def restore(): Unit

  // Transformations

  def translate(x: Double, y: Double): Unit
  def scale(x: Double, y: Double): Unit
  def rotate(degrees: Double): Unit

  def transform(
      mxx: Double, myx: Double,
      mxy: Double, myy: Double,
      mxt: Double, myt: Double): Unit

  def setTransform(
      mxx: Double, myx: Double,
      mxy: Double, myy: Double,
      mxt: Double, myt: Double): Unit

  // State, aka configuration

  def globalAlpha: Double
  def globalAlpha_=(value: Double): Unit

  def globalCompositeOperation: GlobalCompositeOperation
  def globalCompositeOperation_=(value: GlobalCompositeOperation): Unit

  def fill: Paint
  def fill_=(value: Paint): Unit

  def stroke: Paint
  def stroke_=(value: Paint): Unit

  def lineWidth: Double
  def lineWidth_=(value: Double): Unit

  def font: Font
  def font_=(value: Font): Unit

  def textAlign: TextAlignment
  def textAlign_=(value: TextAlignment): Unit

  def textBaseline: TextBaseline
  def textBaseline_=(value: TextBaseline): Unit

  // Drawing text

  def fillText(text: String, x: Double, y: Double): Unit
  def strokeText(text: String, x: Double, y: Double): Unit

  // Paths

  def beginPath(): Unit
  def moveTo(x0: Double, y0: Double): Unit
  def lineTo(x1: Double, y1: Double): Unit
  def quadraticCurveTo(xc: Double, yc: Double, x1: Double, y1: Double): Unit
  def bezierCurveTo(xc1: Double, yc1: Double, xc2: Double, yc2: Double,
      x1: Double, y1: Double): Unit
  def arcTo(x1: Double, y1: Double, x2: Double, y2: Double,
      radius: Double): Unit
  def rect(x: Double, y: Double, w: Double, h: Double): Unit
  def closePath(): Unit

  // Drawing of the path

  def fillPath(): Unit
  def strokePath(): Unit

  // Convenience drawing of common paths

  def fillRect(x: Double, y: Double, w: Double, h: Double): Unit
  def strokeRect(x: Double, y: Double, w: Double, h: Double): Unit

  // Drawing images

  def drawImage(img: Image, x: Double, y: Double): Unit
  def drawImage(img: Image, x: Double, y: Double, w: Double, h: Double): Unit
  def drawImage(img: Image,
      sx: Double, sy: Double, sw: Double, sh: Double,
      dx: Double, dy: Double, dw: Double, dh: Double): Unit

  // Special operations

  def multiplyByColor(x: Double, y: Double, w: Double, h: Double, color: Color): Unit

  // Clipping

  def clip(): Unit

  // Miscellaneous

  def isPointInPath(x: Double, y: Double): Boolean

  def clearRect(x: Double, y: Double, w: Double, h: Double): Unit
}
