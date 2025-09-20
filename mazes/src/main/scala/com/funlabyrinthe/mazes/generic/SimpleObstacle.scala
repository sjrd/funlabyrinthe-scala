package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.PushButton

class SimpleObstacle(using ComponentInit) extends Obstacle derives Reflector:
  category = ComponentCategory("customObstacles", "Custom Obstacles")

  var condition: ObstacleCondition = ObstacleCondition.NeverDestroy
  var message: String = ""
  var showMessageOnlyFirstTime: Boolean = false

  @noinspect
  var messageShownToPlayers: Set[Player] = Set.empty

  override def reflect() = autoReflect[SimpleObstacle]

  override def pushing(context: MoveContext): Unit =
    import context._

    cancel()

    if keyEvent.isDefined then
      val success = condition match
        case ObstacleCondition.NeverDestroy  => false
        case ObstacleCondition.AlwaysDestroy => true

      if success then
        pos() += noObstacle
      else if message != "" then
        val firstTime = !messageShownToPlayers.contains(player)
        if firstTime then
          messageShownToPlayers += player
        if !showMessageOnlyFirstTime || firstTime then
          player.showMessage(message)
  end pushing
end SimpleObstacle
