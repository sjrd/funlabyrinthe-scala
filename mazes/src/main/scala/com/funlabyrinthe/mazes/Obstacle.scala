package com.funlabyrinthe
package mazes

import core._

class Obstacle(implicit override val universe: MazeUniverse,
    originalID: ComponentID) extends VisualComponent {

  def this(id: ComponentID)(implicit universe: MazeUniverse) =
    this()(universe, id)

  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Unit @control = ()
}
