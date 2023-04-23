package com.funlabyrinthe.runnerjs

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.mazes._

import com.funlabyrinthe.graphics.{ html => ghtml }
import com.funlabyrinthe.htmlenv.ResourceLoader
import ghtml.Conversions._

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit =
    MainImpl
}

object MainImpl {
  private val resourceLoader = new ResourceLoader("./Resources/")

  private val environment = new UniverseEnvironment(
      ghtml.HTML5GraphicsSystem, resourceLoader)

  val universe = new Universe(environment)
  universe.addModule(new Mazes(universe))
  universe.initialize()

  given Universe = universe
  val mazes = Mazes.mazes
  import mazes._

  val map = new Map(Dimensions(13, 9, 2), Grass)
  for (pos <- map.minRef until map.maxRef by (2, 2)) {
    pos() = Wall
  }
  map(3, 1, 0) += EastArrow
  for (pos <- map.ref(4, 4, 0) until_+ (3, 3))
    pos() = Water
  map(1, 5, 0) += Buoy
  map(3, 7, 0) += SilverKey
  map(7, 1, 0) += SilverBlock
  map(7, 3, 0) += GoldenBlock
  map(9, 3, 0) += UpStairs
  map(9, 3, 1) += DownStairs
  map(11, 1, 1) += Treasure
  map.outside(0) = Outside
  map(11, 3, 1) += Crossroads
  map(5, 1, 0) += DirectTurnstile
  map(3, 5, 0) += IndirectTurnstile
  for (pos <- map.ref(4, 7, 0) until_+ (5, 1))
    pos() += EastArrow

  val player = new Player
  val controller = player.controller
  player.position = Some(SquareRef(map, Position(1, 1, 0)))
  player.plugins += DefaultMessagesPlugin

  var playerBusy: Boolean = false
  var keyEventCont: Option[KeyEvent => Control[Any]] = None

  val canvas = dom.document.createElement(
      "canvas").asInstanceOf[dom.HTMLCanvasElement]
  val coreCanvas = new ghtml.CanvasWrapper(canvas)

  dom.document.getElementById("canvascontainer").appendChild(canvas)

  js.timers.setInterval(100) {
    val viewSize = controller.viewSize
    canvas.width = viewSize._1.toInt
    canvas.height = viewSize._2.toInt

    val context = new DrawContext(
        coreCanvas.getGraphicsContext2D(),
        new Rectangle2D(0, 0, viewSize._1, viewSize._2))
    controller.drawView(context)
  }

  def processControlResult(controlResult: Control[Any]): Unit = {
    controlResult match {
      case Control.Done(_) =>
        playerBusy = false

      case Control.Sleep(ms, cont) =>
        js.timers.setTimeout(ms) {
          processControlResult(cont(()))
        }

      case Control.WaitForKeyEvent(cont) =>
        keyEventCont = Some(cont)
    }
  }

  dom.document.addEventListener("keydown", { (event0: dom.Event) =>
    val event = event0.asInstanceOf[dom.KeyboardEvent]
    if (keyEventCont.isDefined) {
      val cont = keyEventCont.get
      keyEventCont = None
      processControlResult(cont(event))
    } else if (!playerBusy) {
      playerBusy = true
      processControlResult(controller.onKeyEvent(event))
    }
  })
}
