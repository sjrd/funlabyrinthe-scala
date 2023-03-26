package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

case class FieldIR(name: String, tpe: Type, param: Option[TermSymbol],
    accessor: Option[MethodSymbol]) {

  def field = accessor.map(_.accessed.asTerm)
  def getter = accessor.map(_.getter).flatMap(
      sym => if (sym != NoSymbol) Some(sym) else None)
  def setter = accessor.map(_.setter).flatMap(
      sym => if (sym != NoSymbol) Some(sym) else None)
  def isParam = param.map(_.owner.name == termNames.CONSTRUCTOR).getOrElse(false)
  def isPublic = accessor.map(_.isPublic).getOrElse(false)

  // this part is interesting to picklers
  def hasGetter = getter.isDefined

  // this part is interesting to unpicklers
  def hasSetter = setter.isDefined
  def isErasedParam = isParam && accessor.isEmpty // TODO: this should somehow communicate with the constructors phase!
  def isReifiedParam = isParam && accessor.nonEmpty
  def isNonParam = !isParam
}
