package com.funlabyrinthe
package mazes

import core._

abstract class Effect(using ComponentInit) extends VisualComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  def execute(context: MoveContext): Control[Unit] = doNothing()
}
