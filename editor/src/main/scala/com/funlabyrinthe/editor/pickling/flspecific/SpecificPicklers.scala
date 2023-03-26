package com.funlabyrinthe.editor.pickling
package flspecific

import com.funlabyrinthe.editor.reflect._

import com.funlabyrinthe.core.{ Universe, _ }

class SpecificPicklers(val universe: Universe) {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  def registerSpecificPicklers(registry: PicklingRegistry): Unit = {
    registry.registerSubType(
        InspectedType.staticMonoClass[Universe], (_, _) => UniversePickler, 95)
    registry.registerSubType(
        InspectedType.staticMonoClass[ResourceLoader], (_, _) => ResourceLoaderPickler, 95)

    registry.registerSubTypeReadWrite(
        InspectedType.staticMonoClass[Component], (_, _) => ComponentRefPickler, 95)
  }

  abstract class GlobalPickler(global: AnyRef, fakeString: String) extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
      assert(data.value == global, s"Trying to pickle another $fakeString")
      StringPickle(fakeString)
    }

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case StringPickle(`fakeString`) if !data.isReadOnly =>
          data.value = global
        case _ => ()
      }
    }
  }

  object UniversePickler extends GlobalPickler(
      universe, "<universe>")

  object ResourceLoaderPickler extends GlobalPickler(
      universe.resourceLoader, "<resource-loader>")

  object ComponentRefPickler extends Pickler {
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
      val component = data.value.asInstanceOf[Component]
      StringPickle(component.id)
    }

    def unpickle(data: InspectedData, pickle: Pickle)(
        implicit ctx: Context): Unit = {
      pickle match {
        case StringPickle(id) =>
          data.value = universe.getComponentByID(id)
        case _ => ()
      }
    }
  }
}
