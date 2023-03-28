package com.funlabyrinthe

import core.input._

import cps.customValueDiscard
import scala.annotation.implicitNotFound

package object core {
  transparent inline def control[R](
    using
    @implicitNotFound("To use `control`, you must add `import cps.customValueDiscard` at the top of the file.")
    ev: cps.ValueDiscard.CustomTag
  )(
    inline expr: cps.CpsMonadContext[Control] ?=> R
  ): Control[R] = {
    cps.async[Control](x ?=> expr(using x))
  }

  transparent inline def exec[R](
    c: Control[R]
  )(
    using
    @implicitNotFound("`exec` can only be used inside a `control { ... }` block. Did you forget it?")
    inline ctx: cps.CpsMonadContext[Control]
  ): R = {
    cps.await(c)
  }

  def doNothing(): Control[Unit] = Control.Done(())

  def sleep(ms: Int): Control[Unit] = {
    if (ms <= 0)
      doNothing()
    else
      Control.Sleep(ms, _ => doNothing())
  }

  def waitForKeyEvent(): Control[KeyEvent] =
    Control.WaitForKeyEvent(event => Control.Done(event))
}
