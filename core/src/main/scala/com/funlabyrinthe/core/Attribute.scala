package com.funlabyrinthe.core

import scala.quoted.*

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final class Attribute[T] private[core] (
  val name: String,
  val defaultValue: T,
  val pickleable: Pickleable[T],
  val inspectable: Inspectable[T],
):
  override def toString(): String = name
end Attribute

object Attribute:
  private[core] inline def create[T](defaultValue: T)(using pickleable: Pickleable[T], inspectable: Inspectable[T]): Attribute[T] =
    ${ createImpl('defaultValue)('pickleable, 'inspectable) }

  private[core] def createImpl[T](using Quotes, Type[T])(defaultValue: Expr[T])(pickleable: Expr[Pickleable[T]], inspectable: Expr[Inspectable[T]]): Expr[Attribute[T]] =
    import quotes.reflect.*

    val nameExpr = ComponentInit.materializeIDImpl("an attribute name")
    '{ createInternal[T]($nameExpr, $defaultValue, $pickleable, $inspectable) }
  end createImpl

  private[core] def createInternal[T](
    name: String,
    defaultValue: T,
    pickleable: Pickleable[T],
    inspectable: Inspectable[T],
  ): Attribute[T] =
    new Attribute[T](name, defaultValue, pickleable, inspectable)
  end createInternal
end Attribute
