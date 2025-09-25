package com.funlabyrinthe.graphics.html

import scala.concurrent.Future

import scala.scalajs.js
import scala.scalajs.js.typedarray.*

import com.funlabyrinthe.core.graphics.Image
import com.funlabyrinthe.htmlenv.GIFReader

import org.scalajs.dom
import org.scalajs.dom.RequestInit
import org.scalajs.dom.HTMLCanvasElement
import org.scalajs.dom.CanvasRenderingContext2D

import Conversions.asHTMLElement

final class GIFImage(fileBuffer: ArrayBuffer) extends Image:
  private var _width: Int = 0
  private var _height: Int = 0
  private var _isComplete: Boolean = false
  private var _frames: Array[(Int, dom.OffscreenCanvas)] = null
  private var _publicFrames: IArray[Image] = Constants.EmptyImageArray
  private var _totalTime: Int = 0

  def isComplete: Boolean = _isComplete

  def width: Int = _width
  def height: Int = _height

  def isAnimated: Boolean = _publicFrames.length > 0
  def time: Int = _totalTime
  def frames: IArray[Image] = _publicFrames

  load()

  private def load(): Unit =
    val buffer = TypedArrayBuffer.wrap(fileBuffer)
    decode(new GIFReader(buffer))
    _isComplete = true
  end load

  private def decode(gifReader: GIFReader): Unit =
    _width = gifReader.width
    _height = gifReader.height

    _frames = new Array(gifReader.frameCount)
    for index <- 0 until gifReader.frameCount do
      val frameInfo = gifReader.frameInfo(index)
      val pixels = new Array[Byte](4 * frameInfo.width * frameInfo.height)
      gifReader.decodeAndBlitFrameRGBA(index, pixels)

      val canvas = new dom.OffscreenCanvas(width, height)
      val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      canvas.width = _width
      canvas.height = _height

      if index > 0 then
        ctx.drawImage(_frames(index - 1)._2.asHTMLElement, 0, 0)

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

    if gifReader.frameCount > 1 then
      _totalTime = _frames(_frames.length - 1)._1
      _publicFrames = IArray.tabulate(gifReader.frameCount) { i =>
        val prevCumulativeDelay = if i == 0 then 0 else _frames(i - 1)._1
        val (cumulativeDelay, canvas) = _frames(i)
        CanvasWrapper(canvas, cumulativeDelay - prevCumulativeDelay)
      }
  end decode

  def frameAt(tickCount: Long): Option[dom.OffscreenCanvas] =
    if _frames == null || _frames.length <= 1 then
      None
    else
      val tickCountMod = java.lang.Long.remainderUnsigned(tickCount, Integer.toUnsignedLong(_totalTime)).toInt
      val index = _frames.indexWhere(tickCountMod < _._1)
      Some(_frames(index)._2)
  end frameAt
end GIFImage
