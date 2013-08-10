package com.funlabyrinthe.core.input

sealed abstract class KeyCode

object KeyCode {
  case object Enter extends KeyCode

  case object Left extends KeyCode
  case object Up extends KeyCode
  case object Right extends KeyCode
  case object Down extends KeyCode

  case object Other extends KeyCode
}
