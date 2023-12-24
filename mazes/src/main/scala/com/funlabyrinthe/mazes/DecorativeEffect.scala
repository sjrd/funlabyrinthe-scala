package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

/** A generic effect class for decorative effects that do nothing. */
class DecorativeEffect(using ComponentInit) extends Effect:
  category = ComponentCategory("neutrals", "Neutrals")
end DecorativeEffect

object DecorativeEffect:
  def make(name: String, painterItem: Painter.PainterItem)(using ComponentInit): DecorativeEffect =
    val effect = new DecorativeEffect
    effect.name = name
    effect.painter += painterItem
    effect
  end make
end DecorativeEffect
