package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

private[corebridge] object JSPI:
  @inline
  def async[A](computation: => A): js.Promise[A] =
    throw new Error("async stub")

  @inline
  def await[A](p: js.Promise[A]): A =
    throw new Error("await stub")
end JSPI
