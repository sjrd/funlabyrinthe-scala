package com.funlabyrinthe.core

import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class CorePlayerPlugin(using ComponentInit) extends Component:
  category = ComponentCategory("plugin", "Plugins")

  var zindex: Int = 0

  def drawView(player: CorePlayer, context: DrawContext): Unit = ()

  def onKeyEvent(player: CorePlayer, event: KeyEvent): Control[Unit] = doNothing()

  def perform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty

  def onMessage[A](player: CorePlayer): PartialFunction[Message[A], Control[A]] = PartialFunction.empty
end CorePlayerPlugin

object CorePlayerPlugin:
  given PluginOrdering: Ordering[CorePlayerPlugin] with
    override def compare(lhs: CorePlayerPlugin, rhs: CorePlayerPlugin) = {
      val zdiff = lhs.zindex - rhs.zindex
      if (zdiff != 0) zdiff
      else lhs.id.compareTo(rhs.id) // tie-break
    }
  end PluginOrdering
end CorePlayerPlugin
