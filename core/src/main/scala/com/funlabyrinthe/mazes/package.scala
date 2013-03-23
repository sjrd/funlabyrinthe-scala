package com.funlabyrinthe

import com.funlabyrinthe.core._

import scala.language.implicitConversions

package object mazes {
  implicit def getMazeUniversePlugin(universe: Universe) =
    universe.plugin[MazePlugin]
}
