package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class SimpleField(using ComponentInit) extends Field:
  category = ComponentCategory("customFields", "Custom Fields")

  var condition: FieldCondition = FieldCondition.AlwaysAllow
  var message: String = ""
  var showMessageOnlyFirstTime: Boolean = false

  @noinspect
  var messageShownToPlayers: Set[Player] = Set.empty

  override def entering(context: MoveContext): Unit =
    import context._

    val success = condition match
      case FieldCondition.AlwaysAllow             => true
      case FieldCondition.NeverAllow              => false
      case FieldCondition.AllowIfAbility(ability) => player.tryPerform(ability)

    if !success then
      cancel()
      if message != "" then
        val firstTime = !messageShownToPlayers.contains(player)
        if firstTime then
          messageShownToPlayers += player
        if !showMessageOnlyFirstTime || firstTime then
          player.showMessage(message)
  end entering
end SimpleField
