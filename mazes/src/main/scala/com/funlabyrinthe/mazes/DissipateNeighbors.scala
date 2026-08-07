package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

object DissipateNeighbors:
  inline val SquareSize = 30
  inline val NeighborSize = 10
  inline val NeighborAlphaDiff = 0xff / NeighborSize

  val NeighborGradientMasks: Array[Shape.Box] = {
    val lines = Direction.values.map {
      case Direction.North =>
        Point(0, 0) -> Point(0, NeighborSize)
      case Direction.East =>
        Point(SquareSize, 0) -> Point(SquareSize - NeighborSize, 0)
      case Direction.South =>
        Point(0, SquareSize) -> Point(0, SquareSize - NeighborSize)
      case Direction.West =>
        Point(0, 0) -> Point(NeighborSize, 0)
    }
    val rect = Rectangle(Point.zero, Size(SquareSize, SquareSize))
    for (fromPoint, toPoint) <- lines yield
      val fill = Fill.LinearGradient(fromPoint, RGBA.Black, toPoint, RGBA.Transparent)
      Shape.Box(rect, fill, Stroke.None, Point(SquareSize / 2, SquareSize / 2))
  }

  val NeighborCornerGradientMasks: Array[Shape.Box] = {
    val lines = Direction.values.map {
      case Direction.North =>
        Point(SquareSize, 0) -> Point(SquareSize - NeighborSize/2, NeighborSize/2)
      case Direction.East =>
        Point(SquareSize, SquareSize) -> Point(SquareSize - NeighborSize/2, SquareSize - NeighborSize/2)
      case Direction.South =>
        Point(0, SquareSize) -> Point(NeighborSize/2, SquareSize - NeighborSize/2)
      case Direction.West =>
        Point(0, 0) -> Point(NeighborSize/2, NeighborSize/2)
    }
    val rect = Rectangle(Point.zero, Size(SquareSize, SquareSize))
    for (fromPoint, toPoint) <- lines yield
      val fill = Fill.LinearGradient(fromPoint, RGBA.Black, toPoint, RGBA.Transparent)
      Shape.Box(rect, fill, Stroke.None, Point(SquareSize / 2, SquareSize / 2))
  }

  /** Subclass of PresentSquareContext so that we can detect infinite recursions. */
  private class DissipateNeighborsPresentSquareContext(
    _tickCount: Long,
    _where: Option[SquareRef],
    _purpose: DrawPurpose,
    _cellSize: Size,
  ) extends PresentSquareContext(_tickCount, _where, _purpose, _cellSize) {
    override def withWhere(where: Option[SquareRef]): DissipateNeighborsPresentSquareContext =
      new DissipateNeighborsPresentSquareContext(tickCount, where, purpose, cellSize)

    override def withPurpose(purpose: DrawPurpose): DissipateNeighborsPresentSquareContext =
      new DissipateNeighborsPresentSquareContext(tickCount, where, purpose, cellSize)
  }

  def presentDissipateNeighbors(context: PresentSquareContext, predicate: Field => Boolean)(
      using universe: Universe): Batch[SceneNode] = {

    (context, context.where) match {
      case (_: DissipateNeighborsPresentSquareContext, _) =>
        // Cut out an infinite recursion
        Batch.empty

      case (_, None) =>
        Batch.empty

      case (_, Some(pos)) =>
        val thisField = pos().field

        def testField(pos: SquareRef): Boolean =
          val field = pos().field
          field != thisField && predicate(field)

        val nestedContext = new DissipateNeighborsPresentSquareContext(
          context.tickCount,
          context.where,
          context.purpose,
          context.cellSize,
        )

        def dissipateOne(field: Field, mask: SceneNode): Batch[SceneNode] =
          Batch(Masked(mask, Group(field.present(nestedContext))))

        val batches = for dir <- Direction.values yield {
          val neighborPos = pos +> dir

          if testField(neighborPos) then
            // Regular line dissipation
            dissipateOne(neighborPos().field, NeighborGradientMasks(dir.ordinal))
          else
            // Maybe we need a corner
            val otherNeighborPos = pos +> dir.right
            val cornerNeighborPos = otherNeighborPos +> dir
            if !testField(otherNeighborPos) && testField(cornerNeighborPos) then
              dissipateOne(cornerNeighborPos().field, NeighborCornerGradientMasks(dir.ordinal))
            else
              Batch.empty
        }

        batches.reduce(_ ++ _)
    }
  }

  def presentDissipateGroundNeighbors(context: PresentSquareContext)(using Universe): Batch[SceneNode] =
    presentDissipateNeighbors(context, _.isInstanceOf[Ground])
end DissipateNeighbors
