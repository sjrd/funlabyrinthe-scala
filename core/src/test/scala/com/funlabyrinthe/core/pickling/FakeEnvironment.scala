package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.UniverseEnvironment

object FakeEnvironment:
  val Instance: UniverseEnvironment =
    new UniverseEnvironment(isEditing = true)
end FakeEnvironment
