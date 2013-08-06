package com.funlabyrinthe.mazes
package std

import javafx.scene.paint.Color

trait Lock

case object NoLock extends Lock

case class ColorLock(color: Color) extends Lock
