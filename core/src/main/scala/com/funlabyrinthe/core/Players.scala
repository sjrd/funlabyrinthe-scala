package com.funlabyrinthe.core

import scala.language.implicitConversions

import scala.collection.mutable
import scalafx.scene.input.KeyEvent

trait Players { universe: Universe =>
  import universe._

  trait StaticPlayerPlugin {
    val player: Player
  }

  object StaticPlayerPlugin {
    implicit def toPlayer(plugin: StaticPlayerPlugin) = plugin.player
  }

  trait PlayerController {
    def viewSize: (Double, Double)

    def drawView(context: DrawContext) {
      graphics.fillWithOpaqueBackground(context)
    }

    def onKeyEvent(keyEvent: KeyEvent): Unit = ()
  }

  object PlayerController {
    object Dummy extends PlayerController {
      def viewSize = (270.0, 270.0) // the everlasting default view size
    }
  }

  class Player extends NamedComponent {
    var controller: PlayerController = PlayerController.Dummy

    def viewSize = controller.viewSize
    def drawView(context: DrawContext) = controller.drawView(context)
    def onKeyEvent(keyEvent: KeyEvent) = controller.onKeyEvent(keyEvent)
  }
}
