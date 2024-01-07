package com.funlabyrinthe.core

import scala.quoted.*

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.reflect.InspectedType

final class Attribute[T] private[core] (
  val name: String,
  val defaultValue: T,
  val pickleable: Pickleable[T],
  val inspectedType: InspectedType
):
  override def toString(): String = name
end Attribute

object Attribute:
  private[core] inline def create[T](defaultValue: T)(using pickleable: Pickleable[T]): Attribute[T] =
    ${ createImpl('defaultValue)('pickleable) }

  private[core] def createImpl[T](using Quotes, Type[T])(defaultValue: Expr[T])(pickleable: Expr[Pickleable[T]]): Expr[Attribute[T]] =
    import quotes.reflect.*

    val nameExpr = ComponentID.findSpliceOwnerName() match
      case Some(name) =>
        Literal(StringConstant(name)).asExprOf[String]
      case None =>
        report.errorAndAbort("Cannot automatically derive an attribute name here. Did you assign to a `val`?")

    val tpe = TypeRepr.of[T]
    val inspectedTypeExpr = Reflector.toInspectedType(tpe) match
      case Some(expr) =>
        expr
      case None =>
        report.errorAndAbort(s"Invalid type for an attribute: ${tpe.show}")

    '{ createInternal[T]($nameExpr, $defaultValue, $pickleable, $inspectedTypeExpr) }
  end createImpl

  private[core] def createInternal[T](
    name: String,
    defaultValue: T,
    pickleable: Pickleable[T],
    inspectedType: InspectedType
  ): Attribute[T] =
    new Attribute[T](name, defaultValue, pickleable, inspectedType)
  end createInternal
end Attribute
