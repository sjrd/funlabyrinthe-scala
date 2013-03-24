package com.funlabyrinthe.core

sealed abstract class Direction {
  final def next: Direction = this match {
    case North => East
    case East => South
    case South => West
    case West => North
  }

  final def previous: Direction = this match {
    case North => West
    case East => North
    case South => East
    case West => South
  }

  final def opposite: Direction = this match {
    case North => South
    case East => West
    case South => North
    case West => East
  }
}

case object North extends Direction
case object East extends Direction
case object South extends Direction
case object West extends Direction
