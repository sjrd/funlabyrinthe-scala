package com.funlabyrinthe.editor.renderer

import org.scalajs.dom
import org.scalajs.dom.ImageBitmap

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

object LaminarUtils:
  type CanvasElement = ReactiveElement[dom.HTMLCanvasElement]

  extension (value: Double)
    def px: String = s"${value}px"

  def drawFromSignal(source: Signal[ImageBitmap]): Modifier[CanvasElement] =
    inContext { canvasElem =>
      source --> { image =>
        val ctx = canvasElem.ref.getContext("bitmaprenderer").asInstanceOf[ImageBitmapRenderingContext]
        ctx.transferFromImageBitmap(image)
      }
    }
  end drawFromSignal

end LaminarUtils
