package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._

class TempReadWriteData(val name: String, val tpe: InspectedType,
    reprForErrorMessage: String => String) extends WritableInspectedData {

  def this(name: String, tpe: InspectedType, initialValue: Any) = {
    this(name, tpe, (_: String) => "") // callback will never be called
    myValue = Some(initialValue)
  }

  //override val isReadOnly = false

  private var myValue: Option[Any] = None

  def value_=(v: Any): Unit = myValue = Some(v)

  def value: Any = myValue getOrElse {
    throw new UnsupportedOperationException(
        s"Ouch! Value for ${reprForErrorMessage(name)} has not yet been set")
  }
}
