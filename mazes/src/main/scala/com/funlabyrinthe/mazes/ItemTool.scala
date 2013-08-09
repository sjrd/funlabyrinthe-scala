package com.funlabyrinthe.mazes

import std._

trait ItemTool extends Tool {
  import universe._
  import mazes._

  var item: ItemDef = NoItemDef
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
