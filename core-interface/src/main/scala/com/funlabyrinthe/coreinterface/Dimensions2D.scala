package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait Dimensions2D extends js.Object:
  val width: Double
  val height: Double
end Dimensions2D

object Dimensions2D:
  @inline
  def apply(width: Double, height: Double): Dimensions2D =
    val width0 = width
    val height0 = height
    new Dimensions2D {
      val width = width0
      val height = height0
    }
  end apply
end Dimensions2D
