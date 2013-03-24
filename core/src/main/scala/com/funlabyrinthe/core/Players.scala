package com.funlabyrinthe.core

import scala.language.implicitConversions

import scala.collection.mutable

trait Players { universe: Universe =>
  trait StaticPlayerPlugin {
    val player: Player
  }

  object StaticPlayerPlugin {
    implicit def toPlayer(plugin: StaticPlayerPlugin) = plugin.player
  }

  class Player extends VisualComponent {
  }
}
