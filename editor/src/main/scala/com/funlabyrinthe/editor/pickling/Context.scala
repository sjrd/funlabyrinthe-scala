package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._

trait Context {
  val registry: PicklingRegistry

  private implicit def implicitSelf: this.type = this

  def unpickleViaTempReadWrite(name: String, tpe: InspectedType,
      reprForErrorMessage: String => String, pickle: Pickle): Any = {

    val data = new TempReadWriteData(name, tpe, reprForErrorMessage)
    val optPickler = registry.createPickler(data)

    optPickler.fold[Any] {
      throw new UnsupportedOperationException(
          s"Ouch! Cannot find a pickler for param $name of $tpe")
    } { paramPickler =>
      paramPickler.unpickle(data, pickle)
      data.value
    }
  }
}
