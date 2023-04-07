package com.funlabyrinthe.core.reflect

import scala.quoted.*

final class InspectedTypeable[T] private (val inspectedType: InspectedType)

object InspectedTypeable:
  private def make[T](inspectedType: InspectedType): InspectedTypeable[T] =
    new InspectedTypeable[T](inspectedType)

  inline given materialize[T]: InspectedTypeable[T] = ${ materializeImpl[T] }

  private def materializeImpl[T](using Quotes, Type[T]): Expr[InspectedTypeable[T]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    Reflector.toInspectedType(tpe) match
      case Some(inspectedTypeExpr) =>
        '{ make[T]($inspectedTypeExpr) }
      case None =>
        report.errorAndAbort(s"Cannot represent the type ${tpe.show} as an InspectedType")
  end materializeImpl
end InspectedTypeable
