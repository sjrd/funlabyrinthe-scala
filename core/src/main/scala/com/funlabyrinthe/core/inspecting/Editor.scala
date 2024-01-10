package com.funlabyrinthe.core.inspecting

import com.funlabyrinthe.core.graphics.Painter.PainterItem

sealed abstract class Editor:
  type ValueType

object Editor:
  case object Text extends Editor:
    type ValueType = String

  case object Switch extends Editor:
    type ValueType = Boolean

  final case class SmallInteger(val minValue: Int, val maxValue: Int, val step: Int) extends Editor:
    type ValueType = Int

  final case class StringChoices(val choices: List[String]) extends Editor:
    type ValueType = String

  final case class MultiStringChoices(val choices: List[String]) extends Editor:
    type ValueType = List[String]

  case object Painter extends Editor:
    type ValueType = List[PainterItem]

  case object Color extends Editor:
    /** RGBA packed in 32 bits. */
    type ValueType = Int

  val Int8: SmallInteger = SmallInteger(Byte.MinValue, Byte.MaxValue, 1)
  val Int16: SmallInteger = SmallInteger(Short.MinValue, Short.MaxValue, 1)
  val Int32: SmallInteger = SmallInteger(Int.MinValue, Int.MaxValue, 1)
end Editor
