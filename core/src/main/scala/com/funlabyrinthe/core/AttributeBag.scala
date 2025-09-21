package com.funlabyrinthe.core

import scala.collection.mutable

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

final class AttributeBag extends Reflectable:
  private val attributes = mutable.HashMap.empty[Attribute[?], Any]

  private[core] def registerAttribute[T](attribute: Attribute[T]): Unit =
    if attributes.keysIterator.exists(_.id == attribute.id) then
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

  override protected def reflectProperties(registerData: InspectedData => Unit): Unit =
    super.reflectProperties(registerData)
    Reflectable.autoReflectProperties(this, registerData)

    attributes.keysIterator.toList.sortBy(_.id).foreach {
      case attribute: Attribute[v] =>
        registerData(
          WritableInspectedData.make[v](
            attribute.fullID,
            () => this(attribute),
            (newValue) => this(attribute) = newValue.asInstanceOf[v],
            Some(attribute.pickleable),
            Some(attribute.inspectable),
          )
        )
    }
  end reflectProperties
end AttributeBag
