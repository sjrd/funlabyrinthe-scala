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
  def loadUniverse(
    moduleClassNames: js.Array[String],
    pickleString: String,
    globalConfig: intf.GlobalConfig,
  ): js.Promise[Universe] =
    val coreUniverse = initializeUniverse(moduleClassNames, globalConfig)

    val intfUniverse = new Universe(coreUniverse)
    intfUniverse.load(pickleString)
    coreUniverse.markLoaded()
    js.Promise.resolve(intfUniverse)
  end loadUniverse

  private def initializeUniverse(
    moduleClassNames: js.Array[String],
    globalConfig: intf.GlobalConfig,
  ): core.Universe =
    val environment = createEnvironment(globalConfig)
    val modules = loadModules(moduleClassNames)
    core.Universe.initialize(environment, modules)
  end initializeUniverse

  private def createEnvironment(globalConfig: intf.GlobalConfig): core.UniverseEnvironment =
    val onResourceLoaded: () => Unit =
      globalConfig.onResourceLoaded.fold(() => ())(f => f)
    val resourceLoader = new ResourceLoader("./Resources/", onResourceLoaded)
    val isEditing = globalConfig.isEditing.getOrElse(false)
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
