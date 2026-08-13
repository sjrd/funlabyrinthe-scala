package com.funlabyrinthe.core

import scala.annotation.tailrec

import scala.quoted.*

import com.funlabyrinthe.core.pickling.Pickleable

final case class UniqueCallSite(fullID: String) derives Pickleable

object UniqueCallSite {
  given UniqueCallSiteOrdering: Ordering[UniqueCallSite] {
    def compare(x: UniqueCallSite, y: UniqueCallSite): Int = x.fullID.compareTo(y.fullID)
  }

  // the DummyImplicit is require to make this a def
  inline given materializeUniqueCallSite: UniqueCallSite =
    UniqueCallSite(materializeFullID())

  private inline def materializeFullID(): String =
    ${ materializeFullIDImpl() }

  private def materializeFullIDImpl(using Quotes)(): Expr[String] = {
    import quotes.reflect.*

    findTopLevelClassName() match
      case Some(name) =>
        val pos = Position.ofMacroExpansion
        Literal(StringConstant(s"$name.${pos.startLine}.${pos.startColumn}")).asExprOf[String]
      case None =>
        report.errorAndAbort(
          s"Cannot automatically materialize a UniqueCallSite here. "
            + "This should not happen. Please file a bug report."
        )
  }

  private def findTopLevelClassName()(using Quotes): Option[String] = {
    import quotes.reflect.*

    @tailrec
    def loop(sym: Symbol): Option[String] =
      if !sym.exists then None // hopefully we don't get there
      else if sym.isClassDef && sym.owner.isPackageDef then Some(sym.fullName)
      else loop(sym.maybeOwner)

    loop(Symbol.spliceOwner)
  }
}
