package com.funlabyrinthe.core

abstract class ComponentCreator()(
    implicit universe: Universe, originalID: ComponentID)
    extends Component:

  type CreatedComponentType <: Component

  private var createdComponents: List[CreatedComponentType] = Nil

  protected def baseID: String

  protected def createComponent(id: ComponentID): CreatedComponentType

  final def createNewComponent(): CreatedComponentType =
    val baseID = this.baseID
    val id = Iterator.from(1).map(idx => baseID + idx).find(id => universe.getComponentByIDOption(id).isEmpty).get
    createNewComponent(id)
  end createNewComponent

  final def createNewComponent(id: String): CreatedComponentType =
    val component = createComponent(ComponentID(id))
    createdComponents ::= component
    component
  end createNewComponent

  private[core] def allCreatedComponents: List[CreatedComponentType] =
    createdComponents.reverse
end ComponentCreator
