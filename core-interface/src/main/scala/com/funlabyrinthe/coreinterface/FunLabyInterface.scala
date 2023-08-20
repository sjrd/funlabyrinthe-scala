package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait FunLabyInterface extends js.Object:
  def createNewUniverse(): js.Promise[Universe]

  def loadUniverse(pickle: js.Object): js.Promise[Universe]
end FunLabyInterface
