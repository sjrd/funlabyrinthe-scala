package com.funlabyrinthe.core

import com.funlabyrinthe.core.pickling.InPlacePickleable

abstract class ComponentCreator(using ComponentInit) extends Component:
  type CreatedComponentType <: Component

  private var createdComponents: List[CreatedComponentType] = Nil

  protected def baseID: String = this.id.stripSuffix("Creator")

  protected def createComponent()(using init: ComponentInit): CreatedComponentType

  final def createNewComponent(): CreatedComponentType =
    val baseID = this.baseID
    val id = Iterator.from(1).map(idx => baseID + idx).find(id => universe.getComponentByIDOption(id).isEmpty).get
    createNewComponent(id)
  end createNewComponent

  final def createNewComponent(id: String): CreatedComponentType =
    val init = ComponentInit(universe, id, this)
    val component = createComponent()(using init)
    if component.editVisualTag.isEmpty() then
      component.editVisualTag = id.reverse.takeWhile(c => c >= '0' && c <= '9').reverse
    InPlacePickleable.storeDefaults(component)
    createdComponents ::= component
    component
  end createNewComponent

  private[core] def allCreatedComponents: List[CreatedComponentType] =
    createdComponents.reverse
end ComponentCreator
