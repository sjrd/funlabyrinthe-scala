package com.funlabyrinthe.core

import scala.collection.immutable.HashMap

import com.funlabyrinthe.core.scene.Painter

abstract class ItemDef(using ComponentInit) extends Component {
  var name: String = id

  // override to make non-transient and inspectable
  override def icon: Painter = super.icon
  override def icon_=(value: Painter): Unit = super.icon_=(value)

  private var _countByPlayer: HashMap[CorePlayer, Int] = HashMap.empty

  @noinspect // TODO Make it inspectable
  def countByPlayer: HashMap[CorePlayer, Int] = _countByPlayer

  def countByPlayer_=(value: HashMap[CorePlayer, Int]): Unit = {
    val oldMap = _countByPlayer
    val newMap = value.filter(_._2 != 0)

    // Atomically set the entire map
    _countByPlayer = newMap

    // Then trigger all changes
    for player <- universe.players do
      val oldCount = oldMap.getOrElse(player, 0)
      val newCount = newMap.getOrElse(player, 0)
      if newCount != oldCount then
        countChanged(player, oldCount, newCount)
  }

  @transient @noinspect
  object count {
    def apply(player: CorePlayer): Int = countByPlayer.getOrElse(player, 0)
    def update(player: CorePlayer, count: Int): Unit = countByPlayer += player -> count

    def apply(player: ReifiedPlayer): Int = apply(player.corePlayer)
    def update(player: ReifiedPlayer, count: Int): Unit = update(player.corePlayer, count)
  }

  category = ComponentCategory("items", "Items")

  def shouldDisplay(player: CorePlayer): Boolean = true

  def displayText(player: CorePlayer): String =
    s"$name: ${count(player)}"

  def perform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty

  protected def countChanged(player: CorePlayer, previousCount: Int, newCount: Int): Unit = ()
}

object ItemDef {
  def all(using universe: Universe): List[ItemDef] =
    universe.components[ItemDef]
}
