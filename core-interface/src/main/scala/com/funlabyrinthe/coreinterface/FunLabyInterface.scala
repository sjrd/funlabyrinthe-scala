package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait FunLabyInterface extends js.Object:
  def loadUniverse(
    moduleClassNames: js.Array[String],
    pickleString: String,
    globalConfig: GlobalConfig,
  ): js.Promise[js.Tuple2[Universe, js.Array[PicklingError]]]
end FunLabyInterface
