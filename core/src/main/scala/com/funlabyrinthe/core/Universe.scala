package com.funlabyrinthe.core

import scala.language.{ implicitConversions, higherKinds }

import scala.collection.mutable

import graphics.GraphicsSystem

import scala.reflect.{ClassTag, classTag}

import com.funlabyrinthe.core.pickling.*

final class Universe(env: UniverseEnvironment) {
  // Being myself implicit in subclasses
  protected final implicit def universe: this.type = this

  // Environmental systems
  val graphicsSystem: GraphicsSystem = env.graphicsSystem
  val resourceLoader: ResourceLoader = env.resourceLoader

  // Image loader and painters

  type GraphicsContext = graphics.GraphicsContext
  type DrawContext = graphics.DrawContext
  type Rectangle2D = graphics.Rectangle2D
  type Painter = graphics.Painter

  lazy val EmptyPainter = new Painter(resourceLoader)
  lazy val DefaultIconPainter = EmptyPainter + "Miscellaneous/Plugin"

  // Categories

  private[core] val _categoriesByID =
    new mutable.HashMap[String, ComponentCategory]

  val DefaultCategory = ComponentCategory("default", "Default")

  // Components

  private val _components = new mutable.ArrayBuffer[Component]
  private val _componentsByID = new mutable.HashMap[String, Component]

  def allComponents: IndexedSeq[Component] = _components.toIndexedSeq
  def components[A <: Component : ClassTag]: IndexedSeq[A] = {
    allComponents.collect {
      case c: A => c
    }
  }

  private[core] def componentAdded(component: Component): Unit = {
    _components += component
    if (!component.id.isEmpty())
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

  def initialize(): Unit = {
  }

  // Termination (end of game)

  def terminate(): Unit = ()
}

object Universe:
  given UniversePickleable: InPlacePickleable[Universe] with
    override def pickle(universe: Universe)(using Context): Pickle =
      val modulePickles = universe.allModules.map { module =>
        val moduleName = module.getClass().getName()
        StringPickle(moduleName)
      }
      val modulesPickle = ListPickle(modulePickles)

      val additionalComponentPickles =
        for case creator: ComponentCreator <- universe.allComponents.toList yield
          val createdIDs = creator.allCreatedComponents.map(_.id)
          creator.id -> Pickleable.pickle(createdIDs)
      val additionalComponentsPickle = ObjectPickle(additionalComponentPickles)

      val componentPickles = universe.allComponents.sortBy(_.id).map { component =>
        val pickle = summon[Context].registry.pickle(component)
        component.id -> pickle
      }
      val componentsPickle = ObjectPickle(componentPickles.toList)

      ObjectPickle(
        List(
          "modules" -> modulesPickle,
          "additionalComponents" -> additionalComponentsPickle,
          "components" -> componentsPickle,
        )
      )
    end pickle

    override def unpickle(universe: Universe, pickle: Pickle)(using Context): Unit =
      pickle match
        case pickle: ObjectPickle =>
          for case modulesPickle: ListPickle <- pickle.getField("modules") do
            for case modulePickle: StringPickle <- modulesPickle.elems do
              val moduleName = modulePickle.value
              universe.addModule(summon[Context].registry.createModule(universe, moduleName))

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
                  summon[Context].registry.unpickle(component, componentPickle)
            case _ =>
              ()
        case _ =>
          ()
    end unpickle
  end UniversePickleable
end Universe
