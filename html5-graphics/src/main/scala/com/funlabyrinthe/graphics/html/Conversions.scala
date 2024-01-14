package com.funlabyrinthe.graphics.html

import scala.scalajs.js

import com.funlabyrinthe.core.graphics.*

object Conversions {
  private val ColorRGB = """rgb([\d.]+,[\d.]+,[\d.]+)""".r
  private val ColorRGBA = """rgba([\d.]+,[\d.]+,[\d.]+,[\d.]+)""".r

  def coreColorComponent2html(component: Double): Double =
    js.Math.round(component * 255)

  def htmlColorComponent2core(component: Double): Double =
    component / 255

  def corePaint2html(paint: Paint): String = {
    import Conversions.{ coreColorComponent2html => coreCC2html }

    paint match {
      case Color(r, g, b, o) =>
        s"rgba(${coreCC2html(r)},${coreCC2html(g)},${coreCC2html(b)},$o)"
      case _ => ???
    }
  }

  def htmlPaint2core(paint: String): Paint = {
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

  def coreFont2html(font: Font): String = {
    val style = if (font.italic) "italic " else ""
    s"$style${font.weight} ${font.size}px ${font.family.head}"
  }

  def htmlFont2core(font: String): Font = {
    ???
  }

  def coreTextAlign2html(align: TextAlignment): String = {
    align match {
      case TextAlignment.Left   => "left"
      case TextAlignment.Center => "center"
      case TextAlignment.Right  => "right"
    }
  }

  def htmlTextAlign2core(align: String): TextAlignment = {
    (align: String) match {
      case "left" | "start" => TextAlignment.Left
      case "center"         => TextAlignment.Center
      case "right" | "end"  => TextAlignment.Right
    }
  }

  def coreTextBaseline2html(baseline: TextBaseline): String = {
    baseline match {
      case TextBaseline.Top        => "top"
      case TextBaseline.Middle     => "middle"
      case TextBaseline.Alphabetic => "alphabetic"
      case TextBaseline.Bottom     => "bottom"
    }
  }

  def htmlTextBaseline2core(baseline: String): TextBaseline = {
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
}
