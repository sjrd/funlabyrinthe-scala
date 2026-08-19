package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

import com.funlabyrinthe.mazes.*

class PlankPlugin(using ComponentInit) extends PlayerPlugin:
  import PlankPlugin.*

  @transient @noinspect
  var inUse: Set[Player] = Set.empty

  override def presentUnder(player: Player, context: PresentSquareContext): Batch[SceneNode] = {
    import context.*

    if inUse(player) then {
      // Find the actual square where we need to draw the plank
      val diff = player.position match {
        case Some(pos) if pos().field.isInstanceOf[PlankOverridingField] =>
          Point.zero
        case _ =>
          player.direction match
            case Direction.North => Point(0, -30)
            case Direction.East  => Point(30, 0)
            case Direction.South => Point(0, 30)
            case Direction.West  => Point(-30, 0)
          }

      // Choose the rect
      val baseRect =
        if player.direction == Direction.North || player.direction == Direction.South then
          NSRect
        else
          WERect
      val rect = baseRect.moveBy(diff)

      Batch(Shape.Box(rect, Fill.Color(PlankColor), Stroke.None))
    } else {
      Batch.empty
    }
  }

  override def entering(context: EnteringContext): Unit = {
    if shouldActivatePlank(context) then
      activatePlank(context)
  }

  private def shouldActivatePlank(context: EnteringContext): Boolean = {
    import context.*
    import PlankInteraction.Kind

    val behind = pos +> player.direction

    def testInteraction(ref: SquareRef, kind: Kind): Boolean = {
      val message = PlankInteraction(
        kind,
        player,
        passOverPos = pos,
        leaveFrom = src,
        arriveAt = behind,
      )
      ref().dispatch(message, ref).getOrElse(false)
    }

    testInteraction(dest, Kind.PassOver)
      && (testInteraction(src, Kind.LeaveFrom) || testInteraction(behind, Kind.ArriveAt))
  }

  private def activatePlank(context: EnteringContext): Unit = {
    import context.*

    transientComponent(PlankOverridingField.install(player, dest))
    inUse += player
    temporize()
  }
end PlankPlugin

object PlankPlugin:
  val PlankColor = RGBA(0.3137254901960784, 0.1568627450980392, 0.0)

  private val NSRect: Rectangle =
    Rectangle.cwh(Point.zero, 30 - 12, 30 + 10)

  private val WERect: Rectangle =
    Rectangle.cwh(Point.zero, 30 + 10, 30 - 12)
end PlankPlugin
