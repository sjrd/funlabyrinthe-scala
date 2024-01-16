package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.graphics.Color

trait Lock

case object NoLock extends Lock

case class ColorLock(color: Color) extends Lock
