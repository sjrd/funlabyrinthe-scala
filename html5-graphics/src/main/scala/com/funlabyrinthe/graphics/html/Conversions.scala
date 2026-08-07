package com.funlabyrinthe.graphics.html

import scala.scalajs.js

import org.scalajs.dom

object Conversions {
  def coreColorComponent2html(component: Double): Double =
    js.Math.round(component * 255)

  extension (canvas: dom.OffscreenCanvas) def asHTMLElement: dom.HTMLElement =
    canvas.asInstanceOf[dom.HTMLElement]
}
