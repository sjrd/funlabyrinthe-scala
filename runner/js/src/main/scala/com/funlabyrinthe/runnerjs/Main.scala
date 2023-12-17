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
  private val resourceLoader = new ResourceLoader("./Resources/", () => ())

  private val environment = new UniverseEnvironment(
      ghtml.HTML5GraphicsSystem, resourceLoader)

  val universe = new Universe(environment)
  universe.addModule(new Mazes(universe))
  universe.initialize()

  val corePlayer = universe.createSoloPlayer()

  def controller = corePlayer.controller

  given Universe = universe
  val mazes = Mazes.mazes
  import mazes._

  val map = mazes.MapCreator.createNewComponent()
  map.resize(Dimensions(13, 9, 3), Grass)
  for (pos <- map.minRef until map.maxRef by (2, 2)) {
    pos() = Wall
  }
  map(3, 1, 0) += EastArrow
  for (pos <- map.ref(4, 4, 0) until_+ (3, 3) if pos != map.ref(5, 5, 0))
    pos() = Water
  map(1, 3, 0) += Hole
  map(0, 1, 0) += Plank
  map(1, 5, 0) += Buoy
  map(3, 7, 0) += SilverKey
  map(7, 1, 0) += SilverBlock
  map(7, 3, 0) += GoldenBlock
  map(9, 3, 0) += UpStairs
  map(9, 3, 1) += DownStairs
  for z <- 0 to 2 do
    map(9, 5, z) += Lift
  for z <- 1 to 2 do
    map(7, 5, z) += Lift
  for z <- 0 to 1 do
    map(5, 5, z) += Lift
  map(3, 5, 1) += Lift
  map(11, 1, 1) += Treasure
  map.outside(0) = Outside
  map(11, 3, 1) += Crossroads
  map(5, 1, 0) += DirectTurnstile
  map(3, 5, 0) += IndirectTurnstile
  for (pos <- map.ref(4, 7, 0) until_+ (5, 1))
    pos() += EastArrow

  val player = corePlayer.reified[Player]
  player.position = Some(SquareRef(map, Position(1, 1, 0)))

  val boat1 = BoatCreator.createNewComponent()
  boat1.position = Some(map.ref(5, 4, 0))

  corePlayer.autoDetectController()

  var playerBusy: Boolean = false
  var keyEventCont: Option[KeyEvent => Control[Any]] = None

  var lastTickCountUpdate: Long = System.nanoTime() / 1000000L

  def updateTickCount(): Unit =
    val refTickCount: Long = System.nanoTime() / 1000000L
    universe.advanceTickCount(refTickCount - lastTickCountUpdate)
    lastTickCountUpdate = refTickCount
  end updateTickCount

  val canvas = dom.document.createElement(
      "canvas").asInstanceOf[dom.HTMLCanvasElement]
  val coreCanvas = new ghtml.CanvasWrapper(canvas)

  dom.document.getElementById("canvascontainer").appendChild(canvas)

  js.timers.setInterval(100) {
    updateTickCount()

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
          updateTickCount()
          processControlResult(cont(()))
        }

      case Control.WaitForKeyEvent(cont) =>
        keyEventCont = Some(cont)
    }
  }

  dom.document.addEventListener("keydown", { (event0: dom.Event) =>
    updateTickCount()
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
