package com.funlabyrinthe.graphics.jfx

import scala.language.implicitConversions

import com.funlabyrinthe.core.{ graphics, input }
import graphics._
import input._

import javafx.{ geometry => jfxg }
import javafx.scene.{ input => jfxsi }
import javafx.scene.{ paint => jfxsp }
import javafx.scene.{ text => jfxst }

object Conversions {
  implicit def corePaint2jfx(paint: Paint): jfxsp.Paint = {
    paint match {
      case Color(r, g, b, o) => new jfxsp.Color(r, g, b, o)
      case _ => ???
    }
  }

  implicit def jfxPaint2core(paint: jfxsp.Paint): Paint = {
    paint match {
      case c: jfxsp.Color =>
        Color(c.getRed, c.getGreen, c.getBlue, c.getOpacity)
      case _ => ???
    }
  }

  implicit def coreFont2jfx(font: Font): jfxst.Font = {
    val weight = jfxst.FontWeight.findByWeight(font.weight)
    val posture =
      if (font.italic) jfxst.FontPosture.ITALIC
      else jfxst.FontPosture.REGULAR
    jfxst.Font.font(font.family.head, weight, posture, font.size)
  }

  implicit def jfxFont2core(font: jfxst.Font): Font = {
    ???
  }

  implicit def coreTextAlign2jfx(align: TextAlignment): jfxst.TextAlignment = {
    align match {
      case TextAlignment.Left   => jfxst.TextAlignment.LEFT
      case TextAlignment.Center => jfxst.TextAlignment.CENTER
      case TextAlignment.Right  => jfxst.TextAlignment.RIGHT
    }
  }

  implicit def jfxTextAlign2core(align: jfxst.TextAlignment): TextAlignment = {
    align match {
      case jfxst.TextAlignment.LEFT    => TextAlignment.Left
      case jfxst.TextAlignment.CENTER  => TextAlignment.Center
      case jfxst.TextAlignment.RIGHT   => TextAlignment.Right
      case jfxst.TextAlignment.JUSTIFY => TextAlignment.Left
    }
  }

  implicit def coreTextBaseline2jfx(baseline: TextBaseline): jfxg.VPos = {
    baseline match {
      case TextBaseline.Top        => jfxg.VPos.TOP
      case TextBaseline.Middle     => jfxg.VPos.CENTER
      case TextBaseline.Alphabetic => jfxg.VPos.BASELINE
      case TextBaseline.Bottom     => jfxg.VPos.BOTTOM
    }
  }

  implicit def jfxTextBaseline2core(baseline: jfxg.VPos): TextBaseline = {
    baseline match {
      case jfxg.VPos.TOP      => TextBaseline.Top
      case jfxg.VPos.CENTER   => TextBaseline.Middle
      case jfxg.VPos.BASELINE => TextBaseline.Alphabetic
      case jfxg.VPos.BOTTOM   => TextBaseline.Bottom
    }
  }

  implicit def jfxKeyEvent2core(event: jfxsi.KeyEvent): KeyEvent = {
    new KeyEvent(event.getCode(), event.isShiftDown, event.isControlDown,
        event.isAltDown, event.isMetaDown)
  }

  implicit def jfxKeyCode2core(code: jfxsi.KeyCode): KeyCode = {
    import jfxsi.{ KeyCode => jk }
    import input.{ KeyCode => ck }

    code match {
      case jk.LEFT  | jk.KP_LEFT  => ck.Left
      case jk.UP    | jk.KP_UP    => ck.Up
      case jk.RIGHT | jk.KP_RIGHT => ck.Right
      case jk.DOWN  | jk.KP_DOWN  => ck.Down

      case jk.ENTER => ck.Enter

      case _ => ck.Other
    }
  }

  implicit def jfxMouseEvent2core(event: jfxsi.MouseEvent): MouseEvent = {
    new MouseEvent(event.getX(), event.getY(), event.getButton())
  }

  implicit def jfxMouseButton2core(button: jfxsi.MouseButton): MouseButton = {
    button match {
      case jfxsi.MouseButton.NONE      => MouseButton.None
      case jfxsi.MouseButton.PRIMARY   => MouseButton.Primary
      case jfxsi.MouseButton.MIDDLE    => MouseButton.Middle
      case jfxsi.MouseButton.SECONDARY => MouseButton.Secondary
    }
  }
}
