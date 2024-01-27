package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.mazes.*

class Switch(using ComponentInit) extends CounterEffect derives Reflector:
  var isOn: Boolean = false

  @transient
  def offPainter: Painter = painter
  def offPainter_=(value: Painter): Unit = painter = value

  var onPainter: Painter = universe.EmptyPainter

  offPainter += "Buttons/SwitchOff"
  onPainter += "Buttons/SwitchOn"

  override def reflect() = autoReflect[Switch]

  override protected def doDraw(context: DrawSquareContext): Unit =
    if isOn then
      doDrawOn(context)
    else
      doDrawOff(context)
  end doDraw

  protected def doDrawOff(context: DrawSquareContext): Unit =
    offPainter.drawTo(context)

  protected def doDrawOn(context: DrawSquareContext): Unit =
    onPainter.drawTo(context)

  override def execute(context: MoveContext): Control[Unit] = control {
    super.execute(context)

    isOn = !isOn
    if isOn then
      switchOn(context)
    else
      switchOff(context)
  }

  def switchOn(context: MoveContext): Control[Unit] = doNothing()

  def switchOff(context: MoveContext): Control[Unit] = doNothing()
end Switch
