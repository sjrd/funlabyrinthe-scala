package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait Player extends js.Object:
  def viewSize(): Size

  def presentView(): js.typedarray.Int8Array

  def keyDown(event: KeyboardEvent): Unit
end Player
