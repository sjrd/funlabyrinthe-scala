package com.funlabyrinthe.mazes

import std._

trait ItemTool extends Tool {
  import universe._
  import mazes._

  // Arg, need to avoid accessing NoItemDef during constructor
  private var myItem: ItemDef = null
  def item: ItemDef = if (myItem eq null) NoItemDef else myItem
  def item_=(i: ItemDef): Unit = myItem = i

  var count: Int = 1
  var message: String = ""

  override def find(context: MoveContext) = {
    import context._

    pos() += NoTool

    if (item != NoItemDef) {
      item.count(player) += count
      player.showMessage(message)
    }
  }
}
