package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

private[corebridge] object JSPI:
  trait SuspendingComputation[+A]:
    def apply(): A
  end SuspendingComputation

  @JSExportTopLevel("magicJSPIExport")
  protected[corebridge] def magicJSPIExport[A](computation: SuspendingComputation[A]): A =
    computation()

  @noinline
  def executeSuspending[A](computation: SuspendingComputation[A]): js.Promise[A] =
    js.`import`[js.Dynamic]("./main.js").`then` { mod =>
      mod.magicJSPIExport(computation.asInstanceOf[js.Any]).asInstanceOf[js.Promise[A]]
    }
  end executeSuspending

  @noinline
  def await[A](p: js.Promise[A]): A =
    js.Dynamic.global.magicJSPIAwait(p).asInstanceOf[A]
end JSPI
