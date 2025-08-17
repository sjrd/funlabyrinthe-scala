package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*

/** Base trait for components that want to react when they are selected in the
 *  component palette when the user clicks on a [[Map]].
 */
trait MapEditingHooksComponent extends Component:
  /** Called when this component is selected in the component palette and
   *  the user clicks on a [[Map]].
   *
   *  The default implementation does nothing.
   */
  protected def onEditMouseClickOnMap(event: MouseEvent, pos: SquareRef)(
      using EditingServices): Unit =
    ()
  end onEditMouseClickOnMap
end MapEditingHooksComponent

private[mazes] object MapEditingHooksComponent:
  private[mazes] def onEditMouseClickOnMap(
      component: MapEditingHooksComponent, event: MouseEvent, pos: SquareRef)(
      using EditingServices): Unit =
    component.onEditMouseClickOnMap(event, pos)
  end onEditMouseClickOnMap
end MapEditingHooksComponent
