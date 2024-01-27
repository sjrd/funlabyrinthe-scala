package com.funlabyrinthe.core

import com.funlabyrinthe.core.graphics.Painter

abstract class ItemDef(using ComponentInit) extends Component derives Reflector {
  var name: String = id

  // override to make non-transient and inspectable
  override def icon: Painter = super.icon
  override def icon_=(value: Painter): Unit = super.icon_=(value)

  @transient @noinspect // FIXME We actually need to pickle this
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

  override def reflect() = autoReflect[ItemDef]

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
