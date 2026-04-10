package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.graphics.Color

import indigo.{Fill, FillType}

final case class Material(
  asset: String,
  alpha: Double,
  tint: Color,
  overlay: Fill,
  fillType: FillType,
)
