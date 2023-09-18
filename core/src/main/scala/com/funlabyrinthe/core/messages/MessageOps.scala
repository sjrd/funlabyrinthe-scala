package com.funlabyrinthe.core.messages

import com.funlabyrinthe.core.*

object MessageOps:
  def showMessage(player: CorePlayer, message: String): Control[Unit] = {
    import messages.*

    if (message != "")
      player.dispatch(ShowMessage(message))
    else
      doNothing()
  }

  def showSelectionMessage(
    player: CorePlayer,
    prompt: String,
    answers: List[String],
    default: Int = 0,
    showOnlySelected: Boolean = false,
  ): Control[Int] =
    import messages.*

    require(answers.nonEmpty, "Cannot call showSelectionMessage with an list of answers")
    val default1 = constrainToRange(default, 0, answers.size - 1)
    val options = ShowSelectionMessage.Options()
      .withDefault(default1)
      .withShowOnlySelected(showOnlySelected)
    val msg = ShowSelectionMessage(prompt, answers, options)
    player.dispatch(msg)
  end showSelectionMessage

  def showSelectNumberMessage(
    player: CorePlayer,
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
      resultInAnswers <-
        showSelectionMessage(player, prompt, answers, default = defaultInAnswers, showOnlySelected = true)
    yield
      max - resultInAnswers
  end showSelectNumberMessage

  private def constrainToRange(value: Int, min: Int, max: Int): Int =
    if value < min then min
    else if value > max then max
    else value
end MessageOps
