package com.funlabyrinthe.core

import scala.collection.mutable

abstract class ItemDef(using ComponentInit) extends NamedComponent {
  import universe._

  val count: CorePlayer.mutable.PerPlayerData[Int] = new CorePlayer.mutable.PerPlayerData[Int] {
    protected def initial(player: CorePlayer): Int = 0

    override def update(player: CorePlayer, value: Int): Unit =
      val prevValue = apply(player)
      if value != prevValue then
        super.update(player, value)
        countChanged(player, prevValue, value)
    end update
  }

  category = ComponentCategory("items", "Items")

  def shouldDisplay(player: CorePlayer): Boolean = true

  def displayText(player: CorePlayer): String =
    s"$name: ${count(player)}"

  def perform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty

  protected def countChanged(player: CorePlayer, previousCount: Int, newCount: Int): Unit = ()
}

object ItemDef {
  def all(using universe: Universe): IndexedSeq[ItemDef] =
    universe.components[ItemDef]
}
