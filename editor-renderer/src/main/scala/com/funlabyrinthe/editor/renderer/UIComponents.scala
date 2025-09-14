package com.funlabyrinthe.editor.renderer

import com.raquo.laminar.api.L.{*, given}

object UIComponents:
  def twoColumns(left: Modifier[HtmlElement], right: Modifier[HtmlElement]): Element =
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
