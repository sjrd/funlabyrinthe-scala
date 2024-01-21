package com.funlabyrinthe.core

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

enum Direction derives Pickleable, Inspectable.StringChoices {
  case North, East, South, West

  final def next: Direction = this match {
    case North => East
    case East => South
    case South => West
    case West => North
  }

  final def right: Direction = next

  final def previous: Direction = this match {
    case North => West
    case East => North
    case South => East
    case West => South
  }

  final def left: Direction = previous

  final def opposite: Direction = this match {
    case North => South
    case East => West
    case South => North
    case West => East
  }

  def toDirection3D: Direction3D = this match
    case North => Direction3D.North
    case East  => Direction3D.East
    case South => Direction3D.South
    case West  => Direction3D.West
  end toDirection3D
}
