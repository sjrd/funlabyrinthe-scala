package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class PosComponent(using ComponentInit)
    extends Component with MapEditingHooksComponent
    derives Reflector:

  var painter: Painter = universe.EmptyPainter

  private var _zIndex: Int = 0
  private var _position: Option[SquareRef] = None

  Mazes.registerPosComponent(this)

  def zIndex: Int = _zIndex

  def zIndex_=(value: Int): Unit =
    Mazes.changingPosComponentZIndex(this) {
      _zIndex = value
    }
  end zIndex_=

  @noinspect
  def position: Option[SquareRef] = _position

  def position_=(value: Option[SquareRef]): Unit =
    val oldPos = _position
    _position = value

    if universe.isLoaded then
      positionChanged(oldPos, value)
  end position_=

  override def reflect() = autoReflect[PosComponent]

  final def drawTo(context: DrawSquareContext): Unit =
    doDraw(context)
    drawEditVisualTag(context)

  protected def doDraw(context: DrawSquareContext): Unit =
    painter.drawTo(context)

  override def drawIcon(context: DrawContext): Unit =
    drawTo(DrawSquareContext(context, None, DrawPurpose.Icon(this)))

  protected def positionChanged(oldPos: Option[SquareRef], newPos: Option[SquareRef]): Unit = ()

  protected def hookEntering(context: MoveContext): Unit = {
    context.hooked = false
  }

  protected def hookExiting(context: MoveContext): Unit = {
    context.hooked = false
  }

  protected def hookEntered(context: MoveContext): Unit = {
    context.hooked = false
  }

  protected def hookExited(context: MoveContext): Unit = {
    context.hooked = false
  }

  protected def hookExecute(context: MoveContext): Unit = {
    context.hooked = false
  }

  protected def hookPushing(context: MoveContext): Unit = {
    context.hooked = false
  }

  final def entering(context: MoveContext): Unit = {
    context.hooked = true
    hookEntering(context)
  }

  final def exiting(context: MoveContext): Unit = {
    context.hooked = true
    hookExiting(context)
  }

  final def entered(context: MoveContext): Unit = {
    context.hooked = true
    hookEntered(context)
  }

  final def exited(context: MoveContext): Unit = {
    context.hooked = true
    hookExited(context)
  }

  final def execute(context: MoveContext): Unit = {
    context.hooked = true
    hookExecute(context)
  }

  final def pushing(context: MoveContext): Unit = {
    context.hooked = true
    hookPushing(context)
  }

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty

  override protected def onEditMouseClickOnMap(event: MouseEvent, pos: SquareRef)(
      using EditingServices): Unit =
    if event.button == MouseButton.Primary then
      if !position.contains(pos) then
        position = Some(pos)
        EditingServices.markModified()
    else
      super.onEditMouseClickOnMap(event, pos)
  end onEditMouseClickOnMap
end PosComponent
