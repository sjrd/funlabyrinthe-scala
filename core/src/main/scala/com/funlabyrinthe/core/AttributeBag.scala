package com.funlabyrinthe.core

import scala.collection.mutable

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

final class AttributeBag extends Reflectable:
  private val attributes = mutable.HashMap.empty[Attribute[?], Any]

  private[core] def registerAttribute[T](attribute: Attribute[T]): Unit =
    if attributes.keysIterator.exists(_.name == attribute.name) then
      throw IllegalStateException(s"Duplicate attribute '$attribute'")
    attributes(attribute) = attribute.defaultValue
  end registerAttribute

  def apply[T](attribute: Attribute[T]): T =
    requireRegistered(attribute)
    attributes(attribute).asInstanceOf[T]
  end apply

  def update[T](attribute: Attribute[T], value: T): Unit =
    requireRegistered(attribute)
    attributes(attribute) = value
  end update

  private def requireRegistered(attribute: Attribute[?]): Unit =
    if !attributes.contains(attribute) then
      throw IllegalArgumentException(s"Unregistered attribute: $attribute")
  end requireRegistered

  override def reflect() = autoReflect[AttributeBag]
end AttributeBag

object AttributeBag:
  given AttributeBagReflector: Reflector[AttributeBag] with
    def reflectProperties(instance: AttributeBag): List[InspectedData] =
      val properties = instance.attributes.keysIterator.toList.sortBy(_.name).map {
        case attribute: Attribute[v] =>
          new ReflectableProp.ReadWrite[instance.type, v](
            attribute.name,
            instance => instance(attribute),
            (instance, newValue) => instance(attribute) = newValue.asInstanceOf[v],
            Some(attribute.pickleable),
            Some(attribute.inspectable),
          )
      }

      properties.map(_.reflect(instance))
    end reflectProperties
  end AttributeBagReflector
end AttributeBag
