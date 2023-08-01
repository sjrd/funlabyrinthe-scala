package com.funlabyrinthe
package mazes

import cps.customValueDiscard

import core._
import input._

abstract class PlayerPlugin(using ComponentInit) extends Component {
  import universe._

  category = ComponentCategory("plugin", "Plugins")

  val tiebreakValue = PlayerPlugin.nextTieBreakValue
  PlayerPlugin.nextTieBreakValue += 1

  var zindex: Int = 0

  var painterBefore: Painter = EmptyPainter
  var painterAfter: Painter = EmptyPainter

  def drawBefore(player: Player, context: DrawContext): Unit = {
    painterBefore.drawTo(context)
  }

  def drawAfter(player: Player, context: DrawContext): Unit = {
    painterAfter.drawTo(context)
  }

  def moving(context: MoveContext): Control[Unit] = doNothing()

  def moved(context: MoveContext): Control[Unit] = doNothing()

  def drawView(player: Player, context: DrawContext): Unit = ()

  def onKeyEvent(player: Player, event: KeyEvent): Control[Unit] = doNothing()

  def perform(player: Player): Player.Perform = PartialFunction.empty

  def onMessage[A](player: Player): PartialFunction[Message[A], Control[A]] = PartialFunction.empty
}

object PlayerPlugin {
  private var nextTieBreakValue = 0

  implicit object PluginOrdering extends Ordering[PlayerPlugin] {
    override def compare(lhs: PlayerPlugin, rhs: PlayerPlugin) = {
      val zdiff = lhs.zindex - rhs.zindex
      if (zdiff != 0) zdiff
      else lhs.tiebreakValue - rhs.tiebreakValue
    }
  }
}
