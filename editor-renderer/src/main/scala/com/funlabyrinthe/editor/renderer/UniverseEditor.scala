package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.core.*

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5

class UniverseEditor(val universeFile: UniverseFile):
  val universe: Universe = universeFile.universe

  lazy val topElement: Element =
    ui5.TabContainer(
      mapEditorTab,
      ui5.Tab(
        _.text := "Sources",
        p("Pseudo sources"),
      )
    )
  end topElement

  private lazy val mapEditorTab: Element =
    ui5.Tab(
      _.text := "Maps",
      p("These are the maps"),
    )
end UniverseEditor
