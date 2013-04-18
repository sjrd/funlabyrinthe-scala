package com.funlabyrinthe.runner

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.mazes._

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
  import universe._
  import mazes._

  object Wall extends Field()(universe) {
    painter += "Fields/Wall"

    override def entering(context: MoveContext) {
      context.cancelled = true
    }
  }

  val map = new Map(Dimensions(13, 9, 1), Grass)
  for (pos <- map.minRef until map.maxRef by (2, 2)) {
    pos() = Wall
  }

  val player = new Player
  val controller = player.controller
  player.position = Some(SquareRef(map, Position(1, 1, 0)))

  val displayTimer = new java.util.Timer("display", true)
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
  displayTimer.scheduleAtFixedRate(displayTask, 500, 100)

  stage = new JFXApp.PrimaryStage {
    title = "FunLabyrinthe"
    width = 600
    height = 500
    scene = new Scene {
      fill = Color.LIGHTGREEN
      content = welcomeRoot
    }

    theCanvas.onKeyPressed = { (event: scalafx.event.Event) =>
      event.delegate match {
        case keyEvent: javafx.scene.input.KeyEvent =>
          controller.onKeyEvent(keyEvent)
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
          defaultButton = true
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
