package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import com.raquo.laminar.api.L.{*, given}

object Renderer:
  @js.native
  @JSGlobal
  object versions extends js.Object {
    def node(): String = js.native
    def chrome(): String = js.native
    def electron(): String = js.native
  }

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.body, new Renderer().appElement)
end Renderer

class Renderer:
  import Renderer.*

  val appElement: Element =
    import versions.*
    div(
      h1("Hello from Electron renderer with Laminar!"),
      p("👋"),
      p(s"This app is using Chrome (v${chrome()}), Node.js (v${node()}), and Electron (v${electron()})"),
    )
end Renderer
