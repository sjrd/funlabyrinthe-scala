package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.core.pickling.*

import com.funlabyrinthe.graphics.html.HTML5GraphicsSystem
import com.funlabyrinthe.htmlenv.ResourceLoader

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

      given core.Universe = coreUniverse

      mapCreator.createNewComponent()
    }

    val intfUniverse = new Universe(coreUniverse)
    coreUniverse.markLoaded()
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
    coreUniverse.markLoaded()
    js.Promise.resolve(intfUniverse)
  end loadUniverse

  private def initializeUniverse(
    moduleClassNames: js.Array[String],
    globalEventHandler: intf.GlobalEventHandler,
  ): core.Universe =
    val environment = createEnvironment(globalEventHandler)
    val modules = loadModules(moduleClassNames)
    core.Universe.initialize(environment, modules)
  end initializeUniverse

  private def createEnvironment(globalEventHandler: intf.GlobalEventHandler): core.UniverseEnvironment =
    val onResourceLoaded: () => Unit =
      globalEventHandler.onResourceLoaded.fold(() => ())(f => f)
    val resourceLoader = new ResourceLoader("./Resources/", onResourceLoaded)
    val isEditing = globalEventHandler.isEditing.getOrElse(false)
    new core.UniverseEnvironment(HTML5GraphicsSystem, resourceLoader, isEditing)
  end createEnvironment

  private def loadModules(moduleClassNames: js.Array[String]): Set[core.Module] =
    import org.portablescala.reflect.Reflect

    val allModules =
      for
        moduleClassName <- moduleClassNames.toList
        cls <- Reflect.lookupLoadableModuleClass(moduleClassName + "$")
        if classOf[core.Module].isAssignableFrom(cls.runtimeClass)
      yield
        cls.loadModule().asInstanceOf[core.Module]

    allModules.toSet
  end loadModules
end FunLabyInterface
