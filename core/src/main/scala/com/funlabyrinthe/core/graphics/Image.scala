package com.funlabyrinthe.core.graphics

trait Image {
  def isComplete: Boolean

  def width: Double
  def height: Double
  def isAnimated: Boolean

  /** For a fixed image, 0; for a frame, its delay; for an animated image, its total time. */
  def time: Int

  /** An empty array if the Image is not animated. */
  def frames: IArray[Image]
}
