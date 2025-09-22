package org.funlabyrinthe.compilerplugin

import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.*

final class FunLabyDefinitions(using initCtx: Context):
  private lazy val ReflectableClassRef = requiredClassRef("com.funlabyrinthe.core.reflect.Reflectable")
  def ReflectableClass(using Context): ClassSymbol = ReflectableClassRef.symbol.asClass

  private lazy val ComponentClassRef = requiredClassRef("com.funlabyrinthe.core.Component")
  def ComponentClass(using Context): ClassSymbol = ComponentClassRef.symbol.asClass

  private lazy val AttributeClassRef = requiredClassRef("com.funlabyrinthe.core.Attribute")
  def AttributeClass(using Context): ClassSymbol = AttributeClassRef.symbol.asClass

  private lazy val ModuleClassRef = requiredClassRef("com.funlabyrinthe.core.Module")
  def ModuleClass(using Context): ClassSymbol = ModuleClassRef.symbol.asClass

  private lazy val UniverseClassRef = requiredClassRef("com.funlabyrinthe.core.Universe")
  def UniverseClass(using Context): ClassSymbol = UniverseClassRef.symbol.asClass

  def UniverseType(using Context): Type = UniverseClassRef

  private lazy val DefinitionAnnotClassRef = requiredClassRef("com.funlabyrinthe.core.definition")
  def DefinitionAnnotClass(using Context): ClassSymbol = DefinitionAnnotClassRef.symbol.asClass

  val reflectPropertiesName = termName("reflectProperties")

  private lazy val reflectPropertiesMethodRef = ReflectableClass.requiredMethodRef("reflectProperties")
  def reflectPropertiesMethod(using Context): TermSymbol = reflectPropertiesMethodRef.symbol.asTerm

  private lazy val autoReflectPropertiesRef = ReflectableClass.companionModule.moduleClass.requiredMethodRef("autoReflectProperties")
  def autoReflectProperties(using Context): TermSymbol = autoReflectPropertiesRef.symbol.asTerm

  private lazy val createAttributeMethodRef = AttributeClass.companionModule.moduleClass.requiredMethodRef("create")
  def createAttributeMethod(using Context): TermSymbol = createAttributeMethodRef.symbol.asTerm

  private lazy val preInitializeMethodRef = ModuleClass.requiredMethodRef("preInitialize")
  def preInitializeMethod(using Context): TermSymbol = preInitializeMethodRef.symbol.asTerm

  private lazy val createComponentsMethodRef = ModuleClass.requiredMethodRef("createComponents")
  def createComponentsMethod(using Context): TermSymbol = createComponentsMethodRef.symbol.asTerm

  private lazy val attributeByIDMethodRef = UniverseClass.requiredMethodRef("attributeByID")
  def attributeByIDMethod(using Context): TermSymbol = attributeByIDMethodRef.symbol.asTerm

  private lazy val findAnyTopComponentByIDMethodRef = UniverseClass.requiredMethodRef("findAnyTopComponentByID")
  def findAnyTopComponentByIDMethod(using Context): TermSymbol = findAnyTopComponentByIDMethodRef.symbol.asTerm
end FunLabyDefinitions
