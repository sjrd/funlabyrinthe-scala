package com.funlabyrinthe.runner

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

object Main extends JFXApp {
  stage = new JFXApp.PrimaryStage {
    title = "FunLabyrinthe"
    width = 600
    height = 450
    scene = new Scene {
      fill = Color.LIGHTGREEN
      content = welcomeRoot
    }
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
            println("""|
                |FunLabyrinthe 6.0
                |Author: Sébastien Doeraene
                |Web site: http://www.funlabyrinthe.com/
                |""".trim().stripMargin)
          }
        })
    }
  }
}
