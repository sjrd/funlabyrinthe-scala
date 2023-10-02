package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.pickling.*

sealed abstract class ReflectableProp[-T](
  val name: String,
  val tpe: InspectedType,
):
  type Value

  def reflect(instance: T): InspectedData
end ReflectableProp

object ReflectableProp:
  final class ReadOnly[-T, V](
    name: String,
    tpe: InspectedType,
    getter: T => V,
    val optInPlacePickleable: Option[InPlacePickleable[V]],
  ) extends ReflectableProp[T](name, tpe):
    type Value = V

    def reflect(instance: T): InspectedData =
      new InspectedData {
        type Value = V

        val name = ReadOnly.this.name
        val tpe = ReadOnly.this.tpe
        def value: V = getter(instance)

        def isPickleable: Boolean = optInPlacePickleable.isDefined

        def storeDefaults(): Unit =
          optInPlacePickleable.get.storeDefaults(value)

        def pickle()(using PicklingContext): Option[Pickle] =
          optInPlacePickleable.get.pickle(value)

        def unpickle(pickle: Pickle)(using PicklingContext): Unit =
          optInPlacePickleable.get.unpickle(value, pickle)
      }
    end reflect
  end ReadOnly

  final class ReadWrite[-T, V](
    name: String,
    tpe: InspectedType,
    getter: T => V,
    setter: (T, Any) => Unit,
    val optPickleable: Option[Pickleable[V]],
  ) extends ReflectableProp[T](name, tpe):
    type Value = V

    def reflect(instance: T): WritableInspectedData =
      new WritableInspectedData {
        type Value = V

        private var storedDefault: Option[Value] = None

        val name = ReadWrite.this.name
        val tpe = ReadWrite.this.tpe
        def value: V = getter(instance)
        def value_=(v: Any): Unit = setter(instance, v)

        def isPickleable: Boolean = optPickleable.isDefined

        def storeDefaults(): Unit =
          storedDefault = Some(value)

        def pickle()(using PicklingContext): Option[Pickle] =
          val v = value
          if storedDefault.contains(v) then None
          else Some(optPickleable.get.pickle(value))

        def unpickle(pickle: Pickle)(using PicklingContext): Unit =
          for unpickledValue <- optPickleable.get.unpickle(pickle) do
            value = unpickledValue
      }
    end reflect
  end ReadWrite
end ReflectableProp
