package com.funlabyrinthe.runner

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.mazes._

import com.funlabyrinthe.graphics.{ jfx => gjfx }
import com.funlabyrinthe.jvmenv.ResourceLoader
import gjfx.Conversions._

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.shape.Rectangle
import scalafx.stage.Stage

import scalafx.geometry.Insets

import scalafx.scene.control.Button
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.text.Text
import scalafx.scene.text.Font

import javafx.scene.paint.Color

import scalafx.animation.Animation
import scalafx.scene.image.{ Image, ImageView }
import scalafx.util.Duration
import scalafx.geometry.Rectangle2D

object Main extends JFXApp3 {
  override def start(): Unit =
    stage = MainImpl.initialStage
}

object MainImpl {
  private val resourceLoader = new ResourceLoader(new URLClassLoader(
      Array(
          new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Projects/Temple de l'eau/Resources/").toURI.toURL,
          new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/").toURI.toURL),
      getClass.getClassLoader))

  private val environment = new UniverseEnvironment(
      gjfx.JavaFXGraphicsSystem, resourceLoader)

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

  val globalTimer = new java.util.Timer("display", true)
  val displayTask = new java.util.TimerTask {
    override def run(): Unit = {
      scalafx.application.Platform.runLater {
        updateTickCount()

        val viewSize = controller.viewSize
        theCanvas.resize(viewSize._1, viewSize._2)

        val context = new DrawContext(
            coreCanvas.getGraphicsContext2D(),
            new graphics.Rectangle2D(0, 0, viewSize._1, viewSize._2))
        controller.drawView(context)
      }
    }
  }
  globalTimer.scheduleAtFixedRate(displayTask, 500, 100)

  lazy val initialStage: JFXApp3.PrimaryStage = new JFXApp3.PrimaryStage {
    title = "FunLabyrinthe"
    width = 600
    height = 500
    scene = new Scene {
      fill = Color.LIGHTGREEN
      content = welcomeRoot
    }

    def processControlResult(controlResult: Control[Any]): Unit = {
      controlResult match {
        case Control.Done(_) =>
          playerBusy = false

        case Control.Sleep(ms, cont) =>
          globalTimer.schedule(new java.util.TimerTask {
            override def run(): Unit = {
              scalafx.application.Platform.runLater {
                updateTickCount()
                processControlResult(cont(()))
              }
            }
          }, ms)

        case Control.WaitForKeyEvent(cont) =>
          keyEventCont = Some(cont)
      }
    }

    theCanvas.onKeyPressed = { (event: scalafx.event.Event) =>
      event.delegate match {
        case keyEvent: javafx.scene.input.KeyEvent =>
          updateTickCount()
          if (keyEventCont.isDefined) {
            val cont = keyEventCont.get
            keyEventCont = None
            processControlResult(cont(keyEvent))
          } else if (!playerBusy) {
            playerBusy = true
            processControlResult(controller.onKeyEvent(keyEvent))
          }
          keyEvent.consume()

        case _ => ()
      }
    }
    theCanvas.requestFocus()
  }

  lazy val welcomeRoot = {
    new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      spacing = 10
      margin = Insets(50, 0, 0, 50)
      children = List(
        new Text {
          text = "Welcome to FunLabyrinthe"
          font = new Font("Verdana", 20)
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "Start Game"
          onAction = { () =>
            println("Let's go!")
          }
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "Exit"
          cancelButton = true
          onAction = { () =>
            initialStage.close()
          }
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "About"
          onAction = { () =>
            println("""
                |FunLabyrinthe 6.0
                |Author: Sébastien Doeraene
                |Web site: http://www.funlabyrinthe.com/
                """.trim().stripMargin)
          }
        },
        theCanvas)
    }
  }

  lazy val theCanvas = {
    val canvas = new scalafx.scene.canvas.Canvas(15*30, 11*30)
    canvas
  }

  lazy val coreCanvas = new gjfx.CanvasWrapper(theCanvas)
}
