package com.funlabyrinthe.coreinterface

import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

trait EditableComponent extends js.Object:
  def fullID: String
  def shortID: String

  def category: ComponentCategory

  def presentIcon(): Int8Array

  val isComponentCreator: Boolean
  def createNewComponent(): EditableComponent

  val isCopiable: Boolean
  def copy(): EditableComponent

  val isDestroyable: Boolean
  def destroy(): js.Array[PicklingError]

  def inspect(): InspectedObject
end EditableComponent
