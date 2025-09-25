package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.UniverseEnvironment
import com.funlabyrinthe.core.graphics.*

object FakeEnvironment:
  val Instance: UniverseEnvironment =
    new UniverseEnvironment(new FakeGraphicsSystem, new FakeResourceLoader, isEditing = true)
end FakeEnvironment
