package com.funlabyrinthe.core

import scala.quoted.*

final class ComponentInit(val universe: Universe, val id: String, val owner: ComponentOwner)

object ComponentInit:
  def transient(universe: Universe): ComponentInit =
    ComponentInit(universe, "", TransientOwner)

  private[core] def materializeIDImpl(using Quotes)(what: String): Expr[String] =
    import quotes.reflect.*

    findSpliceOwnerName() match
      case Some(name) =>
        Literal(StringConstant(name)).asExprOf[String]
      case None =>
        report.errorAndAbort(s"Cannot automatically materialize $what here. Did you assign to a `val`?")
  end materializeIDImpl

  private def findSpliceOwnerName()(using Quotes): Option[String] =
    import quotes.reflect.*

    def loop(owner: Symbol): Option[String] =
      if !owner.exists then None
      else if owner.isValDef then Some(owner.name)
      else loop(owner.maybeOwner)

    loop(Symbol.spliceOwner.maybeOwner)
  end findSpliceOwnerName
end ComponentInit
