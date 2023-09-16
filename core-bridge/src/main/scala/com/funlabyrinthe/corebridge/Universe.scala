package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.core.pickling.{PicklingRegistry, Context, Pickle}
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.coreinterface as intf

final class Universe(underlying: core.Universe) extends intf.Universe:
  private val editableComponentsCache = new WeakMap[core.Component, EditableComponent]
  private val editableMapsCache = new WeakMap[core.EditableMap, EditableMap]

  private val picklingRegistry: PicklingRegistry =
    val registry = new PicklingRegistry(underlying)
    SpecificPicklers.registerSpecificPicklers(registry, underlying)
    registry
  end picklingRegistry

  def load(pickleString: String): Unit =
    picklingRegistry.unpickle(underlying, Pickle.fromString(pickleString))

  def save(): String =
    picklingRegistry.pickle(underlying).toString()

  def allEditableComponents(): js.Array[intf.EditableComponent] =
    for
      coreComponent <- underlying.allComponents.toJSArray
      if !coreComponent.isInstanceOf[core.SquareMap]
    yield
      getEditableComponent(coreComponent)
  end allEditableComponents

  def getEditableComponentByID(id: String): js.UndefOr[intf.EditableComponent] =
    for coreComponent <- underlying.getComponentByIDOption(id).orUndefined yield
      getEditableComponent(coreComponent)

  private def getEditableComponent(coreComponent: core.Component): EditableComponent =
    editableComponentsCache.get(coreComponent).getOrElse {
      val intfComponent = new EditableComponent(this, coreComponent)
      editableComponentsCache.set(coreComponent, intfComponent)
      intfComponent
    }
  end getEditableComponent

  def allEditableMaps(): js.Array[intf.EditableMap] =
    for
      case coreMap: core.EditableMap <- underlying.allComponents.toJSArray
    yield
      getEditableMap(coreMap)
  end allEditableMaps

  def getEditableMapByID(id: String): js.UndefOr[intf.EditableMap] =
    for case coreMap: core.EditableMap <- underlying.getComponentByIDOption(id).orUndefined yield
      getEditableMap(coreMap)

  private def getEditableMap(coreMap: core.EditableMap): EditableMap =
    editableMapsCache.get(coreMap).getOrElse {
      val intfMap = new EditableMap(this, coreMap)
      editableMapsCache.set(coreMap, intfMap)
      intfMap
    }
  end getEditableMap

  private def createPicklingContext(): Context =
    new Context {
      val registry: PicklingRegistry = picklingRegistry
    }
end Universe
