package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics.Image

final class Animated(val frames: IArray[Image]) extends Image:
  def isComplete: Boolean = true

  val width: Int = frames.head.width
  val height: Int = frames.head.height

  def isAnimated: Boolean = true
  val time = frames.map(_.time).sum

  private val cumulativeDelays = frames.scanLeft(0)((prev, frame) => prev + frame.time).tail

  def frameAt(tickCount: Long): Image =
    val tickCountMod = java.lang.Long.remainderUnsigned(tickCount, Integer.toUnsignedLong(time)).toInt
    val index = cumulativeDelays.indexWhere(tickCountMod < _)
    frames(index)
end Animated
