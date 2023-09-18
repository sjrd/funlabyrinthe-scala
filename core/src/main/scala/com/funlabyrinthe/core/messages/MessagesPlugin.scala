package com.funlabyrinthe.core.messages

import com.funlabyrinthe.core._

abstract class MessagesPlugin(using ComponentInit) extends CorePlayerPlugin {
  def showMessage(player: CorePlayer, message: String): Control[Unit]

  def showSelectionMessage(
    player: CorePlayer,
    prompt: String,
    answers: List[String],
    options: ShowSelectionMessage.Options,
  ): Control[Int]

  override def onMessage[A](player: CorePlayer): PartialFunction[Message[A], Control[A]] = {
    case ShowMessage(msg) =>
      showMessage(player, msg)
    case ShowSelectionMessage(prompt, answers, options) =>
      showSelectionMessage(player, prompt, answers, options)
  }
}
