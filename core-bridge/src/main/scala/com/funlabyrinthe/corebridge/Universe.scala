package com.funlabyrinthe.corebridge

import java.io.IOException

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.core.pickling.*

import com.funlabyrinthe.coreinterface as intf

final class Universe(underlying: core.Universe) extends intf.Universe:
  private val editableComponentsCache = new WeakMap[core.Component, EditableComponent]
  private val editableMapsCache = new WeakMap[core.EditableMap, EditableMap]

  def load(pickleString: String): js.Array[intf.PicklingError] =
    Errors.protect {
      val context = PicklingContext.make(underlying)
      InPlacePickleable.unpickle(underlying, Pickle.fromString(pickleString))(using context)
      context.errors.toJSArray.map { coreError =>
        new intf.PicklingError {
          val component = coreError.component.map(_.id).orUndefined
          val path = coreError.path.toJSArray
          val message = coreError.message
        }
      }
    }
  end load

  def save(): String =
    Errors.protect {
      val context = PicklingContext.make(underlying)
      val result = InPlacePickleable.pickle(underlying)(using context).getOrElse(ObjectPickle(Nil)).toString()
      if context.errors.nonEmpty then
        // When saving, all errors are fatal
        throw IOException(
          context.errors.mkString("There were some errors while saving the universe:\n", "\n", "")
        )
      result
    }
  end save

  def allEditableComponents(): js.Array[intf.EditableComponent] =
    for
      coreComponent <- underlying.allComponents.toJSArray
      if !coreComponent.isInstanceOf[core.SquareMap]
    yield
      getEditableComponent(coreComponent)
  end allEditableComponents

  def getEditableComponentByID(id: String): js.UndefOr[intf.EditableComponent] =
    for coreComponent <- underlying.lookupNestedComponentByFullID(id).orUndefined yield
      getEditableComponent(coreComponent)

  def getEditableComponent(coreComponent: core.Component): EditableComponent =
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
    for case coreMap: core.EditableMap <- underlying.lookupNestedComponentByFullID(id).orUndefined yield
      getEditableMap(coreMap)

  private def getEditableMap(coreMap: core.EditableMap): EditableMap =
    editableMapsCache.get(coreMap).getOrElse {
      val intfMap = new EditableMap(this, coreMap)
      editableMapsCache.set(coreMap, intfMap)
      intfMap
    }
  end getEditableMap

  def startGame(): intf.RunningGame =
    Errors.protect {
      underlying.startGame()
      new RunningGame(underlying)
    }
  end startGame
end Universe
