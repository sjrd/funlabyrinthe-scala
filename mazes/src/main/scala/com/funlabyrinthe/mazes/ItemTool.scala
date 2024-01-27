package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

class ItemTool(using ComponentInit) extends Tool {
  var item: Option[ItemDef] = None

  var count: Int = 1
  var message: String = ""

  override def find(context: MoveContext) = control {
    import context._

    pos() += noTool

    if (item.isDefined) {
      item.get.count(player) += count
      player.showMessage(message)
    }
  }
}

object ItemTool:
  def make(item: ItemDef, message: String)(using ComponentInit): ItemTool =
    val tool = new ItemTool
    tool.painter = item.icon
    tool.item = Some(item)
    tool.message = message
    tool
  end make
end ItemTool
