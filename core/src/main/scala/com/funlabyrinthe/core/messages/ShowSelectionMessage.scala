package com.funlabyrinthe.core.messages

import com.funlabyrinthe.core.*

final case class ShowSelectionMessage(
  prompt: String,
  answers: List[String],
  options: ShowSelectionMessage.Options,
) extends Message[Int]

object ShowSelectionMessage:
  final class Options private (
    val default: Int,
    val showOnlySelected: Boolean,
  ):
    def this() =
      this(default = 0, showOnlySelected = false)

    def withDefault(default: Int): Options =
      copy(default = default)

    def withShowOnlySelected(showOnlySelected: Boolean): Options =
      copy(showOnlySelected = showOnlySelected)

    private def copy(
      default: Int = this.default,
      showOnlySelected: Boolean = this.showOnlySelected,
    ): Options =
      Options(default, showOnlySelected)
    end copy

    override def equals(that: Any): Boolean = that match
      case that: Options =>
        this.default == that.default
          && this.showOnlySelected == that.showOnlySelected
    end equals

    override def hashCode(): Int = (default, showOnlySelected).##
  end Options
end ShowSelectionMessage
