package com.funlabyrinthe.core

import com.funlabyrinthe.core.pickling.Pickleable

enum Direction3D derives Pickleable:
  case North, East, South, West, Up, Down

  def toDirection: Option[Direction] = this match
    case North => Some(Direction.North)
    case East  => Some(Direction.East)
    case South => Some(Direction.South)
    case West  => Some(Direction.West)
    case Up    => None
    case Down  => None
  end toDirection
end Direction3D
