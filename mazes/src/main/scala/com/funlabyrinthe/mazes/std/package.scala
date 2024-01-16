package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core._
import graphics.Color

package object std {
  val SilverLock = ColorLock(Color.Silver)
  val GoldenLock = ColorLock(Color.Gold)

  implicit class PlayerOps(val player: Player) extends AnyVal {
    private def mazes: Mazes = player.universe.module[Mazes]

    // Items

    def silverKeys: Int = mazes.silverKeys.count(player)
    def silverKeys_=(value: Int): Unit = mazes.silverKeys.count(player) = value

    def goldenKeys: Int = mazes.goldenKeys.count(player)
    def goldenKeys_=(value: Int): Unit = mazes.goldenKeys.count(player) = value
  }
}
