package com.funlabyrinthe.gamerunner.scene

import scala.reflect.ClassTag

import java.nio.ByteBuffer
import scala.scalajs.js.typedarray.Int8Array
import scala.scalajs.js.typedarray.TypedArrayBuffer
import java.nio.ByteOrder

object SceneReader {
  def readSceneUpdateFragment(buf: Int8Array): SceneUpdateFragment =
    new SceneReader(TypedArrayBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)).readSceneUpdateFragment()
}

final class SceneReader(buf: ByteBuffer) {
  def readBatch[A](readA: () => A)(using ClassTag[A]): Batch[A] = {
    val len = buf.getInt()
    val arr = new Array[A](len)
    for i <- 0 until len do
      arr(i) = readA()
    IArray.unsafeFromArray(arr)
  }

  def readString(): String = {
    val len = buf.getInt()
    var s = ""
    for i <- 0 until len do
      s += buf.getChar()
    s
  }

  def readFontKey(): FontKey =
    FontKey(readString())

  def readPoint(): Point =
    Point(buf.getInt(), buf.getInt())

  def readSize(): Size =
    Size(buf.getInt(), buf.getInt())

  def readRadians(): Radians =
    Radians(buf.getDouble())

  def readRectangle(): Rectangle =
    Rectangle(readPoint(), readSize())

  def readCircle(): Circle =
    Circle(readPoint(), buf.getInt())

  def readRGBA(): RGBA =
    RGBA(buf.getDouble(), buf.getDouble(), buf.getDouble(), buf.getDouble())

  def readFillColor(): Fill.Color =
    Fill.Color(readRGBA())

  def readFillLinearGradient(): Fill.LinearGradient =
    Fill.LinearGradient(readPoint(), readRGBA(), readPoint(), readRGBA())

  def readFillRadialGradient(): Fill.RadialGradient =
    Fill.RadialGradient(readPoint(), readRGBA(), readPoint(), readRGBA())

  def readFill(): Fill = {
    buf.get().toInt match {
      case 1 => readFillColor()
      case 2 => readFillLinearGradient()
      case 3 => readFillRadialGradient()
    }
  }

  def readStroke(): Stroke =
    Stroke(buf.getInt(), readRGBA())

  def readMaterial(): Material =
    Material(readString(), buf.getDouble(), readRGBA())

  def readGraphic(): Graphic =
    Graphic(readMaterial(), readRectangle(), readPoint(), readPoint())

  def readGroup(): Group =
    Group(readBatch(() => readSceneNode()), readPoint(), readRadians(), readPoint())

  def readShapeBox(): Shape.Box =
    Shape.Box(readRectangle(), readFill(), readStroke(), readPoint())

  def readShapeCircle(): Shape.Circle =
    Shape.Circle(readCircle(), readFill(), readStroke(), readPoint())

  def readShapeLine(): Shape.Line =
    Shape.Line(readPoint(), readPoint(), readStroke(), readPoint())

  def readShapePolygon(): Shape.Polygon =
    Shape.Polygon(readBatch(() => readPoint()), readFill(), readStroke(), readPoint())

  def readText(): Text =
    Text(readPoint(), readString(), readFontKey(), readRGBA(), readPoint())

  def readMasked(): Masked =
    Masked(readSceneNode(), readSceneNode())

  def readSceneNode(): SceneNode = {
    buf.get().toInt match {
      case 1 => readGraphic()
      case 2 => readGroup()
      case 3 => readShapeBox()
      case 4 => readShapeCircle()
      case 5 => readShapeLine()
      case 6 => readShapePolygon()
      case 7 => readText()
      case 8 => readMasked()
    }
  }

  def readSceneUpdateFragment(): SceneUpdateFragment =
    SceneUpdateFragment(readBatch(() => readSceneNode()))
}
