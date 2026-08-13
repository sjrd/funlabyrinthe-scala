package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Sky(using ComponentInit) extends Field {
  painter += "Fields/Sky"

  var message: String = "Look at that beautiful sky. Too bad you can't fly."

  override def entering(context: EnteringContext): Unit = {
    import context._

    cancel()
    player.showMessageOnce(message)
  }
}
