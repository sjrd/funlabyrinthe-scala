package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait EditableComponent extends js.Object:
  def fullID: String
  def shortID: String

  def category: ComponentCategory

  def drawIcon(): dom.ImageBitmap

  val isComponentCreator: Boolean
  def createNewComponent(): EditableComponent

  val isCopiable: Boolean
  def copy(): EditableComponent

  val isDestroyable: Boolean
  def destroy(): js.Array[PicklingError]

  def inspect(): InspectedObject
end EditableComponent
