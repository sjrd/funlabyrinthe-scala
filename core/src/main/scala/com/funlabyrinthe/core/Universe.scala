package com.funlabyrinthe.core

import scala.annotation.constructorOnly

import scala.collection.mutable

import graphics.GraphicsSystem

import scala.reflect.{ClassTag, TypeTest, classTag}

import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.messages.*
import com.funlabyrinthe.core.pickling.*

final class Universe private (
  @constructorOnly env: UniverseEnvironment,
  val allModules: List[Module]
):
  // Being myself implicit within this class
  private given Universe = this

  // Environmental systems
  val graphicsSystem: GraphicsSystem = env.graphicsSystem
  val resourceLoader: ResourceLoader = env.resourceLoader

  val isEditing: Boolean = env.isEditing

  // Modules

  private val modulesByID: Map[String, Module] = allModules.map(m => m.moduleID -> m).toMap

  // Lifetime

  private var _isLoaded: Boolean = false
  private var _gameStarted: Boolean = false
  private var _tickCount: Long = 0

  /** Has this universe finished loading? */
  def isLoaded: Boolean = _isLoaded

  /** Has the game started? */
  def gameStarted: Boolean = _gameStarted

  /** Number of milliseconds elapsed since the start of the game.
   *
   *  During editing, `tickCount` remains `0`.
   */
  def tickCount: Long = _tickCount

  /** Marks the universe as loaded.
   *
   *  This is called only by the FunLabyrinthe engine. Do not use this method
   *  in user code.
   */
  def markLoaded(): Unit =
    _isLoaded = true

  /** Advances the `tickCount` by `diff` ms.
   *
   *  This is called only by the FunLabyrinthe runner. Do not use this method
   *  in user code.
   */
  def advanceTickCount(diff: Long): Unit =
    _tickCount += diff
    for component <- _frameUpdatesComponents do
      component.frameUpdate(diff)
  end advanceTickCount

  // Painters

  lazy val EmptyPainter = new Painter(graphicsSystem, resourceLoader, Nil)
  lazy val DefaultIconPainter = EmptyPainter + "Miscellaneous/Plugin"

  // Categories

  private[core] val _categoriesByID =
    new mutable.HashMap[String, ComponentCategory]

  // Registered attributes

  private val registeredAttributes: mutable.LinkedHashMap[(Module, String), Attribute[?]] =
    mutable.LinkedHashMap.empty

  private[core] def registerAttribute[T](module: Module, attribute: Attribute[T]): attribute.type =
    if players.nonEmpty then
      throw IllegalStateException(s"Cannot register attributes when players already exist")

    val pair = (module, attribute.id)
    if registeredAttributes.contains(pair) then
      throw IllegalArgumentException(s"Duplicate attribute of module $module with ID '${attribute.id}'")
    registeredAttributes(pair) = attribute
    attribute
  end registerAttribute

  def attributeByID(module: Module, id: String): Attribute[?] =
    registeredAttributes.getOrElse((module, id), {
      throw IllegalArgumentException(s"Unknown attribute of module $module with ID '$id'")
    })
  end attributeByID

  // Components

  private val _allComponents = new mutable.ListBuffer[Component]
  private val _topComponentsByID = new mutable.HashMap[(Module, String), Component]
  private val _frameUpdatesComponents = new mutable.ListBuffer[FrameUpdates]

  def allComponents: List[Component] = _allComponents.toList

  def components[A <: Component](using test: TypeTest[Component, A]): List[A] = {
    allComponents.collect {
      case test(c) => c
    }
  }

  def lookupTopComponentByID(module: Module, id: String): Option[Component] =
    _topComponentsByID.get((module, id))

  def findTopComponentByID[A <: Component](module: Module, id: String)(using test: TypeTest[Component, A]): A =
    lookupTopComponentByID(module, id) match
      case Some(test(c)) =>
        c
      case Some(other) =>
        throw IllegalArgumentException(s"Component '$id' in module $module has an unexpected type ${other.getClass().getName()}")
      case None =>
        for (key, value) <- _topComponentsByID do println(s"$key -> $value")
        throw IllegalArgumentException(s"Cannot find component with ID '$id' in module $module")
  end findTopComponentByID

  private[core] def topComponentAdded(module: Module, component: Component): Unit =
    subComponentAdded(component)
    if !component.id.isEmpty() then
      _topComponentsByID += (module, component.id) -> component
  end topComponentAdded

  private[core] def subComponentAdded(component: Component): Unit =
    _allComponents += component
    component match
      case component: FrameUpdates =>
        _frameUpdatesComponents += component
      case _ =>
        ()
  end subComponentAdded

  private[core] def topComponentIDChanging(module: Module, component: Component, newID: String): Unit =
    val pair = (module, newID)
    require(!_topComponentsByID.contains(pair), s"Duplicate component ID '$newID' in module $module")
    _topComponentsByID -= ((module, component.id))
    _topComponentsByID += pair -> component
  end topComponentIDChanging

  private[core] def topComponentIDExists(module: Module, id: String): Boolean =
    _topComponentsByID.contains((module, id))

  def lookupNestedComponentByFullID(fullIDString: String): Option[Component] =
    def followPath(component: Component, subPath: List[String]): Option[Component] =
      subPath match
        case Nil =>
          Some(component)
        case subID :: pathRest =>
          component.lookupSubComponentByID(subID) match
            case Some(subComponent) => followPath(subComponent, pathRest)
            case None               => None
    end followPath

    val path = fullIDString.split(':').toList
    path match
      case moduleID :: topID :: pathRest =>
        for
          module <- modulesByID.get(moduleID)
          topComponent <- lookupTopComponentByID(module, topID)
          nested <- followPath(topComponent, pathRest)
        yield
          nested
      case _ =>
        None
  end lookupNestedComponentByFullID

  // Core components

  val defaultMessagesPlugin =
    new messages.DefaultMessagesPlugin(using ComponentInit(this, "defaultMessagesPlugin", ComponentOwner.Module(Core)))

  // Players and extensions

  def players: List[CorePlayer] = components[CorePlayer].toList

  private val _reifiedPlayers =
    mutable.LinkedHashMap.empty[Class[? <: ReifiedPlayer], ReifiedPlayer.Factory[ReifiedPlayer]]

  private[core] def registerReifiedPlayer[A <: ReifiedPlayer](
    reifiedPlayerClass: Class[A],
    factory: ReifiedPlayer.Factory[A]
  ): Unit =
    if players.nonEmpty then
      throw IllegalStateException(s"Cannot register reified players when players already exist")
    if _reifiedPlayers.contains(reifiedPlayerClass) then
      throw IllegalStateException(s"Attempting to register twice the reified player ${reifiedPlayerClass.getName()}")
    _reifiedPlayers += reifiedPlayerClass -> factory
  end registerReifiedPlayer

  def createSoloPlayer(): CorePlayer =
    if players.nonEmpty then
      throw IllegalStateException(s"Cannot create a solo player because there are already players $players")
    createPlayer("player")
  end createSoloPlayer

  private def createPlayer(id: String): CorePlayer =
    val init = ComponentInit(this, id, ComponentOwner.Module(Core))
    val player = new CorePlayer(using init)
    for attribute <- registeredAttributes.valuesIterator do
      player.attributes.registerAttribute(attribute)
    for (cls, factory) <- _reifiedPlayers do
      cls match
        case cls: Class[a] =>
          val init = ComponentInit(this, cls.getName(), ComponentOwner.Component(player))
          val reified = cls.cast(factory(using init)(player))
          player.registerReified(cls, reified)
          InPlacePickleable.storeDefaults(reified)
    InPlacePickleable.storeDefaults(player)
    player
  end createPlayer

  // Initialization

  private def initialize(): Unit =
    allModules.foreach(Module.preInitialize(_))
    allModules.foreach(Module.createComponents(_))
    allModules.foreach(Module.initialize(_))

    InPlacePickleable.storeDefaults(this)
  end initialize

  // Game lifecycle

  def startGame(): Unit =
    _gameStarted = true

    val dummyShowMessage = ShowMessage("unused")
    val dummyShowSelectionMessage = ShowSelectionMessage("unused", List("unused"), ShowSelectionMessage.Options())

    for player <- players do
      if !player.canDispatch(dummyShowMessage) || !player.canDispatch(dummyShowSelectionMessage) then
        player.plugins += defaultMessagesPlugin
      if !player.autoDetectController() then
        throw IllegalStateException(s"Cannot start game because player $player cannot detect a controller")

    allModules.foreach(Module.startGame(_))
  end startGame

  def terminate(): Unit = ()
