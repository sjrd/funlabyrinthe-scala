package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

class PicklingRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  type Entry = RegistryEntry

  PrimitivePicklers.registerPrimitivePicklers(this)
  CollectionPickler.registerCollectionPicklers(this)

  def registerExactType(tpe: InspectedType, picklerFactory: PicklerFactory,
      matchPercent0: Int = 90) =
    register(new ExactType(tpe, picklerFactory, matchPercent0))

  def registerExactTypeReadWrite(tpe: InspectedType, picklerFactory: PicklerFactory,
      matchPercent0: Int = 90) =
    register(new ExactType(tpe, picklerFactory, matchPercent0) with ReadWriteOnly)

  def registerSubType(tpe: InspectedType, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0))

  def registerSubTypeReadOnly(tpe: InspectedType, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0) with ReadOnlyOnly)

  def registerSubTypeReadWrite(tpe: InspectedType, picklerFactory: PicklerFactory,
      matchPercent0: Int = 50) =
    register(new SubType(tpe, picklerFactory, matchPercent0) with ReadWriteOnly)

  def registerPickleable[T]()(using InspectedTypeable[T], Pickleable[T]): Unit =
    registerSubTypeReadWrite(
      summon[InspectedTypeable[T]].inspectedType,
      (ctx, data) => summon[Pickleable[T]].toPickler
    )
  end registerPickleable

  def createPickler(data: InspectedData)(implicit ctx: Context): Option[Pickler] = {
    println(s"looking pickler for ${data.tpe}, read-only=${data.isReadOnly}")
    val r = findEntry(data) map (_.createPickler(data))
    if (r.isEmpty)
      println("not found")
    else
      println("found of class "+r.get.getClass.getName)
    r
  }

  def pickle(value: Reflectable): Option[Pickle] = {
    implicit val context = createContext()
    val tpe = InspectedType.monoClass(value.getClass())
    val data = createTopLevelData(value, tpe)
    createPickler(data).map(_.pickle(data))
  }

  def unpickle(value: Reflectable, pickle: Pickle): Unit = {
    implicit val context = createContext()
    val tpe = InspectedType.monoClass(value.getClass())
    val data = createTopLevelData(value, tpe)
    createPickler(data).foreach(_.unpickle(data, pickle))
  }

  private def createContext() = {
    new Context {
      val registry = PicklingRegistry.this
    }
  }

  private def createTopLevelData(value0: Any, tpe0: InspectedType): InspectedData = {
    new InspectedData {
      val name = ""
      val tpe = tpe0

      def value: Any = value0
    }
  }
}
