package com.funlabyrinthe.core.pickling.flspecific

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.pickling.*

class SpecificPicklers(val universe: Universe) {
  def registerSpecificPicklers(registry: PicklingRegistry): Unit = {
    registry.registerPickleable[Component]()
  }

  given ComponentRefPickleable: Pickleable[Component] with
    def pickle(value: Component)(using Context): Pickle = {
      val component = value.asInstanceOf[Component]
      StringPickle(component.id)
    }

    def unpickle(pickle: Pickle)(using Context): Option[Component] = {
      pickle match {
        case StringPickle(id) =>
          Some(universe.getComponentByID(id))
        case _ =>
          None
      }
    }
  end ComponentRefPickleable
}

object SpecificPicklers:
  def registerSpecificPicklers(registry: PicklingRegistry, universe: Universe): Unit =
    new SpecificPicklers(universe).registerSpecificPicklers(registry)
end SpecificPicklers
