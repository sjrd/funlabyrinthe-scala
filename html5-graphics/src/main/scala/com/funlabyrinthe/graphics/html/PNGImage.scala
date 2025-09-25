package com.funlabyrinthe.graphics.html

import scala.scalajs.js.typedarray.ArrayBuffer

import com.funlabyrinthe.core.graphics.Image

/* Logic to parse the APNG format ported from
 * https://github.com/davidmz/apng-js
 * Copyright (c) 2016 David Mzareulyan -- MIT License
 *
 * We didn't reuse it as is, because we actually want to support
 * non-animated PNG as well, in a single run.
 */

final class PNGImage(fileBuffer: ArrayBuffer) extends Image:
  def isComplete: Boolean = false

  def width: Int = 0
  def height: Int = 0
  def isAnimated: Boolean = false

  def time: Int = 0
  def frames: IArray[Image] = Constants.EmptyImageArray
end PNGImage
