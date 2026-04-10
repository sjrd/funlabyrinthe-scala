package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.wasm.JSPI.allowOrphanJSAwait

private[corebridge] object JSPI:
  @inline
  def async[A](computation: => A): js.Promise[A] =
    js.async {
      val _ = ()
      computation
    }

  @inline
  def await[A](p: js.Promise[A]): A =
    js.await(p)
end JSPI
