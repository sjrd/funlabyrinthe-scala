package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait FunLabyInterface extends js.Object:
  def createNewUniverse(): js.Promise[Universe]

  def loadUniverse(pickleString: String): js.Promise[Universe]
end FunLabyInterface
