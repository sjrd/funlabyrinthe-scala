package com.funlabyrinthe.runner

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.mazes._

import scala.util.continuations._

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp
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

object Main extends JFXApp {
  class MyUniverse extends Universe with MazeUniverse {
    override lazy val classLoader = new URLClassLoader(
        Array(
            new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Projects/Temple de l'eau/Resources/").toURI.toURL,
            new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/").toURI.toURL),
        getClass.getClassLoader)
  }

  implicit val universe: MyUniverse = new MyUniverse
  universe.initialize()
  import universe._
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
  var keyEventCont: Option[KeyEvent => ControlResult] = None

  val globalTimer = new java.util.Timer("display", true)
  val displayTask = new java.util.TimerTask {
    override def run() {
      scalafx.application.Platform.runLater {
        val viewSize = controller.viewSize
        theCanvas.resize(viewSize._1, viewSize._2)

        val context = new DrawContext(
            theCanvas.graphicsContext2D,
            new Rectangle2D(0, 0, viewSize._1, viewSize._2))
        controller.drawView(context)
      }
    }
  }
  globalTimer.scheduleAtFixedRate(displayTask, 500, 100)

  stage = new JFXApp.PrimaryStage {
    title = "FunLabyrinthe"
    width = 600
    height = 500
    scene = new Scene {
      fill = Color.LIGHTGREEN
      content = welcomeRoot
    }

    def processControlResult(controlResult: ControlResult): Unit = {
      controlResult match {
        case ControlResult.Done =>
          playerBusy = false

        case ControlResult.Sleep(ms, cont) =>
          globalTimer.schedule(new java.util.TimerTask {
            override def run() {
              scalafx.application.Platform.runLater {
                processControlResult(cont())
              }
            }
          }, ms)

        case ControlResult.WaitForKeyEvent(cont) =>
          keyEventCont = Some(cont)
      }
    }

    theCanvas.onKeyPressed = { (event: scalafx.event.Event) =>
      event.delegate match {
        case keyEvent: javafx.scene.input.KeyEvent =>
          if (keyEventCont.isDefined) {
            val cont = keyEventCont.get
            keyEventCont = None
            processControlResult(cont(keyEvent))
          } else if (!playerBusy) {
            playerBusy = true
            processControlResult(reset {
              controller.onKeyEvent(keyEvent)
              ControlResult.Done
            })
          }

          case _ => ()
      }
    }
    theCanvas.requestFocus
  }

  lazy val welcomeRoot = {
    new VBox {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
      spacing = 10
      margin = Insets(50, 0, 0, 50)
      content = List(
        new Text {
          text = "Welcome to FunLabyrinthe"
          font = new Font("Verdana", 20)
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "Start Game"
          onAction = {
            println("Let's go!")
          }
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "Exit"
          cancelButton = true
          onAction = {
            stage.close
          }
        },
        new Button {
          maxWidth = 200
          maxHeight = 150
          text = "About"
          onAction = {
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
    val canvas = new Canvas(15*30, 11*30)
    canvas
  }
}
