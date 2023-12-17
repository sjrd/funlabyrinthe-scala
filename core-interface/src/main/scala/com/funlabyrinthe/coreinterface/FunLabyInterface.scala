package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait FunLabyInterface extends js.Object:
  def createNewUniverse(
    moduleClassNames: js.Array[String],
    globalEventHandler: GlobalEventHandler,
  ): js.Promise[Universe]

  def loadUniverse(
    moduleClassNames: js.Array[String],
    pickleString: String,
    globalEventHandler: GlobalEventHandler,
  ): js.Promise[Universe]
end FunLabyInterface
