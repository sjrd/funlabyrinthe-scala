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

    def silverKeys: Int = mazes.SilverKeys.count(player)
    def silverKeys_=(value: Int): Unit = mazes.SilverKeys.count(player) = value

    def goldenKeys: Int = mazes.GoldenKeys.count(player)
    def goldenKeys_=(value: Int): Unit = mazes.GoldenKeys.count(player) = value

    // Messages

    def showMessage(message: String): Control[Unit] = control {
      if (message != "")
        player.dispatch(ShowMessage(message))
    }

    def showSelectionMessage(
      prompt: String,
      answers: List[String],
      default: Int = 0,
      showOnlySelected: Boolean = false,
    ): Control[Int] =
      require(answers.nonEmpty, "Cannot call showSelectionMessage with an list of answers")
      val default1 = constrainToRange(default, 0, answers.size - 1)
      val options = ShowSelectionMessage.Options()
        .withDefault(default1)
        .withShowOnlySelected(showOnlySelected)
      val msg = ShowSelectionMessage(prompt, answers, options)
      player.dispatch(msg)
    end showSelectionMessage

    def showSelectNumberMessage(
      prompt: String,
      min: Int,
      max: Int,
      default: Int = Int.MinValue,
    ): Control[Int] =
      require(max >= min, s"Cannot call showSelectNumberMessage with an empty range $min..$max")
      val default1 = constrainToRange(default, min, max)

      val answers = (max to min by -1).map(_.toString()).toList
      val defaultInAnswers = max - default1

      for
        resultInAnswers <- showSelectionMessage(prompt, answers, default = defaultInAnswers, showOnlySelected = true)
      yield
        max - resultInAnswers
    end showSelectNumberMessage
  }

  private def constrainToRange(value: Int, min: Int, max: Int): Int =
    if value < min then min
    else if value > max then max
    else value
}
