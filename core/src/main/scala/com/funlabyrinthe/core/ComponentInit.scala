package com.funlabyrinthe.core

import scala.annotation.tailrec

import scala.quoted.*

final class ComponentInit(val universe: Universe, val id: String, val owner: ComponentOwner)

object ComponentInit:
  inline given materializeComponentInitForDefinition(using universe: Universe): ComponentInit =
    ComponentInit(universe, materializeID("a component ID"), checkAutoDefinitionOwner)

  private[core] inline def checkAutoDefinitionOwner: ComponentOwner =
    ${ checkAutoDefinitionOwnerImpl }

  private[core] def checkAutoDefinitionOwnerImpl(using Quotes): Expr[ComponentOwner] =
    import quotes.reflect.*

    @tailrec
    def enclosingClass(owner: Symbol): Option[Symbol] =
      if !owner.exists then None
      else if owner.isClassDef then Some(owner)
      else enclosingClass(owner.maybeOwner)

    enclosingClass(Symbol.spliceOwner) match
      case Some(cls) =>
        val clsThis = This(cls)
        if clsThis.tpe <:< TypeRepr.of[Module] then
          '{ ComponentOwner.Module(${clsThis.asExprOf[Module]}) }
        else
          report.errorAndAbort(
            "Cannot automatically find a Module owner for this definition. "
              + "Did you annotate it with `@definition`?"
          )
      case None =>
        report.errorAndAbort(
          "Cannot automatically find a Module owner for this definition. "
            + "Did you annotate it with `@definition`?"
        )
  end checkAutoDefinitionOwnerImpl

  private[core] inline def materializeID(inline what: String): String =
    ${ materializeIDImpl('what) }

  private[core] def materializeIDImpl(using Quotes)(what: Expr[String]): Expr[String] =
    import quotes.reflect.*

    val whatStr = what.valueOrAbort

    findSpliceOwnerName() match
      case Some(name) =>
        Literal(StringConstant(name)).asExprOf[String]
      case None =>
        report.errorAndAbort(
          s"Cannot automatically materialize $whatStr here. "
            + "Did you assign to a `val` or to an `@definition def`?"
        )
  end materializeIDImpl

  private def findSpliceOwnerName()(using Quotes): Option[String] =
    import quotes.reflect.*

    @tailrec
    def loop(owner: Symbol): Option[String] =
      if !owner.exists then None
      else if owner.isValDef then Some(owner.name)
      else loop(owner.maybeOwner)

    loop(Symbol.spliceOwner.maybeOwner)
  end findSpliceOwnerName
end ComponentInit
