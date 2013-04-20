package com.funlabyrinthe.editor

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
  stage = new JFXApp.PrimaryStage {
    title = "FunLabyrinthe editor"
    width = 600
    height = 500
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
          text = "Welcome to FunLabyrinthe editor"
          font = new Font("Verdana", 20)
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
        })
    }
  }
}
