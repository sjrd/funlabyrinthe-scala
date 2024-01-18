package com.funlabyrinthe.core

import scala.language.{ implicitConversions, higherKinds }

import scala.collection.mutable

import graphics.GraphicsSystem

import scala.reflect.{ClassTag, TypeTest, classTag}

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.messages.*
import com.funlabyrinthe.core.pickling.*

final class Universe(env: UniverseEnvironment) {
  // Being myself implicit in subclasses
  protected final implicit def universe: this.type = this

  // Environmental systems
  val graphicsSystem: GraphicsSystem = env.graphicsSystem
  val resourceLoader: ResourceLoader = env.resourceLoader

  val isEditing: Boolean = env.isEditing

  // Tick count

  private var _tickCount: Long = 0

  /** Number of milliseconds elapsed since the start of the game.
   *
   *  During editing, `tickCount` remains `0`.
   */
  def tickCount: Long = _tickCount

  /** Advances the `tickCount` by `diff` ms.
   *
   *  This is called only by the FunLabyrinthe runner. Do not use this method
   *  in user code.
   */
  def advanceTickCount(diff: Long): Unit =
    _tickCount += diff

  // Image loader and painters

  type GraphicsContext = graphics.GraphicsContext
  type DrawContext = graphics.DrawContext
  type Rectangle2D = graphics.Rectangle2D
  type Painter = graphics.Painter

  lazy val EmptyPainter = new Painter(graphicsSystem, resourceLoader, Nil)
  lazy val DefaultIconPainter = EmptyPainter + "Miscellaneous/Plugin"

  // Categories

  private[core] val _categoriesByID =
    new mutable.HashMap[String, ComponentCategory]

  val DefaultCategory = ComponentCategory("default", "Default")

  // Registered attributes

  private val registeredAttributes: mutable.LinkedHashMap[String, Attribute[?]] = mutable.LinkedHashMap.empty

  private[core] inline def newAttribute[T](defaultValue: T)(using Pickleable[T], Inspectable[T]): Attribute[T] =
    registerAttribute(Attribute.create[T](defaultValue))

  private[core] def registerAttribute[T](attribute: Attribute[T]): attribute.type =
    if players.nonEmpty then
      throw IllegalStateException(s"Cannot register attributes when players already exist")
    if registeredAttributes.contains(attribute.name) then
      throw IllegalArgumentException(s"Duplicate attribute with name '${attribute.name}'")
    registeredAttributes(attribute.name) = attribute
    attribute
  end registerAttribute

  def attributeByName(name: String): Attribute[?] =
    registeredAttributes.getOrElse(name, {
      throw IllegalArgumentException(s"Unknown attribute with name '$name'")
    })
  end attributeByName

  // Components

  private val _components = new mutable.ArrayBuffer[Component]
  private val _componentsByID = new mutable.HashMap[String, Component]

  def allComponents: IndexedSeq[Component] = _components.toIndexedSeq
  def components[A <: Component](using test: TypeTest[Component, A]): IndexedSeq[A] = {
    allComponents.collect {
      case test(c) => c
    }
  }

  def componentByID[A <: Component](id: String)(using test: TypeTest[Component, A]): A =
    getComponentByID(id) match
      case test(c) =>
        c
      case other =>
        throw IllegalArgumentException(s"Component '$id' has an unexpected type ${other.getClass().getName()}")
  end componentByID

  private[core] def componentAdded(component: Component): Unit = {
    if !component.id.isEmpty() then
      _components += component
      _componentsByID += component.id -> component
  }

  private[core] def componentIDChanged(component: Component,
      oldID: String, newID: String): Unit = {
    _componentsByID -= oldID
    if (!newID.isEmpty())
      _componentsByID += newID -> component
  }

  private[core] def componentIDExists(id: String) = _componentsByID contains id

  def getComponentByID(id: String): Component = {
    _componentsByID(id)
  }

  def getComponentByIDOption(id: String): Option[Component] =
    _componentsByID.get(id)

  // Core components

