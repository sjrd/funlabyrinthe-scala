package com.funlabyrinthe.graphics.html

import scala.language.implicitConversions

import scala.scalajs.js

import com.funlabyrinthe.core.{ graphics, input }
import graphics._
import input._

object Conversions {
  private val ColorRGB = """rgb([\d.]+,[\d.]+,[\d.]+)""".r
  private val ColorRGBA = """rgba([\d.]+,[\d.]+,[\d.]+,[\d.]+)""".r

  def coreColorComponent2html(component: Double): js.Number =
    js.Math.round(component * 255)

  def htmlColorComponent2core(component: js.Number): Double =
    component / 255

  def corePaint2html(paint: Paint): js.String = {
    import Conversions.{ coreColorComponent2html => coreCC2html }

    paint match {
      case Color(r, g, b, o) =>
        s"rgba(${coreCC2html(r)},${coreCC2html(g)},${coreCC2html(b)},$o)"
      case _ => ???
    }
  }

  def htmlPaint2core(paint: js.String): Paint = {
    import Conversions.{ htmlColorComponent2core => htmlCC2core }

    (paint: String) match {
      case ColorRGBA(r, g, b, a) =>
        Color(htmlCC2core(r.toDouble), htmlCC2core(g.toDouble),
            htmlCC2core(b.toDouble), a.toDouble)

      case ColorRGB(r, g, b) =>
        Color(htmlCC2core(r.toDouble), htmlCC2core(g.toDouble),
            htmlCC2core(b.toDouble))

      case _ => ???
    }
  }

  def coreFont2html(font: Font): js.String = {
    val style = if (font.italic) "italic " else ""
    s"$style${font.weight} ${font.size}px ${font.family.head}"
  }

  def htmlFont2core(font: js.String): Font = {
    ???
  }

  def coreTextAlign2html(align: TextAlignment): js.String = {
    align match {
      case TextAlignment.Left   => "left"
      case TextAlignment.Center => "center"
      case TextAlignment.Right  => "right"
    }
  }

  def htmlTextAlign2core(align: js.String): TextAlignment = {
    (align: String) match {
      case "left" | "start" => TextAlignment.Left
      case "center"         => TextAlignment.Center
      case "right" | "end"  => TextAlignment.Right
    }
  }

  def coreTextBaseline2html(baseline: TextBaseline): js.String = {
    baseline match {
      case TextBaseline.Top        => "top"
      case TextBaseline.Middle     => "middle"
      case TextBaseline.Alphabetic => "alphabetic"
      case TextBaseline.Bottom     => "bottom"
    }
  }

  def htmlTextBaseline2core(baseline: js.String): TextBaseline = {
    (baseline: String) match {
      case "top"        => TextBaseline.Top
      case "middle"     => TextBaseline.Middle
      case "alphabetic" => TextBaseline.Alphabetic
      case "bottom"     => TextBaseline.Bottom

      // approximations
      case "hanging"     => TextBaseline.Top
      case "ideographic" => TextBaseline.Alphabetic
    }
  }

  implicit def htmlKeyEvent2core(event: jsdefs.KeyboardEvent): KeyEvent = {
    new KeyEvent(htmlKeyCode2core(event.keyCode), event.shiftKey,
        event.ctrlKey, event.altKey, event.metaKey)
  }

  def htmlKeyCode2core(code: js.Number): KeyCode = {
    import input.{ KeyCode => ck }

    code.toInt match {
      case 37 => ck.Left
      case 38 => ck.Up
      case 39 => ck.Right
      case 40 => ck.Down

      case 13 => ck.Enter

      case _ => ck.Other
    }
  }

  implicit def htmlMouseEvent2core(event: jsdefs.MouseEvent): MouseEvent = {
    new MouseEvent(event.clientX, event.clientY,
        htmlMouseButton2core(event.button))
  }

  def htmlMouseButton2core(button: js.Number): MouseButton = {
    button.toInt match {
      case 1 => MouseButton.Primary
      case 2 => MouseButton.Middle
      case 3 => MouseButton.Secondary
      case _ => MouseButton.None
    }
  }
}
