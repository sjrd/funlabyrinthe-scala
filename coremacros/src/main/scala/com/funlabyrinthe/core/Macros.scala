package com.funlabyrinthe.core

import language.experimental.macros

import scala.reflect.macros.Context

object Macros {
  def materializeID_impl(c: Context) = {
    val strID = definingValName(c,
        "You must either assign the result directly to `val`, "+
        "or specify explicitly the ID of the component.")
    val idLiteral = c.literal(strID)
    c.universe.reify {
      new ComponentID(idLiteral.splice)
    }
  }

  private def definingValName(c: Context,
      invalidEnclosingTreeMessage: => String): String = {
    import c.universe.{ Apply => ApplyTree, _ }

    def processName(n: Name): String =
      n.decoded.trim // trim is not strictly correct, but macros don't expose the API necessary

    def enclosingVal(trees: List[c.Tree]): String = {
      trees match {
        case vd @ ValDef(_, name, _, _) :: ts =>
          processName(name)

        case (_: ApplyTree | _: Select | _: TypeApply) :: xs =>
          enclosingVal(xs)

        // lazy val x: X = <methodName> has this form for some reason (only when the explicit type is present, though)
        case Block(_, _) :: DefDef(mods, name, _, _, _, _) :: xs
        if mods.hasFlag(Flag.LAZY) =>
          processName(name)

        // val x = new Trait { ... } has this form
        case Block(_, _) :: Block(_, _) :: ClassDef(_, anonName, Nil, _)
            :: Block(_, ApplyTree(Select(New(Ident(anonName2)), _), Nil)) :: xs
        if anonName == anonName2 =>
          enclosingVal(xs)

        case _ =>
          c.error(c.enclosingPosition, invalidEnclosingTreeMessage)
          "<error>"
      }
    }

    enclosingVal(enclosingTrees(c).toList)
  }

  private def enclosingTrees(c: Context): Seq[c.Tree] =
    c.asInstanceOf[reflect.macros.runtime.Context].callsiteTyper.context.enclosingContextChain.map(_.tree.asInstanceOf[c.Tree])
}
