package com.funlabyrinthe.corebridge

import scala.annotation.tailrec

import java.nio.*

import scala.scalajs.js.typedarray.*
import scala.scalajs.js.typedarray.TypedArrayBufferOps.*

import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.scene.Layer.{Blending, BlendMaterial}
import com.funlabyrinthe.core.scene.Layer.BlendMaterial.Normal
import com.funlabyrinthe.core.scene.Layer.BlendMaterial.CustomBlendMaterial
import com.funlabyrinthe.core.shaders.ShaderData
import com.funlabyrinthe.core.shaders.UniformBlock
import com.funlabyrinthe.core.shaders.ShaderPrimitive

object SceneWriter {
}

final class SceneWriter {
  private var buf = makeBuf(64 * 1024)

  @tailrec
  final def writeSceneUpdateFragment(fragment: SceneUpdateFragment): Int8Array = {
    try
      buf.clear()
      writeSceneUpdateFragment0(fragment)
      new Int8Array(buf.typedArray().buffer, 0, buf.position())
    catch case _: BufferOverflowException =>
      buf = makeBuf(buf.capacity() * 4)
      println(s"Reallocating to ${buf.capacity() / 1024} KB")
      writeSceneUpdateFragment(fragment)
  }

  private def makeBuf(size: Int): ByteBuffer =
    TypedArrayBuffer.wrap(new Int8Array(size)).order(ByteOrder.LITTLE_ENDIAN)

  private def writeBatch[A](batch: Batch[A])(writeA: A => Unit): Unit = {
    buf.putInt(batch.size)
    batch.foreach(writeA(_))
  }

  private def writeOption[A](option: Option[A])(writeA: A => Unit): Unit = {
    option match {
      case None =>
        buf.put(0.toByte)
      case Some(value) =>
        buf.put(1.toByte)
        writeA(value)
    }
  }

  private def writeString(s: String): Unit = {
    val len = s.length()
    buf.putInt(len)
    for i <- 0 until len do
      buf.putChar(s.charAt(i))
  }

  private def writeSceneUpdateFragment0(fragment: SceneUpdateFragment): Unit = {
    val SceneUpdateFragment(layers) = fragment
    writeBatch(layers)(writeLayer(_))
  }

  private def writeLayer(layer: Layer): Unit = {
    writeBatch(layer.nodes)(writeSceneNode(_))
    writeOption(layer.blending)(writeBlending(_))
  }

  private def writeFontKey(fontKey: FontKey): Unit = {
    writeString(fontKey.key)
  }

  private def writePoint(point: Point): Unit = {
    buf.putInt(point.x)
    buf.putInt(point.y)
  }

  private def writeSize(size: Size): Unit = {
    buf.putInt(size.width)
    buf.putInt(size.height)
  }

  private def writeRadians(radians: Radians): Unit =
    buf.putDouble(radians.toDouble)

  private def writeRectangle(rect: Rectangle): Unit = {
    writePoint(rect.topLeft)
    writeSize(rect.size)
  }

  private def writeCircle(circle: Circle): Unit = {
    writePoint(circle.center)
    buf.putInt(circle.radius)
  }

  private def writeRGBA(rgba: RGBA): Unit = {
    buf.putDouble(rgba.red)
    buf.putDouble(rgba.green)
    buf.putDouble(rgba.blue)
    buf.putDouble(rgba.alpha)
  }

  private def writeFill(fill: Fill): Unit = {
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
      case Fill.RadialGradient(fromPoint, fromColor, toPoint, toColor) =>
        buf.put(3.toByte)
        writePoint(fromPoint)
        writeRGBA(fromColor)
        writePoint(toPoint)
        writeRGBA(toColor)
    }
  }

  private def writeStroke(stroke: Stroke): Unit = {
    buf.putInt(stroke.width)
    writeRGBA(stroke.color)
  }

  private def writeMaterial(material: Material): Unit = {
    val Material(asset, alpha, tint) = material
    writeString(asset)
    buf.putDouble(alpha)
    writeRGBA(tint)
  }

  private def writeGraphic(graphic: Graphic): Unit = {
    val Graphic(material, crop, position, ref) = graphic
    writeMaterial(material)
    writeRectangle(crop)
    writePoint(position)
    writePoint(ref)
  }

  private def writeGroup(group: Group): Unit = {
    val Group(children, position, rotation, ref) = group
    writeBatch(children)(writeSceneNode(_))
    writePoint(position)
    writeRadians(rotation)
    writePoint(ref)
  }

  private def writeShapeBox(box: Shape.Box): Unit = {
    val Shape.Box(dimensions, fill, stroke, ref) = box
    writeRectangle(dimensions)
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  private def writeShapeCircle(circle: Shape.Circle): Unit = {
    val Shape.Circle(circle0, fill, stroke, ref) = circle
    writeCircle(circle0)
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  private def writeShapeLine(line: Shape.Line): Unit = {
    val Shape.Line(start, end, stroke, ref) = line
    writePoint(start)
    writePoint(end)
    writeStroke(stroke)
    writePoint(ref)
  }

  private def writeShapePolygon(polygon: Shape.Polygon): Unit = {
    val Shape.Polygon(vertices, fill, stroke, ref) = polygon
    writeBatch(vertices)(writePoint(_))
    writeFill(fill)
    writeStroke(stroke)
    writePoint(ref)
  }

  private def writeText(text: Text): Unit = {
    val Text(position, text0, font, textColor, ref) = text
    writePoint(position)
    writeString(text0)
    writeFontKey(font)
    writeRGBA(textColor)
    writePoint(ref)
  }

  private def writeMasked(masked: Masked): Unit = {
    val Masked(mask, child) = masked
    writeSceneNode(mask)
    writeSceneNode(child)
  }

  private def writeSceneNode(node: SceneNode): Unit = {
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

  private def writeBlending(blending: Blending): Unit = {
    writeBlendMaterial(blending.blendMaterial)
  }

  private def writeBlendMaterial(blendMaterial: BlendMaterial): Unit = {
    blendMaterial match
      case Normal =>
        buf.put(1.toByte)
      case CustomBlendMaterial(shaderData) =>
        buf.put(2.toByte)
        writeShaderData(shaderData)
  }

  private def writeShaderData(shaderData: ShaderData): Unit = {
    writeString(shaderData.shader.fullID)
    writeBatch(shaderData.blocks)(writeUniformBlock(_))
  }

  private def writeUniformBlock(block: UniformBlock): Unit = {
    writeString(block.blockName)
    writeBatch(block.fields) { (name, value) =>
      writeString(name)
      writeShaderPrimitive(value)
    }
  }

  private def writeShaderPrimitive(primitive: ShaderPrimitive): Unit = {
    primitive match
      case ShaderPrimitive.float(value) =>
        buf.put(1.toByte)
        buf.putFloat(value)
      case ShaderPrimitive.vec2(x, y) =>
        buf.put(2.toByte)
        buf.putFloat(x)
        buf.putFloat(y)
      case ShaderPrimitive.vec3(x, y, z) =>
        buf.put(3.toByte)
        buf.putFloat(x)
        buf.putFloat(y)
        buf.putFloat(z)
      case ShaderPrimitive.vec4(x, y, z, w) =>
        buf.put(4.toByte)
        buf.putFloat(x)
        buf.putFloat(y)
        buf.putFloat(z)
        buf.putFloat(w)
  }
}
