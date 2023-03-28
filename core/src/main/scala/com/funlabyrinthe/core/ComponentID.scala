package com.funlabyrinthe.core

import scala.quoted.*

class ComponentID(val id: String) extends AnyVal {
  override def toString() = id
}

object ComponentID {
  def apply(id: String): ComponentID = new ComponentID(id)

  implicit inline def materializeID: ComponentID = ${ materializeIDImpl }

  def materializeIDImpl(using Quotes): Expr[ComponentID] =
    import quotes.reflect.*

    val name = Symbol.spliceOwner.name
    val nameExpr = Literal(StringConstant(name)).asExprOf[String]
    '{ new ComponentID($nameExpr) }
  end materializeIDImpl
}
