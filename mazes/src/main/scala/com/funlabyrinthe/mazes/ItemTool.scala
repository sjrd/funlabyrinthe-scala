package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics.Painter

import std._

import Mazes.mazes

class ItemTool(using ComponentInit) extends Tool {
  var item: Option[ItemDef] = None

  var count: Int = 1
  var message: String = ""

  override def find(context: MoveContext) = control {
    import context._

    pos() += mazes.NoTool

    if (item.isDefined) {
      item.get.count(player) += count
      player.showMessage(message)
    }
  }
}

object ItemTool:
  def make(name: String, item: ItemDef, message: String)(
      using ComponentInit): ItemTool =

    val tool = new ItemTool
    tool.name = name
    tool.painter = item.painter
    tool.item = Some(item)
    tool.message = message
    tool
  end make
end ItemTool
