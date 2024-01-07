package com.funlabyrinthe.core

import scala.quoted.*

class ComponentID(val id: String) extends AnyVal {
  override def toString() = id
}

object ComponentID {
  val Transient: ComponentID = ComponentID("")

  def apply(id: String): ComponentID = new ComponentID(id)

  private[core] def materializeIDImpl(using Quotes): Expr[ComponentID] =
    import quotes.reflect.*

    findSpliceOwnerName() match
      case Some(name) =>
        val nameExpr = Literal(StringConstant(name)).asExprOf[String]
        '{ new ComponentID($nameExpr) }
      case None =>
        report.errorAndAbort("Cannot automatically materialize a Component ID here. Did you assign to a `val`?")
  end materializeIDImpl

  private[core] def findSpliceOwnerName()(using Quotes): Option[String] =
    import quotes.reflect.*

    def loop(owner: Symbol): Option[String] =
      if !owner.exists then None
      else if owner.isValDef then Some(owner.name)
      else loop(owner.maybeOwner)

    loop(Symbol.spliceOwner.maybeOwner)
  end findSpliceOwnerName
}
