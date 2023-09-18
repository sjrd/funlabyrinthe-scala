package com.funlabyrinthe.core.reflect

import scala.collection.mutable

import scala.quoted.*

trait Reflector[T]:
  def reflectProperties(instance: T): List[InspectedData]
end Reflector

object Reflector:
  inline def derived[T]: Reflector[T] = ${ derivedImpl[T] }

  private def derivedImpl[T](using Quotes, Type[T]): Expr[Reflector[T]] =
    import quotes.reflect.*

    val tp = TypeRepr.of[T]
    val tpCls = tp.classSymbol.getOrElse {
      report.errorAndAbort(s"Cannot derive a Reflector for $tp because it is not a class")
    }
    if !isMonomorphic(tpCls) then
      report.errorAndAbort(s"Cannot derive a Reflector for $tp because it has type parameters")
    if !isStaticOwner(tpCls.owner) then
      report.errorAndAbort(s"Cannot derive a Reflector for $tp because it is not static")

    val tpSym = tp.typeSymbol
    val propertiesExprs: List[Expr[ReflectableProp[T]]] = deriveProperties(tp, tpCls)
    val propertiesExpr: Expr[List[ReflectableProp[T]]] = Expr.ofList(propertiesExprs)
    '{ new ReflectorImpl[T]($propertiesExpr) }
  end derivedImpl

  private def isStaticOwner(using Quotes)(sym: quotes.reflect.Symbol): Boolean =
    import quotes.reflect.*

    if sym.isPackageDef then true
    else if sym.flags.is(Flags.Module) then isStaticOwner(sym.owner)
    else false
  end isStaticOwner

  private def deriveProperties[T](using Quotes, Type[T])(
    clsType: quotes.reflect.TypeRepr, cls: quotes.reflect.Symbol
  ): List[Expr[ReflectableProp[T]]] =
    import quotes.reflect.*

    val foundNames = mutable.Set.empty[String]
    val result = mutable.ListBuffer.empty[Expr[ReflectableProp[T]]]

    val transientAnnotClass = TypeRepr.of[scala.transient].classSymbol.get

    // We don't want Any.##
    foundNames += "##"

    for
      member <- cls.fieldMembers ::: cls.methodMembers
      if !member.flags.is(Flags.Protected) && !member.flags.is(Flags.Private) && !member.privateWithin.isDefined
      if member.paramSymss.isEmpty
      if !member.hasAnnotation(transientAnnotClass)
      if foundNames.add(member.name)
    do
      val tpe = clsType.memberType(member).widenByName

      toInspectedType(tpe) match
        case None =>
          // do nothing

        case Some(inspectedTypeExpr) =>
          val nameExpr: Expr[String] = Expr(member.name)

          val getterExpr: Expr[T => Any] = '{
            { (instance: T) =>
              ${ Select(('instance).asTerm, member).asExpr }
            }
          }

          val optSetterMember = cls.methodMember(member.name + "_=").find { meth =>
            !meth.flags.is(Flags.Protected)
              && {
                clsType.memberType(meth) match
                  case MethodType(_, List(argType), resultType) =>
                    tpe =:= argType && resultType =:= TypeRepr.of[Unit]
              }
          }

          val reflectablePropExpr: Expr[ReflectableProp[T]] = optSetterMember match
            case None =>
              '{
                new ReflectableProp.ReadOnly[T](
                  $nameExpr,
                  $inspectedTypeExpr,
                  $getterExpr,
                )
              }

            case Some(setterMember) =>
              tpe.asType match
                case '[u] =>
                  val setterExpr: Expr[(T, Any) => Unit] = '{
                    { (instance: T, value: Any) =>
                      val valueAsU: u = value.asInstanceOf[u]
                      ${ Apply(Select(('instance).asTerm, setterMember), List(('valueAsU).asTerm)).asExpr }
                    }
                  }

                  '{
                    new ReflectableProp.ReadWrite[T](
                      $nameExpr,
                      $inspectedTypeExpr,
                      $getterExpr,
                      $setterExpr,
                    )
                  }
          end reflectablePropExpr

          result += reflectablePropExpr
    end for

    result.toList
  end deriveProperties

  private[reflect] def toInspectedType(using Quotes)(tpe: quotes.reflect.TypeRepr): Option[Expr[InspectedType]] =
    import quotes.reflect.*

    tpe.classSymbol match
      case Some(cls) =>
        if cls == TypeRepr.of[List[Any]].typeSymbol then
          val List(arg) = tpe.typeArgs
          toInspectedType(arg).map { elemType =>
            '{ InspectedType.listOf($elemType) }
          }
        else if isMonomorphic(cls) then
          val result: Expr[InspectedType] =
            if tpe =:= TypeRepr.of[Any] then '{ InspectedType.Any }
            else if tpe =:= TypeRepr.of[AnyRef] then '{ InspectedType.AnyRef }
            else if tpe =:= TypeRepr.of[String] then '{ InspectedType.String }
            else if tpe =:= TypeRepr.of[Boolean] then '{ InspectedType.Boolean }
            else if tpe =:= TypeRepr.of[Char] then '{ InspectedType.Char }
            else if tpe =:= TypeRepr.of[Byte] then '{ InspectedType.Byte }
            else if tpe =:= TypeRepr.of[Short] then '{ InspectedType.Short }
            else if tpe =:= TypeRepr.of[Int] then '{ InspectedType.Int }
            else if tpe =:= TypeRepr.of[Long] then '{ InspectedType.Long }
            else if tpe =:= TypeRepr.of[Float] then '{ InspectedType.Float }
            else if tpe =:= TypeRepr.of[Double] then '{ InspectedType.Double }
            else
              tpe.asType match
                case '[t] =>
                  val classOfConstant = Literal(ClassOfConstant(tpe)).asExprOf[Class[t]]
                  if cls.flags.is(Flags.Enum) then
                    val valueExprs = cls.children.map(sym => Ident(sym.termRef).asExprOf[t])
                    val valuesExpr = Expr.ofList(valueExprs)
                    '{ InspectedType.enumClass[t]($classOfConstant, $valuesExpr) }
                  else
                    '{ InspectedType.monoClass($classOfConstant) }
          end result
          Some(result)
        else
          None

      case None =>
        None
  end toInspectedType

  def isMonomorphic(using Quotes)(cls: quotes.reflect.Symbol): Boolean =
    import quotes.reflect.*
    cls.typeRef <:< TypeRepr.of[Any]
end Reflector
