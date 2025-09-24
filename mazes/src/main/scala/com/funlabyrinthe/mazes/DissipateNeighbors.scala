package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

object DissipateNeighbors:
  inline val SquareSize = 30
  inline val NeighborSize = 10
  inline val NeighborAlphaDiff = 0xff / NeighborSize

  val NeighborGradients: Array[LinearGradient] =
    val lines = Direction.values.map {
      case Direction.North =>
        Point2D(0, 0) -> Point2D(0, NeighborSize)
      case Direction.East =>
        Point2D(SquareSize, 0) -> Point2D(SquareSize - NeighborSize, 0)
      case Direction.South =>
        Point2D(0, SquareSize) -> Point2D(0, SquareSize - NeighborSize)
      case Direction.West =>
        Point2D(0, 0) -> Point2D(NeighborSize, 0)
    }
    val colorStops = List(0.0 -> Color.White.withAlpha(0), 1.0 -> Color.White)
    lines.map(line => LinearGradient(line._1, line._2, colorStops))
  end NeighborGradients

  val NeighborCornerGradients: Array[LinearGradient] =
    val lines = Direction.values.map {
      case Direction.North =>
        Point2D(SquareSize, 0) -> Point2D(SquareSize - NeighborSize/2, NeighborSize/2)
      case Direction.East =>
        Point2D(SquareSize, SquareSize) -> Point2D(SquareSize - NeighborSize/2, SquareSize - NeighborSize/2)
      case Direction.South =>
        Point2D(0, SquareSize) -> Point2D(NeighborSize/2, SquareSize - NeighborSize/2)
      case Direction.West =>
        Point2D(0, 0) -> Point2D(NeighborSize/2, NeighborSize/2)
    }
    val colorStops = List(0.0 -> Color.White.withAlpha(0), 1.0 -> Color.White)
    lines.map(line => LinearGradient(line._1, line._2, colorStops))
  end NeighborCornerGradients

  /** Subclass of DrawSquareContext so that we can detect infinite recursions. */
  private class DissipateNeighborsDrawSquareContext(
    _gc: GraphicsContext,
    _tickCount: Long,
    _rect: Rectangle2D,
    _where: Option[SquareRef],
    _purpose: DrawPurpose,
  ) extends DrawSquareContext(_gc, _tickCount, _rect, _where, _purpose):
    override def withGraphicsContext(gc: GraphicsContext, rect: Rectangle2D): DrawSquareContext =
      new DissipateNeighborsDrawSquareContext(gc, tickCount, rect, where, purpose)

    override def withRect(rect: Rectangle2D): DrawSquareContext =
      new DissipateNeighborsDrawSquareContext(gc, tickCount, rect, where, purpose)

    override def withWhere(where: Option[SquareRef]): DrawSquareContext =
      new DissipateNeighborsDrawSquareContext(gc, tickCount, rect, where, purpose)

    override def withPurpose(purpose: DrawPurpose): DrawSquareContext =
      new DissipateNeighborsDrawSquareContext(gc, tickCount, rect, where, purpose)
  end DissipateNeighborsDrawSquareContext

  def dissipateNeighbors(context: DrawSquareContext, predicate: Field => Boolean)(using universe: Universe): Unit =
    (context, context.where) match
      case (_: DissipateNeighborsDrawSquareContext, _) =>
        // Cut out an infinite recursion
        ()

      case (_, None) =>
        ()

      case (_, Some(pos)) =>
        val thisField = pos().field

        def testField(pos: SquareRef): Boolean =
          val field = pos().field
          field != thisField && predicate(field)

        lazy val (nestedCanvas, nestedContext) =
          val canvas = universe.graphicsSystem.createCanvas(SquareSize, SquareSize)
          val ctx = new DissipateNeighborsDrawSquareContext(
            canvas.getGraphicsContext2D(),
            context.tickCount,
            Rectangle2D(0, 0, SquareSize, SquareSize),
            context.where,
            context.purpose,
          )
          (canvas, ctx)

        def dissipateOne(field: Field, gradient: Paint): Unit =
          val gc = nestedContext.gc
          gc.save()
          nestedContext.gc.clearRect(0, 0, SquareSize, SquareSize)
          field.drawTo(nestedContext)
          gc.globalCompositeOperation = GlobalCompositeOperation.DestinationOut
          gc.fill = gradient
          gc.fillRect(0, 0, SquareSize, SquareSize)
          gc.restore()

          context.gc.drawImage(nestedCanvas, context.tickCount, context.rect.minX, context.rect.minY)
        end dissipateOne

        for dir <- Direction.values do
          val neighborPos = pos +> dir

          if testField(neighborPos) then
            // Regular line dissipation
            dissipateOne(neighborPos().field, NeighborGradients(dir.ordinal))
          else
            // Maybe we need a corner
            val otherNeighborPos = pos +> dir.right
            val cornerNeighborPos = otherNeighborPos +> dir
            if !testField(otherNeighborPos) && testField(cornerNeighborPos) then
              dissipateOne(cornerNeighborPos().field, NeighborCornerGradients(dir.ordinal))
        end for
    end match
  end dissipateNeighbors

  def dissipateGroundNeighbors(context: DrawSquareContext)(using Universe): Unit =
    dissipateNeighbors(context, _.isInstanceOf[Ground])
end DissipateNeighbors
