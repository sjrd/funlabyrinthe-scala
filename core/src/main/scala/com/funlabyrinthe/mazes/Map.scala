package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

class Map(_dimensions: Dimensions, _fill: Square)(
    override implicit val universe: MazeUniverse) extends ZonedSquareMap {

  type Square = com.funlabyrinthe.mazes.Square

  resize(_dimensions, _fill)
}
