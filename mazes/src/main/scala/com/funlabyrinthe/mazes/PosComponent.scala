package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

abstract class PosComponent(using ComponentInit) extends Component derives Reflector:
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
    drawTo(DrawSquareContext(context))

  protected def positionChanged(oldPos: Option[SquareRef], newPos: Option[SquareRef]): Unit = ()

  protected def hookEntering(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  protected def hookExiting(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  protected def hookEntered(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  protected def hookExited(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  protected def hookExecute(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  protected def hookPushing(context: MoveContext): Control[Unit] = control {
    context.hooked = false
  }

  final def entering(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookEntering(context)
  }

  final def exiting(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookExiting(context)
  }

  final def entered(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookEntered(context)
  }

  final def exited(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookExited(context)
  }

  final def execute(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookExecute(context)
  }

  final def pushing(context: MoveContext): Control[Unit] = control {
    context.hooked = true
    hookPushing(context)
  }

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty
end PosComponent
