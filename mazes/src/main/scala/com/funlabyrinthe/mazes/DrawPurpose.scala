package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

sealed abstract class DrawPurpose

object DrawPurpose:
  val Unknown: DrawPurpose =
    // Prevents anyone from exhaustively matching on DrawPurpose
    new DrawPurpose {}

  final case class Icon(val component: Component) extends DrawPurpose

  final case class EditMap(val map: Map, val floor: Int) extends DrawPurpose

  final case class PlayerView(val player: Player) extends DrawPurpose
end DrawPurpose
