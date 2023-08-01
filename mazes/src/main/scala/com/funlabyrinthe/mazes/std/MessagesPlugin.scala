package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

abstract class MessagesPlugin(using ComponentInit) extends PlayerPlugin {
  def showMessage(player: Player, message: String): Control[Unit]

  def showSelectionMessage(
    player: Player,
    prompt: String,
    answers: List[String],
    options: ShowSelectionMessage.Options,
  ): Control[Int]

  override def onMessage[A](player: Player): PartialFunction[Message[A], Control[A]] = {
    case ShowMessage(msg) =>
      showMessage(player, msg)
    case ShowSelectionMessage(prompt, answers, options) =>
      showSelectionMessage(player, prompt, answers, options)
  }
}
