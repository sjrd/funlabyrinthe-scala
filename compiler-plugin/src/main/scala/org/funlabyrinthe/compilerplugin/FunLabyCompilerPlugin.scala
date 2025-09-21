package org.funlabyrinthe.compilerplugin

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.*

final class FunLabyCompilerPlugin extends StandardPlugin:
  val name: String = "funlabyrinthe"
  val description: String = "Support for FunLabyrinthe APIs"

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    List(new FunLabyPhase())
end FunLabyCompilerPlugin
