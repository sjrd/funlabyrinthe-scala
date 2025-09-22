package org.funlabyrinthe.compilerplugin

import scala.compiletime.uninitialized

import dotty.tools.dotc.core.Constants.*
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
import dotty.tools.dotc.util.Store
import dotty.tools.dotc.report
import dotty.tools.dotc.ast.TreeTypeMap

final class FunLabyPhase(fndefn: FunLabyDefinitions) extends PluginPhase with InfoTransformer:
  import FunLabyPhase.*
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

  private var MyState: Store.Location[MyState] = uninitialized
  private def myState(using Context) = ctx.store(MyState)

  override def initContext(ctx: FreshContext): Unit =
    MyState = ctx.addLocation[MyState]()

  extension (cls: Symbol)
    private def strictDerivesFrom(base: Symbol)(using Context): Boolean =
      cls != base && cls.derivesFrom(base)

  override def infoMayChange(sym: Symbol)(using Context): Boolean =
    sym.isClass
      && (sym.strictDerivesFrom(fndefn.ReflectableClass) || sym.strictDerivesFrom(fndefn.ModuleClass))

  def transformInfo(tp: Type, sym: Symbol)(using Context): Type = tp match
    case tp @ ClassInfo(_, cls, _, decls, _) =>
      def forceOverrideOf(origDefs: TermSymbol*): ClassInfo =
        val decls1 = decls.cloneScope
        var changed = false

        for origDef <- origDefs do
          val hasExisting = decls.denotsNamed(origDef.name).filterWithPredicate { d =>
            d.symbol.is(Method) && d.info.matches(origDef.info)
          }.exists

          if !hasExisting then
            val overriding = newSymbol(
              cls,
              origDef.name,
              Method | Override | (origDef.flags & Protected),
              cls.thisType.memberInfo(origDef),
              coord = cls.coord,
            )
            decls1.enter(overriding)
            changed = true
        end for

        if changed then
          tp.derivedClassInfo(decls = decls1)
        else
          tp
      end forceOverrideOf

      if cls.strictDerivesFrom(fndefn.ReflectableClass) then
        forceOverrideOf(fndefn.reflectPropertiesMethod)
      else if cls.strictDerivesFrom(fndefn.ModuleClass) then
        forceOverrideOf(fndefn.createComponentsMethod)
      else
        tp

    case _ =>
      tp
  end transformInfo

  override def prepareForPackageDef(tree: PackageDef)(using Context): Context =
    val state = new MyState()
    val packageObjectDef = tree.stats.collectFirst {
      case cd @ TypeDef(name, rhs) if cd.symbol.isPackageObject => cd
    }
    packageObjectDef match
      case None =>
        ()
      case Some(packageObjectDef) =>
        val template = packageObjectDef.rhs.asInstanceOf[Template]
        val funlabyDefinitions = template.body.collect {
          case dd: DefDef if dd.symbol.hasAnnotation(fndefn.DefinitionAnnotClass) => dd
        }
        if funlabyDefinitions.nonEmpty then
          val module = packageObjectDef.symbol.owner.info.decls.find { sym =>
            sym.is(ModuleClass) && sym.derivesFrom(fndefn.ModuleClass)
              && sym.source == ctx.compilationUnit.source
          }
          if !module.exists then
            report.error(
              s"Found at least one @definition, but there was no Module object. "
                + s"Add an `object ${ctx.compilationUnit.source.name.stripSuffix(".scala")} extends Module`.",
              funlabyDefinitions.head.sourcePos
            )
          else
            state.componentDefinitions = funlabyDefinitions
            for sym <- funlabyDefinitions.map(_.symbol) do
              state.componentDefinitionSyms(sym) = ()
            state.thisModuleClass = Some(module.asClass)
    end match
    ctx.fresh.updateStore(MyState, state)
  end prepareForPackageDef

  override def transformDefDef(tree: DefDef)(using Context): Tree =
    val sym = tree.symbol
    if sym.hasAnnotation(fndefn.DefinitionAnnotClass) then
      if myState.componentDefinitionSyms.contains(sym) then
        if myState.thisModuleClass.isDefined then
          tree.paramss match
            case List(List(universeParam: ValDef))
                if universeParam.symbol.is(GivenVal) && (universeParam.symbol.info =:= fndefn.UniverseType) =>
              val resultType = sym.info.finalResultType
              if resultType.derivesFrom(fndefn.ComponentClass) then
                val componentName = sym.name.toString()
                val newRhs =
                  ref(universeParam.symbol)
                    .select(fndefn.findAnyTopComponentByIDMethod)
                    .appliedTo(ref(myState.thisModuleClass.get.sourceModule), Literal(Constant(componentName)))
                    .asInstance(resultType)
                cpy.DefDef(tree)(rhs = newRhs.withSpan(tree.rhs.span))
              else
                report.error(s"Illegal result type for @definition method: ${resultType.show}", tree.sourcePos)
                tree
            case _ =>
              report.error(s"Illegal signature for @definition method; expected a single (using Universe) argument.", tree.sourcePos)
              tree
        else
          // Silently ignore; an error was already reported by prepareForPackageDef
          tree
      else
        report.error(s"Illegal @definition method; all such methods must be defined at the top-level of the file.", tree.sourcePos)
        tree
    else if sym.owner.strictDerivesFrom(fndefn.ModuleClass) && sym.matches(fndefn.createComponentsMethod) then
      val List(Nil, List(universeParam)) = tree.paramss: @unchecked
      cpy.DefDef(tree)(rhs = patchCreateComponentsMethod(sym, universeParam.symbol, tree.rhs))
    else
      tree
  end transformDefDef

  override def transformTemplate(tree: Template)(using Context): Tree =
    val cls = ctx.owner.asClass
    if cls.strictDerivesFrom(fndefn.ReflectableClass) then
      val myReflectPropertiesMethod = fndefn.reflectPropertiesMethod.overridingSymbol(cls)
      assert(myReflectPropertiesMethod.exists, s"transformInfo should have added `reflectProperties` in $cls")

      val hasExisting = tree.body.exists {
        case dd: DefDef => dd.symbol == myReflectPropertiesMethod
        case _          => false
      }
      if hasExisting then
        tree
      else
        val newMethod = synthesizeReflectPropertiesMethod(
            cls, myReflectPropertiesMethod, tree.span)
        cpy.Template(tree)(tree.constr, tree.parents, tree.derived, tree.self, tree.body :+ newMethod)
    else if cls.strictDerivesFrom(fndefn.ModuleClass) then
      val myCreateComponentsMethod = fndefn.createComponentsMethod.overridingSymbol(cls)
      assert(myCreateComponentsMethod.exists, s"transformInfo should have added `createComponents` in $cls")

      val hasExisting = tree.body.exists {
        case dd: DefDef => dd.symbol == myCreateComponentsMethod
        case _          => false
      }
      if hasExisting then
        tree // the patch was done in transformDefDef
      else
        val newMethod = DefDef(myCreateComponentsMethod.asTerm, { paramRefss =>
          val List(Nil, List(universeRef)) = paramRefss: @unchecked
          patchCreateComponentsMethod(myCreateComponentsMethod, universeRef.symbol, unitLiteral)
        }).withSpan(tree.span.toSynthetic)
        cpy.Template(tree)(body = tree.body :+ newMethod)
    else
      tree
  end transformTemplate

  private def synthesizeReflectPropertiesMethod(
    cls: ClassSymbol,
    sym: Symbol,
    span: Span,
  )(using Context): DefDef =
    // If we move our phase after PostTyper, uncomment the following line
    //ctx.compilationUnit.needsInlining = true

    DefDef(sym.asTerm, { paramRefss =>
      val List(List(registerDataRef)) = paramRefss: @unchecked
      Block(
        List(
          Super(This(cls), tpnme.EMPTY).select(sym.nextOverriddenSymbol).appliedTo(registerDataRef),
          ref(fndefn.autoReflectProperties).appliedToType(cls.thisType).appliedTo(This(cls), registerDataRef),
        ),
        unitLiteral,
      )
    }).withSpan(span.toSynthetic)
  end synthesizeReflectPropertiesMethod

  private def patchCreateComponentsMethod(sym: Symbol, universeParamSym: Symbol, existingRhs: Tree)(using Context): Tree =
    if !myState.thisModuleClass.contains(sym.owner) then
      existingRhs
    else
      val transformedStats = myState.componentDefinitions.map { defDef =>
        val List(List(oldUniverseParam)) = defDef.paramss: @unchecked
        val newRhs = TreeTypeMap(
          substFrom = List(oldUniverseParam.symbol),
          substTo = List(universeParamSym),
          oldOwners = List(defDef.symbol),
          newOwners = List(sym),
        ).apply(defDef.rhs)
        val valSym = newSymbol(
          owner = sym,
          name = defDef.name,
          EmptyFlags,
          info = newRhs.tpe,
          coord = defDef.symbol.coord,
        )
        ValDef(valSym, newRhs, inferred = true).withSpan(defDef.span)
      }
      Block(transformedStats, existingRhs)
  end patchCreateComponentsMethod
end FunLabyPhase

object FunLabyPhase:
  private final class MyState:
    var componentDefinitions: List[tpd.DefDef] = Nil
    val componentDefinitionSyms = MutableSymbolMap[Unit]()
    var thisModuleClass: Option[ClassSymbol] = None
  end MyState
end FunLabyPhase
