package com.funlabyrinthe.editor.renderer

import org.scalajs.dom
import org.scalajs.dom.ImageBitmap
import org.scalajs.dom.MutationObserver

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

  def hackElemInsideShadowRoot(query: String)(hack: dom.HTMLElement => Unit): Modifier[HtmlElement] =
    onMountCallback { ctx =>
      val shadowRoot = ctx.thisNode.ref.shadowRoot
      new MutationObserver({ (records, observer) =>
        for
          record <- records
          if record.`type` == "childList"
        do
          record.target match
            case target: dom.HTMLElement =>
              target.querySelector(query) match
                case hackTarget: dom.HTMLElement =>
                  observer.disconnect()
                  hack(hackTarget)
                case _ =>
                  ()
            case _ =>
              ()
        end for
      }).observe(shadowRoot, new {
        subtree = true
        childList = true
      })
    }
  end hackElemInsideShadowRoot

end LaminarUtils
