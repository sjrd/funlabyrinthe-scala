package com.funlabyrinthe
package mazes

import core._
import input._

class PlayerPlugin(override implicit val universe: MazeUniverse)
extends Component {
  import universe._

  category = ComponentCategory("plugin", "Plugins")

  var zindex: Int = 0

  var painterBefore: Painter = EmptyPainter
  var painterAfter: Painter = EmptyPainter

  def drawBefore(context: DrawContext): Unit = {
    painterBefore.drawTo(context)
  }

  def drawAfter(context: DrawContext): Unit = {
    painterAfter.drawTo(context)
  }

  def moving(context: MoveContext): Unit = ()

  def moved(context: MoveContext): Unit = ()

  def drawView(context: DrawContext): Unit = ()

  def onKeyEvent(event: KeyEvent): Unit = ()

  def perform(player: Player): Player#Perform = PartialFunction.empty
}

object PlayerPlugin {
  implicit object PluginOrdering extends Ordering[PlayerPlugin] {
    override def compare(lhs: PlayerPlugin, rhs: PlayerPlugin) =
      lhs.zindex - rhs.zindex
  }
}
