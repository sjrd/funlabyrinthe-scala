package com.funlabyrinthe.core

import scala.reflect.{ClassTag, classTag}

abstract class ComponentCreator[C <: Component](using ComponentInit, ClassTag[C]) extends Component:
  type CreatedComponentType = C

  private val constructor: ComponentInit => C =
    Universe.lookupAdditionalComponentConstructor[C]().getOrElse {
      throw IllegalArgumentException(
        s"Cannot make a ComponentCreator for class ${classTag[C].runtimeClass.getName()}, "
          + "because that class is not static or does not have a (using ComponentInit) constructor."
      )
    }
  end constructor

  protected def baseID: String = this.id.stripSuffix("Creator")

  protected def initializeNewComponent(component: C): Unit =
    if component.editVisualTag == "" then
      component.editVisualTag = component.id.reverse.takeWhile(c => c >= '0' && c <= '9').reverse
  end initializeNewComponent

  final def createNewComponent(): CreatedComponentType =
    createNewComponent(universe.makeNewAdditionalComponentInit(baseID))

  final def createNewComponent(id: String): CreatedComponentType =
    createNewComponent(ComponentInit(universe, id, ComponentOwner.Module(AdditionalComponents)))

  private def createNewComponent(init: ComponentInit): CreatedComponentType =
    val component = constructor(init)
    initializeNewComponent(component)
    component
  end createNewComponent

  final def createdComponentWithID(id: String): CreatedComponentType =
    val cls = classTag[C].runtimeClass.asInstanceOf[Class[C]]
    cls.cast(universe.findTopComponentByID[Component](AdditionalComponents, id))
end ComponentCreator
