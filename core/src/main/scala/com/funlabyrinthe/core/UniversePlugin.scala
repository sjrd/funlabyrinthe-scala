package com.funlabyrinthe.core

import scala.language.implicitConversions

import scala.collection.mutable

trait UniversePlugin {
  val universe: Universe
  import universe._

  type StaticPlayerPlugin <: universe.StaticPlayerPlugin

  protected def createStaticPlayerPlugin(
      player: Player): StaticPlayerPlugin

  private val _playersPlugins =
    new mutable.WeakHashMap[Player, StaticPlayerPlugin]

  implicit def playerToMyPlugin(player: Player): StaticPlayerPlugin = {
    _playersPlugins.getOrElseUpdate(player, createStaticPlayerPlugin(player))
  }
}
