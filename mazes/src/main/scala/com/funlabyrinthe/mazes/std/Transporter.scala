package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Transporter(using ComponentInit) extends Effect derives Reflector:
  import Transporter.*

  var kind: TransporterKind = TransporterKind.Next

  category = ComponentCategory("transporters", "Transporters")

  painter += "Transporters/Transporter"

  override def reflect() = autoReflect[Transporter]

  override def execute(context: MoveContext): Unit = {
    var destSquare = findDestination(context)

    if destSquare != context.pos then
      context.temporize()
      context.player.moveTo(destSquare)
  }

  protected def findDestination(context: MoveContext): SquareRef = kind match
    case TransporterKind.Inactive => context.pos
    case TransporterKind.Next     => findNext(context.pos)
    case TransporterKind.Previous => findPrevious(context.pos)
    case TransporterKind.Random   => findRandom(context.pos)
  end findDestination

  protected final def findNext(source: SquareRef): SquareRef =
    val dims = source.map.dimensions

    def next(pos: Position): Position =
      if pos.x + 1 < dims.x then
        Position(pos.x + 1, pos.y, pos.z)
      else if pos.y + 1 < dims.y then
        Position(0, pos.y + 1, pos.z)
      else if pos.z + 1 < dims.z then
        Position(0, 0, pos.z + 1)
      else
        Position.Zero
    end next

    findDeterministic(source, next)
  end findNext

  protected final def findPrevious(source: SquareRef): SquareRef =
    val dims = source.map.dimensions

    def previous(pos: Position): Position =
      if pos.x > 0 then
        Position(pos.x - 1, pos.y, pos.z)
      else if pos.y > 0 then
        Position(dims.x - 1, pos.y - 1, pos.z)
      else if pos.z > 0 then
        Position(dims.x - 1, dims.y - 1, pos.z - 1)
      else
        Position(dims.x - 1, dims.y - 1, dims.z - 1)
    end previous

    findDeterministic(source, previous)
  end findPrevious

  private def findDeterministic(source: SquareRef, next: Position => Position): SquareRef =
    val map = source.map

    def loop(candidate: Position): SquareRef =
      if map(candidate).effect == this || candidate == source.pos then
        map.ref(candidate)
      else
        loop(next(candidate))
    end loop

    loop(next(source.pos))
  end findDeterministic

  protected final def findRandom(source: SquareRef): SquareRef =
    val candidates =
      for
        candidate <- source.map.allRefs
        if candidate().effect == this && candidate != source
      yield candidate

    if candidates.isEmpty then source
    else candidates(scala.util.Random.nextInt(candidates.size))
  end findRandom
end Transporter
