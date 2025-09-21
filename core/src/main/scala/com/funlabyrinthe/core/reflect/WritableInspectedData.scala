package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.Component
import com.funlabyrinthe.core.inspecting.*
import com.funlabyrinthe.core.pickling.*

trait WritableInspectedData extends InspectedData:
  def value_=(v: Any): Unit
end WritableInspectedData

object WritableInspectedData:
  def make[V](
    name: String,
    getter: () => V,
    setter: Any => Unit,
    optPickleable: Option[Pickleable[V]],
    optInspectable: Option[Inspectable[V]],
  ): WritableInspectedData { type Value = V } =
    new Impl(name, getter, setter, optPickleable, optInspectable)
  end make

  private final class Impl[V](
    val name: String,
    getter: () => V,
    setter: Any => Unit,
    optPickleable: Option[Pickleable[V]],
    optInspectable: Option[Inspectable[V]],
  ) extends WritableInspectedData:
    type Value = V

    private var storedDefault: Option[Value] = None

    def value: V = getter()
    def value_=(v: Any): Unit = setter(v)

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
  end Impl
end WritableInspectedData
