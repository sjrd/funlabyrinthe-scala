package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import com.raquo.laminar.api.L.{*, given}

import com.funlabyrinthe.core.*
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.graphics.html as ghtml
import com.funlabyrinthe.mazes.*

object Renderer:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.body, new Renderer().appElement)
end Renderer

class Renderer:
  import Renderer.*

  val appElement: Element =
    val universe = makeUniverse()
    val universeFile = new UniverseFile(universe)
    val universeEditor = new UniverseEditor(universeFile)
    universeEditor.topElement
  end appElement

  private def makeUniverse(): Universe =
    val resourceLoader = new ResourceLoader("./Resources/")

    val environment = new UniverseEnvironment(
        ghtml.HTML5GraphicsSystem, resourceLoader)

    val universe = new Universe(environment)
    universe.addModule(new Mazes(universe))
    universe.initialize()

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

    val player = new Player(using ComponentInit(universe, ComponentID("player"), mazes))
    val controller = player.controller
    player.position = Some(SquareRef(map, Position(1, 1, 0)))
    player.plugins += DefaultMessagesPlugin

    val boat1 = BoatCreator.createNewComponent()
    boat1.position = Some(map.ref(5, 4, 0))

    universe
  end makeUniverse
end Renderer
