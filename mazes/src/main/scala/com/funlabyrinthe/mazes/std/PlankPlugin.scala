package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.mazes.*

class PlankPlugin(using ComponentInit) extends PlayerPlugin:
  import PlankPlugin.*

  @transient @noinspect
  object inUse extends CorePlayer.mutable.SimplePerPlayerData[Boolean](false)

  override def drawBefore(player: Player, context: DrawSquareContext): Unit =
    import context.*

    if inUse(player) then
      // Find the actual square where we need to draw the plank
      val targetRect = player.position match
        case Some(pos) if pos().field.isInstanceOf[PlankOverridingField] =>
          rect
        case _ =>
          val (diffX, diffY) = player.direction match
            case Some(Direction.North) => (0, -30)
            case Some(Direction.East)  => (30, 0)
            case Some(Direction.South) => (0, 30)
            case Some(Direction.West)  => (-30, 0)
            case None                  => (0, 0)
          Rectangle2D(rect.minX + diffX, rect.minY + diffY, rect.width, rect.height)
      end targetRect

      // Draw the plank
      val squareSize = 30
      val plankRect =
        if player.direction.exists(d => d == Direction.North || d == Direction.South) then
          Rectangle2D(targetRect.minX + 6, targetRect.minY - 5, squareSize - 12, squareSize + 10)
        else
          Rectangle2D(targetRect.minX - 5, targetRect.minY + 6, squareSize + 10, squareSize - 12)

      gc.fill = PlankColor
      gc.fillRect(plankRect.minX, plankRect.minY, plankRect.width, plankRect.height)
  end drawBefore

  override def moving(context: MoveContext): Unit =
    if shouldActivatePlank(context) then
      activatePlank(context)
  end moving

  private def shouldActivatePlank(context: MoveContext): Boolean =
    import context.*
    import PlankInteraction.Kind

    val resultOption =
      for
        src <- context.src
        dest <- context.dest
        direction <- player.direction
        if isRegular && src.pos +> direction == dest.pos
      yield
        val behind = context.dest.get +> direction

        def testInteraction(ref: SquareRef, kind: Kind): Boolean =
          val message = PlankInteraction(
            kind,
            player,
            passOverPos = dest,
            leaveFrom = src,
            arriveAt = behind,
          )
          ref().dispatch(message, ref).getOrElse(false)
        end testInteraction

        testInteraction(dest, Kind.PassOver)
          && (testInteraction(src, Kind.LeaveFrom) || testInteraction(behind, Kind.ArriveAt))
    end resultOption

    resultOption.getOrElse(false)
  end shouldActivatePlank

  private def activatePlank(context: MoveContext): Unit =
    import context.*

    transientComponent(PlankOverridingField.install(player, dest.get))
    inUse(player) = true
    temporize()
  end activatePlank
end PlankPlugin

object PlankPlugin:
  val PlankColor = Color(0.3137254901960784, 0.1568627450980392, 0.0)
end PlankPlugin
