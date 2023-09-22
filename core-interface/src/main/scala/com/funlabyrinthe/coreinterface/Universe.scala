package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait Universe extends js.Object:
  def save(): String

  def allEditableComponents(): js.Array[EditableComponent]

  def getEditableComponentByID(id: String): js.UndefOr[EditableComponent]

  def allEditableMaps(): js.Array[EditableMap]

  def getEditableMapByID(id: String): js.UndefOr[EditableMap]

  def startGame(): RunningGame
end Universe
