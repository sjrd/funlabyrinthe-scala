package com.funlabyrinthe.core.reflect

import scala.collection.immutable.TreeSet
import scala.collection.mutable

import scala.quoted.*

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.{InPlacePickleable, Pickleable}
import com.funlabyrinthe.core.{Component, Universe, noinspect}

private[reflect] object Reflector:
  def autoReflectPropertiesImpl[T <: Reflectable & Singleton](using Quotes, Type[T])(
      instance: Expr[T], registerData: Expr[InspectedData => Unit]): Expr[Unit] =
    import quotes.reflect.*

    val thisType: ThisType = TypeRepr.of[T] match
      case tp: ThisType =>
        tp
      case tp =>
        report.errorAndAbort(s"The type argument to autoReflectPropertiesImpl must be `this.type`; got ${tp.show}")

    val tpCls = thisType.classSymbol.getOrElse {
      report.errorAndAbort(s"Unexpected error: the this type ${thisType.show} does not have a class symbol")
    }

    val propertiesExprs: List[Expr[InspectedData]] = deriveProperties(instance, thisType, tpCls)

    val registrations: List[Statement] =
      for propertyExpr <- propertiesExprs yield
        ('{ $registerData($propertyExpr) }).asTerm

    Block(registrations, Literal(UnitConstant())).asExprOf[Unit]
  end autoReflectPropertiesImpl

  private def deriveProperties[T](using Quotes, Type[T])(
    instance: Expr[T], thisType: quotes.reflect.TypeRepr, cls: quotes.reflect.Symbol
  ): List[Expr[InspectedData]] =
    import quotes.reflect.*

    val foundNames = mutable.Set.empty[String]
    val result = mutable.ListBuffer.empty[Expr[InspectedData]]

    val transientAnnotClass = TypeRepr.of[scala.transient].classSymbol.get
    val noinspectAnnotClass = TypeRepr.of[noinspect].classSymbol.get

    // We don't want Any.##
    foundNames += "##"

    for
      member <- cls.declaredFields ::: cls.declaredMethods
      if !member.flags.is(Flags.Protected) && !member.flags.is(Flags.Private) && !member.privateWithin.isDefined
      if member.paramSymss.isEmpty
      if !member.name.contains("$default$")
      if foundNames.add(member.name)
    do
      val shouldPickle = !member.hasAnnotation(transientAnnotClass)
      val shouldInspect = !member.hasAnnotation(noinspectAnnotClass)

      val tpe = thisType.memberType(member).widenByName
      tpe.asType match
        case '[u] if shouldPickle || shouldInspect =>
          val nameExpr: Expr[String] = Expr(member.name)

          val getterExpr: Expr[() => u] = '{
            { () =>
              ${ Select(This(cls), member).asExprOf[u] }
            }
          }

          val optSetterMember = cls.methodMember(member.name + "_=").find { meth =>
            !meth.flags.is(Flags.Protected)
              && {
                thisType.memberType(meth) match
                  case MethodType(_, List(argType), resultType) =>
                    tpe =:= argType && resultType =:= TypeRepr.of[Unit]
              }
          }

          val reflectablePropExpr: Expr[InspectedData] = optSetterMember match
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
                InspectedData.make[u](
                  $nameExpr,
                  $getterExpr,
                  ${exprOfOption(optInPlacePickleableExpr)},
                )
              }

            case Some(setterMember) =>
              val setterExpr: Expr[Any => Unit] = '{
                { (value: Any) =>
                  val valueAsU: u = value.asInstanceOf[u]
                  ${ Apply(Select(This(cls), setterMember), List(('valueAsU).asTerm)).asExpr }
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
                WritableInspectedData.make[u](
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
