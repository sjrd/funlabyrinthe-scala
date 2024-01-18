package com.funlabyrinthe.core.reflect

import scala.collection.immutable.TreeSet
import scala.collection.mutable

import scala.quoted.*

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.{InPlacePickleable, Pickleable}
import com.funlabyrinthe.core.{Component, Universe, noinspect}

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
    if tpCls.declaredTypes.exists(_.isTypeParam) then
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
    val noinspectAnnotClass = TypeRepr.of[noinspect].classSymbol.get

    // We don't want Any.##
    foundNames += "##"

    for
      member <- cls.fieldMembers ::: cls.methodMembers
      if !member.flags.is(Flags.Protected) && !member.flags.is(Flags.Private) && !member.privateWithin.isDefined
      if member.paramSymss.isEmpty
      if !member.name.contains("$default$")
      if foundNames.add(member.name)
    do
      val shouldPickle = !member.hasAnnotation(transientAnnotClass)
      val shouldInspect = !member.hasAnnotation(noinspectAnnotClass)

      val tpe = clsType.memberType(member).widenByName
      tpe.asType match
        case '[u] if shouldPickle || shouldInspect =>
          val nameExpr: Expr[String] = Expr(member.name)

          val getterExpr: Expr[T => u] = '{
            { (instance: T) =>
              ${ Select(('instance).asTerm, member).asExprOf[u] }
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
              val alwaysIgnore = tpe <:< TypeRepr.of[Component] || tpe <:< TypeRepr.of[Universe]

              val optInPlacePickleableExpr: Option[Expr[InPlacePickleable[u]]] =
                if !shouldPickle || alwaysIgnore then None
                else
                  val optInPlacePickleableExpr = Expr.summon[InPlacePickleable[u]]
                  if optInPlacePickleableExpr.isEmpty then
                    report.error(
                      s"The immutable property ${member.name} of type ${tpe.show} cannot be pickled in-place "
                        + s"because there is no available InPlacePickleable[${tpe.show}].\n"
                        + "If it does not need to be persisted between saves, annotate it with @transient."
                    )
                  optInPlacePickleableExpr
              end optInPlacePickleableExpr

              if shouldInspect && !alwaysIgnore then
                report.warning(
                  s"The immutable property ${member.name} of type ${tpe.show} cannot be inspected and "
                    + "will not appear in the editor.\n"
                    + "If it does not need to be inspected in the editor, annotate it @noinspect to silence this warning."
                )

              '{
                new ReflectableProp.ReadOnly[T, u](
                  $nameExpr,
                  $getterExpr,
                  ${exprOfOption(optInPlacePickleableExpr)},
                )
              }

            case Some(setterMember) =>
              val setterExpr: Expr[(T, Any) => Unit] = '{
                { (instance: T, value: Any) =>
                  val valueAsU: u = value.asInstanceOf[u]
                  ${ Apply(Select(('instance).asTerm, setterMember), List(('valueAsU).asTerm)).asExpr }
                }
              }

              val optPickleableExpr: Option[Expr[Pickleable[u]]] =
                if !shouldPickle then None
                else
                  val optPickleableExpr = Expr.summon[Pickleable[u]]
                  if optPickleableExpr.isEmpty then
                    report.error(
                      s"The mutable property ${member.name} of type ${tpe.show} cannot be pickled "
                        + s"because there is no available Pickleable[${tpe.show}].\n"
                        + "If it does not need to be persisted between saves, annotate it with @transient."
                    )
                  optPickleableExpr
              end optPickleableExpr

              val optInspectableExpr: Option[Expr[Inspectable[u]]] =
                if !shouldInspect then None
                else
                  val optInspectableExpr = Expr.summon[Inspectable[u]]
                  if optInspectableExpr.isEmpty then
                    report.warning(
                      s"The mutable property ${member.name} of type ${tpe.show} cannot be inspected and "
                        + "will not appear in the editor.\n"
                        + "If it does not need to be inspected in the editor, annotate it @noinspect to silence this warning."
                    )
                  optInspectableExpr
              end optInspectableExpr

              '{
                new ReflectableProp.ReadWrite[T, u](
                  $nameExpr,
                  $getterExpr,
                  $setterExpr,
                  ${exprOfOption(optPickleableExpr)},
                  ${exprOfOption(optInspectableExpr)},
                )
              }
          end reflectablePropExpr

          result += reflectablePropExpr

        case _ =>
          ()
      end match
    end for

    result.toList
  end deriveProperties

  private def exprOfOption[T](xs: Option[Expr[T]])(using Type[T])(using Quotes): Expr[Option[T]] =
    if (xs.isEmpty) '{ None }
    else '{ Some(${xs.get}) }
end Reflector
