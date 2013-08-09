package com.funlabyrinthe
package mazes

import core._
import input._

class PlayerPlugin(override implicit val universe: MazeUniverse)
extends Component {
  import universe._

  category = ComponentCategory("plugin", "Plugins")

  val tiebreakValue = PlayerPlugin.nextTieBreakValue
  PlayerPlugin.nextTieBreakValue += 1

  var zindex: Int = 0

  var painterBefore: Painter = EmptyPainter
  var painterAfter: Painter = EmptyPainter

  def drawBefore(context: DrawContext): Unit = {
    painterBefore.drawTo(context)
  }

  def drawAfter(context: DrawContext): Unit = {
    painterAfter.drawTo(context)
  }

  def moving(context: MoveContext): Unit @control = ()

  def moved(context: MoveContext): Unit @control = ()

  def drawView(player: Player, context: DrawContext): Unit = ()

  def onKeyEvent(player: Player, event: KeyEvent): Unit @control = ()

  def perform(player: Player): Player#Perform = PartialFunction.empty

  def onMessage(player: Player, message: Any): Boolean @control = false
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
