package com.funlabyrinthe.core.reflect

final class ReflectableProp[-T](
  val name: String,
  val tpe: InspectedType,
  getter: T => Any,
  optSetter: Option[(T, Any) => Unit],
):
  def reflect(instance: T): InspectedData =
    optSetter match
      case None =>
        new InspectedData {
          val name = ReflectableProp.this.name
          val tpe = ReflectableProp.this.tpe
          def value: Any = getter(instance)
        }

      case Some(setter) =>
        new WritableInspectedData {
          val name = ReflectableProp.this.name
          val tpe = ReflectableProp.this.tpe
          def value: Any = getter(instance)
          def value_=(v: Any): Unit = setter(instance, v)
        }
  end reflect
end ReflectableProp
