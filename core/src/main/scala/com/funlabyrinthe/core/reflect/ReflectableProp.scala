package com.funlabyrinthe.core.reflect

sealed abstract class ReflectableProp[-T](
  val name: String,
  val tpe: InspectedType,
):
  def reflect(instance: T): InspectedData
end ReflectableProp

object ReflectableProp:
  final class ReadOnly[-T](name: String, tpe: InspectedType, getter: T => Any)
      extends ReflectableProp[T](name, tpe):

    def reflect(instance: T): InspectedData =
      new InspectedData {
        val name = ReadOnly.this.name
        val tpe = ReadOnly.this.tpe
        def value: Any = getter(instance)
      }
    end reflect
  end ReadOnly

  final class ReadWrite[-T](name: String, tpe: InspectedType, getter: T => Any, setter: (T, Any) => Unit)
      extends ReflectableProp[T](name, tpe):

    def reflect(instance: T): WritableInspectedData =
      new WritableInspectedData {
        val name = ReadWrite.this.name
        val tpe = ReadWrite.this.tpe
        def value: Any = getter(instance)
        def value_=(v: Any): Unit = setter(instance, v)
      }
    end reflect
  end ReadWrite
end ReflectableProp
