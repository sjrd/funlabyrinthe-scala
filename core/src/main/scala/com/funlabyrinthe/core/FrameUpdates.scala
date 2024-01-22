package com.funlabyrinthe.core

/** Base trait for components that want to react to frame updates.
 *
 *  If a component extends `FrameUpdates`, its `frameUpdate()` will be called
 *  on every frame. The `universe.tickCount` reflects the current tick count,
 *  while the `ticksSinceLastFrame` argument indicates the number of ticks
 *  that elapsed since the last frame.
 *
 *  `ticksSinceLastFrame` may in theory be zero, if two frames are computed
 *  within the same millisecond. `frameUpdate()` should not assume that
 *  `ticksSinceLastFrame` is greater than zero.
 */
trait FrameUpdates extends Component:
  def frameUpdate(ticksSinceLastFrame: Long): Unit = ()
end FrameUpdates