  val defaultMessagesPlugin =
    new messages.DefaultMessagesPlugin(using ComponentInit(this, ComponentID("defaultMessagesPlugin"), CoreOwner))

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
    val init = ComponentInit(this, ComponentID(id), CoreOwner)
    val player = new CorePlayer(using init)
    for attribute <- registeredAttributes.valuesIterator do
      player.attributes.registerAttribute(attribute)
    for (cls, factory) <- _reifiedPlayers do
      cls match
        case cls: Class[a] =>
          val init = ComponentInit(universe, ComponentID(s"$id::${cls.getName()}"), CoreOwner)
          val reified = cls.cast(factory(using init)(player))
          player.registerReified(cls, reified)
          InPlacePickleable.storeDefaults(reified)
    InPlacePickleable.storeDefaults(player)
    player
  end createPlayer

  // Modules

  private val _modules: mutable.ListBuffer[Module] = mutable.ListBuffer.empty
  private val _moduleByDesc: mutable.Map[Class[?], Module] = mutable.Map.empty

  def addModule[M <: Module](module: M): Unit =
    if !_modules.contains(module) then
      _modules += module
      _moduleByDesc(module.getClass()) = module
  end addModule

  def module[M <: Module](using ClassTag[M]): M =
    val cls = classTag[M].runtimeClass
    _moduleByDesc.getOrElse(cls, {
      throw IllegalArgumentException(
        s"The module ${cls.getName()} cannot be found in this universe; " +
        "was it registered with universe.addModule?"
      )
    }).asInstanceOf[M]
  end module

  def allModules: List[Module] = _modules.toList

  // Initialization

  def initialize(): Unit =
    allModules.foreach(Module.preInitialize(_))
    allModules.foreach(Module.createComponents(_))
    allModules.foreach(Module.initialize(_))

    InPlacePickleable.storeDefaults(this)
  end initialize

  // Game lifecycle

  def startGame(): Unit =
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
}

object Universe:
  given UniversePickleable: InPlacePickleable[Universe] with
    override def storeDefaults(universe: Universe): Unit =
      for component <- universe.allComponents do
        InPlacePickleable.storeDefaults(component)
    end storeDefaults

    override def pickle(universe: Universe)(using PicklingContext): Option[Pickle] =
      val pickleFields = List.newBuilder[(String, Pickle)]

      if universe.tickCount != 0L then
        pickleFields += "tickCount" -> IntegerPickle(universe.tickCount)

      val playerPickles = universe.players.map(p => StringPickle(p.id))
      pickleFields += "players" -> ListPickle(playerPickles)

      val additionalComponentPickles =
        for case creator: ComponentCreator <- universe.allComponents.toList yield
          val createdIDs = creator.allCreatedComponents.map(_.id)
          creator.id -> Pickleable.pickle(createdIDs)
      pickleFields += "additionalComponents" -> ObjectPickle(additionalComponentPickles)

      val componentPickles =
        for
          component <- universe.allComponents.sortBy(_.id).toList
          componentPickle <- InPlacePickleable.pickle(component)
        yield
          component.id -> componentPickle
      pickleFields += "components" -> ObjectPickle(componentPickles)

      Some(ObjectPickle(pickleFields.result()))
    end pickle

    override def unpickle(universe: Universe, pickle: Pickle)(using PicklingContext): Unit =
      pickle match
        case pickle: ObjectPickle =>
          for case tickCountPickle: IntegerPickle <- pickle.getField("tickCount") do
            universe._tickCount = tickCountPickle.longValue

          for case ListPickle(playerPickles) <- pickle.getField("players") do
            for case StringPickle(id) <- playerPickles do
              universe.createPlayer(id)

          for case ObjectPickle(additionalComponentPickles) <- pickle.getField("additionalComponents") do
            for (creatorID, createdIDsPickle) <- additionalComponentPickles do
              for case creator: ComponentCreator <- universe.getComponentByIDOption(creatorID) do
                for createdIDs <- Pickleable.unpickle[List[String]](createdIDsPickle) do
                  for createdID <- createdIDs do
                    creator.createNewComponent(createdID)

          pickle.getField("components") match
            case Some(componentsPickle: ObjectPickle) =>
              for (componentID, componentPickle) <- componentsPickle.fields do
                for component <- universe.getComponentByIDOption(componentID) do
                  InPlacePickleable.unpickle(component, componentPickle)
            case _ =>
              ()
        case _ =>
          ()
    end unpickle
  end UniversePickleable
end Universe
