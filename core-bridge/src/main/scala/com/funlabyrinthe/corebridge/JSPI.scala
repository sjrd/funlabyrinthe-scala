package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

private[corebridge] object JSPI:
  trait SuspendingComputation[+A]:
    def apply(): A
  end SuspendingComputation

  @inline
  def executeSuspending[A](computation: SuspendingComputation[A]): js.Promise[A] =
    throw new Error("executeSuspending")

  @inline
  def await[A](p: js.Promise[A]): A =
    throw new Error("await")
end JSPI