end Universe

object Universe:
  def initialize(env: UniverseEnvironment, modules: Set[Module]): Universe =
    val resolvedModules = resolveModuleDependencies(modules)
    val result = new Universe(env, resolvedModules)
    result.initialize()
    result
  end initialize

  given UniversePickleable: InPlacePickleable[Universe] with
    override def storeDefaults(universe: Universe): Unit =
      for component <- universe.allComponents do
        InPlacePickleable.storeDefaults(component)
    end storeDefaults

    override def pickle(universe: Universe)(using PicklingContext): Option[Pickle] =
      val pickleFields = List.newBuilder[(String, Pickle)]

      if universe.gameStarted then
        pickleFields += "gameStarted" -> BooleanPickle(true)

      if universe.tickCount != 0L then
        pickleFields += "tickCount" -> IntegerPickle(universe.tickCount)

      val playerPickles = universe.players.map(p => StringPickle(p.id))
      pickleFields += "players" -> ListPickle(playerPickles)

      val additionalComponentPickles =
        for case creator: ComponentCreator <- universe.allComponents.toList yield
          val createdIDs = creator.allCreatedComponents.map(_.id)
          creator.fullID -> Pickleable.pickle(createdIDs)
      pickleFields += "additionalComponents" -> ObjectPickle(additionalComponentPickles)

      val componentPickles =
        for
          component <- universe.allComponents.sortBy(_.id).toList
          componentPickle <- InPlacePickleable.pickle(component)
        yield
          component.fullID -> componentPickle
      pickleFields += "components" -> ObjectPickle(componentPickles)

      Some(ObjectPickle(pickleFields.result()))
    end pickle

    override def unpickle(universe: Universe, pickle: Pickle)(using PicklingContext): Unit =
      pickle match
        case pickle: ObjectPickle =>
          for case BooleanPickle(gameStarted) <- pickle.getField("gameStarted") do
            universe._gameStarted = gameStarted

          for case tickCountPickle: IntegerPickle <- pickle.getField("tickCount") do
            universe._tickCount = tickCountPickle.longValue

          for case ListPickle(playerPickles) <- pickle.getField("players") do
            for case StringPickle(id) <- playerPickles do
              universe.createPlayer(id)

          for case ObjectPickle(additionalComponentPickles) <- pickle.getField("additionalComponents") do
            for (creatorID, createdIDsPickle) <- additionalComponentPickles do
              for case creator: ComponentCreator <- universe.lookupNestedComponentByFullID(creatorID) do
                for createdIDs <- Pickleable.unpickle[List[String]](createdIDsPickle) do
                  for createdID <- createdIDs do
                    creator.createNewComponent(createdID)

          pickle.getField("components") match
            case Some(componentsPickle: ObjectPickle) =>
              for (componentID, componentPickle) <- componentsPickle.fields do
                for component <- universe.lookupNestedComponentByFullID(componentID) do
                  InPlacePickleable.unpickle(component, componentPickle)
            case _ =>
              ()
        case _ =>
          ()
    end unpickle
  end UniversePickleable

  // private[core] for tests
  private[core] def resolveModuleDependencies(moduleSet: Set[Module]): List[Module] =
    // Eventual list of modules in reverse order
    var result: List[Module] = Core :: Nil // always put Core first, even if it is not in moduleSet

    // Sort by moduleID first for stability -- exclude Core which we already added
    var remainingModules = (moduleSet - Core).toList.sortBy(_.moduleID)

    while remainingModules.nonEmpty do
      // Extract all the modules that have their dependencies satisfied
      val (satisfied, unsatisfied) = remainingModules.partition { module =>
        Module.dependencies(module).forall(result.contains(_))
      }
      if satisfied.isEmpty then
        throw IllegalArgumentException(
          "Cannot resolve the module dependencies--is there a cycle between `dependsOn`?\n"
            + s"Resolved modules: ${result.reverse.mkString(", ")}\n"
            + s"Unresolved modules: ${remainingModules.mkString(", ")}"
        )

      result = satisfied reverse_::: result
      remainingModules = unsatisfied
    end while

    result.reverse
  end resolveModuleDependencies
end Universe
