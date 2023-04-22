package com.funlabyrinthe.editor

import com.funlabyrinthe.core.*

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.Stage

final class UniverseEditorScene(stage: Stage, val universe: Universe) extends Scene:
  thisScene =>

  content = new UniverseEditor(stage)(universe) {
    prefWidth <== thisScene.width
    prefHeight <== thisScene.height
  }
  stylesheets += classOf[UniverseEditorScene].getResource("editor.css").toExternalForm()
  stylesheets += classOf[inspector.jfx.Inspector].getResource("inspector.css").toExternalForm()
end UniverseEditorScene
