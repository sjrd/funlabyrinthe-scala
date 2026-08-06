package com.funlabyrinthe.scene

import scala.reflect.ClassTag

import java.nio.{ByteBuffer, ByteOrder}

import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array
import scala.scalajs.js.typedarray.TypedArrayBuffer

import indigo.scenegraph.{Blend, Blending}
import indigo.scenegraph.materials.BlendMaterial
import indigo.shaders.{ShaderData, ShaderId, ShaderPrimitive, UniformBlock, Uniform, UniformBlockName}

object SceneReader {
  def readSceneUpdateFragment(buf: Int8Array): SceneUpdateFragment =
    new SceneReader(TypedArrayBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)).readSceneUpdateFragment()
}

final class SceneReader(buf: ByteBuffer) {
  private def readBatch[A](readA: () => A): Batch[A] = {
    val len = buf.getInt()
    val arr = js.Array[A]()
    for i <- 0 until len do
      arr.push(readA())
    Batch.fromJSArray(arr)
  }

  private def readOption[A](readA: () => A): Option[A] = {
    if buf.get().toInt != 0 then
      Some(readA())
    else
      None
  }

  def readString(): String = {
    val len = buf.getInt()
    var s = ""
    for i <- 0 until len do
      s += buf.getChar()
    s
  }

  def readSceneUpdateFragment(): SceneUpdateFragment =
    SceneUpdateFragment(readBatch(() => readLayer()))

  private def readLayer(): Layer =
    Layer(readBatch(() => readSceneNode()), readOption(() => readBlending()))

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

  private def readBlending(): Blending = {
    Blending(
      Blend.Normal,
      Blend.Normal,
      readBlendMaterial(),
      None,
    )
  }

  private def readBlendMaterial(): BlendMaterial = {
    buf.get().toInt match {
      case 1 =>
        BlendMaterial.Normal
      case 2 =>
        CustomBlendMaterial(readShaderData())
    }
  }

  private def readShaderData(): ShaderData = {
    val shaderID = ShaderId(readString())
    val blocks = readBatch(() => readUniformBlock())
    ShaderData(shaderID, blocks)
  }

  private def readUniformBlock(): UniformBlock = {
    val blockName = UniformBlockName(readString())
    val fields = readBatch { () =>
      (Uniform(readString()), readShaderPrimitive())
    }
    UniformBlock(blockName, fields)
  }

  private def readShaderPrimitive(): ShaderPrimitive = {
    buf.get().toInt match {
      case 1 =>
        ShaderPrimitive.float(buf.getFloat())
      case 2 =>
        ShaderPrimitive.vec2(buf.getFloat(), buf.getFloat())
      case 3 =>
        ShaderPrimitive.vec3(buf.getFloat(), buf.getFloat(), buf.getFloat())
      case 4 =>
        ShaderPrimitive.vec4(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat())
    }
  }
}
