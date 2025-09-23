package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.CorePlayer.Perform

class SimpleItem(using ComponentInit) extends ItemDef:
  category = ComponentCategory("customItems", "Custom Items")

  var providedAbility: Option[Ability] = None
  var requiredCount: Int = 1
  var consumeOnUse: Boolean = false

  override def perform(player: CorePlayer): Perform = {
    case ability if providedAbility.contains(ability) && player.has(requiredCount, this) =>
      if consumeOnUse then
        count(player) -= requiredCount
  }
end SimpleItem
