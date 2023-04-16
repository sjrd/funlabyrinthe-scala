package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

class PicklingRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  type Entry = RegistryEntry

  PrimitivePicklers.registerPrimitivePicklers(this)
  registerInPlacePickleable[Reflectable]()

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

  def registerInPlacePickleable[T]()(using InspectedTypeable[T], InPlacePickleable[T]): Unit =
    val inspectedType = summon[InspectedTypeable[T]].inspectedType
    registerSubTypeReadOnly(
      inspectedType,
      { (ctx, data) =>
        new MutableMembersPickler {
          val tpe = inspectedType
        }
      }
    )
  end registerInPlacePickleable

  def createPickler(data: InspectedData)(implicit ctx: Context): Option[Pickler] =
    findEntry(data).map(_.createPickler(data))

  def pickle[T](value: T)(using InPlacePickleable[T]): Pickle = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].pickle(value)
  }

  def unpickle[T](value: T, pickle: Pickle)(using InPlacePickleable[T]): Unit = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].unpickle(value, pickle)
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
