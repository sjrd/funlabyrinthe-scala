package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

import scala.reflect.runtime.universe._

class PicklingRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  type Entry = RegistryEntry

  PrimitivePicklers.registerPrimitiveEditors(this)
  CollectionPickler.registerCollectionPicklers(this)

  def registerExactType(tpe: Type, picklerFactory: PicklerFactory,
      matchPercent0: Int = 90) =
    register(new ExactType(tpe, picklerFactory, matchPercent0))

  def registerExactTypeReadWrite(tpe: Type, picklerFactory: PicklerFactory,
      matchPercent0: Int = 90) =
    register(new ExactType(tpe, picklerFactory, matchPercent0) with ReadWriteOnly)

  def registerSubType(tpe: Type, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0))

  def registerSubTypeReadOnly(tpe: Type, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0) with ReadOnlyOnly)

  def registerSubTypeReadWrite(tpe: Type, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0) with ReadWriteOnly)

  def createPickler(data: InspectedData)(implicit ctx: Context): Option[Pickler] = {
    println(s"looking pickler for ${data.tpe}, read-only=${data.isReadOnly}")
    val r = findEntry(data) map (_.createPickler(data))
    if (r.isEmpty)
      println("not found")
    else
      println("found of class "+r.get.getClass.getName)
    r
  }

  def pickle[A : TypeTag](value: A): Option[Pickle] = {
    pickle(value, ReflectionUtils.guessRuntimeTypeOfValue(value))
  }

  def pickle(value: Any, tpe: Type): Option[Pickle] = {
    implicit val context = createContext()
    val data = createTopLevelData(value, tpe)
    createPickler(data).map(_.pickle(data))
  }

  def unpickle[A : TypeTag](value: A, pickle: Pickle): Unit = {
    unpickle(value, pickle, ReflectionUtils.guessRuntimeTypeOfValue(value))
  }

  def unpickle(value: Any, pickle: Pickle, tpe: Type): Unit = {
    implicit val context = createContext()
    val data = createTopLevelData(value, tpe)
    createPickler(data).foreach(_.unpickle(data, pickle))
  }

  private def createContext() = {
    new Context {
      val registry = PicklingRegistry.this
    }
  }

  private def createTopLevelData(value0: Any, tpe0: Type): InspectedData = {
    new InspectedData {
      val name = ""
      val tpe = tpe0
      override val isReadOnly = true

      def value: Any = value0
      def value_=(v: Any): Unit = ???
    }
  }
}
