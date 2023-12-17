package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.core.pickling.*

import com.funlabyrinthe.graphics.html.HTML5GraphicsSystem
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.mazes.{Mazes, Player}

@JSExportTopLevel("FunLabyInterface")
object FunLabyInterface extends intf.FunLabyInterface:
  def createNewUniverse(
    moduleClassNames: js.Array[String],
    globalEventHandler: intf.GlobalEventHandler,
  ): js.Promise[Universe] =
    val coreUniverse = initializeUniverse(moduleClassNames, globalEventHandler)

    coreUniverse.createSoloPlayer()

    locally {
      import com.funlabyrinthe.mazes.*
      import com.funlabyrinthe.mazes.Mazes.mazes

      given core.Universe = coreUniverse

      val mainMap = mazes.MapCreator.createNewComponent()
      mainMap.resize(core.Dimensions(13, 9, 1), mazes.Grass)
      for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
        pos() = mazes.Wall
      }
    }

    val intfUniverse = new Universe(coreUniverse)
    js.Promise.resolve(intfUniverse)
  end createNewUniverse

  def loadUniverse(
    moduleClassNames: js.Array[String],
    pickleString: String,
    globalEventHandler: intf.GlobalEventHandler,
  ): js.Promise[Universe] =
    val coreUniverse = initializeUniverse(moduleClassNames, globalEventHandler)

    val intfUniverse = new Universe(coreUniverse)
    intfUniverse.load(pickleString)
    js.Promise.resolve(intfUniverse)
  end loadUniverse

  private def initializeUniverse(
    moduleClassNames: js.Array[String],
    globalEventHandler: intf.GlobalEventHandler,
  ): core.Universe =
    val environment = createEnvironment(globalEventHandler)
    val coreUniverse = new core.Universe(environment)
    loadModules(coreUniverse, moduleClassNames)
    coreUniverse.initialize()
    coreUniverse
  end initializeUniverse

  private def createEnvironment(globalEventHandler: intf.GlobalEventHandler): core.UniverseEnvironment =
    val onResourceLoaded: () => Unit =
      globalEventHandler.onResourceLoaded.fold(() => ())(f => f)
    val resourceLoader = new ResourceLoader("./Resources/", onResourceLoaded)
    new core.UniverseEnvironment(HTML5GraphicsSystem, resourceLoader)
  end createEnvironment

  private def loadModules(coreUniverse: core.Universe, moduleClassNames: js.Array[String]): Unit =
    import org.portablescala.reflect.Reflect

    for moduleClassName <- moduleClassNames do
      for
        cls <- Reflect.lookupInstantiatableClass(moduleClassName)
        if classOf[core.Module].isAssignableFrom(cls.runtimeClass)
        ctor <- cls.getConstructor(classOf[core.Universe])
      do
        coreUniverse.addModule(ctor.newInstance(coreUniverse).asInstanceOf[core.Module])
  end loadModules
end FunLabyInterface
