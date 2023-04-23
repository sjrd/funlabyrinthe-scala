package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core._

import std._

import Mazes.mazes

trait ItemTool extends Tool {
  // Arg, need to avoid accessing NoItemDef during constructor
  private var myItem: ItemDef = null
  def item: ItemDef = if (myItem eq null) mazes.NoItemDef else myItem
  def item_=(i: ItemDef): Unit = myItem = i

  var count: Int = 1
  var message: String = ""

  override def find(context: MoveContext) = control {
    import context._

    pos() += mazes.NoTool

    if (item != mazes.NoItemDef) {
      item.count(player) += count
      player.showMessage(message)
    }
  }
}
