package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

class PicklingRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry.{ReadOnlyOnly, ReadWriteOnly}
  import RegistryEntry.{ExactType, PicklerFactory, SubType}

  type Entry = RegistryEntry

  PrimitivePicklers.registerPrimitivePicklers(this)
  registerInPlacePickleable[Reflectable]()

  def registerPickleable[T]()(using InspectedTypeable[T], Pickleable[T]): Unit =
    val inspectedType = summon[InspectedTypeable[T]].inspectedType
    val factory: PicklerFactory = (ctx, data) => summon[Pickleable[T]].toPickler
    register(new ExactType(inspectedType, factory, 90) with ReadWriteOnly)
  end registerPickleable

  def registerInPlacePickleable[T]()(using InspectedTypeable[T], InPlacePickleable[T]): Unit =
    val inspectedType = summon[InspectedTypeable[T]].inspectedType
    val factory: PicklerFactory = { (ctx, data) =>
      new MutableMembersPickler {
        val tpe = inspectedType
      }
    }
    register(new SubType(inspectedType, factory, 50) with ReadOnlyOnly)
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
