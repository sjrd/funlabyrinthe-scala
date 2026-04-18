package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait Player extends js.Object:
  def viewWidth: Double
  def viewHeight: Double

  def drawView(canvas: dom.HTMLCanvasElement): Unit

  def presentView(): js.typedarray.Int8Array

  def keyDown(event: KeyboardEvent): Unit
end Player
