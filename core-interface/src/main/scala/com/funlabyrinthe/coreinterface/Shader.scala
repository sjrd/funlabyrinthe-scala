package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait Shader extends js.Object {
  val fullID: String
  val tpe: Int

  var vertex: js.UndefOr[String] = js.undefined
  var fragment: js.UndefOr[String] = js.undefined
}

object Shader {
  inline val TypeBlend = 1
}
