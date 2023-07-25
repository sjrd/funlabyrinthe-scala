package com.funlabyrinthe.core.pickling

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

import com.funlabyrinthe.core.{Module, Universe}
import com.funlabyrinthe.core.reflect._

final class PicklingRegistry:
  import PicklingRegistry.*

  private val pickleables = mutable.ListBuffer.empty[PickleableEntry]
  private val inPlacePickableables = mutable.ListBuffer.empty[InPlacePickleableEntry]

  private val modules = mutable.HashMap.empty[String, Universe => Module]

  PrimitivePicklers.registerPrimitivePicklers(this)
  registerInPlacePickleable[Reflectable]()

  def registerPickleable[T]()(using InspectedTypeable[T], Pickleable[T]): Unit =
    val inspectedType = summon[InspectedTypeable[T]].inspectedType
    pickleables += new PickleableEntry(inspectedType, summon[Pickleable[T]])
  end registerPickleable

  def registerInPlacePickleable[T]()(using InspectedTypeable[T], InPlacePickleable[T]): Unit =
    val inspectedType = summon[InspectedTypeable[T]].inspectedType
    inPlacePickableables += new InPlacePickleableEntry(inspectedType, summon[InPlacePickleable[T]])
  end registerInPlacePickleable

  def createPickler(data: InspectedData)(implicit ctx: Context): Option[Pickler] =
    if data.isReadOnly then
      inPlacePickableables.find(_.appliesTo(data.tpe)).map(_.inPlacePickleable.toPickler)
    else
      pickleables.find(_.appliesTo(data.tpe)).map(_.pickleable.toPickler)
  end createPickler

  def pickle[T](value: T)(using InPlacePickleable[T]): Pickle = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].pickle(value)
  }

  def unpickle[T](value: T, pickle: Pickle)(using InPlacePickleable[T]): Unit = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].unpickle(value, pickle)
  }

  def registerModule[M <: Module](createModule: Universe => M)(using ClassTag[M]): Unit =
    modules(classTag[M].runtimeClass.getName()) = createModule

  def createModule(universe: Universe, name: String): Module =
    val creator = modules(name)
    creator(universe)

  private def createContext() = {
    new Context {
      val registry = PicklingRegistry.this
    }
  }
end PicklingRegistry

object PicklingRegistry:
  private final class PickleableEntry(val inspectedType: InspectedType, val pickleable: Pickleable[?]):
    def appliesTo(tpe: InspectedType): Boolean =
      tpe.isEquiv(inspectedType)
  end PickleableEntry

  private final class InPlacePickleableEntry(
    val inspectedType: InspectedType,
    val inPlacePickleable: InPlacePickleable[?]
  ):
    def appliesTo(tpe: InspectedType): Boolean =
      tpe.isSubtype(inspectedType)
  end InPlacePickleableEntry
end PicklingRegistry
