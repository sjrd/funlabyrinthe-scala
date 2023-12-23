package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait EditableComponent extends js.Object:
  def id: String
  def category: ComponentCategory

  def drawIcon(): dom.ImageBitmap

  def isComponentCreator: Boolean
  def createNewComponent(): EditableComponent

  def inspect(): InspectedObject
end EditableComponent
