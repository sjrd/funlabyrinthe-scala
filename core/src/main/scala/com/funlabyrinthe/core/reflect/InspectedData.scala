package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.Component

trait InspectedData {
  val name: String

  type Value

  def value: Value

  def valueString: String = value.toString

  final def isReadOnly: Boolean = !this.isInstanceOf[WritableInspectedData]
  final def asWritable: WritableInspectedData = this.asInstanceOf[WritableInspectedData]

  def isPickleable: Boolean

  def storeDefaults(): Unit

  def pickle()(using PicklingContext): Option[Pickle]

  def unpickle(pickle: Pickle)(using PicklingContext): Unit

  def prepareRemoveReferences(reference: Component, actions: InPlacePickleable.PreparedActions)(
      using PicklingContext): Unit

  def inspectable: Option[Inspectable[Value]]
}

object InspectedData:
  def make[V](
    name: String,
    getter: () => V,
    optInPlacePickleable: Option[InPlacePickleable[V]],
  ): InspectedData { type Value = V } =
    new Impl(name, getter, optInPlacePickleable)
  end make

  private final class Impl[V](
    val name: String,
    getter: () => V,
    optInPlacePickleable: Option[InPlacePickleable[V]],
  ) extends InspectedData:
    type Value = V

    def value: V = getter()

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
  end Impl
end InspectedData
