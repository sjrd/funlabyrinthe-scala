package com.funlabyrinthe.core

import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class CorePlayerPlugin(using ComponentInit) extends Component derives Reflector:
  category = ComponentCategory("plugin", "Plugins")

  var zindex: Int = 0

  override def reflect() = autoReflect[CorePlayerPlugin]

  def drawView(player: CorePlayer, context: DrawContext): Unit = ()

  def onKeyEvent(player: CorePlayer, event: KeyEvent): Unit = ()

  def perform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty

  def onMessage[A](player: CorePlayer): PartialFunction[Message[A], A] = PartialFunction.empty
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
