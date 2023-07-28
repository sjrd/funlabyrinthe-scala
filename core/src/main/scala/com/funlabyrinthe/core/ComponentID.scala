package com.funlabyrinthe.core

import scala.quoted.*

class ComponentID(val id: String) extends AnyVal {
  override def toString() = id
}

object ComponentID {
  val Transient: ComponentID = ComponentID("")

  def apply(id: String): ComponentID = new ComponentID(id)

  def materializeIDImpl(using Quotes): Expr[ComponentID] =
    import quotes.reflect.*

    def findGoodOwner(owner: Symbol): Option[Symbol] =
      if !owner.exists then None
      else if owner.isValDef then Some(owner)
      else findGoodOwner(owner.maybeOwner)

    findGoodOwner(Symbol.spliceOwner.maybeOwner) match
      case Some(owner) =>
        val nameExpr = Literal(StringConstant(owner.name)).asExprOf[String]
        '{ new ComponentID($nameExpr) }
      case None =>
        report.errorAndAbort("Cannot automatically materialize a Component ID here. Did you assign to a `val`?")
  end materializeIDImpl
}
