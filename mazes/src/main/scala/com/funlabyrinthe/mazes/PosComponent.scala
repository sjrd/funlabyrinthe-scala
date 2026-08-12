package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.scene.*

abstract class PosComponent(using ComponentInit)
    extends Component with MapEditingHooksComponent:

  var painter: Painter = universe.EmptyPainter

  private var _zIndex: Int = 0
  private var _position: Option[SquareRef] = None

  Mazes.registerPosComponent(this)

  override protected def onDestroyed(): Unit =
    super.onDestroyed()
    Mazes.unregisterPosComponent(this)

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

  final def present(context: PresentSquareContext): Batch[SceneNode] =
    doPresent(context) ++ presentEditVisualTag()

  protected def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(painter)

  override def presentIcon(): Batch[SceneNode] = {
    if isTemplate && templateIcon.items.nonEmpty then
      super.presentIcon()
    else
      val base = present(PresentSquareContext(tickCount = 0L, None, DrawPurpose.Icon(this), Size(30, 30)))
      if isTemplate then
        base ++ universe.CreatorIconPainter.present()
      else
        base
  }

  protected def positionChanged(oldPos: Option[SquareRef], newPos: Option[SquareRef]): Unit = ()

  protected def hookEntering(context: EnteringContext): Unit =
    context.hooked = false

  protected def hookPushing(context: EnteringContext): Unit =
    context.hooked = false

  protected def hookEntered(context: EnteredContext): Unit =
    context.hooked = false

  protected def hookExecute(context: ExecuteContext): Unit =
    context.hooked = false

  protected def hookExiting(context: ExitingContext): Unit =
    context.hooked = false

  protected def hookExited(context: ExitedContext): Unit =
    context.hooked = false

  final def entering(context: EnteringContext): Unit = {
    context.hooked = true
    hookEntering(context)
  }

  final def pushing(context: EnteringContext): Unit = {
    context.hooked = true
    hookPushing(context)
  }

  final def entered(context: EnteredContext): Unit = {
    context.hooked = true
    hookEntered(context)
  }

  final def execute(context: ExecuteContext): Unit = {
    context.hooked = true
    hookExecute(context)
  }

  final def exiting(context: ExitingContext): Unit = {
    context.hooked = true
    hookExiting(context)
  }

  final def exited(context: ExitedContext): Unit = {
    context.hooked = true
    hookExited(context)
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
