package com.funlabyrinthe.editor.renderer

import com.raquo.laminar.api.L.{*, given}

object UIComponents:
  def twoColumns(left: Element, right: Element): Element =
    div(
      cls := "funlaby-twocolumns",
      div(
        cls := "funlaby-column",
        left,
      ),
      div(
        cls := "funlaby-column",
        right,
      ),
    )
  end twoColumns
end UIComponents
