package com.funlabyrinthe
package mazes

import core._

class Tool(implicit override val universe: MazeUniverse,
    originalID: ComponentID) extends VisualComponent {

  def this(id: ComponentID)(implicit universe: MazeUniverse) =
    this()(universe, id)

  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Unit @control = ()
}
