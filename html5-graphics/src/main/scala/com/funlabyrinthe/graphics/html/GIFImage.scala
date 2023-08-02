package com.funlabyrinthe.graphics.html

import scala.scalajs.js.typedarray.*

import com.funlabyrinthe.core.graphics.Image
import com.funlabyrinthe.htmlenv.GIFReader

import org.scalajs.dom
import org.scalajs.dom.RequestInit
import org.scalajs.dom.HTMLCanvasElement
import org.scalajs.dom.CanvasRenderingContext2D

final class GIFImage(url: String) extends Image:
  private var _width: Int = 0
  private var _height: Int = 0
  private var _frames: Array[(Int, HTMLCanvasElement)] = null
  private var _totalTime: Int = 0

  load()

  def width: Double = _width.toDouble
  def height: Double = _height.toDouble

  private def load(): Unit =
    org.scalajs.dom.fetch(url).`then` { response =>
      response.arrayBuffer().`then` { body =>
        val buffer = TypedArrayBuffer.wrap(body)
        decode(new GIFReader(buffer))
      }
    }
  end load

  private def decode(gifReader: GIFReader): Unit =
    _width = gifReader.width
    _height = gifReader.height

    _frames = new Array(gifReader.frameCount)
    for index <- 0 until gifReader.frameCount do
      val frameInfo = gifReader.frameInfo(index)
      val pixels = new Array[Byte](4 * frameInfo.width * frameInfo.height)
      gifReader.decodeAndBlitFrameRGBA(index, pixels)

      val canvas = dom.document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
      val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      canvas.width = _width
      canvas.height = _height

      if index > 0 then
        ctx.drawImage(_frames(index - 1)._2, 0, 0)

      val imageData = ctx.createImageData(width, height)
      imageData.data.asInstanceOf[Uint8ClampedArray]
        .set(new Uint8ClampedArray(pixels.toTypedArray.buffer))
      ctx.putImageData(imageData, -frameInfo.x, -frameInfo.y)

      val frameDelay = frameInfo.delay * 10 // the GIF format uses hundreds of seconds; we use milliseconds
      val cumulativeDelay =
        if index == 0 then frameDelay
        else _frames(index - 1)._1 + frameDelay

      _frames(index) = cumulativeDelay -> canvas
    end for

    _totalTime = _frames(_frames.length - 1)._1
  end decode

  def currentFrame: Option[HTMLCanvasElement] =
    if _frames == null then
      None
    else
      val tickCount = ((System.nanoTime() / 1000000L) % _totalTime).toInt
      val index = _frames.indexWhere(tickCount < _._1)
      Some(_frames(index)._2)
  end currentFrame
end GIFImage
