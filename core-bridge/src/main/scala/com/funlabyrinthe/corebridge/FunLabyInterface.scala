package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.graphics.html.HTML5GraphicsSystem
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.mazes.{Mazes, Player}

@JSExportTopLevel("FunLabyInterface")
object FunLabyInterface extends intf.FunLabyInterface:
  def createNewUniverse(): js.Promise[Universe] =
    val environment = createEnvironment()
    val coreUniverse = new core.Universe(environment)
    coreUniverse.addModule(new Mazes(coreUniverse))
    coreUniverse.initialize()

    locally {
      import com.funlabyrinthe.mazes.*
      import com.funlabyrinthe.mazes.Mazes.mazes

      given core.Universe = coreUniverse

      new Player(using core.ComponentInit(coreUniverse, core.ComponentID("player"), coreUniverse.module[Mazes]))

      val mainMap = mazes.MapCreator.createNewComponent()
      mainMap.resize(core.Dimensions(13, 9, 1), mazes.Grass)
      for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
        pos() = mazes.Wall
      }

      val player = coreUniverse.getComponentByID("player").asInstanceOf[Player]
      player.position = Some( core.SquareRef(mainMap,  core.Position(1, 1, 0)))
    }

    val intfUniverse = new Universe(coreUniverse)
    js.Promise.resolve(intfUniverse)
  end createNewUniverse

  def loadUniverse(pickleString: String): js.Promise[Universe] =
    val environment = createEnvironment()
    val coreUniverse = new core.Universe(environment)
    coreUniverse.addModule(new Mazes(coreUniverse))
    coreUniverse.initialize()

    val intfUniverse = new Universe(coreUniverse)
    intfUniverse.load(pickleString)
    js.Promise.resolve(intfUniverse)
  end loadUniverse

  private def createEnvironment(): core.UniverseEnvironment =
    val resourceLoader = new ResourceLoader("./Resources/")
    new core.UniverseEnvironment(HTML5GraphicsSystem, resourceLoader)
  end createEnvironment
end FunLabyInterface
