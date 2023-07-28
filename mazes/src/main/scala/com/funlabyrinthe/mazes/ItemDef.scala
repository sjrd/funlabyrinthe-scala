package com.funlabyrinthe
package mazes

import core._

import scala.collection.mutable

abstract class ItemDef(using ComponentInit) extends NamedComponent {
  import universe._

  val count: Player.mutable.PerPlayerData[Int] = new Player.mutable.PerPlayerData[Int] {
    protected def initial(player: Player): Int = 0

    override def update(player: Player, value: Int): Unit =
      val prevValue = apply(player)
      if value != prevValue then
        super.update(player, value)
        countChanged(player, prevValue, value)
    end update
  }

  category = ComponentCategory("items", "Items")

  def shouldDisplay(player: Player): Boolean = true

  def displayText(player: Player): String =
    s"$name: ${count(player)}"

  def perform(player: Player): Player.Perform = PartialFunction.empty

  protected def countChanged(player: Player, previousCount: Int, newCount: Int): Unit = ()
}

object ItemDef {
  def all(implicit universe: Universe): IndexedSeq[ItemDef] =
    universe.components[ItemDef]
}
