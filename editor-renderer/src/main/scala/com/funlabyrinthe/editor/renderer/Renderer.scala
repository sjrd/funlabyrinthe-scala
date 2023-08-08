package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

object Renderer:
  @js.native
  @JSGlobal
  object versions extends js.Object {
    def node(): String = js.native
    def chrome(): String = js.native
    def electron(): String = js.native
  }

  def main(args: Array[String]): Unit =
    val infoP = dom.document.getElementById("info").asInstanceOf[HTMLElement]
    infoP.innerText = s"This app is using Chrome (v${versions.chrome()}), Node.js (v${versions.node()}), and Electron (v${versions.electron()})"
end Renderer
