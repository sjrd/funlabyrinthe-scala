package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

final class EditableMap(universe: Universe, underlying: core.EditableMap)
    extends EditableMap.Base(universe, underlying, underlying.getEditInterface())

private object EditableMap:
  abstract class Base(universe: Universe, underlying: core.EditableMap, editInterface: core.MapEditInterface)
      extends intf.EditableMap:
    def fullID: String = underlying.fullID
    def shortID: String = underlying.id

    def floors: Int = editInterface.floors

    def getFloorSize(floor: Int): intf.Size =
      val coreSize = editInterface.getFloorSize(floor)
      intf.Size(coreSize.width, coreSize.height)

    def presentFloor(floor: Int): Int8Array = {
      Errors.protect {
        universe.writeSceneUpdateFragment(editInterface.presentFloor(floor))
      }
    }

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      editInterface.getDescriptionAt(x, y, floor)

    def onMouseClicked(
      x: Double,
      y: Double,
      floor: Int,
      selectedComponent: intf.EditableComponent,
      editingServices: intf.EditingServices,
    ): js.Promise[Unit] =
      JSPI.async {
        EditingServices.withEditingServices(editingServices) {
          val event = new core.input.MouseEvent(x, y, core.input.MouseButton.Primary)
          val component = selectedComponent.asInstanceOf[EditableComponent]
          editInterface.onMouseClicked(event, floor, component.underlying)
        }
      }
    end onMouseClicked

    def newResizingView(): intf.EditableMap.ResizingView =
      val resizingIntf = editInterface.newResizingView()
      Resizing(universe, underlying, resizingIntf)
    end newResizingView
  end Base

  private class Resizing(
    universe: Universe,
    underlying: core.EditableMap,
    editInterface: core.MapEditInterface.ResizingView
  ) extends Base(universe, underlying, editInterface) with intf.EditableMap.ResizingView:
    def canResize(direction: intf.EditableMap.ResizingDirection, grow: Boolean): Boolean =
      editInterface.canResize(toCoreDirection(direction), grow)

    def resize(direction: intf.EditableMap.ResizingDirection, grow: Boolean): Unit =
      editInterface.resize(toCoreDirection(direction), grow)

    private def toCoreDirection(direction: intf.EditableMap.ResizingDirection): core.Direction3D = direction match
      case "north" => core.Direction3D.North
      case "east"  => core.Direction3D.East
      case "south" => core.Direction3D.South
      case "west"  => core.Direction3D.West
      case "up"    => core.Direction3D.Up
      case "down"  => core.Direction3D.Down
    end toCoreDirection

    def commit(): Unit =
      editInterface.commit()
  end Resizing
end EditableMap
