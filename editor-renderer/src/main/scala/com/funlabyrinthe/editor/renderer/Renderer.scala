package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import com.raquo.laminar.api.L.{*, given}

object Renderer:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.body, new Renderer().appElement)
end Renderer

class Renderer:
  import Renderer.*

  val universeFileVar: Var[Option[UniverseFile]] = Var(None)
  val universeFileSignal = universeFileVar.signal.distinct

  val appElement: Element =
    div(
      child <-- universeFileSignal.map { universeFile =>
        universeFile match
          case None               => new ProjectSelector(universeFileVar.writer).topElement
          case Some(universeFile) => new UniverseEditor(universeFile).topElement
      }
    )
  end appElement
end Renderer
