package org.funlabyrinthe.compilerplugin

import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.DenotTransformers.InfoTransformer
import dotty.tools.dotc.core.Flags.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.*

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.tpd

import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.semanticdb.ExtractSemanticDB
import dotty.tools.dotc.typer.TyperPhase
import dotty.tools.dotc.util.Spans.Span

final class FunLabyPhase extends PluginPhase with InfoTransformer:
  import tpd.*

  val phaseName = "funlaby"
  override val description = "Support for FunLabyrinthe APIs"

  override val runsAfter = Set(
    TyperPhase.name,
    ExtractSemanticDB.phaseNamePrefix + ExtractSemanticDB.PhaseMode.ExtractSemanticInfo.toString(),
  )
  override val runsBefore = Set(
    dotty.tools.dotc.transform.PostTyper.name, // this is when super accessors are made
  )

  private def getReflectableClass()(using Context): ClassSymbol =
    requiredClass("com.funlabyrinthe.core.reflect.Reflectable")

  private val reflectPropertiesName = termName("reflectProperties")

  private def getReflectPropertiesMethod(reflectableClass: ClassSymbol)(using Context): TermSymbol =
    reflectableClass.requiredMethod(reflectPropertiesName)

  override def infoMayChange(sym: Symbol)(using Context): Boolean =
    sym.isClass && sym.isSubClass(getReflectableClass())

  def transformInfo(tp: Type, sym: Symbol)(using Context): Type = tp match
    case tp @ ClassInfo(_, cls, _, decls, _) =>
      val ReflectableClass = getReflectableClass()

      if cls != ReflectableClass && cls.isSubClass(getReflectableClass()) then
        val reflectPropertiesMethod = getReflectPropertiesMethod(ReflectableClass)

        val hasExisting = decls.denotsNamed(reflectPropertiesName).filterWithPredicate { d =>
          d.symbol.is(Method) && d.info.matches(reflectPropertiesMethod.info)
        }.exists

        if !hasExisting then
          val decls1 = decls.cloneScope

          val myReflectPropertiesMethod = newSymbol(
            cls,
            reflectPropertiesName,
            Method | Override | Protected,
            reflectPropertiesMethod.info.asSeenFrom(cls.thisType, ReflectableClass),
            coord = cls.coord,
          ).asTerm
          decls1.enter(myReflectPropertiesMethod)

          tp.derivedClassInfo(decls = decls1)
        else
          tp
      else
        tp

    case _ =>
      tp
  end transformInfo

  override def transformTemplate(tree: Template)(using Context): Tree =
    val cls = ctx.owner.asClass
    val ReflectableClass = getReflectableClass()
    if !cls.derivesFrom(ReflectableClass) || cls == ReflectableClass then
      tree
    else
      val baseReflectPropertiesMethod = getReflectPropertiesMethod(ReflectableClass)
      val myReflectPropertiesMethod = baseReflectPropertiesMethod.overridingSymbol(cls)
      assert(myReflectPropertiesMethod.exists, s"transformInfo should have added `reflectProperties` in $cls")

      val hasExisting = tree.body.exists {
        case dd: DefDef => dd.symbol == myReflectPropertiesMethod
        case _          => false
      }
      if hasExisting then
        tree
      else
        val newMethod = synthesizeReflectPropertiesMethod(
            cls, myReflectPropertiesMethod, baseReflectPropertiesMethod, tree.span)
        cpy.Template(tree)(tree.constr, tree.parents, tree.derived, tree.self, tree.body :+ newMethod)
  end transformTemplate

  private def synthesizeReflectPropertiesMethod(
    cls: ClassSymbol,
    sym: Symbol,
    baseReflectPropertiesMethod: TermSymbol,
    span: Span,
  )(using Context): DefDef =
    // If we move our phase after PostTyper, uncomment the following line
    //ctx.compilationUnit.needsInlining = true

    val autoReflectPropertiesSym = baseReflectPropertiesMethod.owner.companionModule.moduleClass
      .requiredMethod("autoReflectProperties")

    DefDef(sym.asTerm, { paramRefss =>
      val List(List(registerDataRef)) = paramRefss: @unchecked
      Block(
        List(
          Super(This(cls), tpnme.EMPTY).select(sym.nextOverriddenSymbol).appliedTo(registerDataRef),
          ref(autoReflectPropertiesSym).appliedToType(cls.thisType).appliedTo(This(cls), registerDataRef),
        ),
        unitLiteral,
      )
    }).withSpan(span.toSynthetic)
  end synthesizeReflectPropertiesMethod
end FunLabyPhase
