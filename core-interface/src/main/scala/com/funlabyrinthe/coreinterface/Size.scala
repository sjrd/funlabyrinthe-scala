package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait Size extends js.Object {
  val width: Int
  val height: Int
}

object Size {
  @inline
  def apply(width: Int, height: Int): Size =
    val width0 = width
    val height0 = height
    new Size {
      val width = width0
      val height = height0
    }
  end apply
}
