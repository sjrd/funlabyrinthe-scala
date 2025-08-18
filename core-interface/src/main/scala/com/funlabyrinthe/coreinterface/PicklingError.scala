package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait PicklingError extends js.Object:
  val component: js.UndefOr[String]
  val path: js.Array[String]
  val message: String
end PicklingError
