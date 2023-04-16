package com.funlabyrinthe.editor.pickling
package flspecific

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

import com.funlabyrinthe.core.{ Universe, _ }

class SpecificPicklers(val universe: Universe) {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  def registerSpecificPicklers(registry: PicklingRegistry): Unit = {
    registry.registerSubTypeReadWrite(
        InspectedType.staticMonoClass[Component], (_, _) => ComponentRefPickler, 95)
  }

  object ComponentRefPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
      val component = data.value.asInstanceOf[Component]
      StringPickle(component.id)
    }

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case StringPickle(id) =>
          data.asWritable.value = universe.getComponentByID(id)
        case _ => ()
      }
    }
  }
}
