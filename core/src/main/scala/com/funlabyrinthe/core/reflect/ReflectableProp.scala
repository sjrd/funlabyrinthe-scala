package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.Component

sealed abstract class ReflectableProp[-T](
  val name: String,
):
  type Value

  def reflect(instance: T): InspectedData
end ReflectableProp

object ReflectableProp:
  final class ReadOnly[-T, V](
    name: String,
    getter: T => V,
    val optInPlacePickleable: Option[InPlacePickleable[V]],
  ) extends ReflectableProp[T](name):
    type Value = V

    def reflect(instance: T): InspectedData =
      new InspectedData {
        type Value = V

        val name = ReadOnly.this.name
        def value: V = getter(instance)

        def isPickleable: Boolean = optInPlacePickleable.isDefined

        def storeDefaults(): Unit =
          optInPlacePickleable.get.storeDefaults(value)

        def pickle()(using PicklingContext): Option[Pickle] =
          optInPlacePickleable.get.pickle(value)

        def unpickle(pickle: Pickle)(using PicklingContext): Unit =
          optInPlacePickleable.get.unpickle(value, pickle)

        def prepareRemoveReferences(reference: Component, actions: InPlacePickleable.PreparedActions)(
            using PicklingContext): Unit =
          for inPlacePickleable <- optInPlacePickleable do
            inPlacePickleable.prepareRemoveReferences(value, reference, actions)
        end prepareRemoveReferences

        def inspectable: Option[Inspectable[V]] = None
      }
    end reflect
  end ReadOnly

  final class ReadWrite[-T, V](
    name: String,
    getter: T => V,
    setter: (T, Any) => Unit,
    val optPickleable: Option[Pickleable[V]],
    val optInspectable: Option[Inspectable[V]],
  ) extends ReflectableProp[T](name):
    type Value = V

    def reflect(instance: T): WritableInspectedData =
      new WritableInspectedData {
        type Value = V

        private var storedDefault: Option[Value] = None

        val name = ReadWrite.this.name
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

        def prepareRemoveReferences(reference: Component, actions: InPlacePickleable.PreparedActions)(
            using PicklingContext): Unit =
          for pickleable <- optPickleable do
            pickleable.removeReferences(value, reference) match
              case Pickleable.RemoveRefResult.Unchanged =>
                () // nothing to do
              case Pickleable.RemoveRefResult.Changed(newValue) =>
                actions.prepare {
                  value = newValue
                }
              case Pickleable.RemoveRefResult.Failure =>
                PicklingContext.error(s"There are references to $reference that cannot be cleared")
        end prepareRemoveReferences

        def inspectable: Option[Inspectable[V]] = optInspectable
      }
    end reflect
  end ReadWrite
end ReflectableProp
