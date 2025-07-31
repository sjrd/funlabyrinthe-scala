package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait FunLabyInterface extends js.Object:
  def createNewUniverse(
    moduleClassNames: js.Array[String],
    globalConfig: GlobalConfig,
  ): js.Promise[Universe]

  def loadUniverse(
    moduleClassNames: js.Array[String],
    pickleString: String,
    globalConfig: GlobalConfig,
  ): js.Promise[Universe]
end FunLabyInterface
