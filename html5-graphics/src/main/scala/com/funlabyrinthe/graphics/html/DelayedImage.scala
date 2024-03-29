package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics.Image

final class DelayedImage extends Image:
  private var _underlying: Option[Image] = None
  private var isError: Boolean = false

  def underlying: Option[Image] = _underlying

  def width: Double = underlying.fold(0.0)(_.width)
  def height: Double = underlying.fold(0.0)(_.height)
  def isComplete: Boolean = underlying.fold(false)(_.isComplete) || isError

  def complete(underlying: Image): Unit =
    if isError || _underlying.isDefined then
      throw new IllegalStateException(s"Image was already completed")
    _underlying = Some(underlying)
  end complete

  def completeAsError(): Unit =
    if isError || _underlying.isDefined then
      throw new IllegalStateException(s"Image was already completed")
    isError = true
end DelayedImage
