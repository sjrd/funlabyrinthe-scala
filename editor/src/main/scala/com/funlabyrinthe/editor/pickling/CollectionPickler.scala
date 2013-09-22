package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

import scala.reflect.runtime.universe._

import scala.collection.mutable.{ Builder, ListBuffer }

trait CollectionPickler[Elem, Repr] extends Pickler {
  val elemTpe: Type

  def toSeq(coll: Repr): Seq[Elem]
  def makeBuilder(): Builder[Elem, Repr]

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    val seq = toSeq(data.value.asInstanceOf[Repr])

    val pickledElems = new ListBuffer[Pickle]

    for (elem <- seq) {
      val elemData = new InspectedData {
        val name = ""
        val tpe = elemTpe
        override val isReadOnly = false

        def value: Any = elem
        def value_=(v: Any): Unit = ???
      }

      for (elemPickler <- ctx.registry.createPickler(elemData))
        pickledElems += elemPickler.pickle(elemData)
    }

    ListPickle(pickledElems.result())
  }
}
