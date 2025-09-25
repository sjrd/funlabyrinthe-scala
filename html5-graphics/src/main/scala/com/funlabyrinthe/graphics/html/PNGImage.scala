package com.funlabyrinthe.graphics.html

import scala.concurrent.ExecutionContext

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.scalajs.js.typedarray.TypedArrayBuffer

import org.scalajs.dom

import com.funlabyrinthe.core.graphics.Image
import scala.concurrent.Future
import com.funlabyrinthe.graphics.html.Conversions.asHTMLElement
import java.nio.ByteOrder

/* Logic to parse the APNG format ported from
 * https://github.com/davidmz/apng-js
 * Copyright (c) 2016 David Mzareulyan -- MIT License
 *
 * We didn't reuse it as is, because we actually want to support
 * non-animated PNG as well, in a single run.
 */

final class PNGImage(fileBuffer: ArrayBuffer)(using ExecutionContext) extends Image:
  import PNGImage.*

  private val info = PNGParser.parse(TypedArrayBuffer.wrap(fileBuffer).order(ByteOrder.BIG_ENDIAN))
  println(info)
  println("---------------------")

  private var _cumulativeDelays: IArray[Int] | Null = null
  private var _frames: IArray[dom.HTMLElement] | Null = null
  private var _publicFrames: IArray[Image] | Null = null
  private var _isComplete: Boolean = false

  def isComplete: Boolean = _isComplete

  val width: Int = info.width
  val height: Int = info.height

  val isAnimated: Boolean = info.isAnimated

  private var _totalTime: Int = 0

  val future: Future[this.type] =
    if !isAnimated then
      for image <- makeImageForBlob(new dom.Blob(js.Array(fileBuffer))) yield
        _cumulativeDelays = EmptyIntIArray
        _frames = IArray(image)
        _publicFrames = Constants.EmptyImageArray
        _isComplete = true
        this
    else
      val frameInfos = IArray.from(info.frames)
      for rawFrames <- Future.traverse(frameInfos)(f => makeImageForBlob(f.imageData.get)) yield
        _cumulativeDelays = frameInfos.scanLeft(0)((prev, f) => prev + f.delay).tail
        val composed = composeFrames(width, height, frameInfos, IArray.from(rawFrames))
        _frames = composed.map(_.asHTMLElement)
        _publicFrames = composed.zip(frameInfos).map { (frame, info) => CanvasWrapper(frame, info.delay)}
        _totalTime = _cumulativeDelays.nn.last
        _isComplete = true
        this
  end future

  def time: Int = _totalTime
  def frames: IArray[Image] = _publicFrames.nn

  def frameAt(tickCount: Long): Option[dom.HTMLElement] =
    if isComplete then
      if !isAnimated then
        Some(_frames.nn(0))
      else
        val tickCountMod = java.lang.Long.remainderUnsigned(tickCount, Integer.toUnsignedLong(_totalTime)).toInt
        val index = _cumulativeDelays.nn.indexWhere(tickCountMod < _)
        Some(_frames.nn(index))
    else
      None
  end frameAt
end PNGImage

object PNGImage:
  private val EmptyIntIArray: IArray[Int] = IArray[Int]()

  private def makeImageForBlob(blob: dom.Blob)(using ExecutionContext): Future[dom.HTMLImageElement] =
    val url = dom.URL.createObjectURL(blob)
    val image = new dom.Image()
    image.src = url
    val promise = image.asInstanceOf[js.Dynamic].decode().asInstanceOf[js.Promise[Unit]]
    promise.toFuture.map(_ => image) //.andThen(_ => dom.URL.revokeObjectURL(url))
  end makeImageForBlob

  private def composeFrames(
    width: Int,
    height: Int,
    frameInfos: IArray[PNGParser.Frame],
    rawFrames: IArray[dom.HTMLImageElement]
  ): IArray[dom.OffscreenCanvas] =
    // https://github.com/davidmz/apng-js/blob/52f6fab62ffabe2abac3467d6abf82fe98ba4018/src/library/player.js#L58-L90

    val buffer = new dom.OffscreenCanvas(width, height)
    val context = buffer.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    val resultArray = new Array[dom.OffscreenCanvas](frameInfos.length)

    for i <- 0 until frameInfos.length do
      val info = frameInfos(i)

      // Save current fresh data if we need to dispose this frame afterward
      val savedData =
        if info.disposeOp != 2 then null
        else context.getImageData(info.left, info.top, info.width, info.height)

      // Draw this frame on top of the buffer
      if info.blendOp == 0 then
        context.clearRect(info.left, info.top, info.width, info.height)
      context.drawImage(rawFrames(i), info.left, info.top, info.width, info.height)
      dom.console.log(buffer)

      // Copy to a fresh canvas for the result array
      val composedFrame = new dom.OffscreenCanvas(width, height)
      composedFrame
        .getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
        //.putImageData(context.getImageData(0, 0, width, height), 0, 0)
        .drawImage(composedFrame.asHTMLElement, 0, 0)
      resultArray(i) = composedFrame
      dom.console.log(composedFrame)

      // Apply the dispose operation
      info.disposeOp match
        case 1 =>
          context.clearRect(info.left, info.top, info.width, info.height)
        case 2 =>
          context.putImageData(savedData.nn, info.left, info.top)
        case _ =>
          ()
      end match
    end for

    IArray.from(resultArray)
  end composeFrames
end PNGImage
