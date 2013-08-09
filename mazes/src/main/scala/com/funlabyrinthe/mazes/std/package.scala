package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

import javafx.scene.paint.Color

package object std {
  val SilverLock = ColorLock(Color.SILVER)
  val GoldenLock = ColorLock(Color.GOLD)

  implicit class PlayerOps(val player: Player) extends AnyVal {
    import player.universe.mazes._

    // Items

    def silverKeys: Int = SilverKeys.count(player)
    def silverKeys_=(value: Int): Unit = SilverKeys.count(player) = value

    def goldenKeys: Int = GoldenKeys.count(player)
    def goldenKeys_=(value: Int): Unit = GoldenKeys.count(player) = value

    // Messages

    def showMessage(message: String): Unit @control = {
      if (message != "")
        player.dispatch(ShowMessage(message))
    }
  }
}
