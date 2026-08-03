package com.funlabyrinthe.corebridge

import java.nio.ByteBuffer
import java.nio.ByteOrder

import scala.scalajs.js.typedarray.*
import scala.scalajs.js.typedarray.TypedArrayBufferOps.*

import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.scene.Shape.Box
import com.funlabyrinthe.core.scene.Shape.Line
import com.funlabyrinthe.core.scene.Shape.Polygon

object SceneWriter {
  def writeSceneUpdateFragment(fragment: SceneUpdateFragment): Int8Array = {
    val writer = new SceneWriter()
    writer.writeSceneUpdateFragment(fragment)
    writer.result()
  }
}

private final class SceneWriter {
  private val buf = TypedArrayBuffer.wrap(new Int8Array(64 * 1024)).order(ByteOrder.LITTLE_ENDIAN)

  def result(): Int8Array =
    new Int8Array(buf.typedArray().buffer, 0, buf.position())

  def writeBatch[A](batch: Batch[A])(writeA: A => Unit): Unit = {
    buf.putInt(batch.size)
    batch.foreach(writeA(_))
  }

  def writeString(s: String): Unit = {
    val len = s.length()
    buf.putInt(len)
    for i <- 0 until len do
      buf.putChar(s.charAt(i))
  }

  def writeFontKey(fontKey: FontKey): Unit = {
    writeString(fontKey.key)
  }

  def writePoint(point: Point): Unit = {
    buf.putInt(point.x)
    buf.putInt(point.y)
  }

  def writeSize(size: Size): Unit = {
    buf.putInt(size.width)
    buf.putInt(size.height)
  }

  def writeRectangle(rect: Rectangle): Unit = {
    writePoint(rect.topLeft)
    writeSize(rect.size)
  }

  def writeCircle(circle: Circle): Unit = {
    writePoint(circle.center)
    buf.putInt(circle.radius)
  }

  def writeRGBA(rgba: RGBA): Unit = {
    buf.putDouble(rgba.red)
    buf.putDouble(rgba.green)
    buf.putDouble(rgba.blue)
    buf.putDouble(rgba.alpha)
  }

  def writeFill(fill: Fill): Unit = {
    fill match {
      case Fill.Color(color) =>
        buf.put(1.toByte)
        writeRGBA(color)
      case Fill.LinearGradient(fromPoint, fromColor, toPoint, toColor) =>
        buf.put(2.toByte)
        writePoint(fromPoint)
        writeRGBA(fromColor)
        writePoint(toPoint)
        writeRGBA(toColor)
    }
  }

  def writeStroke(stroke: Stroke): Unit = {
    buf.putInt(stroke.width)
    writeRGBA(stroke.color)
  }

  def writeMaterial(material: Material): Unit = {
    val Material(asset, alpha, tint) = material
    writeString(asset)
    buf.putDouble(alpha)
    writeRGBA(tint)
  }

  def writeGraphic(graphic: Graphic): Unit = {
    val Graphic(material, crop, position, ref) = graphic
    writeMaterial(material)
    writeRectangle(crop)
    writePoint(position)
    writePoint(ref)
  }

  def writeGroup(group: Group): Unit = {
    val Group(children, position, ref) = group
    writeBatch(children)(writeSceneNode(_))
    writePoint(position)
    writePoint(ref)
  }

  def writeShapeBox(box: Shape.Box): Unit = {
    val Shape.Box(dimensions, fill, stroke, ref) = box
    writeRectangle(dimensions)
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  def writeShapeCircle(circle: Shape.Circle): Unit = {
    val Shape.Circle(circle0, fill, stroke, ref) = circle
    writeCircle(circle0)
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  def writeShapeLine(line: Shape.Line): Unit = {
    val Shape.Line(start, end, stroke, ref) = line
    writePoint(start)
    writePoint(end)
    writeStroke(stroke)
    writePoint(ref)
  }

  def writeShapePolygon(polygon: Shape.Polygon): Unit = {
    val Shape.Polygon(vertices, fill, stroke, ref) = polygon
    writeBatch(vertices)(writePoint(_))
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  def writeText(text: Text): Unit = {
    val Text(position, text0, font, textColor, ref) = text
    writePoint(position)
    writeString(text0)
    writeFontKey(font)
    writeRGBA(textColor)
    writePoint(ref)
  }

  def writeMasked(masked: Masked): Unit = {
    val Masked(mask, child) = masked
    writeSceneNode(mask)
    writeSceneNode(child)
  }

  def writeSceneNode(node: SceneNode): Unit = {
    node match
      case node: Graphic =>
        buf.put(1.toByte)
        writeGraphic(node)
      case node: Group =>
        buf.put(2.toByte)
        writeGroup(node)
      case node: Shape.Box =>
        buf.put(3.toByte)
        writeShapeBox(node)
      case node: Shape.Circle =>
        buf.put(4.toByte)
        writeShapeCircle(node)
      case node: Shape.Line =>
        buf.put(5.toByte)
        writeShapeLine(node)
      case node: Shape.Polygon =>
        buf.put(6.toByte)
        writeShapePolygon(node)
      case node: Text =>
        buf.put(7.toByte)
        writeText(node)
      case node: Masked =>
        buf.put(8.toByte)
        writeMasked(node)
  }

  def writeSceneUpdateFragment(fragment: SceneUpdateFragment): Unit = {
    val SceneUpdateFragment(nodes) = fragment
    writeBatch(nodes)(writeSceneNode(_))
  }
}
