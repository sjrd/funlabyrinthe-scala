package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

trait MutableMembersPickler extends Pickler {
  val tpe: InspectedType

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    pickle(data, Set.empty)
  }

  protected def pickle(data: InspectedData, exclude: Set[String])(
      implicit ctx: Context): Pickle = {
    import ReflectionUtils._

    val pickledFields = for {
      (propData, propPickler) <-
          Utils.reflectingPicklersForFields(data.value, this.tpe).toList
      if !exclude.contains(propData.name)
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

        for {
          (propData, propPickler) <-
              Utils.reflectingPicklersForFields(data.value, this.tpe).toList
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
