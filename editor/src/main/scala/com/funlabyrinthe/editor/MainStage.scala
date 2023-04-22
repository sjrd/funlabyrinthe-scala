package com.funlabyrinthe.editor

import scalafx.application.JFXApp3

final class MainStage extends JFXApp3.PrimaryStage:
  title = "FunLabyrinthe editor"
  width = 1000
  height = 800
  scene = new ProjectSelectorScene(this)
end MainStage
