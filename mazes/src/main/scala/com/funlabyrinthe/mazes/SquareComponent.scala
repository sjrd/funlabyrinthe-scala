package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class SquareComponent(using ComponentInit)
    extends Component with MapEditingHooksComponent:

  var painter: Painter = universe.EmptyPainter

  final def drawTo(context: DrawSquareContext): Unit =
    doDraw(context)
    drawEditVisualTag(context)

  protected def doDraw(context: DrawSquareContext): Unit =
    painter.drawTo(context)

  override def drawIcon(context: DrawContext): Unit =
    drawTo(DrawSquareContext(context, None, DrawPurpose.Icon(this)))

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit

  private[SquareComponent] final def editMapAddInternal(pos: SquareRef)(using EditingServices): Unit =
    editMapAdd(pos)

  private[SquareComponent] final def editMapRemoveInternal(pos: SquareRef)(using EditingServices): Unit =
    editMapRemove(pos)

  override protected def onEditMouseClickOnMap(event: MouseEvent, pos: SquareRef)(
      using EditingServices): Unit =

    if event.button != MouseButton.Primary then
      super.onEditMouseClickOnMap(event, pos)
    else if pos.isOutside && !this.isInstanceOf[Field] then
      EditingServices.error("Only fields can be placed outside the boundaries of the map.")
    else
      val redirectedPos = pos().field.editMapRedirectInternal(pos, this)
      if redirectedPos != pos then
        return onEditMouseClickOnMap(event, redirectedPos)

      def removeObstacle(): Unit =
        if pos().obstacle != this then
          pos().obstacle.editMapRemoveInternal(pos)

      def removeTool(): Unit =
        if pos().tool != this then
          pos().tool.editMapRemoveInternal(pos)

      def removeEffect(): Unit =
        if pos().effect != this then
          pos().effect.editMapRemoveInternal(pos)

      def removeField(): Unit =
        if pos().field != this then
          pos().field.editMapRemoveInternal(pos)

      // Removals
      this match
        case component: Field =>
          if pos.isOutside then
            removeField()
          else
            removeObstacle()
            removeTool()
            removeEffect()
            removeField()
        case component: Effect =>
          removeObstacle()
          removeTool()
          removeEffect()
        case component: Tool =>
          removeObstacle()
          removeTool()
        case component: Obstacle =>
          removeObstacle()

      if !pos().parts.contains(this) then
        this.editMapAddInternal(pos)
  end onEditMouseClickOnMap
end SquareComponent
