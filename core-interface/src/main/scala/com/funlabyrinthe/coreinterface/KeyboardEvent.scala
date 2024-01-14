package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait KeyboardEvent extends js.Object:
  val physicalKey: String
  val keyString: String
  val repeat: Boolean
  val shiftDown: Boolean
  val controlDown: Boolean
  val altDown: Boolean
  val metaDown: Boolean
end KeyboardEvent
