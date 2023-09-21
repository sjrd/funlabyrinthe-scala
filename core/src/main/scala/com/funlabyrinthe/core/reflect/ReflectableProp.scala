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

    private val optPickler = optInPlacePickleable.map(_.toPickler)

    def reflect(instance: T): InspectedData =
      new InspectedData {
        type Value = V

        val name = ReadOnly.this.name
        val tpe = ReadOnly.this.tpe
        def value: V = getter(instance)

        def optPickler: Option[Pickler] = ReadOnly.this.optPickler
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

    private val optPickler = optPickleable.map(_.toPickler)

    def reflect(instance: T): WritableInspectedData =
      new WritableInspectedData {
        type Value = V

        val name = ReadWrite.this.name
        val tpe = ReadWrite.this.tpe
        def value: V = getter(instance)
        def value_=(v: Any): Unit = setter(instance, v)

        def optPickler: Option[Pickler] = ReadWrite.this.optPickler
      }
    end reflect
  end ReadWrite
end ReflectableProp
