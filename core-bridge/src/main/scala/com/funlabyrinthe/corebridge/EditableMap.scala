package com.funlabyrinthe.corebridge

import scala.scalajs.js

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

    def getFloorRect(floor: Int): intf.Dimensions2D =
      val coreRect = editInterface.getFloorRect(floor)
      intf.Dimensions2D(coreRect.width, coreRect.height)

    def drawFloor(floor: Int): dom.ImageBitmap =
      val coreRect = editInterface.getFloorRect(floor)
      val canvas = new dom.OffscreenCanvas(coreRect.width, coreRect.height)
      val gc = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      val drawContext = new core.graphics.DrawContext(new GraphicsContextWrapper(gc), coreRect)
      editInterface.drawFloor(drawContext, floor)
      canvas.transferToImageBitmap()
    end drawFloor

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      editInterface.getDescriptionAt(x, y, floor)

    def onMouseClicked(
      x: Double,
      y: Double,
      floor: Int,
      selectedComponent: intf.EditableComponent,
    ): intf.EditUserActionResult =
      val event = new core.input.MouseEvent(x, y, core.input.MouseButton.Primary)
      val component = selectedComponent.asInstanceOf[EditableComponent]
      val coreResult = editInterface.onMouseClicked(event, floor, component.underlying)
      editUserActionResultCoreToIntf(coreResult)
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

  private def editUserActionResultCoreToIntf(coreResult: core.EditUserActionResult): intf.EditUserActionResult =
    coreResult match
      case core.EditUserActionResult.Done =>
        new intf.EditUserActionResult.Done {
          val kind = "done"
        }
      case core.EditUserActionResult.Unchanged =>
        new intf.EditUserActionResult.Unchanged {
          val kind = "unchanged"
        }
      case core.EditUserActionResult.Error(message0) =>
        new intf.EditUserActionResult.Error {
          val kind = "error"
          val message = message0
        }
      case core.EditUserActionResult.AskConfirmation(message0, onConfirm0) =>
        val onConfirm1: js.Function0[intf.EditUserActionResult] = { () =>
          editUserActionResultCoreToIntf(onConfirm0())
        }
        new intf.EditUserActionResult.AskConfirmation {
          val kind = "askConfirmation"
          val message = message0
          val onConfirm = onConfirm1
        }
      case core.EditUserActionResult.Sequence(first0, second0) =>
        val first1 = editUserActionResultCoreToIntf(first0)
        val second1: js.Function0[intf.EditUserActionResult] = { () =>
          editUserActionResultCoreToIntf(second0())
        }
        new intf.EditUserActionResult.Sequence {
          val kind = "sequence"
          val first = first1
          val second = second1
        }
  end editUserActionResultCoreToIntf
end EditableMap
