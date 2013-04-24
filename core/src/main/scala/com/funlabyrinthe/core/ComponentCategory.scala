package com.funlabyrinthe.core

class ComponentCategory private(val id: String, _text: String) {
  def text: String = _text

  override def toString() = s"ComponentCategory($id, $text)"
}

object ComponentCategory {
  def apply(id: String, text: String)(implicit uni: Universe): ComponentCategory =
    uni._categoriesByID.getOrElseUpdate(id, new ComponentCategory(id, text))
}
