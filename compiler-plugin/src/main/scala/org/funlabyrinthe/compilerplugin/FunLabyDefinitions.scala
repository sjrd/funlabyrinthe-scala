package org.funlabyrinthe.compilerplugin

import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Symbols.*

final class FunLabyDefinitions(using initCtx: Context):
  private lazy val ReflectableClassRef = requiredClassRef("com.funlabyrinthe.core.reflect.Reflectable")
  def ReflectableClass(using Context): ClassSymbol = ReflectableClassRef.symbol.asClass

  val reflectPropertiesName = termName("reflectProperties")

  private lazy val reflectPropertiesMethodRef = ReflectableClass.requiredMethodRef("reflectProperties")
  def reflectPropertiesMethod(using Context): TermSymbol = reflectPropertiesMethodRef.symbol.asTerm

  private lazy val autoReflectPropertiesRef = ReflectableClass.companionModule.moduleClass.requiredMethodRef("autoReflectProperties")
  def autoReflectProperties(using Context): TermSymbol = autoReflectPropertiesRef.symbol.asTerm
end FunLabyDefinitions
