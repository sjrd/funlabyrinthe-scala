package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait RunningGame extends js.Object:
  val players: js.Array[Player]

  def allShaders(): js.Array[Shader]

  def advanceTickCount(delta: Double): Unit
end RunningGame
