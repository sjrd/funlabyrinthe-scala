package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core.*

class Sky(using ComponentInit) extends Field {
  painter += "Fields/Sky"

  var message: String = "What are you trying to do? You can't fly."

  override def entering(context: MoveContext) = {
    import context._

    cancel()
    player.showMessage(message)
  }
}
