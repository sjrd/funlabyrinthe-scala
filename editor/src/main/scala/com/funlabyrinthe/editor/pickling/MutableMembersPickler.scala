package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

import scala.reflect.runtime.universe._

trait MutableMembersPickler extends Pickler {
  val tpe: Type

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    import ReflectionUtils._

    val instanceMirror = reflectInstance(data.value)
    val tpe = guessRuntimeTypeOf(instanceMirror, this.tpe)
    println(s"members of ${data.value} of type $tpe")

    val pickledFields = for {
      (propData, propPickler) <-
          Utils.reflectingPicklersForFields(instanceMirror, tpe).toList
    } yield {
      println(s"  ${propData.name}: ${propData.tpe}")
      (propData.name, propPickler.pickle(propData))
    }

    ObjectPickle(pickledFields)
  }

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit = {
    import ReflectionUtils._

    pickle match {
      case ObjectPickle(pickleFields) =>
        val pickleMap = Map(pickleFields:_*)

        val instanceMirror = reflectInstance(data.value)
        val tpe = guessRuntimeTypeOf(instanceMirror, this.tpe)
        println(s"members of ${data.value} of type $tpe")

        for {
          (propData, propPickler) <-
              Utils.reflectingPicklersForFields(instanceMirror, tpe).toList
        } {
          println(s"  ${propData.name}: ${propData.tpe}")
          pickleMap.get(propData.name) foreach { propPickle =>
            propPickler.unpickle(propData, propPickle)
          }
        }

      case _ =>
        ()
    }
  }
}
